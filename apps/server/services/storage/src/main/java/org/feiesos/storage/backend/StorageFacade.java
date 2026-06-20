package org.feiesos.storage.backend;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.entity.FileNode;
import org.feiesos.storage.dto.StorageObject;
import org.feiesos.storage.mapper.FileNodeMapper;
import org.feiesos.storage.service.AuthzService;
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
import java.util.stream.Collectors;

@Component
public class StorageFacade {

    private final AuthzService authzService;
    private final StorageRouter router;
    private final FileNodeMapper fileNodeMapper;

    public StorageFacade(AuthzService authzService, StorageRouter router, FileNodeMapper fileNodeMapper) {
        this.authzService = authzService;
        this.router = router;
        this.fileNodeMapper = fileNodeMapper;
    }

    public Long resolvePathToId(String path, Long userId) {
        if (path == null || path.equals("/") || path.isEmpty()) {
            return 0L;
        }
        Long currentId = 0L;
        String[] parts = path.replaceAll("^/|/$", "").split("/");
        for (String part : parts) {
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
        return fileNodeMapper.selectList(new LambdaQueryWrapper<FileNode>()
                .eq(FileNode::getParentId, parentId)
                .eq(FileNode::getOwnerId, userId)
                .eq(FileNode::getIsDeleted, false)
                .orderByDesc(FileNode::getIsDir)
                .orderByDesc(FileNode::getCreateTime));
    }

    public List<FileNode> browse(String path, Long userId) {
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
        ensureAncestorsAccessible(node, userId);
        StorageBackend backend = router.route(node);
        return backend.read(node.getStoragePath());
    }

    @Transactional
    public FileNode upload(String name, Long parentId, Long userId, InputStream data, long size) {
        authzService.checkPermission(userId, "file:write");
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

    public void uploadChunk(String md5, int index, InputStream data, long size) {
        String chunkPath = "temp/" + md5 + "/" + index;
        try {
            router.defaultBackend().write(chunkPath, data, size);
        } finally {
            try { data.close(); } catch (IOException ignored) {}
        }
    }

    @Transactional(rollbackFor = Exception.class)
    public FileNode mergeChunks(String md5, String fileName, Long parentId, Long userId) {
        authzService.checkPermission(userId, "file:write");

        String tempPrefix = "temp/" + md5;
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
                    return Integer.parseInt(name);
                }))
                .collect(Collectors.toList());

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
                try {
                    is.close();
                } catch (IOException ignored) {
                }
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
    public FileNode createDirectory(String name, Long parentId, Long userId) {
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

    private void ensureAncestorsAccessible(FileNode node, Long userId) {
        Long parentId = node.getParentId();
        while (parentId != null && parentId != 0L) {
            FileNode parent = fileNodeMapper.selectByIdIncludingDeleted(parentId);
            if (parent == null || Boolean.TRUE.equals(parent.getIsDeleted())) {
                throw new BusinessException("文件不存在");
            }
            if (!userId.equals(parent.getOwnerId())) {
                throw new BusinessException(403, "无权访问该文件");
            }
            parentId = parent.getParentId();
        }
    }
}
