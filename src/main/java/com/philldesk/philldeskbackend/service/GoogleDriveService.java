package com.philldesk.philldeskbackend.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface GoogleDriveService {
    String uploadFile(MultipartFile file) throws Exception;
    void deleteFile(String fileId) throws Exception;
    String getFileUrl(String fileId) throws Exception;
}
