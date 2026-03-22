/*
 * Copyright (c) 2026 newjiang. All Rights Reserved.
 */

import java.io.File;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

/**
 * 更新README
 *
 * @author newjiang
 * @since 2026-03-20
 */
public class UpdateReadMe {
    public static void main(String[] args) throws IOException {
        // 1.读取前言
        String filePath = System.getProperty("user.dir");
        StringBuilder builder = new StringBuilder();
        File rootFile = Paths.get(filePath).toFile();
        Path pathReadMe = Paths.get(filePath + "/README.md");
        List<String> lines = Files.readAllLines(pathReadMe);
        for (String line : lines) {
            if (line.startsWith("<!-- INDEX_START -->")) {
                break;
            } else {
                builder.append(line).append(System.lineSeparator());
            }
        }

        // 2.解析章节
        builder.append("<!-- INDEX_START -->").append(System.lineSeparator());
        builder.append("# 索引").append(System.lineSeparator());
        parseChapter(rootFile, 0, null, builder);
        builder.append("<!-- INDEX_END -->").append(System.lineSeparator());

        // 3.覆盖的README.md
        Files.writeString(pathReadMe, builder.toString());
        System.out.println("更新完毕！！");
    }

    private static void parseChapter(File root, int level, String parent, StringBuilder builder) {
        File[] listFiles = Objects.isNull(root) ? null : root.listFiles();
        if (Objects.isNull(listFiles)) {
            return;
        }
        for (File file : listFiles) {
            String fileName = file.getName();
            String parentPathString = parent == null ? "" : parent + "/";
            String fileEncodePath = (parentPathString + fileName).replace(" ", "%2F");
            if (file.isDirectory() && fileName.startsWith("chapter-")) {
                builder.append("  ".repeat(level))
                        .append("* [").append(fileName).append("]")
                        .append("(").append(fileEncodePath).append(")")
                        .append(System.lineSeparator());
                parseChapter(file, level + 1, parentPathString + fileName, builder);
            }
            if (file.isFile() && fileName.endsWith(".md") && !"README.md".equalsIgnoreCase(fileName)) {
                builder.append("  ".repeat(level))
                        .append("* [").append(fileName).append("]")
                        .append("(").append(fileEncodePath).append(")")
                        .append(System.lineSeparator());
            }
        }
    }
}
