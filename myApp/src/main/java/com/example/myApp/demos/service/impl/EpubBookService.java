package com.example.myApp.demos.service.impl;

import cn.hutool.core.util.RandomUtil;
import com.example.myApp.demos.Constants;
import com.example.myApp.demos.dto.BookDto;
import com.example.myApp.demos.dto.ChapterDto;
import com.example.myApp.demos.entity.Essay;
import com.example.myApp.demos.mapper.EssayMapper;
import com.example.myApp.demos.service.BookService;
import com.example.myApp.demos.service.ImageService;
import com.example.myApp.demos.service.UserService;
import com.example.myApp.demos.util.DirUtil;
import com.example.myApp.demos.vo.ChapterDataVo;
import com.example.myApp.demos.vo.EpubBookCacheBO;
import com.example.myApp.demos.vo.PageImageVo;
import lombok.extern.slf4j.Slf4j;
import nl.siegmann.epublib.domain.Book;
import nl.siegmann.epublib.domain.MediaType;
import nl.siegmann.epublib.domain.Resource;
import nl.siegmann.epublib.domain.Resources;
import nl.siegmann.epublib.domain.Spine;
import nl.siegmann.epublib.domain.SpineReference;
import nl.siegmann.epublib.epub.EpubReader;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.Base64;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
public class EpubBookService implements BookService {
    private final EpubReader epubReader = new EpubReader();

    private final static int DEFAULT_PAGE_SIZE = 3;

    private final static String DEFAULT_COVER = "static/cover.jpg";

    // 正则：匹配h1-h6标题标签
    private static final Pattern H_TAG_PATTERN = Pattern.compile("(?i)<h[1-6].*?>(.*?)</h[1-6]>");
    // 正则：匹配img标签（捕获prefix/src/suffix，保留原有属性）
    private static final Pattern IMG_TAG_PATTERN = Pattern.compile("(?i)<img\\s+([^>]*?)src\\s*=\\s*[\"']([^\"']+)[\"']([^\"']*?)>");

    private static final Pattern MEANINGLESS_ID_PATTERN = Pattern.compile(
            "^(x_|_)?(coverpage|front|back|preface|intro|chapter|part|section|page)?\\d*$",
            Pattern.CASE_INSENSITIVE
    );

    @Value("${server.port:8080}")
    private Integer serverPort;

    @Value("${file.dir}")
    private String fileDir;

    @javax.annotation.Resource
    private RedisTemplate<String, Object> redisTemplate;
    @javax.annotation.Resource
    private ImageService imageService;
    @javax.annotation.Resource
    private EssayMapper essayMapper;
    @javax.annotation.Resource
    private UserService userService;

    private static final String CACHE_KEY_PREFIX = "epub:cache:";
    private static final long CACHE_EXPIRE_HOURS = 24;

    private static final String BOOK = "book";

    @Override
    public ChapterDataVo loadChapter(ChapterDto dto) {
        ChapterDataVo chapterDataVo = new ChapterDataVo();
        String fullPath = dto.getBookPath();
        Integer chapterNum = dto.getChapterNum();

        // 基础参数校验（保持不变）
        if (StringUtils.isBlank(fullPath)) {
            throw new RuntimeException("书籍路径不能为空");
        }
        if (chapterNum == null || chapterNum < 1) {
            throw new RuntimeException("章节号必须为大于0的整数");
        }
        File epubFile = new File(fullPath);
        if (!epubFile.exists() || !epubFile.isFile()) {
            throw new RuntimeException("EPUB文件不存在：" + fullPath);
        }
        if (!fullPath.endsWith(".epub") && !fullPath.endsWith(".EPUB")) {
            throw new RuntimeException("文件不是EPUB格式：" + fullPath);
        }

        try {
            // 获取缓存（现在包含已解析的内容）
            String cacheKey = CACHE_KEY_PREFIX + fullPath;
            EpubBookCacheBO bookCache = this.getEpubBookCache(cacheKey, epubFile);

            List<EpubBookCacheBO.ChapterInfo> chapterList = bookCache.getChapterList();
            Map<String, String> contentMap = bookCache.getChapterContentMap();

            int totalChapter = chapterList.size();
            if (totalChapter == 0) {
                throw new RuntimeException("该EPUB文件无有效章节");
            }
            if (chapterNum > totalChapter) {
                throw new RuntimeException("章节号越界，总章节数：" + totalChapter);
            }

            // 获取当前章节
            EpubBookCacheBO.ChapterInfo currentChapter = chapterList.get(chapterNum - 1);
            String currentContent = contentMap.get(currentChapter.getId());
            String currentTitle = currentChapter.getTitle();

            // 获取下一章标题
            String nextTitle = null;
            if (chapterNum < totalChapter) {
                nextTitle = chapterList.get(chapterNum).getTitle();
            }

            // 封装返回
            chapterDataVo.setChapterTitle(currentTitle);
            chapterDataVo.setContent(currentContent);
            chapterDataVo.setTotalChapters(totalChapter);
            chapterDataVo.setNextChapterTitle(nextTitle);

            log.info("章节加载成功：文件={}, 总章节={}, 当前章节={}, 标题={}, 缓存命中={}",
                    fullPath, totalChapter, chapterNum, currentTitle, !bookCache.isNewParse());
            return chapterDataVo;

        } catch (RuntimeException e) {
            log.error("EPUB业务解析失败：{}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("EPUB文件解析异常：文件={}", fullPath, e);
            throw new RuntimeException("EPUB文件损坏或解析失败：" + e.getMessage());
        }
    }

    /**
     *
     * 目录页查询图书列表
     */
    @Override
    public PageImageVo listBooks(BookDto dto) {
        PageImageVo pageImageVo = new PageImageVo();
        String userId = dto.getUserId();

        if (StringUtils.isEmpty(userId)) throw new RuntimeException(Constants.TOKEN_EXPIRED);
        //默认放在C:\Users\Admin\Pictures\save\book\...
        File bookFile = new File(fileDir, BOOK);
        File userFile = new File(bookFile, userId);
        File publicFile = new File(bookFile, "public");
        //先循环userPath下的文件再循环publicPath下的文件(不要文件夹也不遍历子文件夹) 记录全路径在bookUrls
        String endFix =".epub";
        List<String> userUrls = DirUtil.scanBookFile(userFile, true ,endFix);
        List<String> publicUrls = DirUtil.scanBookFile(publicFile, true ,endFix);
        userUrls.addAll(publicUrls);
        int total = userUrls.size();
        //按dto的pageNum=1 pageSize=10 的限制来返回url
        pageImageVo.setTotal(total);
        setPageResult(pageImageVo, userUrls, total, dto.getPageNo(), dto.getPageSize());
        return pageImageVo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String delBook(String name, String userId) {
        if (StringUtils.isAnyEmpty(name, userId))
            throw new RuntimeException(Constants.PARM_NOT_NULL);
        //用户目录根据id删除对应id.epub和id.jpg文件
        String bookStore = fileDir + "/" + BOOK + "/";
        File userSpace = new File(bookStore, userId);
        if (!userSpace.exists() || !userSpace.isDirectory())
            throw new RuntimeException(Constants.FILE_NOT_FOUND);
        try {
            File jpgFile = new File(userSpace, name + ".jpg");
            if (jpgFile.exists()) {
                jpgFile.delete();
            }
            File epubFile = new File(userSpace, name + ".epub");
            if (epubFile.exists()) {
                epubFile.delete();
            }
        } catch (Exception e) {
            throw new RuntimeException(Constants.FAILED);
        }
        //逻辑删除t_essay表
        essayMapper.delEssayByName(name, userId);
        return "删除成功";
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String uploadBook(MultipartFile file, String userId) throws IOException {
        if (file == null) throw new IllegalArgumentException("待上传文件不能为空");
        if (StringUtils.isEmpty(userId)) throw new RuntimeException(Constants.TOKEN_EXPIRED);
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase().endsWith(".epub")) {
            throw new IllegalArgumentException("仅支持上传 EPUB 格式文件");
        }
        if (originalFilename.length() > 50) throw new RuntimeException("书名长度需小于50");
        File bookFile = new File(fileDir, BOOK);
        File userSpace = new File(bookFile, userId);
        File publicFile = new File(bookFile, "public");
        if (!userSpace.exists()) {
            userSpace.mkdirs();
        }
        Essay essay = new Essay();
        String id = RandomUtil.randomNumbers(9);
        essay.setId(id);
        essay.setCreateUser(userId);
        //上传epub文件到用户目录 但当前用户是否是admin角色 是的话设置为公开分享
        String storagePath;
        if (userService.checkAdminRole(userId)) {
            essay.setIsShare(1);
            essay.setIsPublic(1);
            //放到public目录下
            storagePath = storageBook(file, publicFile);
            extractAndSaveCover(storagePath, publicFile, essay, originalFilename);
        } else {
            storagePath = storageBook(file, userSpace);
            //提取epub的封面
            extractAndSaveCover(storagePath, userSpace, essay, originalFilename);
        }
        //写到t_essay表
        essay.setType(2);
        essay.setUpdateTime(new Date());
        essay.setStoragePath(storagePath);
        essayMapper.createEssay(essay);
        return "上传成功";
    }

    private void extractAndSaveCover(String storagePath, File userSpace, Essay essay, String of) throws IOException {
        // 根据全路径 读取epub为book对象
        Book book = epubReader.readEpub(Files.newInputStream(Paths.get(storagePath)));
        // book.title和用户上传的文件名可能不一样
        String title = of.replace(".epub", "");
        // 获取封面图片资源
        Resource coverImage = book.getCoverImage();
        String coverFileName = title + ".jpg";
        // 复制封面到用户目录下
        File coverFile = new File(userSpace, coverFileName);
        if (coverImage != null && coverImage.getData() != null) {
            FileUtils.writeByteArrayToFile(coverFile, coverImage.getData());
        } else {
            copyDefaultCover(coverFile);
        }
        essay.setSummary(book.getTitle());
        essay.setTitle(title);
    }

    private void copyDefaultCover(File coverFile) {
        try {
            ClassPathResource resource = new ClassPathResource("static/cover.jpg");
            FileUtils.copyInputStreamToFile(resource.getInputStream(), coverFile);
        } catch (IOException e) {
            throw new RuntimeException("复制默认封面失败");
        }
    }

    private String storageBook(MultipartFile file, File userSpaceFile) throws IOException {
        String originalFilename = file.getOriginalFilename();
        File destFile = new File(userSpaceFile, originalFilename);
        file.transferTo(destFile);
        return destFile.getAbsolutePath();
    }

    private void setPageResult(PageImageVo pageVo, List<String> allList, int total, int pageNo, int pageSize) {
        // 计算分页起始索引（pageNo从1开始，索引从0开始）
        int startIndex = (pageNo - 1) * pageSize;
        // 分页截取结果：起始索引超过总长度，返回空列表
        List<String> pageList = new ArrayList<>();
        if (startIndex < total && !allList.isEmpty()) {
            // 计算结束索引：避免超出列表长度
            int endIndex = Math.min(startIndex + pageSize, total);
            pageList = allList.subList(startIndex, endIndex);
        }

        // 封装分页结果
        pageVo.setUrls(pageList);
        pageVo.setPageNo(pageNo);
        pageVo.setPageSize(pageSize);
    }

    /**
     * 获取EPUB书籍缓存（优先Redis，未命中则解析，文件修改则刷新缓存）
     */
    private EpubBookCacheBO getEpubBookCache(String cacheKey, File epubFile) throws Exception {
        long fileLastModified = epubFile.lastModified();
        // 从Redis获取缓存
        Object cacheObj = redisTemplate.opsForValue().get(cacheKey);
        if (cacheObj instanceof EpubBookCacheBO) {
            EpubBookCacheBO bookCache = (EpubBookCacheBO) cacheObj;
            // 缓存有效性校验：删除Book判断，仅校验核心数据
            if (bookCache.getFileLastModified() == fileLastModified
                    && bookCache.getChapterList() != null
                    && bookCache.getImageBase64Map() != null) {
                bookCache.setNewParse(false);
                return bookCache;
            }
            // 缓存失效，删除旧缓存
            redisTemplate.delete(cacheKey);
            log.info("EPUB缓存失效：文件已修改/数据为空，删除旧缓存，key={}", cacheKey);
        }
        // 缓存未命中/失效，解析并缓存
        EpubBookCacheBO newBookCache = this.parseEpubBook(epubFile, fileLastModified);
        newBookCache.setNewParse(true);
        // 仅核心数据非空时存入缓存
        if (!newBookCache.getChapterList().isEmpty() && !newBookCache.getImageBase64Map().isEmpty()) {
            redisTemplate.opsForValue().set(cacheKey, newBookCache, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
            log.info("EPUB解析完成，存入Redis缓存：key={}, 过期时间={}小时", cacheKey, CACHE_EXPIRE_HOURS);
        } else {
            log.warn("EPUB解析无有效数据，不存入缓存：file={}", epubFile.getAbsolutePath());
        }
        return newBookCache;
    }

    /**
     * 解析EPUB核心数据（仅提取章节列表+图片Base64，不缓存Book）
     */
    private EpubBookCacheBO parseEpubBook(File epubFile, long fileLastModified) throws Exception {
        EpubBookCacheBO bookCache = new EpubBookCacheBO();

        try (InputStream inputStream = Files.newInputStream(epubFile.toPath())) {
            Book book = epubReader.readEpub(inputStream);
            Spine spine = book.getSpine();
            Resources allResources = book.getResources();

            // 提取图片Base64映射
            Map<String, String> imageBase64Map = extractEpubImageBase64(book);
            bookCache.setImageBase64Map(imageBase64Map);

            // 提取章节列表并解析内容
            List<EpubBookCacheBO.ChapterInfo> chapterList = new ArrayList<>();
            Map<String, String> chapterContentMap = new HashMap<>();

            List<SpineReference> spineReferences = spine.getSpineReferences();

            for (int i = 0; i < spineReferences.size(); i++) {
                SpineReference ref = spineReferences.get(i);
                Resource resource = ref.getResource();

                if (resource == null) continue;

                // 构建章节信息（可序列化）
                EpubBookCacheBO.ChapterInfo chapterInfo = new EpubBookCacheBO.ChapterInfo();
                chapterInfo.setId(resource.getId());
                chapterInfo.setTitle(resource.getTitle());
                chapterInfo.setHref(resource.getHref());
                chapterInfo.setIndex(i + 1);
                chapterInfo.setSize(resource.getSize());
                chapterList.add(chapterInfo);

                // 立即解析章节内容并缓存（避免后续重复IO）
                String content = parseChapterContent(resource, imageBase64Map);
                String title = getChapterTitle(resource, content, i + 1);
                chapterInfo.setTitle(title); // 更新真实标题

                chapterContentMap.put(resource.getId(), content);
            }

            bookCache.setChapterList(chapterList);
            bookCache.setChapterContentMap(chapterContentMap);
            bookCache.setFileLastModified(fileLastModified);
            bookCache.setCacheTime(System.currentTimeMillis());
        }

        return bookCache;
    }

    /**
     * 解析章节内容（含图片Base64替换+标签清理，前端可直接v-html渲染）
     */
    private String parseChapterContent(Resource resource, Map<String, String> imageBase64Map) throws Exception {
        String content = IOUtils.toString(resource.getInputStream(), StandardCharsets.UTF_8);
        if (StringUtils.isBlank(content)) return "本章无内容";

        // 清理多余标签
        content = content.replaceAll("(?i)<html.*?>|</html>|<head.*?>|</head>|<body.*?>|</body>", "");
        content = content.replaceAll("\\s+", " ");
        content = content.replaceAll("(?i)<br\\s*/?>", "<br>");

        // 替换img标签src为Base64
        Matcher imgMatcher = IMG_TAG_PATTERN.matcher(content);
        StringBuffer newContent = new StringBuffer();
        while (imgMatcher.find()) {
            String prefix = imgMatcher.group(1);
            String src = imgMatcher.group(2);
            String suffix = imgMatcher.group(3);
            String base64 = getImageBase64BySrc(src, imageBase64Map);
            if (base64 != null) {
                imgMatcher.appendReplacement(newContent, "<img " + prefix + "src=\"" + base64 + "\" " + suffix + ">");
            } else {
                imgMatcher.appendReplacement(newContent, imgMatcher.group(0));
            }
        }
        imgMatcher.appendTail(newContent);
        return newContent.toString().trim();
    }

    /**
     * 提取EPUB所有图片为Base64（3.1版本适配：迭代器遍历Resources）
     */
    private Map<String, String> extractEpubImageBase64(Book book) throws IOException {
        Map<String, String> imageMap = new HashMap<>();
        if (book == null || book.getResources() == null) return imageMap;

        Resources allResources = book.getResources();
        for (Resource res : allResources.getResourceMap().values()) {
            if (res == null || res.getInputStream() == null || res.getMediaType() == null) continue;

            MediaType mediaType = res.getMediaType();
            if (mediaType.getName().startsWith("image/")) {
                try {
                    byte[] imageBytes = IOUtils.toByteArray(res.getInputStream());
                    String base64Str = Base64.getEncoder().encodeToString(imageBytes);
                    String base64 = "data:" + mediaType.getName() + ";base64," + base64Str;

                    // 多键映射，提高src匹配率
                    if (StringUtils.isNotBlank(res.getId())) {
                        imageMap.put(res.getId(), base64);
                        imageMap.put("#" + res.getId(), base64);
                    }
                    if (StringUtils.isNotBlank(res.getHref())) {
                        imageMap.put(res.getHref(), base64);
                        imageMap.put(res.getHref().substring(res.getHref().lastIndexOf("/") + 1), base64);
                    }
                } catch (Exception e) {
                    log.warn("图片解析失败：资源ID={}", res.getId() == null ? "未知" : res.getId(), e);
                }
            }
        }
        log.info("EPUB图片提取完成，有效数：{}", imageMap.size());
        return imageMap;
    }

    /**
     * 兼容src多种写法，匹配Base64
     */
    private String getImageBase64BySrc(String src, Map<String, String> imageBase64Map) {
        if (StringUtils.isBlank(src) || imageBase64Map.isEmpty()) return null;

        String base64 = imageBase64Map.get(src);
        if (base64 == null && src.startsWith("#")) base64 = imageBase64Map.get(src.substring(1));
        if (base64 == null) base64 = imageBase64Map.get(src.substring(src.lastIndexOf("/") + 1));
        if (base64 == null && src.contains(".")) base64 = imageBase64Map.get(src.substring(0, src.lastIndexOf(".")));

        return base64;
    }

    private boolean isMeaninglessId(String id) {
        if (StringUtils.isBlank(id)) return true;

        // 匹配 x_front001, _chapter1, page01, section_2 等模式
        if (MEANINGLESS_ID_PATTERN.matcher(id).matches()) return true;

        // 匹配纯数字
        if (id.matches("^\\d+$")) return true;

        // 匹配常见的无意义前缀
        String[] meaninglessPrefixes = {"id", "page", "div", "file", "item", "text", "body", "html", "doc"};
        String lowerId = id.toLowerCase();
        for (String prefix : meaninglessPrefixes) {
            if (lowerId.startsWith(prefix) && lowerId.matches("^" + prefix + "\\d*[_-]?\\d*$")) {
                return true;
            }
        }

        return false;
    }

    /**
     * 解析真实章节标题（增强版，过滤无意义ID）
     */
    private String getChapterTitle(Resource resource, String chapterContent, int chapterIndex) {
        // 1. 尝试从Resource获取标题
        String title = StringUtils.trimToNull(resource.getTitle());
        if (title != null && !isMeaninglessId(title)) {
            return cleanTitle(title);
        }

        // 2. 从内容中查找h1-h6标签
        Matcher hMatcher = H_TAG_PATTERN.matcher(chapterContent);
        while (hMatcher.find()) {
            String hTitle = StringUtils.trimToNull(hMatcher.group(1));
            if (hTitle != null) {
                // 清理HTML标签
                hTitle = hTitle.replaceAll("<[^>]+>", "").trim();
                // 过滤无意义内容
                if (!isMeaninglessId(hTitle) && !hTitle.isEmpty() && hTitle.length() < 100) {
                    return cleanTitle(hTitle);
                }
            }
        }

        // 3. 尝试从资源ID获取，但过滤无意义ID
        String id = StringUtils.trimToNull(resource.getId());
        if (id != null && !isMeaninglessId(id)) {
            return cleanTitle(id);
        }

        // 4. 尝试从文件路径/href推断
        String href = StringUtils.trimToNull(resource.getHref());
        if (href != null) {
            // 从路径中提取文件名
            String fileName = href.substring(href.lastIndexOf('/') + 1);
            fileName = fileName.replaceAll("\\.html?$", "").replaceAll("\\.xhtml?$", "");
            if (!isMeaninglessId(fileName)) {
                return cleanTitle(fileName);
            }
        }

        // 5. 返回默认章节名
        return "第" + chapterIndex + "章";
    }

    /**
     * 清理标题（去除多余空格、数字前缀等）
     */
    private String cleanTitle(String title) {
        if (title == null) return "";

        // 去除首尾空白
        title = title.trim();

        // 去除开头的数字编号（如 "1. " 或 "第一章 " 保留，但 "001 " 去除）
        title = title.replaceAll("^(\\d{3,})\\s*", "");

        // 去除HTML实体编码
        title = title.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&amp;", "&")
                .replace("&quot;", "\"");

        // 合并多个空格
        title = title.replaceAll("\\s+", " ");

        // 长度限制
        if (title.length() > 100) {
            title = title.substring(0, 100) + "...";
        }

        return title;
    }

}