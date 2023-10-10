package org.neocities.daviddev.ntamorphosis.workers;

import org.neocities.daviddev.ntamorphosis.entrypoint.AppConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class VerifierScheduler implements Supplier<String> {
    private final File model;
    private static final Logger logger = LoggerFactory.getLogger(VerifierScheduler.class);


    public VerifierScheduler( File model) {
        this.model = model;
    }

    /**
     * @return 
     */
    @Override
    public String get() {
        String result = "timeout";
        Random rand = new Random();
        ProcessBuilder processBuilder = new ProcessBuilder(
                AppConfig.getInstance().getVerifyTAPath(),
                "-r", Integer.toString(rand.nextInt()),
                "-q", model.getAbsolutePath()
        );

        Process process = null;
        try {
            process = processBuilder.start();
            //logger.info("Waiting for verifyta to return on file {}", model.getAbsolutePath());
            if (process.waitFor(20, TimeUnit.MINUTES)) {
                result = String.valueOf(parseStream(process));
            } else {
                logger.info("verifyta timed out on file {}, attempting to destroy process.", model.getAbsolutePath());
            }
        } catch (IOException | InterruptedException e) {
            logger.error("Error starting and/or waiting for process with model {}.",model.getName(),e);
            System.exit(-1);
        } finally {
            assert process != null;
            process.destroy();
        }
        return result;
    }

    private boolean parseStream(Process p) {
        String output = "";
        String errOutput = "";
        try {
            output = new String(p.getInputStream().readAllBytes());
            errOutput = new String(p.getErrorStream().readAllBytes());
        } catch (IOException e) {
            logger.error("Error while reading verifyta's output with model {}.",model.getName(),e);
            System.exit(-2);
        }

        if (output.isEmpty() && !errOutput.isEmpty()) {
            /**
             * if there are not properties in the model, an error is thrown,
             * we don't consider that an irrecoverable error.
             * todo: actually check for this (empty queries).
             */
            logger.error("Got empty string when reading verifyta, file {} {}", model.getName(), errOutput);
            return true;
            //throw new IllegalStateException("The file "+ model.getAbsolutePath() + " has errors, or is empty.");
        }
        // there can be multiple satisfied and not satisfied queries in the system, thus...
        //todo: refactor this, count the number of queries, or run each query or process each line
        return output.contains("is satisfied") && !output.contains("MAY be satisfied")
                && !output.contains("MAY not be satisfied") && !output.contains("NOT satisfied")
                && !output.contains("aborted");
    }
}