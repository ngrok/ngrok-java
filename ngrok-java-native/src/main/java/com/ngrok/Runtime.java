package com.ngrok;

import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;

class Runtime {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Runtime.class);

    private static String getLibname() {
        // TODO better logic here/use lib
        var osname = System.getProperty("os.name").toLowerCase(Locale.ENGLISH);
        if (osname.contains("nix") || osname.contains("nux")) {
            return "libngrok_java.so";
        } else if (osname.contains("win")) {
            return "ngrok_java.dll";
        } else if (osname.contains("mac")) {
            return "libngrok_java.dylib";
        }
        throw new RuntimeException("unknown OS: " + osname);
    }

    public static void load() {
        String filename = getLibname();
        String tempDir = System.getProperty("java.io.tmpdir");
        File temporaryDir = new File(tempDir, "libngrok_" + System.nanoTime());
        temporaryDir.mkdir();
        temporaryDir.deleteOnExit();

        File temp = new File(temporaryDir, filename);
        try (InputStream is = Runtime.class.getResourceAsStream("/" + filename)) {
            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            temp.delete();
            throw new RuntimeException(e);
        } catch (NullPointerException e) {
            temp.delete();
            throw new RuntimeException(e);
        }

        try {
            System.load(temp.getAbsolutePath());
        } finally {
            if (isPosixCompliant()) {
                // Assume POSIX compliant file system, can be deleted after loading
                temp.delete();
            } else {
                // Assume non-POSIX, and don't delete until last file descriptor closed
                temp.deleteOnExit();
            }
        }
    }

    private static boolean isPosixCompliant() {
        try {
            return FileSystems.getDefault()
                    .supportedFileAttributeViews()
                    .contains("posix");
        } catch (FileSystemNotFoundException
                 | ProviderNotFoundException
                 | SecurityException e) {
            return false;
        }
    }

    static native void init(Logger logger);

    static class Logger {
        private static final String format = "[{}] {}";

        public void log(String level, String target, String message) {
            switch (level) {
                case "TRACE" -> logger.trace(format, target, message);
                case "DEBUG" -> logger.debug(format, target, message);
                case "INFO" -> logger.info(format, target, message);
                case "WARN" -> logger.warn(format, target, message);
                case "ERROR" -> logger.error(format, target, message);
                default -> logger.debug("{}: [{}] {}", level, target, message);
            }
        }
    }
}