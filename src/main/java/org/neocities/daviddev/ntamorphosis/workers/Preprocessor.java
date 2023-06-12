package org.neocities.daviddev.ntamorphosis.workers;

import be.unamur.uppaal.juppaal.*;

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

    public File computeNTAProduct(File model, String path) {
        NTA nta = new NTA(model.getAbsolutePath());
        List<Automaton> automatonList = nta.getAutomata();
        if (automatonList.size() > 1) {
            Automaton B = automatonList.get(0);
            for (int i = 1; i < automatonList.size(); i++) {
                Automaton A = automatonList.get(i);
                B = new Automaton(A, B, "NTAProduct");
            }
            // Make uppaal figure out the coordinates
            B.getLocations().forEach(location -> {
                location.setPositioned(false);
                if (location.getInvariant() != null) {
                    location.getInvariant().setPositioned(false);
                }

                location.getOutgoingTransitions().forEach(transition -> {
                    transition.setPositioned(false);
                });

                location.getIncommingTransitions().forEach(transition -> {
                    transition.setPositioned(false);
                });
            });

            // not interested about this kind of transitions.
            B.getTransitions().removeIf(transition -> isTauTransition(transition) && isLoopTransition(transition));
            nta.getAutomata().clear();
            nta.addAutomaton(B);
            nta.setSystemDeclaration(new SystemDeclaration("system NTAProduct;"));
        }
        File outFile = new File(path+"/"+model.getName());
        nta.writeModelToFile(outFile.getAbsolutePath());

        return outFile;
    }

    private boolean isTauTransition(Transition transition) {
        if (transition.getSync() != null && !transition.getSync().toString().isEmpty()) {
            return false;
        }

        if (transition.getGuard() != null && !transition.getGuardAsString().isEmpty()) {
            return false;
        }

        if (transition.getUpdate() != null && !transition.getUpdate().toString().isEmpty()) {
            return false;
        }

        return true;
    }

    private boolean isLoopTransition(Transition transition) {
        if (transition.getSource() != null && transition.getTarget() != null) {
            return transition.getSource().toString().equals(transition.getTarget().toString());
        }

        return false;
    }
}
