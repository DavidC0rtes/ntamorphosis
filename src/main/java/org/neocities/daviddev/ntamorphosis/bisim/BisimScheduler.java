package org.neocities.daviddev.ntamorphosis.bisim;

import org.neocities.daviddev.ntamorphosis.workers.TracesProvider;
import org.neocities.daviddev.tracematcher.entrypoint.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class BisimScheduler implements Supplier<Boolean> {
    private final File aFile, bFile;
    private final String template;
    private static final Logger logger = LoggerFactory.getLogger(BisimScheduler.class);

    public BisimScheduler(File a, File b) {
        aFile = a; bFile = b;
        template = "";
    }

    @Override
    public Boolean get() {
 //       System.out.printf("Calling bisimulation with %s and %s\n", aFile.getAbsolutePath(), bFile.getAbsolutePath());
//        NTA nta1 = new NTA(aFile.getAbsolutePath());
//        NTA nta2 = new NTA(bFile.getAbsolutePath());

//        System.out.printf("%s has %d locations and %d transitions\n", aFile.getName(), nta1.getAutomata().get(0).getLocations().size(), nta1.getAutomata().get(0).getTransitions().size());
//        System.out.printf("%s has %d locations and %d transitions\n", bFile.getName(), nta2.getAutomata().get(0).getLocations().size(), nta2.getAutomata().get(0).getTransitions().size());

        ProcessBuilder pb = new ProcessBuilder("java", "-jar",
                "/home/david/Documents/ImplThesis_muted/ImplThesis2/out/artifacts/ImplThesis_jar2/ImplThesis.jar",
                aFile.getAbsolutePath(),
                bFile.getAbsolutePath()
        ).redirectErrorStream(true);

        String result;
        try {
            logger.info("Waiting for bisimulation to return, {} {}", aFile.getName(), bFile.getName());
            Process p = pb.start();
            p.waitFor(120, TimeUnit.MINUTES);
            result = new String(p.getInputStream().readAllBytes());
            if (result.isEmpty()) {
                logger.error("Bisimulation algorithm returned empty string with files" +
                        " {} and {}.", aFile.getAbsolutePath(), bFile.getAbsolutePath());
            }
            p.destroy();
            return result.contains("Result of bisimulation check: true");
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }

    }
}
