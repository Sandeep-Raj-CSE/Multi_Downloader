package com.sandeep.downloader;

import java.util.Scanner;

/**
 * Entry point for the Multi-threaded File Downloader.
 * Adds safe handling for console input even when run under Gradle.
 */
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

        // Start the download in a background thread
        Thread downloadThread = new Thread(manager::startDownload);
        downloadThread.start();

        System.out.println("\nCommands: [p]ause | [r]esume | [q]uit");

        // Start console listener in separate thread
        Thread consoleThread = new Thread(() -> {
            try (Scanner scanner = new Scanner(System.in)) {
                while (downloadThread.isAlive() && scanner.hasNextLine()) {
                    String input = scanner.nextLine().trim().toLowerCase();
                    switch (input) {
                        case "p" -> manager.pause();
                        case "r" -> manager.resume();
                        case "q" -> {
                            manager.stop();
                            System.out.println("Download aborted by user.");
                            return;
                        }
                        default -> System.out.println("Unknown command. Use p/r/q.");
                    }
                }
            } catch (Exception e) {
                // Handle closed System.in gracefully
                System.out.println("(No console input detected, continuing automatically...)");
            }
        });

        consoleThread.setDaemon(true); // allow JVM to exit after download
        consoleThread.start();

        try {
            downloadThread.join(); // wait for download to finish
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
