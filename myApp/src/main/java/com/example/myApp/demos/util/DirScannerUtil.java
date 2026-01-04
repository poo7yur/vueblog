package com.example.myApp.demos.util;

import java.io.File;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class DirScannerUtil {

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

    public static List<String> listImages(String dirPath) {
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


