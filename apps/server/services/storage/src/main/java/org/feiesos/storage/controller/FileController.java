package org.feiesos.storage.controller;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.feiesos.api.storage.dto.FileResourceDTO;
import org.feiesos.common.result.R;
import org.feiesos.storage.entity.FileNode;
import org.feiesos.storage.mapper.FileNodeMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.feiesos.storage.service.FileService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileNodeMapper fileNodeMapper;
    private final FileService fileService;

    public FileController(FileNodeMapper fileNodeMapper, FileService fileService) {
        this.fileNodeMapper = fileNodeMapper;
        this.fileService = fileService;
    }

    @GetMapping("/list")
    public R<List<FileNode>> list(@RequestParam(defaultValue = "0") Long parentId,
                                   HttpServletRequest request) {
        Long userId = getUserId(request);
        List<FileNode> list = fileNodeMapper.selectList(
                new LambdaQueryWrapper<FileNode>()
                        .eq(FileNode::getParentId, parentId)
                        .eq(FileNode::getOwnerId, userId)
                        .eq(FileNode::getIsDeleted, false)
                        .orderByDesc(FileNode::getIsDir)
                        .orderByDesc(FileNode::getCreateTime)
        );
        return R.ok(list);
    }

    @GetMapping("/browse")
    public R<List<FileNode>> browse(@RequestParam(value = "path", defaultValue = "/") String path,
                                     HttpServletRequest request) {
        try {
            if (!path.startsWith("/")) {
                path = "/" + path;
            }
            Long userId = getUserId(request);
            List<FileNode> list = fileService.listByPath(path, userId);
            return R.ok(list);
        } catch (Exception e) {
            log.warn("浏览目录失败: {}", e.getMessage());
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/upload")
    public R<FileNode> upload(@RequestParam("file") MultipartFile file,
                               @RequestParam(value = "path", defaultValue = "/") String path,
                               HttpServletRequest request) throws IOException {
        if (file.isEmpty()) {
            return R.fail("上传失败：文件内容不能为空");
        }

        try {
            Long userId = getUserId(request);
            log.info("开始接收上传文件: {}, 大小: {} bytes", file.getOriginalFilename(), file.getSize());
            FileNode node = fileService.upload(file, path, userId);
            return R.ok(node);
        } catch (IOException e) {
            log.error("文件存储发生 I/O 异常: ", e);
            return R.fail("服务器磁盘写入异常，请检查存储路径权限");
        } catch (Exception e) {
            log.error("文件上传系统异常: ", e);
            return R.fail("系统繁忙: " + e.getMessage());
        }
    }

    @PostMapping("/chunk")
    public R<String> uploadChunk(@RequestParam("file") MultipartFile file,
                                  @RequestParam("md5") String md5,
                                  @RequestParam("index") Integer index) throws IOException {
        fileService.uploadChunk(file, md5, index);
        return R.ok("分片 " + index + " 上传成功");
    }

    @PostMapping("/merge")
    public R<FileNode> merge(@RequestParam("md5") String md5,
                              @RequestParam("fileName") String fileName,
                              @RequestParam("path") String path,
                              HttpServletRequest request) throws IOException {
        Long userId = getUserId(request);
        return R.ok(fileService.mergeChunks(md5, fileName, path, userId));
    }

    @PostMapping("/mkdir")
    public R<FileNode> createDirectory(@RequestParam(value = "name") String name,
                                        @RequestParam(value = "path", defaultValue = "/") String path,
                                        HttpServletRequest request) {
        if (name == null || name.trim().isEmpty()) {
            return R.fail("文件夹名称不能为空");
        }

        try {
            Long userId = getUserId(request);
            FileNode node = fileService.createDirectory(name, path, userId);
            return R.ok(node);
        } catch (Exception e) {
            log.error("新建文件夹失败", e);
            return R.fail("新建文件夹失败：" + e.getMessage());
        }
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<Resource> download(@PathVariable("id") Long id,
                                              HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            FileResourceDTO fileResource = fileService.download(id, userId);

            String contentDisposition = "attachment; filename=\"" +
                    java.net.URLEncoder.encode(fileResource.getFileName(), "UTF-8") + "\"";

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, contentDisposition)
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileResource.getFileSize())
                    .body(fileResource.getResource());

        } catch (Exception e) {
            log.error("下载文件失败, id: {}", id, e);
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    public R<String> delete(@PathVariable("id") Long id,
                             HttpServletRequest request) {
        try {
            Long userId = getUserId(request);
            fileService.softDelete(id, userId);
            return R.ok("已移入回收站");
        } catch (Exception e) {
            log.error("删除文件异常, id: {}", id, e);
            return R.fail("删除失败: " + e.getMessage());
        }
    }

    private Long getUserId(HttpServletRequest request) {
        Object userId = request.getAttribute("currentUserId");
        if (userId == null) {
            throw new RuntimeException("未获取到用户信息");
        }
        return (Long) userId;
    }
}
