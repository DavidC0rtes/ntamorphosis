package org.neocities.daviddev.ntamorphosis.bisim;

import de.tudarmstadt.es.juppaal.Automaton;
import de.tudarmstadt.es.juppaal.NTA;
import de.tudarmstadt.es.juppaal.SystemDeclaration;
import eval.Bisimulation;

import java.io.File;
import java.util.List;
import java.util.concurrent.Callable;

public class BisimScheduler implements Callable<Boolean> {
    private final String pathToA, pathtoB;

    public BisimScheduler(String a, String b) {
        pathtoB = b; pathToA = a;
    }
    @Override
    public Boolean call() {
        File aFile = getNTAProduct(new File(pathToA));
        File bFile = getNTAProduct(new File(pathtoB));

        System.out.printf("Calling bisimulation with %s and %s\n", aFile.getAbsolutePath(), bFile.getAbsolutePath());
        new Bisimulation(aFile, bFile, 42).run();
        return false;
    }

    private synchronized File getNTAProduct(File model) {
        NTA nta = new NTA(model.getAbsolutePath());
        List<Automaton> automatonList = nta.getAutomata();
        Automaton B = automatonList.get(0);
        for (int i = 1; i < automatonList.size(); i++) {
            Automaton A = automatonList.get(i);
            B = new Automaton(A, B, "NTAProduct");
        }

        nta.getAutomata().clear();
        nta.addAutomaton(B);
        nta.setSystemDeclaration(new SystemDeclaration("system NTAProduct;"));


        File outFile = new File(model.getParent()+"/"+model.getName()+"_product.xml");
        nta.writeModelToFile(outFile.getAbsolutePath());
        return outFile;
    }
}
