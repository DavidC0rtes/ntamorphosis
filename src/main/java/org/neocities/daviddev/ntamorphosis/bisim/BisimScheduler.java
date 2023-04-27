package org.neocities.daviddev.ntamorphosis.bisim;

import de.tudarmstadt.es.juppaal.NTA;
import evaluation.BisimulationTest1;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

public class BisimScheduler implements Callable<Boolean> {
    private final File aFile, bFile;

    public BisimScheduler(File a, File b) {
        aFile = a; bFile = b;
    }
    @Override
    public Boolean call() {
        System.out.printf("Calling bisimulation with %s and %s\n", aFile.getAbsolutePath(), bFile.getAbsolutePath());
        NTA nta1 = new NTA(aFile.getAbsolutePath());
        NTA nta2 = new NTA(bFile.getAbsolutePath());

        System.out.printf("%s has %d locations and %d transitions\n", aFile.getName(), nta1.getAutomata().get(0).getLocations().size(), nta1.getAutomata().get(0).getTransitions().size());
        System.out.printf("%s has %d locations and %d transitions\n", bFile.getName(), nta2.getAutomata().get(0).getLocations().size(), nta2.getAutomata().get(0).getTransitions().size());

        new BisimulationTest1(aFile, bFile, 42).run();
        return false;
    }
}
