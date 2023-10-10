package org.neocities.daviddev.ntamorphosis.pairprocessing;

import org.neocities.daviddev.ntamorphosis.entrypoint.AppConfig;
import org.neocities.daviddev.tracematcher.entrypoint.Runner;

import java.io.File;
import java.util.HashMap;

public class XMLFileProcessor {

    private final String strategy;
    public XMLFileProcessor(String strategy) {
        this.strategy = strategy;
    }

    public HashMap<String, String[]> runSimmDiff(File file1, File file2, String propDir) {
        System.out.printf("Prop dir is %s\n", propDir);
        Runner runner1 = new Runner(file1, file2, AppConfig.getInstance().getTronPath(), AppConfig.getInstance().getVerifyTAPath(), "traces");
        runner1.parseModels();
        runner1.computeTraces(propDir, strategy);
        runner1.parseTraces(strategy);
        runner1.simulateTraces();

        return runner1.getTracesResult();
    }
}
