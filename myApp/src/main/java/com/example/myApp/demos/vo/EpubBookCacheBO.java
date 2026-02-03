package com.example.myApp.demos.vo;

import lombok.Data;
import java.io.Serializable;
import java.util.List;
import java.util.Map;

@Data
public class EpubBookCacheBO implements Serializable {
    // 图片Base64映射表
    private Map<String, String> imageBase64Map;
    // 章节基本信息列表
    private List<ChapterInfo> chapterList;
    // 章节内容缓存
    private Map<String, String> chapterContentMap;

    private long fileLastModified;
    private long cacheTime;
    private boolean newParse;


    @Data
    public static class ChapterInfo implements Serializable {
        private static final long serialVersionUID = 1L;
        private String id;           // 章节ID
        private String title;        // 章节标题
        private String href;         // 章节链接
        private int index;           // 章节序号
        private long size;            // 原始内容大小
    }

}
