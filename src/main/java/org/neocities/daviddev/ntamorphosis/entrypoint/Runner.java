package org.neocities.daviddev.ntamorphosis.entrypoint;

import Parser.Main.EntryPoint;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.neocities.daviddev.ntamorphosis.bisim.BisimRunner;
import org.neocities.daviddev.ntamorphosis.pairprocessing.XMLFileProcessor;
import org.neocities.daviddev.ntamorphosis.workers.Preprocessor;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Runner {
    private final String csvBisim;
    private String mutationsDir, csvPath;
    private File model;
    private List<String> operators;
    private ExecutorService executorService;
    private HashMap<String, String[]> resultsTron;
    private HashMap<String, String[]> resultsBisim;
    private BisimRunner bisimRunner;

    private enum tronHeaders {
        mutant1, mutant2, template, passed_test, diff_locations, explored_diffs, elapsed_time
    }

/*    public Runner(File model, List<String> operators) {
        this.model = model;
        this.operators = operators;
        this.executorService = Executors.newSingleThreadExecutor();
    }*/
    public Runner(File model, String mutationsDir, String csvPath, String csvBisim) {
        this.model = model;
        this.mutationsDir = mutationsDir;
        this.executorService = Executors.newCachedThreadPool();
        resultsTron = new HashMap<>();
        this.csvPath=csvPath;
        this.csvBisim=csvBisim;
        Preprocessor p = new Preprocessor();
        p.addTauChannel(model);
        execUppaalMutants();
        bisimRunner = new BisimRunner(csvBisim);
        prepareCSV();
        execSimmDiffRRSingles();
    }
    
    public Runner(String dir, String csvPath, String csvBisim) {
        this.mutationsDir = dir;
        this.executorService = Executors.newCachedThreadPool();
        resultsTron = new HashMap<>();
        this.csvPath=csvPath;
        this.csvBisim=csvBisim;
        bisimRunner = new BisimRunner(csvBisim);
        prepareCSV();
        execSimmDiffRRSingles();
    }

    private void execUppaalMutants() {
        Runnable mutationTask = (() ->
                EntryPoint.main(new String[]{
                        "-m="+model, "-p="+mutationsDir, "-all"
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
                        simRunner.parseModels(mutationsDir);
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

     /**
     * Executes SimmDiffUppaal with every generated mutant and its model, keeps a record
     * of mutants that failed the tron test.
     */
    private void execSimmDiffRR() {
        XMLFileProcessor processor = new XMLFileProcessor();
        Runnable tronTask = (() ->
                processor.processXmlFilePairs(mutationsDir)
        );
        wrapUp(tronTask);
    }

    private void execSimmDiffRRSingles() {

        File directory = new File(mutationsDir);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        System.out.printf("%d files in %s \n",xmlFiles.length, mutationsDir);
        for (int i = 0; i < Objects.requireNonNull(xmlFiles).length - 1; i++) {
            for (int j = i + 1; j < xmlFiles.length; j++) {
                File file1 = xmlFiles[i];
                File file2 = xmlFiles[j];
                XMLFileProcessor processor = new XMLFileProcessor();
                Runnable tronTask = (() -> {
                    resultsTron.putAll(processor.runSimmDiff(file1, file2, mutationsDir));
                    resultsTron.putAll(processor.runSimmDiff(file2, file1, mutationsDir));
                    bisimRunner.scheduleJob(file1, file2);
                });

                wrapUp(tronTask);
            }
        }
        bisimRunner.shutdownJobs();
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

        try (FileWriter writer = new FileWriter(csvPath)){
            csvFormat.print(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private synchronized void printCSV() {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(csvPath, true), CSVFormat.DEFAULT)) {

            // Key: filename of mutant.
            // Value: String[]{path to model, tron result}
            for (var entry : resultsTron.entrySet()) {
                Object[] result = entry.getValue();
                printer.printRecord(entry.getKey(), result[0], result[1], result[2],  result[3], result[4], result[5]);
            }
            printer.flush();
            resultsTron.clear();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
