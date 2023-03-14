package org.neocities.daviddev.ntamorphosis.pairprocessing;

import com.google.common.io.Files;
import org.neocities.daviddev.simmdiff.entrypoint.Runner;

import java.io.File;
import java.util.HashMap;

public class XMLFileProcessor {

    public void processXmlFilePairs(String directoryPath, HashMap<String, String[]> resultsTron) {
        File directory = new File(directoryPath);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));

        for (int i = 0; i < xmlFiles.length - 1; i++) {
            for (int j = i + 1; j < xmlFiles.length; j++) {
                File file1 = xmlFiles[i];
                File file2 = xmlFiles[j];

                runSimmDiff(file1, file2, resultsTron);
            }
        }
    }

    public void processXmlFilePairs(File file1, File file2, HashMap<String, String[]> resultsTron) {

        runSimmDiff(file1, file2, resultsTron);
    }

    public void runSimmDiff(File file1, File file2, HashMap<String, String[]> resultsTron) {
        Runner runner1 = new Runner(file1, file2);

        runner1.parseModels();
        runner1.parseTraces();
        runner1.simulateTraces();


        resultsTron.putAll(runner1.getTracesResult());

        Runner runner2 = new Runner(file2, file1);

        runner2.parseModels();
        runner2.parseTraces();
        runner2.simulateTraces();


        resultsTron.putAll(runner2.getTracesResult());
    }
}
