package com.example.myApp.demos.vo;

import lombok.Data;

@Data
public class ChapterDataVo {

    /**
     * 当前章节标题
     */
    private String chapterTitle;
    /**
     * 当前章节内容
     */
    private String content;
    /**
     * 书籍总章节数
     */
    private Integer totalChapters;
    /**
     * 下一章标题（无则为null）
     */
    private String nextChapterTitle;
}
