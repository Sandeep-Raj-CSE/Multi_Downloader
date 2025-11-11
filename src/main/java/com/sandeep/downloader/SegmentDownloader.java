package com.sandeep.downloader;

import com.sandeep.downloader.model.Range;
import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Handles downloading a specific byte range of the file.
 * Pauses gracefully when DownloadManager.pause() is invoked.
 */
public class SegmentDownloader implements Runnable {
    private final String fileURL;
    private final String outputFile;
    private final Range range;
    private final CountDownLatch latch;
    private final AtomicLong downloadedBytes;
    private final DownloadManager manager;

    public SegmentDownloader(String fileURL, String outputFile, Range range,
                             CountDownLatch latch, AtomicLong downloadedBytes, DownloadManager manager) {
        this.fileURL = fileURL;
        this.outputFile = outputFile;
        this.range = range;
        this.latch = latch;
        this.downloadedBytes = downloadedBytes;
        this.manager = manager;
    }

    @Override
    public void run() {
        try (RandomAccessFile output = new RandomAccessFile(outputFile, "rw")) {
            HttpURLConnection conn = (HttpURLConnection) new URL(fileURL).openConnection();
            conn.setRequestProperty("Range", "bytes=" + range.start + "-" + range.end);
            conn.connect();

            try (InputStream in = conn.getInputStream()) {
                output.seek(range.start);
                byte[] buffer = new byte[8192];
                int bytesRead;

                while ((bytesRead = in.read(buffer)) != -1) {
                    // Check for stop signal
                    if (manager.isStopped()) return;

                    // Check for pause
                    manager.waitIfPaused();

                    output.write(buffer, 0, bytesRead);
                    downloadedBytes.addAndGet(bytesRead);
                }
            }

            System.out.println(Thread.currentThread().getName() + " finished " + range);
        } catch (Exception e) {
            System.err.println("❌ Error downloading " + range + ": " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }
}
