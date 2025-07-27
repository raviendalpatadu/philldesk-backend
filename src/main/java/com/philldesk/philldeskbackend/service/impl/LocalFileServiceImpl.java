package com.philldesk.philldeskbackend.service.impl;

import com.philldesk.philldeskbackend.service.LocalFileService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;


@Service
public class LocalFileServiceImpl implements LocalFileService {

    private static final Logger logger = LoggerFactory.getLogger(LocalFileServiceImpl.class);

    private String uploadDirectory = "uploads/prescriptions"; // Default upload directory

    @Override
    public String saveFile(MultipartFile file, Long customerId) throws IOException {
        try {
            logger.info("Saving prescription file locally: {} for customer: {}", file.getOriginalFilename(), customerId);
            // Create upload directory if it doesn't exist (relative to project root)
            Path uploadPath = Paths.get(uploadDirectory);
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
            logger.error("Failed to save prescription file locally: {}", e.getMessage(), e);
            throw new IOException("Failed to save prescription file to local storage: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean deleteFile(String filePath) {
        try {
            // Extract relative path from URL if it's a full URL
            String relativePath = extractRelativePathFromUrl(filePath);
            Path fileToDelete = Paths.get(relativePath);
            
            if (Files.exists(fileToDelete)) {
                Files.delete(fileToDelete);
                logger.info("File deleted successfully: {}", fileToDelete);
                return true;
            } else {
                logger.warn("File not found for deletion: {}", fileToDelete);
                return false;
            }
        } catch (IOException e) {
            logger.error("Failed to delete file: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public byte[] getFile(String filePath) throws IOException {
        try {
            // Extract relative path from URL if it's a full URL
            String relativePath = extractRelativePathFromUrl(filePath);
            Path file = Paths.get(relativePath);
            
            if (Files.exists(file)) {
                return Files.readAllBytes(file);
            } else {
                throw new IOException("File not found: " + file.toString());
            }
        } catch (IOException e) {
            logger.error("Failed to read file: {}", e.getMessage(), e);
            throw new IOException("Failed to read file from local storage: " + e.getMessage(), e);
        }
    }

    @Override
    public boolean fileExists(String filePath) {
        try {
            // Extract relative path from URL if it's a full URL
            String relativePath = extractRelativePathFromUrl(filePath);
            Path file = Paths.get(relativePath);
            return Files.exists(file);
        } catch (Exception e) {
            logger.error("Error checking file existence: {}", e.getMessage(), e);
            return false;
        }
    }
    
    /**
     * Generate a unique filename with customer-specific numbering for prescriptions
     */
    private String generateUniqueFilenameWithNumbering(Path uploadPath, String originalFilename, String extension, Long customerId) {
        // Get current date for numbering
        String currentDate = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        
        // Find the next available number for this customer and date
        int fileNumber = 1;
        String proposedFilename;
        Path proposedFilePath;
        
        do {
            // Format: prescription_customerID_YYYYMMDD_001.pdf
            proposedFilename = String.format("prescription_%d_%s_%03d.%s", 
                customerId, currentDate, fileNumber, extension);
            proposedFilePath = uploadPath.resolve(proposedFilename);
            fileNumber++;
        } while (Files.exists(proposedFilePath));
        
        logger.info("Generated unique prescription filename: {}", proposedFilename);
        return proposedFilename;
    }
    
    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf(".") + 1).toLowerCase();
    }
    
    /**
     * Extract relative path from full URL
     */
    private String extractRelativePathFromUrl(String urlOrPath) {
        if (urlOrPath.startsWith("http")) {
            // Extract path from URL
            String[] parts = urlOrPath.split("/api/files/");
            if (parts.length > 1) {
                return parts[1];
            }
        }
        return urlOrPath;
    }
}
