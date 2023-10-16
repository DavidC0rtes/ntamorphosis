package org.neocities.daviddev.ntamorphosis.workers;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;


public class TraceRunner {

    //private final ExecutorService service;
    private final String strategy, pathToCsv, tracesDir;
    //private final List<CompletableFuture> futures;
    public TraceRunner(String strategy, String pathToCsv, String tracesDir) {
        //service = Executors.newSingleThreadExecutor();
        //futures = new ArrayList<>();
        this.strategy = strategy;
        this.pathToCsv = pathToCsv;
        this.tracesDir = tracesDir;
    }

    public void runTrace(File a, File b, String propDir) {
        TraceSupplier ts = new TraceSupplier(a,b,strategy, propDir, tracesDir);
        writeResultRow(ts.get());
        /*CompletableFuture<Void> traceFuture = CompletableFuture.supplyAsync(
                new TraceSupplier(a,b,strategy, propDir, tracesDir), service
        ).thenAccept(this::writeResultRow
        ).exceptionally(throwable -> {
            throwable.printStackTrace();
            System.exit(-1);
            return null;
        });*/

        //futures.add(traceFuture);
    }

    public void runRandomTrace(File a, File b, String propDir, int n, int k) {
        TraceSupplier ts = new TraceSupplier(a,b,strategy, propDir, tracesDir);
        writeResultRow(ts.getRandom(n, k));
    }


    private synchronized void writeResultRow(String[] row) {
        if (row.length > 0 && !String.join("", row).isBlank()) {
            try (CSVPrinter printer = new CSVPrinter(new FileWriter(pathToCsv, true), CSVFormat.DEFAULT)){
                printer.printRecord((Object[]) row);
                printer.flush();
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /*public void shutdownJobs() {
        service.shutdown();
        if (!futures.stream().allMatch(Future::isDone)) {
            System.out.println("Jobs are not done yet, waiting on them...");
            futures.forEach(CompletableFuture::join);
        }
        try {
            if (!service.awaitTermination(10, TimeUnit.MINUTES)) {
                service.shutdownNow();
            }
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }*/
}
