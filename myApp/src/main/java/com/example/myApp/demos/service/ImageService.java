package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.DirDto;
import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.util.DirScannerUtil;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public interface ImageService {
    DirScannerUtil.Node scanner(String path, HttpServletRequest request) throws IOException;

    List<String> listImages(ImageDto dto);

    void uploadImage(MultipartFile file, String destPath) throws IOException;

    void createDir(DirDto dto) throws IOException;

    void deleteDir(DirDto dto) throws IOException;

}
