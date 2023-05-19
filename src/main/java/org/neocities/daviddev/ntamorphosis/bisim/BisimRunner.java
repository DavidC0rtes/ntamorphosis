package org.neocities.daviddev.ntamorphosis.bisim;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;

public class BisimRunner {
    private final Multimap<String, String> results;
    private final ExecutorService bisimService;
    private final List<CompletableFuture> futures;
    private final String pathToCsv;

    public BisimRunner(String pathToCsv) {
        bisimService = Executors.newFixedThreadPool(4);
        results = Multimaps.synchronizedMultimap(ArrayListMultimap.create());
        this.pathToCsv=pathToCsv;
        futures = new ArrayList<>();
    }

    public void scheduleJob(File a, File b) {
        long start = System.currentTimeMillis();
        CompletableFuture<String> bisimFuture = CompletableFuture.supplyAsync(
                new BisimScheduler(a,b),
                bisimService
        ).completeOnTimeout("timeout", 20, TimeUnit.MINUTES
        ).whenComplete( (result, ex) -> {
            if (ex != null) {
                System.err.printf("Exception with files %s and %s\n", a.getName(), b.getName());
                ex.printStackTrace();
            }
        });

        futures.add(
                bisimFuture.thenAcceptAsync(bisimilar -> {
                    writeResultRow(a.getName(), b.getName(), String.valueOf(bisimilar), System.currentTimeMillis() - start);
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

    private synchronized void writeResultRow(String model1, String model2, String bisim, long elapsedTime) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(pathToCsv, true), CSVFormat.DEFAULT)){
            printer.printRecord(model1, model2, bisim, elapsedTime);
            printer.flush();
            System.out.println("Wrote to "+pathToCsv);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void shutdownJobs() {
        if (!futures.stream().allMatch(Future::isDone)) {
            System.out.println("Jobs are not done yet, waiting on them...");
            futures.forEach(job -> {
                try {
                    job.get(20, TimeUnit.MINUTES);
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error in job");
                    throw new RuntimeException(e);
                } catch (TimeoutException e) {
                    System.out.println("Timed out, skipping...");
                }
            });
        }
        bisimService.shutdown();
        /*try {
            bisimService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }*/
    }
}
