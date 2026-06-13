package org.feiesos.storage.service;

import org.feiesos.api.storage.dto.FileResourceDTO;
import org.feiesos.storage.entity.FileNode;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService {

    List<FileNode> listByPath(String path, Long userId);

    FileNode upload(MultipartFile file, String path, Long userId) throws IOException;

    void uploadChunk(MultipartFile file, String md5, Integer index) throws IOException;

    FileNode mergeChunks(String md5, String fileName, String path, Long userId) throws IOException;

    FileResourceDTO download(Long id, Long userId) throws IOException;

    void softDelete(Long id, Long userId);

    FileNode createDirectory(String name, String path, Long userId);
}
