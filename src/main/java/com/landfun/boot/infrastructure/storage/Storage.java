package com.landfun.boot.infrastructure.storage;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * File storage interface.
 */
public interface Storage {

    /**
     * Upload a file.
     *
     * @param inputStream File input stream
     * @param fileName    File name (with path if necessary)
     * @return File access URL or identifier
     */
    String upload(InputStream inputStream, String fileName);

    /**
     * Download a file.
     *
     * @param fileName     File name
     * @param outputStream Output stream to write content to
     */
    void download(String fileName, OutputStream outputStream);

    /**
     * Delete a file.
     *
     * @param fileName File name
     */
    void delete(String fileName);

    /**
     * Check if a file exists.
     *
     * @param fileName File name
     * @return true if exists
     */
    boolean exists(String fileName);

    /**
     * Get file access URL.
     *
     * @param fileName File name
     * @return Access URL
     */
    String getUrl(String fileName);
}
