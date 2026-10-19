package com.qa.api.commonUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public final class FileUtils {
    private FileUtils() { }

    public static String readText(Path file) {
        TestLogger.info("Reading text file: " + file);
        try {
            String text = Files.readString(file, StandardCharsets.UTF_8);
            TestLogger.info("Text file read successfully: " + file);
            return text;
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot read text file: " + file, exception);
        }
    }

    /** Replaces the existing file content. */
    public static void writeText(Path file, String text) {
        TestLogger.info("Writing text file: " + file);
        try {
            createParentFolder(file);
            Files.writeString(file, text, StandardCharsets.UTF_8);
            TestLogger.info("Text file written successfully: " + file);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot write text file: " + file, exception);
        }
    }

    public static void appendText(Path file, String text) {
        TestLogger.info("Appending to text file: " + file);
        try {
            createParentFolder(file);
            Files.writeString(file, text, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE, StandardOpenOption.APPEND);
            TestLogger.info("Text appended successfully: " + file);
        } catch (IOException exception) {
            throw new IllegalStateException("Cannot append text file: " + file, exception);
        }
    }

    static void createParentFolder(Path file) throws IOException {
        if (file.toAbsolutePath().getParent() != null) {
            Files.createDirectories(file.toAbsolutePath().getParent());
        }
    }
}
