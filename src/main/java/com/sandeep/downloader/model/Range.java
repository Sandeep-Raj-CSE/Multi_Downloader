package com.sandeep.downloader.model;

/**
 * Represents a byte range for a download segment.
 * Also tracks how many bytes are completed.
 */
public class Range {
    public long start;
    public long end;
    public long downloadedBytes; // ✅ new field for resume
    public boolean completed;

    public Range(long start, long end) {
        this.start = start;
        this.end = end;
        this.downloadedBytes = 0;
        this.completed = false;
    }

    public long remaining() {
        return (end - start + 1) - downloadedBytes;
    }

    @Override
    public String toString() {
        return "Range{" + "start=" + start + ", end=" + end +
                ", downloaded=" + downloadedBytes + ", completed=" + completed + '}';
    }
}
