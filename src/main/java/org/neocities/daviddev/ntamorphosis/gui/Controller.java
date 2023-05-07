package org.neocities.daviddev.ntamorphosis.gui;


import javafx.event.ActionEvent;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ChoiceBox;
import javafx.scene.layout.HBox;
import javafx.stage.DirectoryChooser;
import javafx.stage.Stage;

public class Controller {
    public ChoiceBox<String> choiceBox, operatorsChoiceBox = null;
    public HBox mutantsForm, scoreForm = null;
    public Button selectDirectory;
    private final DirectoryChooser dirChooser = new DirectoryChooser();
    public void handleNextBtn(javafx.event.ActionEvent actionEvent) {
        switch (choiceBox.getValue()) {
            case "Generate mutants and mutation score":
                mutantsForm.setVisible(true);
                break;
            case "Only mutation score":
                scoreForm.setVisible(true);
                break;
        }
    }

    public void handleOperatorsChoice(ActionEvent a) {
        if (operatorsChoiceBox.getValue().equals("Cherry pick")) {
            System.out.println("|:");
        }
    }

    public void handleDirChooseBtn(ActionEvent a) {
        Node node = (Node) a.getSource();
        Stage thisStage = (Stage) node.getScene().getWindow();
        dirChooser.showDialog(thisStage);
    }
}
