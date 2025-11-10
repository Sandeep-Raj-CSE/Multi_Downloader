package com.sandeep.downloader;

import com.sandeep.downloader.model.Range;
import java.io.*;
import java.net.*;
import java.util.concurrent.CountDownLatch;

public class SegmentDownloader implements Runnable {
    private final String fileURL;
    private final String outputFile;
    private final Range range;
    private final CountDownLatch latch;

    public SegmentDownloader(String fileURL, String outputFile, Range range, CountDownLatch latch) {
        this.fileURL = fileURL;
        this.outputFile = outputFile;
        this.range = range;
        this.latch = latch;
    }

    @Override
    public void run() {
        try (RandomAccessFile output = new RandomAccessFile(outputFile, "rw")) {
            HttpURLConnection conn = (HttpURLConnection) new URL(fileURL).openConnection();
            conn.setRequestProperty("Range", "bytes=" + range.start + "-" + range.end);
            conn.connect();

            try (InputStream in = conn.getInputStream()) {
                output.seek(range.start);
                byte[] buffer = new byte[4096];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    output.write(buffer, 0, bytesRead);
                }
            }

            System.out.println(Thread.currentThread().getName() + " finished " + range);
        } catch (Exception e) {
            System.err.println("Error downloading range " + range + ": " + e.getMessage());
        } finally {
            latch.countDown();
        }
    }
}
