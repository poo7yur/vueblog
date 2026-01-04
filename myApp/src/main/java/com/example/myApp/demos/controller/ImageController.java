package com.example.myApp.demos.controller;

import com.example.myApp.demos.dto.ImageDto;
import com.example.myApp.demos.entity.R;
import com.example.myApp.demos.util.DirScannerUtil;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.util.List;

@RestController
public class ImageController {

    @Value("${file.dir}")
    private String fileDir;

    @GetMapping("/scanner")
    public R<DirScannerUtil.Node> scanner(@RequestParam(required = false) String path) throws IOException {
        if (StringUtils.isEmpty(path)) path = fileDir;
        return R.ok(DirScannerUtil.scannerTree(path));
    }

    @PostMapping("/listImages")
    public R<List<String>> listImages(@RequestBody ImageDto dto) {
        try {
            if (StringUtils.isEmpty(dto.getPath())) throw new IllegalArgumentException("path can't be empty");
            return R.ok(DirScannerUtil.listImages(dto.getPath()));
        } catch (Exception e) {
            return R.fail(e.getMessage());
        }
    }
}
