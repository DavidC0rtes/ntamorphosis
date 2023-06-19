package org.neocities.daviddev.ntamorphosis.workers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

public class VerifierScheduler implements Supplier<Boolean> {
    private final String pathToVerifyTA;
    private final File model;
    private static final Logger logger = LoggerFactory.getLogger(VerifierScheduler.class);


    public VerifierScheduler(String pathToVerifyTA, File model) {
        this.pathToVerifyTA = pathToVerifyTA;
        this.model = model;
    }

    /**
     * @return 
     */
    @Override
    public Boolean get() {
        Random rand = new Random();
        ProcessBuilder processBuilder = new ProcessBuilder(
                pathToVerifyTA,
                "-r", Integer.toString(rand.nextInt()),
                "-q", model.getAbsolutePath()
        );

        Process process = null;
        try {
            process = processBuilder.start();
            logger.info("Waiting for verifyta to return on file {}", model.getAbsolutePath());
            process.waitFor(5, TimeUnit.MINUTES);
        } catch (IOException | InterruptedException e) {
            logger.error("Error starting and/or waiting for process with model {}.",model.getName(),e);
            System.exit(-1);
        }

        String output = "";
        String errOutput = "";
        try {
            output = new String(process.getInputStream().readAllBytes());
            errOutput = new String(process.getErrorStream().readAllBytes());
        } catch (IOException e) {
            logger.error("Error while reading verifyta's output with model {}.",model.getName(),e);
            System.exit(-2);
        }

        if (output.isEmpty() && !errOutput.isEmpty()) {
            logger.error("Got empty string when reading verifyta, file {}", model.getAbsolutePath());
            throw new IllegalStateException("The file "+ model.getAbsolutePath() + " has errors.");
        }

        return !output.contains("NOT satisfied") && !output.contains("MAY be satisfied");
    }
}
