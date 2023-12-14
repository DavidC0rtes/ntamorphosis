package org.neocities.daviddev.ntamorphosis.bisim;

import org.neocities.daviddev.ntamorphosis.entrypoint.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class BisimScheduler implements Supplier<String> {
    private final File aFile, bFile;
    private final String template;
    private static final Logger logger = LoggerFactory.getLogger(BisimScheduler.class);

    public BisimScheduler(File a, File b) {
        aFile = a; bFile = b;
        template = "";
    }

    public  BisimScheduler(File a, File b, String template) {
        aFile = a; bFile = b;
        this.template = template;
    }

    @Override
    public String get() {
        Process process = startBisimulationProcess();
        if (process != null) {
            String result = waitForBisimulation(process);
            cleanupProcess(process);
            return result;
        } else {
            logger.error("Failed to start bisimulation for files {}, {}", aFile.getName(), bFile.getName());
            return "false";
        }
    }

    private Process startBisimulationProcess() {
        String bisimPath = AppConfig.getInstance().getBisimPath();
        //logger.info("Starting bisimulation for {} and {} template: {}",aFile,bFile, template);
        try {
            ProcessBuilder pb = new ProcessBuilder("java", "-jar",
                    bisimPath,
                    aFile.getAbsolutePath(),
                    bFile.getAbsolutePath(),
                    template
            ).redirectErrorStream(true);

            return pb.start();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    private String waitForBisimulation(Process process) {
        try {
            logger.info("Waiting for bisimulation to return, {} {}", aFile.getAbsolutePath(), bFile.getAbsolutePath());
            String veredict = "";
            if (process.waitFor(60, TimeUnit.MINUTES)) {
                veredict = new String(process.getInputStream().readAllBytes());
                if (veredict.isEmpty()) {
                    logger.error("Bisimulation algorithm returned empty string with files" +
                            " {} and {}.", aFile.getAbsolutePath(), bFile.getAbsolutePath());
                }
            } else {
                logger.info("Bisimulation timed out for files {} and {}", aFile.getName(), bFile.getName());
            }

            cleanupProcess(process);
            if (!veredict.isEmpty()) {
                return String.valueOf(veredict.contains("Result of bisimulation check: true"));
            }
            return "timeout";
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
            throw new RuntimeException(e);
        }
    }
    private void cleanupProcess(Process process) {
        assert process != null;
        process.destroy();
    }
}
