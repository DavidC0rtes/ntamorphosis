package org.neocities.daviddev.ntamorphosis.bisim;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.concurrent.*;

public class BisimRunner {
    private Multimap<String, String> results;
    private final ExecutorService bisimService;
    private final String pathToCsv;

    public BisimRunner(String pathToCsv) {
        bisimService = Executors.newCachedThreadPool();
        results = Multimaps.synchronizedMultimap(ArrayListMultimap.create());
        this.pathToCsv=pathToCsv;
    }

    public void scheduleJob(File a, File b) {
        long start = System.currentTimeMillis();
        System.out.printf("Scheduling job for files %s and %s\n",a.getName(), b.getName());
        Future<Boolean> bisimFuture = bisimService.submit(
                new BisimScheduler(a, b)
        );

        try {
            boolean isBisimilar = bisimFuture.get();
            String key = a.getName().split("\\.")[0];
            if (key.length() == 0) {
                throw new RuntimeException("Empty string as key name");
            }
            results.put(key, b.getName());
            results.put(key,isBisimilar ? "true" : "false");
            results.put(key, String.valueOf(System.currentTimeMillis() - start));
            writeResults();
            results.clear();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
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

    public void shutdownJobs() {
        bisimService.shutdown();
    }
}
