package ru.ivanishkin.optjava.find;

import org.apache.commons.cli.BasicParser;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorCompletionService;
import java.util.concurrent.ForkJoinPool;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//import org.apache.commons.cli.CommandLineParser;


public class Main {

    public static void main(String[] args) throws ExecutionException, InterruptedException, ParseException {
        Options options = new Options();
        options.addOption("n", "name", true, "Search by name");
        options.addOption("d", "data", true, "Search by content");
        options.addOption("h", "help", false, "Print this message");

        final String name;
        final String needle;
        final String startPath;

        CommandLine cmd = new BasicParser().parse(options, args);
        if (cmd.hasOption("help")) {
            System.out.println("Usage: java -jar ./find-1.0-SNAPSHOT-jar-with-dependencies.jar [--name <fileName>] [--data \"<text>\"] <folder|file>");
            return;
        }
        if (cmd.hasOption("name")) {
            name = cmd.getOptionValue("name");
        } else {
            name = "";
        }
        if (cmd.hasOption("data")) {
            needle = cmd.getOptionValue("data");
        } else {
            needle = "";
        }
        String[] positionals = cmd.getArgs();
        if (positionals.length > 0) {
            startPath = positionals[0];
        } else {
            startPath = ".";
        }
        System.out.println("name = " + name);
        System.out.println("needle = " + needle);
        System.out.println("startPath = " + startPath);


        int threads = Runtime.getRuntime().availableProcessors();
        ForkJoinPool threadPool = new ForkJoinPool(threads);
        ExecutorCompletionService<Boolean> completionService = new ExecutorCompletionService<>(threadPool);
        final int bufferSize = 8192;
//        final int bufferSize = 1_288_490_188 / (threads * 10); slow

        List<Path> found = threadPool.submit(() -> {
            try {
                Stream<Path> files = Files.walk(Path.of(startPath)).parallel();
                if (!name.equals("")) {
                    files = files.filter(path -> path.toString().contains(name));
                }
                if (!needle.equals("")) {
                    files = files.filter(path -> new FileContentFilter(
                                    Pattern.compile(Pattern.quote(needle)),
                                    bufferSize,
                                    needle.length(),
                                    completionService
                            ).filter(path)
                    );
                }
                return files.collect(Collectors.toList());
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }).get();

        found.forEach(System.out::println);
    }
}
