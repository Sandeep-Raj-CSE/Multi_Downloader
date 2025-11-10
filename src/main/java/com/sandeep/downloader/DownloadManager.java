package com.sandeep.downloader;

import com.sandeep.downloader.model.Range;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class DownloadManager {
    private final String fileURL;
    private final String outputFile;
    private final int numThreads;

    public DownloadManager(String fileURL, String outputFile, int numThreads) {
        this.fileURL = fileURL;
        this.outputFile = outputFile;
        this.numThreads = numThreads;
    }

    public void startDownload() {
        try {
            // 1️⃣ Get file size from server
            long fileSize = getFileSize(fileURL);
            System.out.println("Hellooooooooooooo");
            if (fileSize <= 0) {
                System.out.println("Could not determine file size!");
                return;
            }
            System.out.println("File size: " + fileSize + " bytes");

            // 2️⃣ Create output file placeholder
            RandomAccessFile output = new RandomAccessFile(outputFile, "rw");
            output.setLength(fileSize);
            output.close();

            // 3️⃣ Calculate ranges
            List<Range> ranges = RangeCalculator.calculateRanges(fileSize, numThreads);

            // 4️⃣ Create thread pool
            ExecutorService pool = Executors.newFixedThreadPool(numThreads);
            CountDownLatch latch = new CountDownLatch(numThreads);

            for (Range range : ranges) {
                pool.execute(new SegmentDownloader(fileURL, outputFile, range, latch));
            }

            latch.await();
            pool.shutdown();
            System.out.println("Download completed successfully!");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

//    private long getFileSize(String fileURL) throws IOException {
//        HttpURLConnection conn = (HttpURLConnection) new URL(fileURL).openConnection();
//        conn.setRequestMethod("HEAD");
//        //conn.getInputStream().close();
//        return conn.getContentLengthLong();
//    }

    private static long getFileSize(String fileURL) throws IOException {
        try {
            URL url = new URL(fileURL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("HEAD");
            conn.connect();

            int responseCode = conn.getResponseCode();
            if (responseCode != HttpURLConnection.HTTP_OK) {
                throw new IOException("Server returned non-OK status: " + responseCode);
            }

            long fileSize = conn.getContentLengthLong();
            if (fileSize <= 0) {
                throw new IOException("Could not determine file size.");
            }

            return fileSize;

        } catch (Exception e) {
            System.err.println("Error fetching file size: " + e.getMessage());
            e.printStackTrace();
            return -1;
        }
    }

}
