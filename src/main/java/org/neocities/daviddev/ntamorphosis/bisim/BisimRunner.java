package org.neocities.daviddev.ntamorphosis.bisim;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.*;

public class BisimRunner {
    private Multimap<String, String> results;
    private final ExecutorService bisimService;
    private List<File> aFiles, bFiles;
    public BisimRunner(List<File> a, List<File> b) {
        bisimService = Executors.newCachedThreadPool();
        results = Multimaps.synchronizedMultimap(ArrayListMultimap.create());
        aFiles = a; bFiles = b;
    }

    public BisimRunner() {
        bisimService = Executors.newCachedThreadPool();
        results = Multimaps.synchronizedMultimap(ArrayListMultimap.create());
    }

    public void scheduleJob(File a, File b) {
        System.out.printf("Scheduling job for files %s and %s\n",a.getName(), b.getName());
        Future<Boolean> bisimFuture = bisimService.submit(
                new Bisimulation(a.getAbsolutePath(), b.getAbsolutePath()));
        long start = System.currentTimeMillis();
        try {
            boolean isBisimilar = bisimFuture.get();
            results.put(b.getName(), a.getName().split("\\.")[0]);
            results.put(b.getName(),isBisimilar ? "true" : "false");
            results.put(b.getName(), String.valueOf(System.currentTimeMillis() - start));
            writeResults();
            results.clear();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkBisimilar() {
        for (File file : aFiles) {
            for (File mutant : bFiles ) {
                String modelName = file.getName().split("\\.")[0];
                if (mutant.getName().startsWith(modelName)) {
                    scheduleJob(file, mutant);
                }
            }
        }
        bisimService.shutdown();
    }

    private synchronized void writeResults() {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter("results_bisim.csv", true), CSVFormat.DEFAULT)){

            for (var entry : results.asMap().entrySet()) {
                Object[] row = entry.getValue().toArray();
                printer.printRecord(entry.getKey(), row[0], row[1], row[2]);
            }
            printer.flush();
            results.clear();
            System.out.println("Wrote to results-bisim!");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdownJobs() {
        bisimService.shutdown();
    }
}
