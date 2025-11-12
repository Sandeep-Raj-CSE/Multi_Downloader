package com.sandeep.downloader;

import com.sandeep.downloader.model.metaData;
import com.sandeep.downloader.model.Range;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates multi-threaded downloads with resume & retry support.
 */
public class DownloadManager {
    private final String fileURL;
    private final String outputFile;
    private final int numThreads;
    private final int maxRetries = 3; // ✅ new
    private volatile boolean paused = false;
    private volatile boolean stopped = false;

    private metaData metadata;

    public DownloadManager(String fileURL, String outputFile, int numThreads) {
        this.fileURL = fileURL;
        this.outputFile = outputFile;
        this.numThreads = numThreads;
    }

    public void startDownload() {
        try {
            // Load existing metadata if available
            metadata = ResumeManager.loadMetadata(outputFile + ".meta.json");

            if (metadata != null && metadata.completed) {
                System.out.println("✅ File already downloaded!");
                return;
            }

            long fileSize;
            List<Range> ranges;

            if (metadata == null) {
                fileSize = getFileSize(fileURL);
                ranges = RangeCalculator.calculateRanges(fileSize, numThreads);
                metadata = new metaData(fileURL, outputFile, fileSize, ranges);
            } else {
                fileSize = metadata.fileSize;
                ranges = metadata.ranges;
                System.out.println("⏩ Resuming previous download...");
            }

            // Prepare file
            RandomAccessFile output = new RandomAccessFile(outputFile, "rw");
            output.setLength(fileSize);
            output.close();

            ExecutorService pool = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);
            AtomicLong downloadedBytes = new AtomicLong(totalDownloadedSoFar(ranges));

            ProgressReporter reporter = new ProgressReporter(fileSize, downloadedBytes, this);
            Thread reporterThread = new Thread(reporter);
            reporterThread.start();

            // Launch only incomplete segments
            for (Range range : ranges) {
                if (!range.completed) {
                    pool.execute(new SegmentDownloader(fileURL, outputFile, range, latch, downloadedBytes, this, maxRetries));
                } else {
                    latch.countDown();
                }
            }

            latch.await();
            reporter.stop();
            reporterThread.join();
            pool.shutdown();

            if (!stopped) {
                metadata.completed = true;
                ResumeManager.saveMetadata(metadata);
                System.out.println("\n✅ Download completed successfully!");
                ResumeManager.deleteMetadata(outputFile + ".meta.json");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long totalDownloadedSoFar(List<Range> ranges) {
        return ranges.stream().mapToLong(r -> r.downloadedBytes).sum();
    }

    private long getFileSize(String fileURL) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(fileURL).openConnection();
        conn.setRequestProperty("Range", "bytes=0-0");
        conn.connect();

        String contentRange = conn.getHeaderField("Content-Range");
        if (contentRange != null && contentRange.contains("/")) {
            return Long.parseLong(contentRange.split("/")[1]);
        }
        long length = conn.getContentLengthLong();
        conn.disconnect();
        return length;
    }

    // --- Pause/Resume controls ---
    public synchronized void pause() { paused = true; System.out.println("⏸️ Paused."); }
    public synchronized void resume() { paused = false; notifyAll(); System.out.println("▶️ Resumed."); }
    public synchronized void stop() { stopped = true; paused = false; notifyAll(); }

    public synchronized void waitIfPaused() throws InterruptedException {
        while (paused && !stopped) { wait(); }
    }

    public boolean isStopped() { return stopped; }
    public metaData getMetadata() { return metadata; }
}
