package com.example.myApp.demos.service;

import com.example.myApp.demos.dto.DirDto;
import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.dto.OptDto;
import com.example.myApp.demos.util.DirUtil;
import com.example.myApp.demos.vo.PageImageVo;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.util.List;

public interface ImageService {
    DirUtil.Node scanner(String path, HttpServletRequest request) throws IOException;

    PageImageVo listImages(ImageDto dto);

    void uploadImage(List<MultipartFile> files, String destPath) throws IOException;

    void createDir(DirDto dto) throws IOException;

    void deleteDir(DirDto dto) throws IOException;

    String removeImage(OptDto dto, HttpServletRequest request) throws IOException;

    String shareImage(OptDto dto ,HttpServletRequest request) throws IOException;

    String likeImage(OptDto dto, HttpServletRequest request) throws IOException;

}
