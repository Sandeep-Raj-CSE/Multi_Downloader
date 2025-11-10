package com.sandeep.downloader;

import com.sandeep.downloader.model.Range;
import java.util.*;

public class RangeCalculator {
    public static List<Range> calculateRanges(long fileSize, int numThreads) {
        List<Range> list = new ArrayList<>();
        long chunkSize = fileSize / numThreads;
        long start = 0;

        for (int i = 0; i < numThreads; i++) {
            long end = (i == numThreads - 1) ? fileSize - 1 : start + chunkSize - 1;
            list.add(new Range(start, end));
            start = end + 1;
        }
        return list;
    }
}
