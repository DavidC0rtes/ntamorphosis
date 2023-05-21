package org.neocities.daviddev.ntamorphosis;

import org.neocities.daviddev.ntamorphosis.entrypoint.Runner;
import org.neocities.daviddev.ntamorphosis.gui.Invoker;
import picocli.CommandLine.*;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

@Command(name = "NTAMorphosis", version = "0.1", mixinStandardHelpOptions = true)
public class Main implements Runnable {
    enum STRATEGIES { random, biased }
    @Option(names = {"-model"}, description = "Path to model file")
    File model;

    @Option(names = {"-op", "--operators"})
    ArrayList<String> operators = new ArrayList<>();

    @Option(names = {"-dir","--mutants-dir"}, description = "Path were the mutants are", defaultValue = "src/main/resources/mutations")
    String outPath;

    @Option(names = {"-csv","--csv-path"}, description = "Csv path", defaultValue = "traces-result.csv")
    String csvPath;

    @Option(names = {"-csvb","--csv-bisim"}, description = "Csv path for bisim report", defaultValue = "results_bisim.csv")
    String csvBisim;

    @Option(names = {"-eq", "--equivalent"}, description = "Compute bisimulation w/ respect to the original model", defaultValue = "false")
    boolean getEquivalent;

    @Option(names = {"-dup", "--duplicates"}, description = "Compute bisimulation between each mutant", defaultValue = "false")
    boolean getDuplicates;

    @Option(names={"-gui"}, description = "Use gui", defaultValue = "true")
    boolean gui;
    @Option(names = "-how", description = "How to generate traces, one of: ${COMPLETION-CANDIDATES}", defaultValue = "biased")
    STRATEGIES strategy;

    public static void main(String[] args) {
        CommandLine commandLine = new CommandLine(new Main());
        commandLine.setCaseInsensitiveEnumValuesAllowed(true);

        int exitCode = commandLine.execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));
        if (gui) {
            new Invoker();
        } else if (getEquivalent || getDuplicates) { // hacer mutaciones y equivalentes
            Runner runner = new Runner(operators, model, outPath, csvBisim);
            if (getDuplicates) runner.checkDuplicates();
            if (getEquivalent) runner.execBisimCheckEquivalent();
        } else { // hacer bisim y trazas (con)sin mutaciones, interpertar operadores.
            new Runner(operators, model, outPath, csvPath, csvBisim, strategy.toString());
        }
    }
}