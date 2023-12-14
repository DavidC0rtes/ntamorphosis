package org.neocities.daviddev.ntamorphosis.bisim;

import be.unamur.uppaal.juppaal.Automaton;
import be.unamur.uppaal.juppaal.NTA;
import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.neocities.daviddev.ntamorphosis.entrypoint.AppConfig;
import org.neocities.daviddev.ntamorphosis.workers.VerifierScheduler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.concurrent.*;

public class BisimRunner {
    private final Multimap<String, String> results;
    private final ExecutorService bisimService;
    private final List<CompletableFuture> futures;
    private static final Logger logger = LoggerFactory.getLogger(BisimRunner.class);
    private final String pathToCsv;
    private Set<String> writtenTemplates = new HashSet<>();
    public BisimRunner(String pathToCsv) {
        bisimService = Executors.newFixedThreadPool(4);
        results = Multimaps.synchronizedMultimap(ArrayListMultimap.create());
        this.pathToCsv=pathToCsv;
        futures = new ArrayList<>();
    }

    /**
     * Schedules a bisimulation call between two UPPAAL model files (as XML). With the assumption that they
     * are related (model and mutant, or 2 mutants of the same model).
     * @param a UPPAAL model as xml
     * @param b UPPAAL model as xml
     */
    public void scheduleJob(File a, File b) {

        CompletableFuture<Boolean> resultA = CompletableFuture.supplyAsync(() -> execVerifyTa(AppConfig.getInstance().getVerifyTAPath(), a), bisimService);
        CompletableFuture<Boolean> resultB = CompletableFuture.supplyAsync(() -> execVerifyTa(AppConfig.getInstance().getVerifyTAPath(), b), bisimService);

        // Wait for both tasks to finish
        long submitTime = System.nanoTime();
        CompletableFuture.allOf(resultA, resultB).join();
        // Combine results
        boolean combinedResult;
        try {
            combinedResult = resultA.get() == resultB.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        if (!combinedResult) {
            NTA nta1 = new NTA(a.getAbsolutePath());
            int cap = nta1.getAutomata().size();

            for (Automaton ta : nta1.getAutomata()) {
                //long submitTime = System.nanoTime();
                String templateName = ta.getName().toString();
                if (!writtenTemplates.contains(a.getName()+b.getName())) {
                    CompletableFuture<String> bisimFuture = CompletableFuture.supplyAsync(
                            new BisimScheduler(a,b, templateName), bisimService
                    );
                    cap--; // how many templates left?
                    addFuture(a, b, submitTime, bisimFuture, cap);
                }
            }
        } else {
            long end = System.nanoTime();
            writeResultRow(a.getName(), b.getName(), String.valueOf(combinedResult),
                    Instant.ofEpochMilli(submitTime).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                    end - submitTime);
            writtenTemplates.add(a.getName()+b.getName());
        }
    }

    private void addFuture(File a, File b, long start, CompletableFuture<String> bisimFuture, int rest) {
        String key = a.getName()+b.getName();
        futures.add(
                bisimFuture.thenAcceptAsync(bisimilar -> {
                    if (!writtenTemplates.contains(key) && (rest <= 0 || bisimilar.equals("false"))) {
                        //System.out.println("MAYBE WRITING");
                        long end = System.nanoTime();
                        //if (a.getName().contains("ventilator") || b.getName().contains("ventilator"))
                        writeResultRow(
                                a.getName(),
                                b.getName(),
                                String.valueOf(bisimilar),
                                Instant.ofEpochMilli(start).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                                Instant.ofEpochMilli(end).atZone(ZoneId.systemDefault()).toLocalDateTime(),
                                end - start
                        );

                        writtenTemplates.add(a.getName()+b.getName());
                    }
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
               // "-t", "0",
                "-r", Integer.toString(rand.nextInt()),
                "-q", model.getAbsolutePath()
        ).redirectErrorStream(true);

        Process process;
        try {
            process = processBuilder.start();
            //
            // logger.info("Waiting verifyta {}", model.getAbsolutePath());
            if (!process.waitFor(10, TimeUnit.SECONDS)) {
               logger.info("Nothing!");
                //process.destroy();
                //return true;
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error starting and/or waiting for process with model {}.",model.getName(),e);
            throw new RuntimeException(e);
        }
        String output;
        try {
            output = new String(process.getInputStream().readAllBytes());
        } catch (IOException e) {
            logger.error("Error while reading verifyta's output with model {}.",model.getName(),e);
            throw new RuntimeException(e);
        }
        if (output.isEmpty()) {
            logger.error("Acquired empty string when reading verifyta.");
            return false;
        }
        process.destroy();
        return !output.contains("NOT satisfied") && !output.contains("MAY be satisfied");
    }
}
