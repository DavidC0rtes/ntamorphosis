package org.neocities.daviddev.ntamorphosis.entrypoint;

import Parser.Main.EntryPoint;
import com.google.common.collect.Sets;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.neocities.daviddev.ntamorphosis.bisim.BisimRunner;
import org.neocities.daviddev.ntamorphosis.pairprocessing.XMLFileProcessor;
import org.neocities.daviddev.ntamorphosis.workers.Preprocessor;
import org.neocities.daviddev.ntamorphosis.workers.TraceRunner;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

public class Runner {
    private  String csvBisim;
    private String mutationsDir, csvTraces;
    private File model;
    private List<String> operators;
    private Preprocessor preprocessor;
    private ExecutorService executorService;
    private HashMap<String, String[]> resultsTron;
    private HashMap<String, String[]> resultsBisim;
    private  BisimRunner bisimRunner;
    private String tracesDir;
    private enum tronHeaders {
        mutant1, mutant2, template, passed_test, diff_locations, explored_diffs, elapsed_time_ms
    }
    public Runner(ArrayList<String> operators, File model, String mutationsDir, String csvTraces, String csvBisim, String strategy) {
        this.executorService = Executors.newCachedThreadPool();
        this.csvTraces = csvTraces;
        this.csvBisim = csvBisim;
        resultsTron = new HashMap<>();
        preprocessor = new Preprocessor();

        configureMutations(operators, model, mutationsDir);
        createCompositionsDir();
        computeCompositions();
        bisimRunner = new BisimRunner(csvBisim);
        prepareTracesCSV();
        execSimmDiffRRSingles(strategy);
    }

    /**
     * Constructor to call when performing bisimulation checks between
     * mutants and the original model, or between mutants only.
     */
    public Runner(ArrayList<String> operators, File model, String mutationsDir, String csvBisim) {
        this.model = model;
        this.csvBisim = csvBisim;
        this.executorService = Executors.newSingleThreadExecutor();
        preprocessor = new Preprocessor();
        bisimRunner = new BisimRunner(csvBisim);

        configureMutations(operators, model, mutationsDir);
    }

    /**
     * Call this constructor when performing traces inclusion only.
     * This will not do compositions, do mutations, nor call bisimulation at any step.
     * @param mutationsDir directory containing mutants.
     * @param csvTraces path to csv to store traces result.
     * @param strategy how to generate traces (random, biased).
     */
    public Runner(String mutationsDir,String csvTraces, String strategy, String tracesDir) {
        //this.executorService = Executors.newCachedThreadPool();
        this.mutationsDir = mutationsDir;
        this.csvTraces = csvTraces;
        resultsTron = new HashMap<>();
        this.tracesDir = tracesDir;
        prepareTracesCSV();
        execTraceMatchRRSingles(strategy);
    }

    private void configureMutations(List<String> operators, File model, String mutationsDir) {
        if (operators.size() > 0) { // do mutations
            this.mutationsDir = mutationsDir.concat(String.valueOf(System.currentTimeMillis()));
            preprocessor.addTauChannel(model);
            execUppaalMutants(operators);
            preProcess(preprocessor);
            preprocessor.computeNTAProduct(model, this.mutationsDir+"/compositions");
            this.mutationsDir += "/compositions";
        } else {
            this.mutationsDir = mutationsDir;
//            // we assume we got a dir of mutants, but not of compositions, we need to make them then
//            //preProcess(preprocessor);
//            createCompositionsDir();
//            computeCompositions();
//            this.mutationsDir += "/compositions";
        }
    }

    private void createCompositionsDir() {
        // Create directory for compositions
        try {
            Files.createDirectories(Path.of(mutationsDir + "/compositions"));
        } catch (IOException e) {
            System.err.println("Failed to create compositions directory "+ mutationsDir + "/compositions");
            throw new RuntimeException(e);
        }
    }

    private void computeCompositions() {
        // compute the product for each generated mutant
        try {
            File directory = new File(mutationsDir);
            File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
            assert xmlFiles != null;
            for (File mutant : xmlFiles) {
                preprocessor.computeNTAProduct(mutant, mutationsDir + "/compositions");
            }
        } catch (NullPointerException ex) {
            System.err.printf("Can't find directory %s\n", mutationsDir);
            ex.printStackTrace();
            System.exit(3);
        }
    }
    private void preProcess(Preprocessor preprocessor) {
        // Create directory for compositions
        try {
            Files.createDirectories(Path.of(mutationsDir + "/compositions"));
        } catch (IOException e) {
            System.err.println("Failed to create compositions directory "+ mutationsDir + "/compositions");
            throw new RuntimeException(e);
        }
        // compute the product for each generated mutant
        try {
            File directory = new File(mutationsDir);
            File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
            assert xmlFiles != null;
            for (File mutant : xmlFiles) {
                preprocessor.computeNTAProduct(mutant, mutationsDir+ "/compositions");
            }
        } catch (NullPointerException ex) {
            System.err.printf("Can't find directory %s\n", mutationsDir);
            System.exit(3);
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
    }

    private void execSimmDiffRRSingles(String strategy) {
        File directory = new File(mutationsDir);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        assert xmlFiles != null;
        System.out.printf("%d files in %s \n",xmlFiles.length, mutationsDir);
        TraceRunner traceRunner = new TraceRunner(strategy, csvTraces, tracesDir);
        for (int i = 0; i < Objects.requireNonNull(xmlFiles).length - 1; i++) {
            for (int j = i + 1; j < xmlFiles.length; j++) {
                File file1 = xmlFiles[i];
                File file2 = xmlFiles[j];
                Runnable tronTask = (() -> {
                    traceRunner.runTrace(file1, file2, mutationsDir);
                    traceRunner.runTrace(file2, file1, mutationsDir);

                    bisimRunner.scheduleJob(
                            new File(mutationsDir+"/compositions", file1.getName()),
                            new File(mutationsDir+"/compositions", file2.getName()));
                });
                wrapUp(tronTask);
            }
        }

        bisimRunner.shutdownJobs();
        executorService.shutdown();
    }

    private void execTraceMatchRRSingles(String strategy) {
        File directory = new File(mutationsDir);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        assert xmlFiles != null;
        System.out.printf("%s files in %s \n",xmlFiles.length, mutationsDir);
        TraceRunner traceRunner = new TraceRunner(strategy, csvTraces, tracesDir);
        for (int i = 0; i < Objects.requireNonNull(xmlFiles).length - 1; i++) {
            for (int j = i + 1; j < xmlFiles.length; j++) {
                traceRunner.runTrace(xmlFiles[i], xmlFiles[j], mutationsDir);
                traceRunner.runTrace(xmlFiles[j], xmlFiles[i], mutationsDir);
            }
        }
    }


    public void execBisimCheckEquivalent() {
        File ntaProduct = preprocessor.computeNTAProduct(model, mutationsDir); // product of the SUT
        File directory = new File(mutationsDir); // <-- points to compositions dir
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        assert xmlFiles != null;
        for (int i = 0; i < Objects.requireNonNull(xmlFiles).length; i++) {
            File file1 = xmlFiles[i];
            if (!file1.getName().equals(ntaProduct.getName())) {
                bisimRunner.scheduleJob(file1, ntaProduct);
            }
        }
        bisimRunner.shutdownJobs();
        executorService.shutdown();
    }

    public void checkDuplicates() {
        File directory = new File(mutationsDir);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));
        assert xmlFiles != null;
        Set<Set<File>> pairs = Sets.combinations(Sets.newHashSet(xmlFiles), 2);
        pairs.forEach(pair -> {
            File[] files = pair.toArray(new File[2]);
            // don't care about the model
            if (!files[0].getName().equals(model.getName()) && !files[1].getName().equals(model.getName()))
                bisimRunner.scheduleJob(files[0], files[1]);
        });
        bisimRunner.shutdownJobs();
        executorService.shutdown();
    }

    private void wrapUp(Runnable tronTask) {
        Future foo = executorService.submit(tronTask);
        try {
            foo.get();
            //printCSV();
        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }
    }

    private void prepareTracesCSV() {
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
