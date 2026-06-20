package org.feiesos.storage.backend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.config.StorageProperties;
import org.feiesos.storage.entity.FileNode;
import org.feiesos.storage.dto.StorageObject;
import org.feiesos.storage.mapper.FileNodeMapper;
import org.feiesos.storage.service.AuthzService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Component
public class StorageFacade {

    private static final Logger log = LoggerFactory.getLogger(StorageFacade.class);

    private final AuthzService authzService;
    private final StorageRouter router;
    private final FileNodeMapper fileNodeMapper;
    private final StorageProperties storageProperties;

    public StorageFacade(AuthzService authzService, StorageRouter router,
                         FileNodeMapper fileNodeMapper, StorageProperties storageProperties) {
        this.authzService = authzService;
        this.router = router;
        this.fileNodeMapper = fileNodeMapper;
        this.storageProperties = storageProperties;
    }

    public Long resolvePathToId(String path, Long userId) {
        if (path == null || path.equals("/") || path.isEmpty()) {
            return 0L;
        }
        Long currentId = 0L;
        String[] parts = path.replaceAll("^/|/$", "").split("/");
        for (String part : parts) {
            if (part.isEmpty()) {
                continue;
            }
            FileNode node = fileNodeMapper.selectOne(new LambdaQueryWrapper<FileNode>()
                    .eq(FileNode::getName, part)
                    .eq(FileNode::getParentId, currentId)
                    .eq(FileNode::getIsDir, true)
                    .eq(FileNode::getOwnerId, userId)
                    .eq(FileNode::getIsDeleted, false));
            if (node == null) {
                throw new BusinessException("路径解析失败，找不到目录: " + part);
            }
            currentId = node.getId();
        }
        return currentId;
    }

    public List<FileNode> listByParent(Long parentId, Long userId) {
        authzService.checkPermission(userId, "file:read");
        return fileNodeMapper.selectList(new LambdaQueryWrapper<FileNode>()
                .eq(FileNode::getParentId, parentId)
                .eq(FileNode::getOwnerId, userId)
                .eq(FileNode::getIsDeleted, false)
                .orderByDesc(FileNode::getIsDir)
                .orderByDesc(FileNode::getCreateTime));
    }

    public List<FileNode> browse(String path, Long userId) {
        authzService.checkPermission(userId, "file:read");
        Long targetId = resolvePathToId(path, userId);
        return fileNodeMapper.selectList(new LambdaQueryWrapper<FileNode>()
                .eq(FileNode::getParentId, targetId)
                .eq(FileNode::getOwnerId, userId)
                .eq(FileNode::getIsDeleted, false)
                .orderByDesc(FileNode::getIsDir)
                .orderByDesc(FileNode::getCreateTime));
    }

    public StorageObject download(Long fileId, Long userId) {
        authzService.checkPermission(userId, "file:read");
        FileNode node = fileNodeMapper.selectById(fileId);
        if (node == null || node.getIsDir()) {
            throw new BusinessException("文件不存在或该路径为文件夹");
        }
        if (!userId.equals(node.getOwnerId())) {
            throw new BusinessException(403, "无权访问该文件");
        }
        StorageBackend backend = router.route(node);
        return backend.read(node.getStoragePath());
    }

    @Transactional
    public FileNode upload(String name, Long parentId, Long userId, InputStream data, long size) {
        authzService.checkPermission(userId, "file:write");

        Long count = fileNodeMapper.selectCount(new LambdaQueryWrapper<FileNode>()
                .eq(FileNode::getParentId, parentId)
                .eq(FileNode::getName, name)
                .eq(FileNode::getIsDeleted, false));
        if (count > 0) {
            throw new BusinessException("该目录下已存在同名文件: " + name);
        }

        String storagePath = UUID.randomUUID().toString() + "_" + name;
        StorageBackend backend = router.defaultBackend();
        try {
            backend.write(storagePath, data, size);
        } finally {
            try { data.close(); } catch (IOException ignored) {}
        }

        FileNode node = new FileNode();
        node.setName(name);
        node.setParentId(parentId);
        node.setIsDir(false);
        node.setSize(size);
        node.setStoragePath(storagePath);
        node.setStorageType(backend.type().name());
        node.setOwnerId(userId);
        node.setCreateTime(LocalDateTime.now());
        fileNodeMapper.insert(node);
        return node;
    }

    public void uploadChunk(String md5, int index, Long userId, InputStream data, long size) {
        authzService.checkPermission(userId, "file:write");
        String chunkPath = chunkTempDir(userId) + "/" + md5 + "/" + index;
        try {
            router.defaultBackend().write(chunkPath, data, size);
        } finally {
            try { data.close(); } catch (IOException ignored) {}
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public FileNode mergeChunks(String md5, String fileName, Long parentId, Long userId) {
        authzService.checkPermission(userId, "file:write");

        String tempPrefix = chunkTempDir(userId) + "/" + md5;
        StorageBackend backend = router.defaultBackend();

        if (!backend.exists(tempPrefix)) {
            throw new BusinessException("合并失败：分片目录不存在");
        }

        List<String> chunkPaths = backend.list(tempPrefix);
        if (chunkPaths.isEmpty()) {
            throw new BusinessException("合并失败：未找到任何分片");
        }

        chunkPaths = chunkPaths.stream()
                .sorted(Comparator.comparingInt(p -> {
                    String name = p.substring(p.lastIndexOf('/') + 1);
                    try {
                        return Integer.parseInt(name);
                    } catch (NumberFormatException e) {
                        log.warn("分片名称非数字，跳过: {}", name);
                        return Integer.MAX_VALUE;
                    }
                }))
                .collect(ArrayList::new, ArrayList::add, ArrayList::addAll);

        if (chunkPaths.isEmpty()) {
            throw new BusinessException("合并失败：无有效分片");
        }

        String finalPath = UUID.randomUUID().toString() + "_" + fileName;

        List<InputStream> streams = new ArrayList<>();
        long totalSize = 0;
        try {
            for (String chunkPath : chunkPaths) {
                StorageObject chunk = backend.read(chunkPath);
                streams.add(chunk.getInputStream());
                totalSize += chunk.getSize();
            }
            SequenceInputStream combined = new SequenceInputStream(Collections.enumeration(streams));
            try {
                backend.write(finalPath, combined, totalSize);
            } finally {
                try { combined.close(); } catch (IOException ignored) {}
            }
        } finally {
            for (InputStream is : streams) {
                try { is.close(); } catch (IOException ignored) {}
            }
        }

        backend.deleteDirectory(tempPrefix);

        FileNode node = new FileNode();
        node.setName(fileName);
        node.setParentId(parentId);
        node.setIsDir(false);
        node.setSize(totalSize);
        node.setStoragePath(finalPath);
        node.setStorageType(backend.type().name());
        node.setFileHash(md5);
        node.setOwnerId(userId);
        node.setCreateTime(LocalDateTime.now());
        fileNodeMapper.insert(node);
        return node;
    }

    @Transactional
    public void delete(Long fileId, Long userId) {
        authzService.checkPermission(userId, "file:delete");
        FileNode node = fileNodeMapper.selectById(fileId);
        if (node == null) {
            throw new BusinessException("文件已不存在");
        }
        if (!userId.equals(node.getOwnerId())) {
            throw new BusinessException(403, "无权删除该文件");
        }

        if (node.getIsDir()) {
            deleteChildrenRecursively(node.getId());
        } else {
            StorageBackend backend = router.route(node);
            backend.delete(node.getStoragePath());
        }

        fileNodeMapper.update(null,
                new LambdaUpdateWrapper<FileNode>()
                        .eq(FileNode::getId, node.getId())
                        .set(FileNode::getIsDeleted, true)
                        .set(FileNode::getDeleteTime, LocalDateTime.now()));
    }

    private void deleteChildrenRecursively(Long parentId) {
        List<FileNode> children = fileNodeMapper.selectList(new LambdaQueryWrapper<FileNode>()
                .eq(FileNode::getParentId, parentId)
                .eq(FileNode::getIsDeleted, false));
        for (FileNode child : children) {
            if (child.getIsDir()) {
                deleteChildrenRecursively(child.getId());
            } else {
                StorageBackend backend = router.route(child);
                backend.delete(child.getStoragePath());
            }
            fileNodeMapper.update(null,
                    new LambdaUpdateWrapper<FileNode>()
                            .eq(FileNode::getId, child.getId())
                            .set(FileNode::getIsDeleted, true)
                            .set(FileNode::getDeleteTime, LocalDateTime.now()));
        }
    }

    @Transactional
    public FileNode createDirectory(String name, Long parentId, Long userId) {
        authzService.checkPermission(userId, "file:write");
        Long count = fileNodeMapper.selectCount(new LambdaQueryWrapper<FileNode>()
                .eq(FileNode::getParentId, parentId)
                .eq(FileNode::getName, name)
                .eq(FileNode::getIsDeleted, false));
        if (count > 0) {
            throw new BusinessException("该目录下已存在同名文件或文件夹");
        }

        FileNode node = new FileNode();
        node.setName(name);
        node.setParentId(parentId);
        node.setIsDir(true);
        node.setSize(0L);
        node.setOwnerId(userId);
        node.setCreateTime(LocalDateTime.now());
        fileNodeMapper.insert(node);
        return node;
    }

    private String chunkTempDir(Long userId) {
        String tempDir = storageProperties.getChunk().getTempDir();
        if (tempDir == null || tempDir.isEmpty()) {
            tempDir = "temp";
        }
        if (tempDir.endsWith("/")) {
            tempDir = tempDir.substring(0, tempDir.length() - 1);
        }
        return tempDir + "/" + userId;
    }
}
