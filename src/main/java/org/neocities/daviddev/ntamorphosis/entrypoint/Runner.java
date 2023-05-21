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
import java.util.*;
import java.util.concurrent.*;

public class Runner {
    private final String csvBisim;
    private String mutationsDir, csvTraces;
    private File model;
    private List<String> operators;
    private Preprocessor preprocessor;
    private ExecutorService executorService;
    private HashMap<String, String[]> resultsTron;
    private HashMap<String, String[]> resultsBisim;
    private BisimRunner bisimRunner;
    private enum tronHeaders {
        mutant1, mutant2, template, passed_test, diff_locations, explored_diffs, elapsed_time
    }
    public Runner(ArrayList<String> operators, File model, String mutationsDir, String csvTraces, String csvBisim, String strategy) {
        this.executorService = Executors.newCachedThreadPool();
        this.csvTraces = csvTraces;
        this.csvBisim = csvBisim;
        resultsTron = new HashMap<>();
        preprocessor = new Preprocessor();

        configureMutations(operators, model, mutationsDir);
        preProcess(preprocessor);
        bisimRunner = new BisimRunner(csvBisim);
        prepareCSV();
        execSimmDiffRRSingles(strategy);
    }

    /*public Runner(File model, String mutationsDir, String csvTraces, String csvBisim, String strategy) {
        this.model = model;
        this.mutationsDir = mutationsDir+System.currentTimeMillis();
        this.executorService = Executors.newCachedThreadPool();
        resultsTron = new HashMap<>();
        this.csvTraces = csvTraces;
        this.csvBisim=csvBisim;
        Preprocessor p = new Preprocessor();
        p.addTauChannel(model);
        execUppaalMutants(operators);
        preProcess(p);
        bisimRunner = new BisimRunner(csvBisim);
        prepareCSV();
        execSimmDiffRRSingles(strategy);
    }*/
    
    /*public Runner(String dir, String csvTraces, String csvBisim, String strategy) {
        this.mutationsDir = dir;
        this.executorService = Executors.newCachedThreadPool();
        resultsTron = new HashMap<>();
        this.csvTraces = csvTraces;
        this.csvBisim=csvBisim;
        bisimRunner = new BisimRunner(csvBisim);
//        Preprocessor p = new Preprocessor();
//        preProcess(p);
        prepareCSV();
        execSimmDiffRRSingles(strategy);
    }*/

    /**
     * Constructor to call when performing bisimulation checks between
     * mutants and the original model, or between mutants only.
     */
    public Runner(ArrayList<String> operators, File model, String mutationsDir, String csvBisim) {
        this.model = model;
        this.csvBisim = csvBisim;
        this.executorService = Executors.newCachedThreadPool();
        preprocessor = new Preprocessor();
        bisimRunner = new BisimRunner(csvBisim);

        configureMutations(operators, model, mutationsDir);
    }

    private void configureMutations(List<String> operators, File model, String mutationsDir) {
        if (operators.size() > 0) { // do mutations
            this.mutationsDir = mutationsDir+System.currentTimeMillis();
            preprocessor.addTauChannel(model);
            execUppaalMutants(operators);
            preProcess(preprocessor);
            preprocessor.computeNTAProduct(model, mutationsDir+"/compositions");
            this.mutationsDir += "/compositions";
        } else {
            this.mutationsDir = mutationsDir;
        }
    }

    public void preProcess(Preprocessor preprocessor) {
        // Create directory for compositions
        try {
            Files.createDirectories(Path.of(mutationsDir + "/compositions"));
        } catch (IOException e) {
            System.err.println("Failed to create compositions directory "+ mutationsDir + "/compositions");
            throw new RuntimeException(e);
        }

        File directory = new File(mutationsDir);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        assert xmlFiles != null;
        for (File mutant : xmlFiles) {
            preprocessor.computeNTAProduct(mutant, mutationsDir + "/compositions");
        }
    }

    private void execUppaalMutants(List<String> operators) {
        String command = operators.contains("all")
                ? "all"
                : String.join(" -",operators);

        System.out.println("command is "+command);
        Runnable mutationTask = (() ->
                EntryPoint.main(new String[]{
                        "-m="+model, "-p="+mutationsDir, "-"+command
                })
        );
        Future<?> foo = executorService.submit(mutationTask);
        try {
            foo.get();
            System.out.println("Finished generating mutants.");
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
        System.exit(0);
    }

    private void execSimmDiffRRSingles(String strategy) {
        File directory = new File(mutationsDir);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        assert xmlFiles != null;
        System.out.printf("%d files in %s \n",xmlFiles.length, mutationsDir);
        for (int i = 0; i < Objects.requireNonNull(xmlFiles).length - 1; i++) {
            for (int j = i + 1; j < xmlFiles.length; j++) {
                File file1 = xmlFiles[i];
                File file2 = xmlFiles[j];
                XMLFileProcessor processor = new XMLFileProcessor(strategy);
                Runnable tronTask = (() -> {

                    // Get composed mutant path
                    File product1 = new File(mutationsDir+"/compositions", file1.getName());
                    File product2 = new File(mutationsDir+"/compositions", file2.getName());

                    resultsTron.putAll(processor.runSimmDiff(file1, file2, mutationsDir));
                    resultsTron.putAll(processor.runSimmDiff(file2, file1, mutationsDir));

                    bisimRunner.scheduleJob(product1, product2);
                });
                wrapUp(tronTask);
            }
        }

        bisimRunner.shutdownJobs();

        executorService.shutdown();
    }

    public void execBisimCheckEquivalent() {
        File directory = new File(mutationsDir);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        assert xmlFiles != null;
        System.out.printf("%d files in %s \n",xmlFiles.length, mutationsDir);
        File product2 = new File(mutationsDir, model.getName());
        for (int i = 0; i < Objects.requireNonNull(xmlFiles).length; i++) {
            File file1 = xmlFiles[i];
            File product1 = new File(mutationsDir, file1.getName());
            bisimRunner.scheduleJob(product1, product2);

            //wrapUp(tronTask);
        }
        bisimRunner.shutdownJobs();
        executorService.shutdown();
    }

    public void checkDuplicates() {
        File directory = new File(mutationsDir);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        assert xmlFiles != null;
        System.out.printf("%d files in %s \n",xmlFiles.length, mutationsDir);
        for (int i = 0; i < Objects.requireNonNull(xmlFiles).length; i++) {
            for (int j = 1; j < xmlFiles.length; j++) {
                File file1 = xmlFiles[i];
                File product1 = new File(mutationsDir, file1.getName());
                File product2 = new File(mutationsDir, xmlFiles[j].getName());
                bisimRunner.scheduleJob(product1, product2);
            }
        }
        bisimRunner.shutdownJobs();
        executorService.shutdown();
    }

    private void wrapUp(Runnable tronTask) {
        Future<?> foo = executorService.submit(tronTask);
        try {
            foo.get();
            //printCSV();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareCSV() {
        CSVFormat csvFormat = CSVFormat.DEFAULT.builder()
                .setHeader(tronHeaders.class)
                .build();

        try (FileWriter writer = new FileWriter(csvTraces)){
            csvFormat.print(writer);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
    private synchronized void printCSV() {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(csvTraces, true), CSVFormat.DEFAULT)) {

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
