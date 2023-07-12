package com.ngrok;

import org.slf4j.LoggerFactory;
import org.slf4j.event.Level;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Locale;

/**
 * A class representing the runtime environment for the ngrok Java client.
 */
class Runtime {

    private static final Logger LOGGER = new Logger(); 

    public static Logger getLogger() {
        return LOGGER;
    }

    /**
     * Returns the name of the native library to load based on the current operating system.
     *
     * @return the name of the native library to load
     * @throws RuntimeException if the operating system is unknown
     */
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

    /**
     * Loads the native library for the ngrok Java client.
     *
     * @throws RuntimeException if an I/O error occurs or the native library fails to load
     */
    public static void load() {
        String filename = getLibname();
        String tempDir = System.getProperty("java.io.tmpdir");
        File temporaryDir = new File(tempDir, "libngrok_" + System.nanoTime());
        temporaryDir.mkdir();
        temporaryDir.deleteOnExit();

        File temp = new File(temporaryDir, filename);
        try (InputStream is = Runtime.class.getResourceAsStream("/" + filename)) {
            Files.copy(is, temp.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException | NullPointerException e) {
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

    /**
     * Returns whether the current file system is POSIX compliant.
     *
     * @return true if the current file system is POSIX compliant, false otherwise
     */
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

    /**
     * Initializes the logger for the native library.
     *
     * @param logger the logger to be initialized for the native library
     */
    static native void init(Logger logger);

    /**
     * A class representing a logger for the runtime environment.
     */
    static class Logger {
        private static final org.slf4j.Logger logger = LoggerFactory.getLogger(Runtime.class);
        private static final String format = "[{}] {}";

        private Logger() { }

        public String getLevel() {
            Level logLevel = Level.INFO;

            Level[] levels = new Level[] {Level.ERROR, Level.WARN, Level.INFO, Level.DEBUG, Level.TRACE};
            for (Level level : levels) {
                if (logger.isEnabledForLevel(level)) {
                    logLevel = level;
                }
            }

            return logLevel.toString();
        }

        public void log(String level, String target, String message) {
            Level lvl = Level.valueOf(level.toUpperCase()); 
            logger.atLevel(lvl).log(format, target, message);
        }
    }
}