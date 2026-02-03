package com.example.myApp.demos.dto;

import lombok.Data;

@Data
public class ChapterDto {

    /**
     * 书籍完整路径（兼容Windows/Linux）
     */
    private String bookPath;
    /**
     * 章节号
     */
    private Integer chapterNum;
}
