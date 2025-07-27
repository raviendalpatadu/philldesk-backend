package com.philldesk.philldeskbackend.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface LocalFileService {
    
    /**
     * Save file to local file system
     * @param file The multipart file to save
     * @param customerId The customer ID for organizing files
     * @return The local file URL/path
     * @throws IOException if file saving fails
     */
    String saveFile(MultipartFile file, Long customerId) throws IOException;
    
    /**
     * Delete file from local file system
     * @param filePath The file path to delete
     * @return true if deletion was successful
     */
    boolean deleteFile(String filePath);
    
    /**
     * Get file from local file system
     * @param filePath The file path
     * @return byte array of file content
     * @throws IOException if file reading fails
     */
    byte[] getFile(String filePath) throws IOException;
    
    /**
     * Check if file exists in local file system
     * @param filePath The file path to check
     * @return true if file exists
     */
    boolean fileExists(String filePath);
}
