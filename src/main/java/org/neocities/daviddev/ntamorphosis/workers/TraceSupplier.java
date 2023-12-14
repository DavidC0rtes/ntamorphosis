package org.neocities.daviddev.ntamorphosis.workers;

import org.neocities.daviddev.ntamorphosis.bisim.BisimScheduler;
import org.neocities.daviddev.ntamorphosis.entrypoint.AppConfig;
import org.neocities.daviddev.tracematcher.entrypoint.Runner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.*;
import java.util.function.Supplier;

public class TraceSupplier implements Supplier<String[]>{
    private final File model1, model2;
    private final String strategy, propDir;
    private final String tracesDir;

    public TraceSupplier(File f1, File f2, String strategy, String propDir, String tracesDir) {
        model1 = f1; model2 = f2;
        this.strategy = strategy;
        this.propDir = propDir;
        this.tracesDir = tracesDir;
    }


    /**
     * @Override
     * @return String[] representing a row to be written in the traces csv.
     */
    public String[] get() {
        HashMap<String, String[]> result = runTracesSimulation();

        return hashMapToString(result);
    }

    /**
     *
     * @return String[] representing a row to be written in the traces csv.
     */
    public String[] getRandom (int n, int k) {
        HashMap<String, String[]> result = runRandTracesSimulation(n,k);

        return hashMapToString(result);
    }

    private HashMap<String, String[]> runTracesSimulation() {
        Runner runner1 = new Runner(
                model1, model2,
                AppConfig.getInstance().getTronPath(),
                AppConfig.getInstance().getVerifyTAPath(),
                tracesDir
        );
        runner1.parseModels();
        runner1.computeTraces(propDir, strategy);
        runner1.parseTraces(strategy);
        runner1.simulateTraces();
        return runner1.getTracesResult();
    }
    private HashMap<String, String[]> runRandTracesSimulation(int nTraces, int timeBound) {
        Runner runner1 = new Runner(
                model1, model2,
                AppConfig.getInstance().getTronPath(),
                AppConfig.getInstance().getVerifyTAPath(),
                tracesDir
        );
        runner1.parseModels();
        runner1.computeRandomTraces(propDir, nTraces, timeBound);
        runner1.parseTraces(strategy);
        runner1.simulateTraces();
        return runner1.getTracesResult();
    }


    private String[] hashMapToString(HashMap<String, String[]> hashMap) {
        if (hashMap.size() > 1) {
            System.out.printf("Hashmap has %d entries\n", hashMap.size());
            throw new IllegalArgumentException("HashMap must contain exactly one entry.");
        } else if (hashMap.size() < 1) {
            return new String[]{};
        }
        Map.Entry<String, String[]> entry = hashMap.entrySet().iterator().next();

        String[] keyArr = new String[]{entry.getKey()};
        String[] result = Arrays.copyOf(keyArr, keyArr.length + entry.getValue().length);
        System.arraycopy(entry.getValue(), 0, result, keyArr.length, entry.getValue().length);

        return result;
    }
}
