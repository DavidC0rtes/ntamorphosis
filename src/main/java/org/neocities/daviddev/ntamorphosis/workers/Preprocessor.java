package org.neocities.daviddev.ntamorphosis.workers;

import de.tudarmstadt.es.juppaal.NTA;

import java.io.File;

public class Preprocessor {
    private final NTA nta;
    private final File model;
    public Preprocessor(File model) {
        this.nta = new NTA(model.getAbsolutePath());
        this.model = model;
    }

    public void addTauChannel() {
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
}
