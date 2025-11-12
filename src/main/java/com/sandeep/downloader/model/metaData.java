package com.sandeep.downloader.model;

import java.util.List;

public class metaData {

    public String url;
    public String outputFile;
    public long fileSize;
    public List<Range> ranges;
    public boolean completed = false;

    public metaData(String url, String outputFile, long fileSize, List<Range> ranges) {
        this.url = url;
        this.outputFile = outputFile;
        this.fileSize = fileSize;
        this.ranges = ranges;
    }
}
