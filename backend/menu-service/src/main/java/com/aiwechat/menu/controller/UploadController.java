package com.aiwechat.menu.controller;

import com.aiwechat.common.model.dto.ApiResponse;
import com.aiwechat.common.util.UserContextHelper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/upload")
public class UploadController {

    private static final String UPLOAD_DIR = "uploaded/";
    private static final long MAX_IMAGE_SIZE = 2 * 1024 * 1024;
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;

    private static final Set<String> ALLOWED_IMAGE_TYPES = Set.of(
            "image/jpeg", "image/png", "image/gif", "image/webp");
    private static final Set<String> ALLOWED_FILE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp", ".pdf", ".doc", ".docx");
    private static final Set<String> ALLOWED_IMAGE_EXTENSIONS = Set.of(
            ".jpg", ".jpeg", ".png", ".gif", ".webp");

    @PostMapping("/avatar")
    public ApiResponse<Map<String, String>> uploadAvatar(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        UserContextHelper.requireUserId(httpRequest);

        if (file.isEmpty()) {
            return ApiResponse.error("请选择要上传的图片");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType)) {
            return ApiResponse.error("只能上传 JPG/PNG/GIF/WEBP 格式图片");
        }

        if (file.getSize() > MAX_IMAGE_SIZE) {
            return ApiResponse.error("图片大小不能超过2MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_IMAGE_EXTENSIONS.contains(extension)) {
            return ApiResponse.error("不支持的图片格式");
        }

        try {
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String relativePath = UPLOAD_DIR + "avatars/" + dateDir + "/";
            File uploadDir = new File(relativePath);

            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File targetFile = new File(relativePath + filename);
            file.transferTo(targetFile);

            String fileUrl = "/uploaded/avatars/" + dateDir + "/" + filename;
            log.info("头像上传成功: {}", fileUrl);

            return ApiResponse.success(Map.of("url", fileUrl, "filename", filename));

        } catch (IOException e) {
            log.error("头像上传失败", e);
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }

    @PostMapping("/file")
    public ApiResponse<Map<String, String>> uploadFile(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest httpRequest) {
        UserContextHelper.requireUserId(httpRequest);

        if (file.isEmpty()) {
            return ApiResponse.error("请选择要上传的文件");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            return ApiResponse.error("文件大小不能超过10MB");
        }

        String extension = getFileExtension(file.getOriginalFilename());
        if (!ALLOWED_FILE_EXTENSIONS.contains(extension)) {
            return ApiResponse.error("不支持的文件类型，允许: " + String.join(", ", ALLOWED_FILE_EXTENSIONS));
        }

        try {
            String originalFilename = file.getOriginalFilename();
            String filename = UUID.randomUUID().toString().replace("-", "") + extension;
            String dateDir = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String relativePath = UPLOAD_DIR + "files/" + dateDir + "/";
            File uploadDir = new File(relativePath);

            if (!uploadDir.exists()) {
                uploadDir.mkdirs();
            }

            File targetFile = new File(relativePath + filename);
            file.transferTo(targetFile);

            String fileUrl = "/uploaded/files/" + dateDir + "/" + filename;
            log.info("文件上传成功: {}", fileUrl);

            return ApiResponse.success(Map.of(
                    "url", fileUrl,
                    "filename", filename,
                    "originalName", originalFilename));

        } catch (IOException e) {
            log.error("文件上传失败", e);
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }

    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return ".jpg";
        }
        return filename.substring(filename.lastIndexOf(".")).toLowerCase();
    }
}
