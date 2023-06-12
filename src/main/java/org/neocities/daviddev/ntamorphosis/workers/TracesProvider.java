package org.neocities.daviddev.ntamorphosis.workers;

import org.neocities.daviddev.simmdiff.entrypoint.Runner;

import java.io.File;

/**
 * Provides one single point of access to the biased traces object.
 */
public final class TracesProvider {
    private static volatile TracesProvider instance;
    private final Runner tracesRunner;

    private TracesProvider() {
        tracesRunner = new Runner();
    }

    public static TracesProvider getInstance() {
        TracesProvider result = instance;
        if (result != null) {
            return result;
        }

        synchronized (TracesProvider.class) {
            if (instance == null)
                instance = new TracesProvider();

            return instance;
        }
    }

    public boolean getTracesVeredict(File a, File b) {
        tracesRunner.setFiles(a, b);
        tracesRunner.parseModels();
        tracesRunner.computeTraces(System.getProperty("user.dir"), "biased");
        tracesRunner.parseTraces("biased");
        tracesRunner.simulateTraces();

        return tracesRunner.getVeredict();
    }

    public void closeProvider() {
        if (instance != null)
            tracesRunner.shutdownExecutor();
    }

}
