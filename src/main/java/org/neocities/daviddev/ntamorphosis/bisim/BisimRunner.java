package org.neocities.daviddev.ntamorphosis.bisim;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.neocities.daviddev.ntamorphosis.workers.TracesProvider;
import org.neocities.daviddev.ntamorphosis.workers.VerifierScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;

public class BisimRunner {
    private final Multimap<String, String> results;
    private final ExecutorService bisimService;
    private final List<CompletableFuture> futures;
    private static final Logger logger = LoggerFactory.getLogger(BisimRunner.class);
    private final String pathToCsv;

    public BisimRunner(String pathToCsv) {
        bisimService = Executors.newFixedThreadPool(5);
        results = Multimaps.synchronizedMultimap(ArrayListMultimap.create());
        this.pathToCsv=pathToCsv;
        futures = new ArrayList<>();
    }

    public void scheduleJob(File a, File b) {
        long submitTime = System.currentTimeMillis();
        // File a is the product of the mutant, for verifyta, we need to pass the original version of the mutant
        Path pathToMutant = Paths.get(a.getParentFile().getParent() + "/" + a.getName());
        if (!Files.exists(pathToMutant)) {
            logger.error("Mutant {} does not exist, or the path is wrong.", pathToMutant);
            System.exit(1);
        }

        File mutantNTA = new File(pathToMutant.toUri());
        CompletableFuture<Boolean> verifyTAFuture = CompletableFuture.supplyAsync(
                new VerifierScheduler("/home/david/.local/etc/uppaal64-4.1.26-2/bin-Linux/verifyta",mutantNTA)
                , bisimService
        ).completeOnTimeout(true, 5, TimeUnit.MINUTES);

        CompletableFuture<Boolean> bisimFuture = verifyTAFuture.thenComposeAsync(satisfied -> {
            if (!satisfied) {
                return CompletableFuture.completedFuture(false);
            } else {
                return CompletableFuture.supplyAsync(new BisimScheduler(a,b), bisimService)
                        .completeOnTimeout(true, 1, TimeUnit.HOURS);
            }
        });
        addFuture(a, b, submitTime, bisimFuture);
    }

    private void addFuture(File a, File b, long start, CompletableFuture<Boolean> bisimFuture) {
        futures.add(
                bisimFuture.thenAcceptAsync(bisimilar -> {
                    long end = System.currentTimeMillis();
                    writeResultRow(
                            a.getName(),
                            b.getName(),
                            String.valueOf(bisimilar),
                            Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                            Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                            end - start
                    );
                })
        );
    }

    private synchronized void writeResults() {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(pathToCsv, true), CSVFormat.DEFAULT)){

            for (var entry : results.asMap().entrySet()) {
                Object[] row = entry.getValue().toArray();
                printer.printRecord(entry.getKey(), row[0], row[1], row[2]);
            }
            printer.flush();
            results.clear();
            System.out.println("Wrote to "+pathToCsv);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private synchronized void writeResultRow(String model1, String model2, String bisim, LocalDateTime startDate, LocalDateTime endDate, long elapsedTime) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(pathToCsv, true), CSVFormat.DEFAULT)){
            printer.printRecord(model1, model2, bisim, startDate, endDate, elapsedTime);
            printer.flush();
            System.out.println("Wrote to "+pathToCsv);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdownJobs() {
        if (!futures.stream().allMatch(Future::isDone)) {
            System.out.println("Jobs are not done yet, waiting on them...");
            futures.forEach(CompletableFuture::join);
        }
        bisimService.shutdown();
        try {
            bisimService.awaitTermination(10, TimeUnit.MINUTES);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private boolean execVerifyTa(String pathToVerifyTA, File model) {
        Random rand = new Random();
        ProcessBuilder processBuilder = new ProcessBuilder(
                pathToVerifyTA,
                "-t", "0",
                "-r", Integer.toString(rand.nextInt()),
                "-q", model.getAbsolutePath()
        );

        Process process = null;
        try {
            process = processBuilder.start();
            process.waitFor(100, TimeUnit.MINUTES);
        } catch (IOException | InterruptedException e) {
            logger.error("Error startind and/or waiting for process with model {}.",model.getName(),e);
            throw new RuntimeException(e);
        }


        String output = null;
        try {
            output = new String(process.getErrorStream().readAllBytes());
        } catch (IOException e) {
            logger.error("Error while reading verifyta's output with model {}.",model.getName(),e);
            throw new RuntimeException(e);
        }
        if (output.isEmpty()) {
            logger.error("Acquired empty string when reading verifyta.");
            return false;
        }

        return !output.contains("NOT satisfied") && !output.contains("MAY be satisfied");
    }
}
