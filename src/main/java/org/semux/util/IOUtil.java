/**
 * Copyright (c) 2017 The Semux Developers
 *
 * Distributed under the MIT software license, see the accompanying file
 * LICENSE or https://opensource.org/licenses/mit-license.php
 */
package org.semux.util;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

public class IOUtil {

    private IOUtil() {
    }

    /**
     * Reads stream into byte array, and close it afterwards.
     * 
     * @param in
     * @return
     * @throws IOException
     */
    public static byte[] readStream(InputStream in) throws IOException {
        ByteArrayOutputStream buf = new ByteArrayOutputStream();

        BufferedInputStream bin = new BufferedInputStream(in);
        for (int c; (c = bin.read()) != -1;) {
            buf.write(c);
        }
        bin.close();

        return buf.toByteArray();
    }

    /**
     * Reads stream a string, and close it afterwards.
     * 
     * @param in
     * @return
     * @throws IOException
     */
    public static String readStreamAsString(InputStream in) throws IOException {
        return Bytes.toString(readStream(in));
    }

    /**
     * Reads file as a byte array.
     * 
     * @param file
     *            The file to read
     * @return a byte array; empty array if the file does not exist
     * @throws IOException
     */
    public static byte[] readFile(File file) throws IOException {
        if (file.exists()) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            try (InputStream in = new BufferedInputStream(new FileInputStream(file))) {
                for (int c; (c = in.read()) != -1;) {
                    out.write(c);
                }
            }

            return out.toByteArray();
        } else {
            return Bytes.EMPTY_BYTES;
        }
    }

    /**
     * Reads file as a string.
     * 
     * @param file
     *            File to read
     * @return The file content as string; empty if the file does not exist
     * @throws IOException
     */
    public static String readFileAsString(File file) throws IOException {
        return Bytes.toString(readFile(file));
    }

    /**
     * Writes a byte array into a file.
     * 
     * @param bytes
     *            A byte array
     * @param file
     *            Destination file
     * @throws IOException
     */
    public static void writeToFile(byte[] bytes, File file) throws IOException {
        try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream(file))) {
            out.write(bytes);
        }
    }

    /**
     * Writes a {@link String} into a file.
     * 
     * @param str
     *            String contents
     * @param file
     *            Destination file
     * @throws IOException
     */
    public static void writeToFile(String str, File file) throws IOException {
        writeToFile(Bytes.of(str), file);
    }

    /**
     * Reads file by lines.
     * 
     * @param file
     *            The file to read
     * @return A list of lines in the file, or empty if the file doesn't exist
     * @throws IOException
     */
    public static List<String> readFileAsLines(File file) throws IOException {
        List<String> lines = new ArrayList<>();

        if (file.isFile()) {
            try (BufferedReader in = new BufferedReader(new FileReader(file))) {
                for (String line; (line = in.readLine()) != null;) {
                    lines.add(line);
                }
            }
        }

        return lines;
    }

    /**
     * Copy a file to a new location.
     * 
     * @param src
     *            Source file
     * @param dst
     *            Destination file
     * @param replaceExisting
     *            Whether to replace existing file
     * @throws IOException
     */
    public static void copyFile(File src, File dst, boolean replaceExisting) throws IOException {
        if (replaceExisting || !dst.exists()) {
            Files.copy(src.toPath(), dst.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }
    }
}
