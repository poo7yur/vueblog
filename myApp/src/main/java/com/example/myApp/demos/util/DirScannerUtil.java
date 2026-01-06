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

    public static Node buildTree(Path dir,String loginUser,Set<String> allUserNames) {
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

    public static List<String> listImages(ImageDto dto) {
        String dirPath = dto.getPath();
        File root = new File(dirPath.trim());
        if (!root.exists() || !root.isDirectory()) {
            throw new IllegalArgumentException("目录不存在：" + dirPath);
        }
        List<String> result = new ArrayList<>();
        walkDir(root, result);
        return result;
    }

    private static void walkDir(File dir, List<String> list) {
        File[] files = dir.listFiles();
        if (files == null) return;
        for (File f : files) {
            if (f.isFile()) {
                list.add(f.getAbsolutePath());
            } else if (f.isDirectory()) {
                walkDir(f, list);
            }
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


