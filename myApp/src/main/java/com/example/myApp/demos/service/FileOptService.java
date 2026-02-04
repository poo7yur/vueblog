package com.example.myApp.demos.service;

import org.springframework.web.multipart.MultipartFile;
import java.io.InputStream;


public interface FileOptService {

    String uploadAvatarFile(MultipartFile file, String objName) throws Exception;

    InputStream readFullPathFile(String fullPath) throws Exception;

    void uploadHtmlFile(String fullPath, String htmlContent) throws Exception;

}
