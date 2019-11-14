package ru.ivanishkin.optjava.find;

import java.io.*;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionService;
import java.util.regex.Pattern;

class Position {
    private final int patternLen;
    private final long fileLen;
    private final int bufSize;
    private final int increment;
    private long value;
//    private boolean finished = false;

    public Position(final int fileLen, final int patternLen, final int initial, final int bufSize) {
        this.fileLen = fileLen;
        this.patternLen = patternLen;
        this.value = initial;
        this.bufSize = bufSize;
        this.increment = bufSize - (patternLen - 1);
    }

    public long next() {
        if (value + bufSize >= fileLen) {
            return -1;
        }
        if (value + increment + bufSize < fileLen) {
            return value + increment;
        } else {
            return fileLen - bufSize;
        }
    }

    public long getValue() {
        return value;
    }
}

public class FileContentFilter {
    private final Pattern pattern;
    private final int bufferSize;
    private final int needleLen;
    private final CompletionService<Boolean> completionService;
    private char[] buffer;


    public FileContentFilter(final Pattern pattern,
                             final int bufferSize,
                             final int needleLen,
                             final CompletionService<Boolean> completionService) {
        this.pattern = pattern;
        this.bufferSize = bufferSize;
        this.needleLen = needleLen;
        this.completionService = completionService;
        if (bufferSize < 2 * needleLen) {
            throw new IllegalArgumentException("bufferSize < 2 * needleLen");
        }
    }

    public boolean filter(final Path path) {
        final File file = path.toFile();
        if (file.isDirectory()) {
            return false;
        }
        file.length();
//        final FileChannel ccdhannel;
//        MappedByteBuffer mappedBuffer;

// when finished
//        channel.close();
        BufferedReader in;
        buffer = new char[bufferSize];
        int read = 0;
        try {
//            channel = new FileInputStream(file).getChannel();
//            mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
            in = new BufferedReader(new InputStreamReader(new FileInputStream(file)), bufferSize * 2);

            read = in.read(buffer, 0, bufferSize);

            boolean continueSearch = true;
            while (continueSearch) {
                if (read == -1) {
                    continueSearch = false;
                } else if (read <= needleLen - 1) {
                    throw new IllegalStateException();
                }
                String string = new String(buffer);
                if (pattern.matcher(string).find()) {
                    return true;
                }
                System.arraycopy(buffer, bufferSize - (needleLen - 1), buffer, 0, (needleLen - 1));
                read = in.read(buffer, needleLen - 1, bufferSize - (needleLen - 1));
            }
        } catch (IOException e) {
            throw new RuntimeException(file.getName(), e);
        }
        return false;
    }
}
