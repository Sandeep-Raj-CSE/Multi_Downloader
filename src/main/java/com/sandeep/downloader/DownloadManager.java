package com.sandeep.downloader;

import com.sandeep.downloader.model.Range;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Coordinates the entire download process and controls pause/resume.
 */
public class DownloadManager {
    private final String fileURL;
    private final String outputFile;
    private final int numThreads;

    private volatile boolean paused = false; // pause flag
    private volatile boolean stopped = false; // quit flag

    private CountDownLatch latch;
    private ProgressReporter reporter;
    private Thread reporterThread;

    public DownloadManager(String fileURL, String outputFile, int numThreads) {
        this.fileURL = fileURL;
        this.outputFile = outputFile;
        this.numThreads = numThreads;
    }

    public void startDownload() {
        try {
            long fileSize = getFileSize(fileURL);
            if (fileSize <= 0) {
                System.out.println("Could not determine file size!");
                return;
            }
            System.out.println("File size: " + fileSize + " bytes");

            RandomAccessFile output = new RandomAccessFile(outputFile, "rw");
            output.setLength(fileSize);
            output.close();

            List<Range> ranges = RangeCalculator.calculateRanges(fileSize, numThreads);

            ExecutorService pool = Executors.newFixedThreadPool(numThreads);
            latch = new CountDownLatch(numThreads);
            AtomicLong downloadedBytes = new AtomicLong(0);

            reporter = new ProgressReporter(fileSize, downloadedBytes, this);
            reporterThread = new Thread(reporter);
            reporterThread.start();

            for (Range range : ranges) {
                pool.execute(new SegmentDownloader(fileURL, outputFile, range, latch, downloadedBytes, this));
            }

            latch.await();
            stopReporter();
            pool.shutdown();

            if (!stopped) {
                System.out.println("\n✅ Download completed successfully!");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
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

    // --- Pause/Resume Controls ---
    public synchronized void pause() {
        paused = true;
        System.out.println("⏸️ Download paused.");
    }

    public synchronized void resume() {
        paused = false;
        notifyAll(); // wake up paused threads
        System.out.println("▶️ Download resumed.");
    }

    public synchronized void stop() {
        stopped = true;
        paused = false;
        notifyAll();
        stopReporter();
    }

    public synchronized void waitIfPaused() throws InterruptedException {
        while (paused && !stopped) {
            wait();
        }
    }

    public boolean isStopped() {
        return stopped;
    }

    private void stopReporter() {
        if (reporter != null) reporter.stop();
        if (reporterThread != null) {
            try {
                reporterThread.join();
            } catch (InterruptedException ignored) {}
        }
    }
}
