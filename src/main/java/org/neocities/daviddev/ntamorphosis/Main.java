package org.neocities.daviddev.ntamorphosis;

import org.neocities.daviddev.ntamorphosis.entrypoint.Runner;
import org.neocities.daviddev.ntamorphosis.gui.Invoker;
import picocli.CommandLine;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

import java.io.File;

@Command(name = "NTAMorphosis", version = "0.1", mixinStandardHelpOptions = true)
public class Main implements Runnable {
    @Option(names = {"--model"}, description = "Path to model file")
    File model;

    @Option(names = {"-op", "--operators"})
    String[] operators;

    @Option(names = "-all", description = "Run with all mutation operators")
    boolean runAll;

    @Option(names = {"-p","--path"}, description = "Path were the models are", defaultValue = "src/main/resources/mutations")
    String outPath;

    @Option(names = {"-csv","--csv-path"}, description = "Csv path", defaultValue = "traces-result.csv")
    String csvPath;

    @Option(names = {"-csvb","--csv-bisim"}, description = "Csv path for bisim report", defaultValue = "results_bisim.csv")
    String csvBisim;

    @Option(names={"-gui"}, description = "Use gui", defaultValue = "true")
    boolean gui;

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        Runner runner;
        if (gui) {
            new Invoker();
        }
        else if (runAll) {
            runner = new Runner(model, outPath, csvPath, csvBisim);
        } else {
            runner = new Runner(outPath, csvPath, csvBisim);
        }
    }
}