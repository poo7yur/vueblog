package com.example.myApp.demos.util;

import com.example.myApp.demos.dto.ImageDto;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class DirScannerUtil {

    public static Node buildTree(Path dir, String loginUser, Set<String> allUserNames) {
        String dirName = dir.getFileName().toString();

        // 如果是“用户目录”且不是当前登录用户的目录，则直接跳过（不返回该分支）
        boolean isUserDir = allUserNames.contains(dirName);
        if (isUserDir && !dirName.equals(loginUser)) {
            return null;
        }

        // 符合要求，构造节点
        Node node = new Node(dirName, dir.toAbsolutePath().toString());

        // 只对目录做子节点扫描
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                stream.forEach(child -> {
                    Node childNode = buildTree(child, loginUser, allUserNames);
                    if (childNode != null) {
                        if (Files.isDirectory(Paths.get(childNode.path))) {
                            node.children.add(childNode);
                        }
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return node;
    }

    public static Node scannerTree(String path) throws IOException {
        Path root = Paths.get(path);
        return walkTree(root);
    }

    private static Node walkTree(Path dir) {
        Node node = new Node(dir.getFileName().toString(), dir.toAbsolutePath().toString());
        if (Files.isDirectory(dir)) {
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                stream.forEach(path -> {
                    if (Files.isDirectory(path)) {
                        node.children.add(walkTree(path));
                    }
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
        return node;
    }

    /**
     * 分页返回指定目录下所有图片的绝对路径
     *
     * @param dto     包含 path / pageNo / pageSize
     * @param imgUrls 出参：当前页的路径列表
     * @return 总图片数（用于前端计算总页数）
     */
    public static int listImages(ImageDto dto, List<String> imgUrls) {
        String dirPath = dto.getPath();
        File root = new File(dirPath.trim());
        if (!root.exists() || !root.isDirectory()) {
            throw new IllegalArgumentException("目录不存在：" + dirPath);
        }

        int pageNo = dto.getPageNo();
        int pageSize = dto.getPageSize();
        if (pageNo < 1 || pageSize < 1) {
            throw new IllegalArgumentException("分页参数非法");
        }

        int[] total = {0};  // 用数组包装，解决Java基本类型无法在方法内修改的问题
        int start = (pageNo - 1) * pageSize + 1; // 闭区间 [start, end]
        int end = pageNo * pageSize;

        walkDir(root, total, start, end, imgUrls);
        return total[0];
    }

    /**
     *
     * @param total   计数器（数组长度=1，可修改）
     * @param start   当前页起始序号（闭）
     * @param end     当前页结束序号（闭）
     * @param imgUrls 当前页路径列表
     */
    private static void walkDir(File root, int[] total, int start, int end, List<String> imgUrls) {
        //获取目录下所有文件/文件夹（仅当前层级）
        File[] files = root.listFiles();
        if (files == null) return;

        // 遍历所有文件/文件夹，仅统计文件
        for (File file : files) {
            // 只处理文件，跳过文件夹
            if (file.isFile()) {
                total[0]++; // 总数+1
                //判断当前文件是否在分页区间内，若是则收集全路径
                int currentIndex = total[0];
                if (currentIndex >= start && currentIndex <= end) {
                    imgUrls.add(file.getAbsolutePath());
                }
            }
            // 不递归处理子文件夹，直接跳过
        }
    }

    public static class Node {
        public String name;
        public String path;
        public List<Node> children = new ArrayList<>();

        public Node(String name, String path) {
            this.name = name;
            this.path = path;
        }

    }

}


