package com.sandeep.downloader;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Displays live progress bar with speed and ETA in console.
 */
public class ProgressReporter implements Runnable {
    private final long totalBytes;
    private final AtomicLong downloadedBytes;
    private final DownloadManager manager;
    private volatile boolean running = true;

    public ProgressReporter(long totalBytes, AtomicLong downloadedBytes, DownloadManager manager) {
        this.totalBytes = totalBytes;
        this.downloadedBytes = downloadedBytes;
        this.manager = manager;
    }

    public void stop() {
        running = false;
    }

    @Override
    public void run() {
        long lastBytes = 0;
        long lastTime = System.currentTimeMillis();

        while (running && !manager.isStopped()) {
            long downloaded = downloadedBytes.get();
            double percent = (downloaded * 100.0) / totalBytes;

            long now = System.currentTimeMillis();
            double elapsedSec = (now - lastTime) / 1000.0;
            long diff = downloaded - lastBytes;
            double speedKBps = (diff / 1024.0) / (elapsedSec > 0 ? elapsedSec : 1);

            double remainingBytes = totalBytes - downloaded;
            double etaSec = (speedKBps > 0) ? (remainingBytes / 1024.0) / speedKBps : 0;

            // Progress bar
            int barWidth = 50;
            int filled = (int) (percent / 2);
            String bar = "=".repeat(Math.max(0, filled)) + " ".repeat(Math.max(0, barWidth - filled));

            System.out.printf(
                    "\r[%s] %.2f%% | Speed: %.2f KB/s | ETA: %.1f s",
                    bar, percent, speedKBps, etaSec
            );

            lastBytes = downloaded;
            lastTime = now;

            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        System.out.println("\n✅ Download complete!");
    }
}
