package org.neocities.daviddev.ntamorphosis.workers;

import de.tudarmstadt.es.juppaal.NTA;

import java.io.File;

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

    public File composeNTA(NTA nta) {
        //todo do product, preserve syncs (how????)
        return null;
    }
}
