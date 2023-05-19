package org.neocities.daviddev.ntamorphosis.bisim;

import de.tudarmstadt.es.juppaal.NTA;
import evaluation.BisimulationTest1;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class BisimSchedulerV2 implements Runnable{
    private final File aFile, bFile;
    private final String pathToCsv;

    public BisimSchedulerV2(File a, File b, String pathToCsv) {
        aFile = a; bFile = b;
        this.pathToCsv = pathToCsv;
    }

    @Override
    public void run() {
        long start = System.currentTimeMillis();
        System.out.printf("Calling bisimulation with %s and %s\n", aFile.getAbsolutePath(), bFile.getAbsolutePath());
        NTA nta1 = new NTA(aFile.getAbsolutePath());
        NTA nta2 = new NTA(bFile.getAbsolutePath());

        System.out.printf("%s has %d locations and %d transitions\n", aFile.getName(), nta1.getAutomata().get(0).getLocations().size(), nta1.getAutomata().get(0).getTransitions().size());
        System.out.printf("%s has %d locations and %d transitions\n", bFile.getName(), nta2.getAutomata().get(0).getLocations().size(), nta2.getAutomata().get(0).getTransitions().size());

        boolean bisimilar = new BisimulationTest1(aFile, bFile, 42).run();

        writeResults(bisimilar, System.currentTimeMillis() - start);
    }

    private synchronized void writeResults(boolean bisimilar, long elapsedTime) {
        try (CSVPrinter printer = new CSVPrinter(new FileWriter(pathToCsv, true), CSVFormat.DEFAULT)){
            printer.printRecord(aFile.getName(), bFile.getName(), bisimilar, elapsedTime);
            printer.flush();
            System.out.println("Wrote to "+pathToCsv);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
