package com.example.myApp.demos.service;

import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;


public interface FileOptService {

    String uploadFile(MultipartFile file) throws Exception;

    void downloadFile(String fileId, String fileName , HttpServletResponse response) throws Exception;

}
