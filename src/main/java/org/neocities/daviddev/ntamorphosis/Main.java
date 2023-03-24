package org.neocities.daviddev.ntamorphosis;

import org.neocities.daviddev.ntamorphosis.entrypoint.Runner;
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

    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Working Directory = " + System.getProperty("user.dir"));

        Runner runner;
        if (runAll) {
            runner = new Runner(model, outPath, csvPath);
        } else {
            runner = new Runner(outPath, csvPath);
        }
    }
}