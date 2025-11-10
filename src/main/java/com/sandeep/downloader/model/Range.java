package com.sandeep.downloader.model;

public class Range {
    public final long start;
    public final long end;

    public Range(long start, long end) {
        this.start = start;
        this.end = end;
    }

    @Override
    public String toString() {
        return "Range{" + "start=" + start + ", end=" + end + '}';
    }
}
