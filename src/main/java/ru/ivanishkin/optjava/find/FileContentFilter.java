package ru.ivanishkin.optjava.find;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class Position {
    private final int patternLen;
    private final long fileLen;
    private final int bufSize;
    private final int increment;
    private long value;
//    private boolean finished = false;

    public Position(final long fileLen, final int patternLen, final int initial, final int bufSize) {
        this.fileLen = fileLen;
        this.patternLen = patternLen;
        this.value = initial;
        this.bufSize = bufSize;
        this.increment = bufSize - (patternLen - 10);
    }

    public long next() {
        if (value + bufSize >= fileLen) {
            if (value > 0) {
                return -1;
            } else {
                value++;
                return 0;
            }
        }
        if (value + increment + bufSize < fileLen) {
            return value += increment;
        } else {
            System.out.println("value = " + value);
            System.out.println("fileLen = " + fileLen);
            System.out.println("bufSize = " + bufSize);
            System.out.println("increment = " + increment);
            value = fileLen - bufSize;
            return value;
        }
    }

    public long getValue() {
        return value;
    }
}

public class FileContentFilter {
    private final Pattern pattern;
    private final int bufferSize;
    private final int patternLen;
    private final ForkJoinPool forkJoinPool;
    private char[] buffer;


    public FileContentFilter(final Pattern pattern,
                             final int bufferSize,
                             final int patternLen,
                             final ForkJoinPool forkJoinPool) {
        this.pattern = pattern;
        this.bufferSize = bufferSize;
        this.patternLen = patternLen;
        this.forkJoinPool = forkJoinPool;
        if (bufferSize < 2 * patternLen) {
            throw new IllegalArgumentException("bufferSize < 2 * needleLen");
        }
    }

    public boolean filter(final Path path) {
        final File file = path.toFile();
        if (file.isDirectory()) {
            return false;
        }
        long fileLen = file.length();

// when finished
//        channel.close();
        MappedByteBuffer mappedBuffer;
        byte[] buf;
        BufferedReader in;
        buffer = new char[bufferSize];
        int read = 0;
        long from = 0;
        int availableSize = bufferSize <= fileLen - from ? bufferSize : (int) (fileLen - from);
        try (FileChannel channel = new RandomAccessFile(file, "r").getChannel()) {
            mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, from, availableSize);
            buf = new byte[availableSize];
            int runningTasks = 0;
            CompletionService<Boolean> completionService = new ExecutorCompletionService<>(forkJoinPool);
            Position pos = new Position(fileLen, patternLen, 0, bufferSize);

//            in = new BufferedReader(new InputStreamReader(new FileInputStream(file)), bufferSize * 2);
//            read = in.read(buffer, 0, bufferSize);
            boolean continueReading = true;
            while (true) {
//                if (continueReading && runningTasks < forkJoinPool.getParallelism()) {
                if (runningTasks < forkJoinPool.getParallelism()) {
                    mappedBuffer.get(buf, 0, availableSize);
                    String input = new String(buf);
//                    System.out.println("input = " + input.substring(0, Math.min(100, input.length())));
                    Matcher matcher = pattern.matcher(input);
                    if (matcher.find()) {
                        return true;
                    }
//                    completionService.submit(matcher::find);c
//                    runningTasks++;
                    from = pos.next();
//                    System.out.println("from = " + from);
                    if (from == -1) {
                        continueReading = false;
                        return false; //!!!!!!!!!!!!!!!!!!!!!!!
//                        continue;
                    }
                    availableSize = bufferSize <= fileLen - from ? bufferSize : (int) (fileLen - from);
                    mappedBuffer = channel.map(FileChannel.MapMode.READ_ONLY, from, availableSize);
                    buf = new byte[availableSize];
                } else {
                    Future<Boolean> future = completionService.take();
                    while (future != null) {
                        runningTasks--;
                        if (future.get()) {
                            return true;
                        }
                        if (continueReading) {
                            future = completionService.poll();
                        } else if (runningTasks > 0) {
                            future = completionService.take();
                        } else {
                            return false;
                        }
                    }
                }
            }


//            boolean continueSearch = true;
//            while (continueSearch) {
//                if (read == -1) {
//                    continueSearch = false;
//                } else if (read <= needleLen - 1) {
//                    throw new IllegalStateException();
//                }
//                String string = new String(buffer);
//                if (pattern.matcher(string).find()) {
//                    return true;
//                }
//                System.arraycopy(buffer, bufferSize - (needleLen - 1), buffer, 0, (needleLen - 1));
//                read = in.read(buffer, needleLen - 1, bufferSize - (needleLen - 1));
//            }
        } catch (IOException | InterruptedException | ExecutionException e) {
            throw new RuntimeException(file.getName(), e);
        }
//        return false;
    }
}
