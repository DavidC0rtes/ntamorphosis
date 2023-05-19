package org.neocities.daviddev.ntamorphosis.bisim;

import de.tudarmstadt.es.juppaal.NTA;
import evaluation.BisimulationTest1;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Supplier;

public class BisimScheduler implements Supplier<Boolean> {
    private final File aFile, bFile;
    private final String template;

    public BisimScheduler(File a, File b) {
        aFile = a; bFile = b;
        template = "";
    }

    @Override
    public Boolean get() {
        System.out.printf("Calling bisimulation with %s and %s\n", aFile.getAbsolutePath(), bFile.getAbsolutePath());
//        NTA nta1 = new NTA(aFile.getAbsolutePath());
//        NTA nta2 = new NTA(bFile.getAbsolutePath());

//        System.out.printf("%s has %d locations and %d transitions\n", aFile.getName(), nta1.getAutomata().get(0).getLocations().size(), nta1.getAutomata().get(0).getTransitions().size());
//        System.out.printf("%s has %d locations and %d transitions\n", bFile.getName(), nta2.getAutomata().get(0).getLocations().size(), nta2.getAutomata().get(0).getTransitions().size());

        ProcessBuilder pb = new ProcessBuilder("java", "-jar",
                "/home/david/Documents/ImplThesis_muted/ImplThesis2/out/artifacts/ImplThesis_jar2/ImplThesis.jar",
                aFile.getAbsolutePath(),
                bFile.getAbsolutePath()
        );

        pb.redirectErrorStream(true);

        String result;
        try {
            Process p = pb.start();
            p.waitFor();
            result = new String(p.getInputStream().readAllBytes());
            return result.contains("Result of bisimulation check: true");
        } catch (IOException | InterruptedException e) {
            throw new RuntimeException(e);
        }

        //return new BisimulationTest1(aFile, bFile, 42).run();
    }
}
