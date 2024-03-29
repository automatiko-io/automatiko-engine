package io.automatiko.engine.services.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public final class IoUtils {

    public static final Charset UTF8_CHARSET = StandardCharsets.UTF_8;

    public static String readFileAsString(File file) {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(new FileInputStream(file), UTF8_CHARSET))) {
            StringBuilder sb = new StringBuilder();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                sb.append(line).append("\n");
            }
            return sb.toString();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    public static long copy(InputStream input, OutputStream output) throws IOException {
        byte[] buffer = createBytesBuffer(input);
        long count = 0;
        int n = 0;
        while ((n = input.read(buffer)) != -1) {
            output.write(buffer, 0, n);
            count += n;
        }
        return count;
    }

    public static List<String> recursiveListFile(File folder) {
        return recursiveListFile(folder, "", f -> true);
    }

    public static List<String> recursiveListFile(File folder, String prefix, Predicate<File> filter) {
        List<String> files = new ArrayList<>();
        for (File child : safeListFiles(folder)) {
            filesInFolder(files, child, prefix, filter);
        }
        return files;
    }

    private static void filesInFolder(List<String> files, File file, String relativePath, Predicate<File> filter) {
        if (file.isDirectory()) {
            relativePath += file.getName() + "/";
            for (File child : safeListFiles(file)) {
                filesInFolder(files, child, relativePath, filter);
            }
        } else {
            if (filter.test(file)) {
                files.add(relativePath + file.getName());
            }
        }
    }

    private static File[] safeListFiles(final File file) {
        final File[] children = file.listFiles();
        if (children != null) {
            return children;
        } else {
            throw new IllegalArgumentException(
                    String.format("Unable to retrieve contents of directory '%s'.", file.getAbsolutePath()));
        }
    }

    public static byte[] readBytesFromInputStream(InputStream input) throws IOException {
        return readBytesFromInputStream(input, true);
    }

    public static byte[] readBytesFromInputStream(InputStream input, boolean closeInput) throws IOException {
        try {
            byte[] buffer = createBytesBuffer(input);
            try (ByteArrayOutputStream output = new ByteArrayOutputStream(buffer.length)) {
                int n = 0;
                while (-1 != (n = input.read(buffer))) {
                    output.write(buffer, 0, n);
                }
                return output.toByteArray();
            }
        } finally {
            try {
                if (closeInput) {
                    input.close();
                }
            } catch (Exception e) {
                // ignore
            }
        }
    }

    private static byte[] createBytesBuffer(InputStream input) throws IOException {
        return new byte[Math.max(input.available(), 8192)];
    }

    public static byte[] readBytes(File f) throws IOException {
        byte[] buf = new byte[1024];

        try (BufferedInputStream bais = new BufferedInputStream(new FileInputStream(f));
                ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            int len;
            while ((len = bais.read(buf)) > 0) {
                baos.write(buf, 0, len);
            }
            return baos.toByteArray();
        }
    }

    public static void write(File f, byte[] data) throws IOException {
        if (f.exists()) {
            // we want to make sure there is a time difference for lastModified and lastRead
            // checks as Linux and http often round to seconds
            // http://saloon.javaranch.com/cgi-bin/ubb/ultimatebb.cgi?ubb=get_topic&f=1&t=019789
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                throw new RuntimeException("Unable to sleep");
            }
        }

        // Attempt to write the file
        writeBytes(f, data);

        // Now check the file was written and re-attempt if it was not
        // Need to do this for testing, to ensure the texts are read the same way,
        // otherwise sometimes you get tail \n sometimes you don't
        int count = 0;
        while (!areByteArraysEqual(data, readBytes(f)) && count < 5) {
            // The file failed to write, try 5 times, calling GC and sleep between each
            // iteration
            // Sometimes windows takes a while to release a lock on a file
            System.gc();
            try {
                Thread.sleep(250);
            } catch (InterruptedException e) {
                throw new RuntimeException("This should never happen");
            }
            writeBytes(f, data);
            count++;
        }

        // areByteArraysEqual

        if (count == 5) {
            throw new IOException("Unable to write to file:" + f.getCanonicalPath());
        }
    }

    public static void writeBytes(File f, byte[] data) throws IOException {
        byte[] buf = new byte[1024];

        try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(f));
                ByteArrayInputStream bais = new ByteArrayInputStream(data)) {
            int len;
            while ((len = bais.read(buf)) > 0) {
                bos.write(buf, 0, len);
            }
        }
    }

    public static boolean areByteArraysEqual(byte[] b1, byte[] b2) {

        if (b1.length != b2.length) {
            return false;
        }

        for (int i = 0, length = b1.length; i < length; i++) {
            if (b1[i] != b2[i]) {
                return false;
            }
        }

        return true;
    }

    public static String asSystemSpecificPath(String urlPath, int colonIndex) {
        String ic = urlPath.substring(Math.max(0, colonIndex - 2), colonIndex + 1);
        if (ic.matches("\\/[a-zA-Z]:")) {
            return urlPath.substring(colonIndex - 2);
        } else if (ic.matches("[a-zA-Z]:")) {
            return urlPath.substring(colonIndex - 1);
        } else {
            return urlPath.substring(colonIndex + 1);
        }
    }

    public static String valueOf(Object data) {
        if (data == null) {
            return null;
        }

        if (data instanceof InputStream) {
            try {
                return new String(readBytesFromInputStream((InputStream) data), StandardCharsets.UTF_8);
            } catch (IOException e) {
                return null;
            }
        }

        return data.toString();
    }

    private IoUtils() {
        // It is forbidden to create instances of util classes.
    }
}
