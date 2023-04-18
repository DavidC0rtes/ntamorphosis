package org.neocities.daviddev.ntamorphosis.pairprocessing;

import org.neocities.daviddev.simmdiff.entrypoint.Runner;

import java.io.File;
import java.util.HashMap;
import java.util.Objects;

public class XMLFileProcessor {

    public void processXmlFilePairs(String directoryPath) {
        File directory = new File(directoryPath);
        File[] xmlFiles = directory.listFiles((dir, name) -> name.endsWith(".xml"));

        for (int i = 0; i < Objects.requireNonNull(xmlFiles).length - 1; i++) {
            for (int j = i + 1; j < xmlFiles.length; j++) {
                File file1 = xmlFiles[i];
                File file2 = xmlFiles[j];

                runSimmDiff(file1, file2, directoryPath);
                runSimmDiff(file2, file1, directoryPath);
            }
        }
    }

/*    public void processXmlFilePairs(File file1, File file2, HashMap<String, String[]> resultsTron) {

        runSimmDiff(file1, file2, resultsTron);
    }*/

    public HashMap<String, String[]> runSimmDiff(File file1, File file2, String propDir) {
        Runner runner1 = new Runner(file1, file2);
        runner1.parseModels(propDir);
        runner1.parseTraces();
        runner1.simulateTraces();

        return runner1.getTracesResult();
    }
}
