package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.service.GoogleDriveService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class GoogleDriveServiceImpl implements GoogleDriveService {

    private static final Logger logger = LoggerFactory.getLogger(GoogleDriveServiceImpl.class);
    
    // Use relative path that works on both Windows and Unix systems
    private static final String UPLOAD_DIR = "uploads/prescriptions/";

    @Override
    public String uploadFile(MultipartFile file) throws Exception {
        try {
            // Create upload directory if it doesn't exist (relative to project root)
            Path uploadPath = Paths.get(UPLOAD_DIR);
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
                logger.info("Created upload directory: {}", uploadPath.toAbsolutePath());
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            }
            String uniqueFilename = UUID.randomUUID().toString() + fileExtension;

            // Save file to local storage
            Path filePath = uploadPath.resolve(uniqueFilename);
            Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);
            
            logger.info("File saved successfully to: {}", filePath.toAbsolutePath());

            // Return the file URL that can be accessed via the API endpoint
            return "/api/files/prescriptions/" + uniqueFilename;
        } catch (IOException e) {
            logger.error("Failed to upload file: {}", e.getMessage(), e);
            throw new Exception("Failed to upload file: " + e.getMessage(), e);
        }
    }

    @Override
    public void deleteFile(String fileId) throws Exception {
        try {
            Path filePath = Paths.get(UPLOAD_DIR).resolve(fileId);
            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                logger.info("File deleted successfully: {}", filePath.toAbsolutePath());
            } else {
                logger.info("File not found for deletion: {}", filePath.toAbsolutePath());
            }
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", e.getMessage(), e);
            throw new Exception("Failed to delete file: " + e.getMessage(), e);
        }
    }

    @Override
    public String getFileUrl(String fileId) throws Exception {
        // Return the API URL that can be accessed by the frontend
        return "/api/files/prescriptions/" + fileId;
    }
}
