package com.sandeep.downloader;

import com.sandeep.downloader.model.Range;
import com.sandeep.downloader.model.metaData;

import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Downloads a specific byte range with retry and resume support.
 */
public class SegmentDownloader implements Runnable {
    private final String fileURL;
    private final String outputFile;
    private final Range range;
    private final CountDownLatch latch;
    private final AtomicLong downloadedBytes;
    private final DownloadManager manager;
    private final int maxRetries;

    public SegmentDownloader(String fileURL, String outputFile, Range range,
                             CountDownLatch latch, AtomicLong downloadedBytes,
                             DownloadManager manager, int maxRetries) {
        this.fileURL = fileURL;
        this.outputFile = outputFile;
        this.range = range;
        this.latch = latch;
        this.downloadedBytes = downloadedBytes;
        this.manager = manager;
        this.maxRetries = maxRetries;
    }

    @Override
    public void run() {
        int attempt = 0;
        while (attempt < maxRetries && !manager.isStopped() && !range.completed) {
            attempt++;
            try (RandomAccessFile output = new RandomAccessFile(outputFile, "rw")) {
                long startByte = range.start + range.downloadedBytes;
                HttpURLConnection conn = (HttpURLConnection) new URL(fileURL).openConnection();
                conn.setRequestProperty("Range", "bytes=" + startByte + "-" + range.end);
                conn.connect();

                if (conn.getResponseCode() != 206 && conn.getResponseCode() != 200)
                    throw new IOException("Invalid response: " + conn.getResponseCode());

                try (InputStream in = conn.getInputStream()) {
                    output.seek(startByte);
                    byte[] buffer = new byte[8192];
                    int bytesRead;

                    while ((bytesRead = in.read(buffer)) != -1) {
                        if (manager.isStopped()) return;
                        manager.waitIfPaused();

                        output.write(buffer, 0, bytesRead);
                        range.downloadedBytes += bytesRead;
                        downloadedBytes.addAndGet(bytesRead);
                    }

                    range.completed = true;
                    System.out.println(Thread.currentThread().getName() + " finished " + range);
                    ResumeManager.saveMetadata(manager.getMetadata());
                    break; // ✅ success, break retry loop
                }

            } catch (Exception e) {
                System.err.println("⚠️ Attempt " + attempt + " failed for " + range + ": " + e.getMessage());
                try { Thread.sleep(1000 * attempt); } catch (InterruptedException ignored) {}
            }
        }
        latch.countDown();
    }
}
