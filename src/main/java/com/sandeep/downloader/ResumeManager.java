package com.sandeep.downloader;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.sandeep.downloader.model.metaData;

import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Handles saving and loading of download progress metadata (.json file)
 */
public class ResumeManager {
    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static void saveMetadata(metaData metadata) {
        try (FileWriter writer = new FileWriter(metadata.outputFile + ".meta.json")) {
            gson.toJson(metadata, writer);
        } catch (IOException e) {
            System.err.println("⚠️ Failed to save metadata: " + e.getMessage());
        }
    }

    public static metaData loadMetadata(String metaPath) {
        try (FileReader reader = new FileReader(metaPath)) {
            return gson.fromJson(reader, metaData.class);
        } catch (IOException e) {
            return null; // no metadata found or invalid
        }
    }

    public static void deleteMetadata(String metaPath) {
        java.io.File f = new java.io.File(metaPath);
        if (f.exists()) f.delete();
    }
}
