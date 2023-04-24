package org.neocities.daviddev.ntamorphosis.workers;

import de.tudarmstadt.es.juppaal.Automaton;
import de.tudarmstadt.es.juppaal.NTA;
import de.tudarmstadt.es.juppaal.SystemDeclaration;

import java.io.File;
import java.util.List;

public class Preprocessor {

    public void addTauChannel(File model) {
        NTA nta = new NTA(model.getAbsolutePath());
        boolean hasTauChannel = false;
        for (String declaration : nta.getDeclarations().getStrings()) {
            if (declaration.contains("broadcast chan") && declaration.contains("tau")) {
                hasTauChannel = true;
                break;
            }
        }

        if (!hasTauChannel) {
            nta.getDeclarations().add("broadcast chan tau;");
            nta.writeModelToFile(model.getAbsolutePath());
        }
    }

    public File getNTAProduct(File model) {
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
