package com.example.myApp.demos.controller;

import com.example.myApp.demos.Constants;
import com.example.myApp.demos.aop.AccessLog;
import com.example.myApp.demos.dto.DirDto;
import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.dto.OptDto;
import com.example.myApp.demos.entity.CommentEntity;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.service.ImageService;
import com.example.myApp.demos.util.DirUtil;
import com.example.myApp.demos.util.JwtUtil;
import com.example.myApp.demos.util.TokenExpiredException;
import com.example.myApp.demos.vo.PageImageVo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@RestController
public class ImageController {

    @Resource
    private ImageService imageService;

    @GetMapping("/scanner")
    public R<DirUtil.Node> scanner(@RequestParam(required = false) String path, HttpServletRequest request) {
        try {
            //解析登录状态
            String loginUser = JwtUtil.parseUser(request);
            String token = request.getHeader("token");
            if (!StringUtils.isEmpty(token) && StringUtils.isEmpty(loginUser))
                throw new TokenExpiredException(401, "token已失效");
            return R.ok(imageService.scanner(path, loginUser));
        } catch (TokenExpiredException e) {
            return R.error(e.getMsg(), e.getCode());
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/listImages")
    public R<PageImageVo> listImages(@RequestBody ImageDto dto) {
        try {
            if (StringUtils.isEmpty(dto.getPath())) throw new IllegalArgumentException(Constants.PATH_NOT_EMPTY);
            return R.ok(imageService.listImages(dto));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/uploadImage")
    public R<String> uploadImage(@RequestParam("file") List<MultipartFile> file, @RequestParam("destPath") String destPath) {
        try {
            imageService.uploadImage(file, destPath);
            return R.ok("upload success");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/removeImage")
    @AccessLog(module = "img manage", description = "delete image")
    public R<String> removeImage(@RequestBody OptDto dto, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(dto.getPath())) throw new IllegalArgumentException(Constants.PATH_NOT_EMPTY);
            return R.ok(imageService.removeImage(dto, request));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/shareImage")
    @AccessLog(module = "img manage", description = "share image")
    public R<String> shareImage(@RequestBody OptDto dto, HttpServletRequest request) {
        try {
            if (StringUtils.isEmpty(dto.getPath())) throw new IllegalArgumentException(Constants.PATH_NOT_EMPTY);
            return R.ok(imageService.shareImage(dto, request));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/likeImage")
    public R<String> likeImage(@RequestBody OptDto dto, HttpServletRequest request) {
        try {
            String msg = imageService.likeImage(dto, request);
            return R.ok(msg);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/commentImage")
    public R<String> commentImage(@RequestBody OptDto dto, HttpServletRequest request) {
        try {
            String msg = imageService.commentImage(dto, request);
            return R.ok(msg);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @GetMapping("/getComment/{id}")
    public R<List<CommentEntity>> getComment(@PathVariable("id") String id) {
        try {
            List<CommentEntity> list = imageService.getComment(id);
            return R.ok(list);
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/createDir")
    public R<String> createDir(@RequestBody DirDto dto) {
        try {
            imageService.createDir(dto);
            return R.ok("create dir success");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }

    @PostMapping("/deleteDir")
    @AccessLog(module = "img manage", description = "remove dir")
    public R<String> deleteDir(@RequestBody DirDto dto) {
        try {
            imageService.deleteDir(dto);
            return R.ok("delete dir success");
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
