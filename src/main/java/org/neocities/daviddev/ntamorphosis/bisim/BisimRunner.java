package org.neocities.daviddev.ntamorphosis.bisim;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
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
//        bisimService = new ThreadPoolExecutor(4,4,0L, TimeUnit.MILLISECONDS,
//                new LinkedBlockingQueue<>(1));
        results = Multimaps.synchronizedMultimap(ArrayListMultimap.create());
        this.pathToCsv=pathToCsv;
        futures = new ArrayList<>();
    }

    public void scheduleJob(File a, File b) {
        long submitTime = System.currentTimeMillis();
        CompletableFuture<String> bisimFuture = CompletableFuture.supplyAsync(
                new BisimScheduler(a,b),
                bisimService
        ).whenCompleteAsync( (result, ex) -> {
            if (ex != null) {
                System.err.printf("Exception with files %s and %s\n", a.getName(), b.getName());
                ex.printStackTrace();
            }
        });
        addFuture(a, b, submitTime, bisimFuture);
    }

    private void addFuture(File a, File b, long start, CompletableFuture<String> bisimFuture) {
        System.out.println("Adding future");
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
            futures.forEach(job -> {
                try {
                    job.get();
                } catch (InterruptedException | ExecutionException e) {
                    System.err.println("Error in job");
                    throw new RuntimeException(e);
                }
            });
        }
        bisimService.shutdown();
        try {
            bisimService.awaitTermination(1, TimeUnit.HOURS);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private long calculateTimeout(long submitTime) {
        long waitingTime = System.currentTimeMillis() - submitTime;
        long timeout = 20 * 60 * 1000; // Total timeout duration in milliseconds
        long effectiveTimeout = Math.max(0, timeout - waitingTime);
        System.out.printf("Effective timeout %d s\n", (effectiveTimeout/1000)/60);
        return effectiveTimeout;
    }
}
