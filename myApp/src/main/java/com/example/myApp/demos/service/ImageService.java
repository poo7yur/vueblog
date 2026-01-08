package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.DirDto;
import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.dto.OptDto;
import com.example.myApp.demos.util.DirScannerUtil;
import com.example.myApp.demos.vo.PageImageVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

public interface ImageService {
    DirScannerUtil.Node scanner(String path, HttpServletRequest request) throws IOException;

    PageImageVo listImages(ImageDto dto);

    void uploadImage(MultipartFile file, String destPath) throws IOException;

    void createDir(DirDto dto) throws IOException;

    void deleteDir(DirDto dto) throws IOException;

    String removeImage(OptDto dto, HttpServletRequest request) throws IOException;

}
