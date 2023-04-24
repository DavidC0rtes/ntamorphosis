package org.neocities.daviddev.ntamorphosis.gui;

import javafx.application.Application;

public class Invoker {
    public Invoker() {
        try {
            Application.launch(MainUI.class);
        } catch( Exception ex ) {
            System.err.println(ex.getCause().getMessage());
        }
    }
}
