package com.sandeep.downloader;

public class App {
    public static void main(String[] args) {
        if (args.length < 3) {
            System.out.println("Usage: java -jar downloader.jar <URL> <outputFile> <numThreads>");
            return;
        }

        String fileURL = args[0];
        String outputFile = args[1];
        int numThreads = Integer.parseInt(args[2]);

        DownloadManager manager = new DownloadManager(fileURL, outputFile, numThreads);
        manager.startDownload();
    }
}
