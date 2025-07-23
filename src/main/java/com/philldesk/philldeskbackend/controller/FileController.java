package com.philldesk.philldeskbackend.controller;

import com.philldesk.philldeskbackend.security.JwtUtils;
import com.philldesk.philldeskbackend.security.UserDetailsServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@RestController
@RequestMapping("/api/files")
@CrossOrigin(origins = "*")
public class FileController {

    private static final String UPLOAD_DIR = "uploads/prescriptions/";
    
    @Autowired
    private JwtUtils jwtUtils;
    
    @Autowired
    private UserDetailsServiceImpl userDetailsService;

    @GetMapping("/prescriptions/{filename}")
    public ResponseEntity<Resource> downloadFile(
            @PathVariable String filename,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate Bearer token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Validate JWT token
            if (!jwtUtils.validateJwtToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Extract username from token
            String username = jwtUtils.getUserNameFromJwtToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            // Resolve the file path
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());

            if (!resource.exists() || !resource.isReadable()) {
                return ResponseEntity.notFound().build();
            }

            // Determine content type
            String contentType = null;
            try {
                contentType = Files.probeContentType(filePath);
            } catch (IOException ex) {
                // Default content type
                contentType = "application/octet-stream";
            }

            // For images and PDFs, we want to display them inline
            String disposition = "inline";
            if (contentType != null && (
                contentType.startsWith("image/") || 
                contentType.equals("application/pdf")
            )) {
                disposition = "inline";
            } else {
                disposition = "attachment; filename=\"" + resource.getFilename() + "\"";
            }

            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType != null ? contentType : "application/octet-stream"))
                    .header(HttpHeaders.CONTENT_DISPOSITION, disposition)
                    .header(HttpHeaders.CACHE_CONTROL, "max-age=3600") // Cache for 1 hour
                    .header("X-Frame-Options", "SAMEORIGIN") // Allow iframe embedding from same origin
                    .body(resource);

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/prescriptions/{filename}/info")
    public ResponseEntity<FileInfo> getFileInfo(
            @PathVariable String filename,
            @RequestHeader("Authorization") String authHeader) {
        try {
            // Validate Bearer token
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            String token = authHeader.substring(7); // Remove "Bearer " prefix
            
            // Validate JWT token
            if (!jwtUtils.validateJwtToken(token)) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            
            // Extract username from token
            String username = jwtUtils.getUserNameFromJwtToken(token);
            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            
            if (userDetails == null) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
            }
            Path filePath = Paths.get(UPLOAD_DIR).resolve(filename).normalize();
            
            if (!Files.exists(filePath)) {
                return ResponseEntity.notFound().build();
            }

            String contentType = Files.probeContentType(filePath);
            long fileSize = Files.size(filePath);
            
            return ResponseEntity.ok().body(new FileInfo(
                filename,
                contentType,
                fileSize,
                Files.getLastModifiedTime(filePath).toString()
            ));

        } catch (Exception ex) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    // Helper class for file information
    public static class FileInfo {
        private String filename;
        private String contentType;
        private long size;
        private String lastModified;

        public FileInfo(String filename, String contentType, long size, String lastModified) {
            this.filename = filename;
            this.contentType = contentType;
            this.size = size;
            this.lastModified = lastModified;
        }

        // Getters
        public String getFilename() { return filename; }
        public String getContentType() { return contentType; }
        public long getSize() { return size; }
        public String getLastModified() { return lastModified; }

        // Setters
        public void setFilename(String filename) { this.filename = filename; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        public void setSize(long size) { this.size = size; }
        public void setLastModified(String lastModified) { this.lastModified = lastModified; }
    }
}
