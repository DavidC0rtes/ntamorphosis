package org.neocities.daviddev.ntamorphosis.bisim;

import evaluation.BisimulationTest1;

import java.io.File;
import java.util.concurrent.Callable;

public class Bisimulation implements Callable<Boolean> {
    private final String pathToA, pathtoB;

    public Bisimulation(String a, String b) {
        pathtoB = b; pathToA = a;
    }
    @Override
    public Boolean call() {
        return new BisimulationTest1(new File(pathToA), new File(pathtoB)).run();
        //@todo: process builder
        /*ProcessBuilder pb = new ProcessBuilder(
                "java",
                "-jar",
                "/home/david/Documents/ImplThesis2/out/artifacts/ImplThesis_Slim/ImplThesis.jar",
                pathToA, pathtoB
        ).inheritIO();
        try {
            pb.start();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }*/
    }
}
