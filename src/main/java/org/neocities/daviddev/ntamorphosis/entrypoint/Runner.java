package org.neocities.daviddev.ntamorphosis.entrypoint;

import Parser.Main.EntryPoint;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.neocities.daviddev.bisimcheck.runners.BisimRunner;
import org.neocities.daviddev.bisimcheck.workers.Bisimulation;
import org.neocities.daviddev.ntamorphosis.pairprocessing.XMLFileProcessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Runner {
    private String mutationsDir;
    private File model;
    private List<String> operators;
    private ExecutorService executorService;
    private HashMap<String, String[]> resultsTron;
    private HashMap<String, String[]> resultsBisim;

    private static enum tronHeaders {
        model, mutant, test_passed, elapsed_time
    }

    public Runner(File model, List<String> operators) {
        this.model = model;
        this.operators = operators;
        this.executorService = Executors.newSingleThreadExecutor();
    }
    public Runner(File model, String mutationsDir) {
        this.model = model;
        this.mutationsDir = mutationsDir;
        this.executorService = Executors.newSingleThreadExecutor();
        resultsTron = new HashMap<>();
        execUppaalMutants();
        execSimmDiff();
    }
    
    public Runner(String dir) {
        this.mutationsDir = dir;
        this.executorService = Executors.newSingleThreadExecutor();
        resultsTron = new HashMap<>();
        prepareCSV();
        execSimmDiffRRSingles();
    }

    private void execUppaalMutants() {
        String modelName = model.getName().split("\\.")[0];
        Runnable mutationTask = (() ->
                EntryPoint.main(new String[]{
                        "-m="+model, "-p="+mutationsDir+"/"+modelName, "-all"
                })
        );
        Future<?> foo = executorService.submit(mutationTask);
        try {
            foo.get();
            System.out.println("Finished mutants generation");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Executes SimmDiffUppaal with every generated mutant and its model, keeps a record
     * of mutants that failed the tron test.
     */
    private void execSimmDiff() {
        String modelName = model.getName().split("\\.")[0];
        Path dir = Paths.get(mutationsDir+"/"+modelName);
        Runnable tronTask = (() ->
        {
            try (Stream<Path> entries = Files.walk(dir, 1)){
                entries.forEach(mutant -> {
                    if (Files.isRegularFile(mutant) && !mutant.endsWith(".csv")) {

                        //System.out.printf("Executing with mutant %s and model %s\n", mutant, model.getName());
                        org.neocities.daviddev.simmdiff.entrypoint.Runner simRunner = new org.neocities.daviddev.simmdiff.entrypoint.Runner(
                                model,
                                mutant.toFile()
                        );
                        simRunner.parseModels();
                        simRunner.parseTraces();
                        simRunner.simulateTraces();
                        resultsTron.putAll(simRunner.getTracesResult());
                    }
                });
            } catch (IOException e) {
                System.err.println("working with model "+modelName);
                throw new RuntimeException(e);
            }
        }
        );
        wrapUp(tronTask);
    }

    private void execSimmDiffV2() {
        String modelName = model.getName().split("\\.")[0];
        Path dir = Paths.get(mutationsDir+"/"+modelName);
        try (Stream<Path> entries = Files.walk(dir, 1)){
            entries.forEach(mutant -> {
                if (Files.isRegularFile(mutant) && !mutant.endsWith(".csv")) {

                    Runnable tronTask = (() -> {
                        //System.out.printf("Executing with mutant %s and model %s\n", mutant, model.getName());
                        org.neocities.daviddev.simmdiff.entrypoint.Runner simRunner = new org.neocities.daviddev.simmdiff.entrypoint.Runner(
                                model,
                                mutant.toFile()
                        );
                        simRunner.parseModels();
                        simRunner.parseTraces();
                        simRunner.simulateTraces();
                        resultsTron.putAll(simRunner.getTracesResult());
                    });
                    wrapUp(tronTask);
                }
            });
        } catch (IOException e) {
            System.err.println("working with model "+modelName);
            throw new RuntimeException(e);
        }
    }

     /**
     * Executes SimmDiffUppaal with every generated mutant and its model, keeps a record
     * of mutants that failed the tron test.
     */
    private void execSimmDiffRR() {
        XMLFileProcessor processor = new XMLFileProcessor();
        Runnable tronTask = (() ->
                processor.processXmlFilePairs(mutationsDir, resultsTron)
        );
        wrapUp(tronTask);
    }

    private void execSimmDiffRRSingles() {

        File directory = new File(mutationsDir);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));

        for (int i = 0; i < xmlFiles.length - 1; i++) {
            for (int j = i + 1; j < xmlFiles.length; j++) {
                File file1 = xmlFiles[i];
                File file2 = xmlFiles[j];

                XMLFileProcessor processor = new XMLFileProcessor();
                Runnable tronTask = (() ->
                        processor.runSimmDiff(file1, file2, resultsTron)
                );

                wrapUp(tronTask);
            }
        }
        executorService.shutdown();
    }
    private void wrapUp(Runnable tronTask) {
        Future<?> foo = executorService.submit(tronTask);
        try {
            foo.get();
            printCSV();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareCSV() {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(tronHeaders.class)
                .build();

        try (FileWriter writer = new FileWriter("traces-result.csv")){
            csvFormat.print(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private synchronized void printCSV() {

        try (CSVPrinter printer = new CSVPrinter(new FileWriter("traces-result.csv", true), CSVFormat.DEFAULT)) {

            // Key: filename of mutant.
            // Value: String[]{path to model, tron result}
            for (var entry : resultsTron.entrySet()) {
                Object[] result = entry.getValue();
                printer.printRecord(entry.getKey(), result[0], result[1], result[2]);
            }
            printer.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void execBisim() {
        List<File> a = new ArrayList<>();
        List<File> b = new ArrayList<>();

        // Key: filename of mutant.
        // Value: String[]{path to model, tron result}
        resultsTron.forEach((key, value) -> {
            if (value[1].equals("true")) {
                a.add(new File(key));
                b.add(new File(value[0]));
            }
        });

        System.out.printf("Got %d mutant(s) to check against Bisimulation tool\n", b.size());
        BisimRunner bisimRunner = new BisimRunner(a, b, false);
        bisimRunner.start();
    }
}
