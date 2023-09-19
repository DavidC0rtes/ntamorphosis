package org.neocities.daviddev.ntamorphosis;

import org.neocities.daviddev.ntamorphosis.entrypoint.Runner;
import org.neocities.daviddev.ntamorphosis.gui.Invoker;
import picocli.CommandLine.*;
import picocli.CommandLine;

import java.io.File;
import java.util.ArrayList;

@Command(name = "NTAMorphosis", version = "0.1", mixinStandardHelpOptions = true)
public class Main implements Runnable {
    enum STRATEGIES { random, biased }
    @Option(names = {"--model", "-m"}, description = "Path to model's file.")
    File model;

    @Option(names = {"-op", "--operators"}, description = "List of operators to apply.")
    ArrayList<String> operators = new ArrayList<>();

    @Option(names = {"-dir","--mutants-dir"}, description = "Directory containing mutant files.", defaultValue = "src/main/resources/mutations")
    String outPath;

    @Option(names = {"-csvt","--csv-traces"}, description = "Path to the output CSV file for TraceMatcher's results.", defaultValue = "traces-result.csv")
    String csvPath;

    @Option(names = {"-csvb","--csv-bisim"}, description = "Path to the output CSV file for bisimulation results.", defaultValue = "results-bisim.csv")
    String csvBisim;

    @Option(names = {"-eq", "--equivalent"}, description = "Compute bisimulation w/ respect to the original model.", defaultValue = "false")
    boolean getEquivalent;

    @Option(names = {"-dup", "--duplicates"}, description = "Compute bisimulation between mutants.", defaultValue = "false")
    boolean getDuplicates;

    @Option(names = "--gui", description = "Use the GUI.", defaultValue = "true", fallbackValue = "true", negatable = true)
    boolean gui;
    @Option(names = "--how", description = " Trace generation strategy (options: ${COMPLETION-CANDIDATES}).", defaultValue = "biased")
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