package org.feiesos.storage.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.feiesos.api.storage.dto.FileResourceDTO;
import org.feiesos.common.exception.BusinessException;
import org.feiesos.storage.entity.FileNode;
import org.feiesos.storage.mapper.FileNodeMapper;
import org.feiesos.storage.service.FileService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.channels.FileChannel;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileServiceImpl implements FileService {
    @Value("${minecloud.upload.path}")
    private String uploadPath;

    @Autowired
    private FileNodeMapper fileNodeMapper;

    private Long resolvePathToId(String path, Long userId) {
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
                    .eq(FileNode::getIsDeleted, false));

            if (node == null) {
                throw new BusinessException("路径解析失败，找不到目录: " + part);
            }
            currentId = node.getId();
        }
        return currentId;
    }

    @Override
    public List<FileNode> listByPath(String path, Long userId) {
        Long targetId = 0L;

        if (path != null && !path.equals("/") && !path.isEmpty()) {
            String[] parts = path.replaceAll("^/|/$", "").split("/");

            for (String part : parts) {
                FileNode node = fileNodeMapper.selectOne(new LambdaQueryWrapper<FileNode>()
                        .eq(FileNode::getName, part)
                        .eq(FileNode::getParentId, targetId)
                        .eq(FileNode::getIsDir, true)
                        .eq(FileNode::getIsDeleted, false));

                if (node == null) {
                    throw new BusinessException("路径不存在: " + part);
                }
                targetId = node.getId();
            }
        }

        return fileNodeMapper.selectList(new LambdaQueryWrapper<FileNode>()
                .eq(FileNode::getParentId, targetId)
                .eq(FileNode::getOwnerId, userId)
                .eq(FileNode::getIsDeleted, false)
                .orderByDesc(FileNode::getIsDir)
                .orderByDesc(FileNode::getCreateTime));
    }

    @Transactional
    @Override
    public FileNode upload(MultipartFile file, String path, Long userId) throws IOException {
        String fileName = file.getOriginalFilename();

        File targetFile = new File(uploadPath + fileName);
        if (!targetFile.getParentFile().exists()) targetFile.getParentFile().mkdirs();
        file.transferTo(targetFile);

        FileNode node = new FileNode();
        node.setName(fileName);
        node.setParentId(resolvePathToId(path, userId));
        node.setIsDir(false);
        node.setSize(file.getSize());
        node.setStoragePath(targetFile.getAbsolutePath());
        node.setOwnerId(userId);
        node.setCreateTime(LocalDateTime.now());

        fileNodeMapper.insert(node);
        return node;
    }

    @Override
    public void uploadChunk(MultipartFile file, String md5, Integer index) throws IOException {
        File storageRoot = new File(uploadPath);
        File tempDir = new File(new File(storageRoot, "temp"), md5);

        log.info("【分片上传】目标目录: {}", tempDir.getAbsolutePath());

        if (!tempDir.exists()) {
            boolean created = tempDir.mkdirs();
            log.info("【分片上传】目录不存在，创建结果: {}", created);
        }

        File chunkFile = new File(tempDir, String.valueOf(index));

        try (InputStream in = file.getInputStream();
             OutputStream out = new FileOutputStream(chunkFile)) {
            byte[] buffer = new byte[1024 * 8];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
        }
        log.info("【分片上传】分片 {} 已保存到: {}", index, chunkFile.getAbsolutePath());
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public FileNode mergeChunks(String md5, String fileName, String path, Long userId) throws IOException {
        Long parentId = resolvePathToId(path, userId);

        File tempDir = new File(new File(uploadPath, "temp"), md5);

        if (!tempDir.exists() || !tempDir.isDirectory()) {
            throw new BusinessException("合并失败：分片目录不存在 -> " + tempDir.getAbsolutePath());
        }

        File[] chunks = tempDir.listFiles();

        if (chunks == null || chunks.length == 0) {
            log.error("目录存在但无法读取分片，路径: {}", tempDir.getAbsolutePath());
            throw new BusinessException("无法读取分片文件，请检查磁盘权限或文件是否被占用");
        }

        Arrays.sort(chunks, Comparator.comparingInt(f -> Integer.parseInt(f.getName())));

        String physicalName = UUID.randomUUID().toString();
        File targetFile = new File(uploadPath, physicalName);

        try (FileChannel destChannel = new FileOutputStream(targetFile).getChannel()) {
            for (File chunk : chunks) {
                try (FileChannel srcChannel = new FileInputStream(chunk).getChannel()) {
                    srcChannel.transferTo(0, srcChannel.size(), destChannel);
                }
                chunk.delete();
            }
        } catch (IOException e) {
            log.error("文件合并 IO 异常", e);
            throw e;
        }

        tempDir.delete();

        FileNode node = new FileNode();
        node.setName(fileName);
        node.setParentId(parentId);
        node.setIsDir(false);
        node.setSize(targetFile.length());
        node.setStoragePath(targetFile.getAbsolutePath());
        node.setFileHash(md5);
        node.setOwnerId(userId);
        node.setIsDeleted(false);
        node.setCreateTime(LocalDateTime.now());

        fileNodeMapper.insert(node);
        return node;
    }

    @Override
    public FileResourceDTO download(Long id, Long userId) throws IOException {
        FileNode node = fileNodeMapper.selectById(id);
        if (node == null || node.getIsDir()) {
            throw new BusinessException("文件不存在或该路径为文件夹");
        }

        if (!userId.equals(node.getOwnerId())) {
            throw new BusinessException(403, "无权访问该文件");
        }

        File file = new File(node.getStoragePath());
        if (!file.exists()) {
            throw new BusinessException("物理文件已丢失");
        }

        Resource resource = new FileSystemResource(file);

        return new FileResourceDTO(resource, node.getName(), file.length());
    }

    @Transactional
    @Override
    public FileNode createDirectory(String name, String path, Long userId) {
        Long parentId = resolvePathToId(path, userId);

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
        node.setIsDeleted(false);
        node.setCreateTime(LocalDateTime.now());

        fileNodeMapper.insert(node);
        return node;
    }

    @Transactional
    @Override
    public void softDelete(Long id, Long userId) {
        FileNode node = fileNodeMapper.selectById(id);
        if (node == null) {
            throw new BusinessException("文件已不存在");
        }

        if (!userId.equals(node.getOwnerId())) {
            throw new BusinessException(403, "无权删除该文件");
        }

        if (node.getIsDir()) {
            // TODO: 递归标记子文件为已删除
        }

        fileNodeMapper.deleteById(node);
        node.setDeleteTime(LocalDateTime.now());
        fileNodeMapper.updateById(node);

        log.info("文件{}已放入回收站", node.getStoragePath());
    }
}
