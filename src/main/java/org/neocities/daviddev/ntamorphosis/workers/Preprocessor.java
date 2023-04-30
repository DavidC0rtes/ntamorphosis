package org.neocities.daviddev.ntamorphosis.workers;

import de.tudarmstadt.es.juppaal.*;
import de.tudarmstadt.es.juppaal.labels.Invariant;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.io.SAXReader;
import org.neocities.daviddev.simmdiff.core.ExtendedNTA;
import org.w3c.dom.DOMException;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static com.google.common.collect.Sets.cartesianProduct;

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
            B.getTransitions().removeIf(transition -> isTauTransition(transition));
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

        if (transition.getUpdate() == null && !transition.getUpdate().toString().isEmpty()) {
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
