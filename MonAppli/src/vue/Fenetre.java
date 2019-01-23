/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package vue;

import dao.DataSource;
import java.io.IOException;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Application;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.Event;
import javafx.scene.Scene;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn.CellDataFeatures;
import javafx.scene.control.TablePosition;
import javafx.scene.control.Tooltip;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.DragEvent;
import javafx.scene.input.Dragboard;
import javafx.scene.input.MouseEvent;
import javafx.scene.input.TransferMode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Callback;
import javafx.util.Duration;
import metier.Concours;
import metier.Film;
import metier.Horaire;
import metier.Projection;
import metier.Salle;
import metier.Salle_Concours;
import org.controlsfx.control.Notifications;

/**
 *
 * @author yohann
 */
public class Fenetre extends Application {
    
    private PlanningHelper planningHelper;
    private TableView table; 
    private ListView<String> listeFilmAPlacer;
    private ListView<String> listeFilmPlacable;
    private ComboBox affichage;
    private ObservableList<String> jcb2List;
    private Label jcb2Text;
    private ComboBox jcb2;
    
    public static void main(String args[]) {
        launch(args);
    }
    
    @Override
    public void start(Stage stage) {
        
        connectionInit();
        
        Scene scene = new Scene(new Group());
        stage.setTitle("Gestion du planning des projections");
        // stage.setWidth(1000);
        stage.setMinWidth(990);
        // stage.setHeight(480);
        stage.setMinHeight(520);
        
        BorderPane border = generateBorderPane();
        VBox v1 = generateListFilmAPlacer();
        VBox v2 = generateListFilmPlacable();
        refreshList();
        VBox v3 = generateHBListFilm(v1, v2);
        HBox hbox = assemblyAll(v3, border);
        
        ((Group) scene.getRoot()).getChildren().addAll(hbox);
        
        scene.getStylesheets().add("styles/styles.css");
        stage.setScene(scene);
        stage.show();
        if(planningHelper.isPlanningFromBD())
        {
            Notifications.create()
                .position(Pos.BOTTOM_RIGHT)
                .title("Planning récupéré")
                .text("Un planning sauvegardé a été récupéré !")
                .showInformation();
        } else {
            if(planningHelper.estGenerable())
            {
                Notifications.create()
                .position(Pos.BOTTOM_RIGHT)
                .title("Planning généré")
                .text("Un planning a été automatiquement généré avec succès !")
                .showInformation();
            } else {
                Notifications.create()
                .position(Pos.BOTTOM_RIGHT)
                .title("Planning non généré")
                .text("Les informations actuelles du festival ne "
                        + "permettent pas de générer un planning correct !")
                .showError();
            }
        }
    }
    
    private VBox generateListFilmAPlacer() {
        Label labelL1 = new Label("Film qu'il faut encore placer :");
        
        listeFilmAPlacer = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList ();
        listeFilmAPlacer.setItems(items);
        listeFilmAPlacer.setPrefWidth(180);
        listeFilmAPlacer.setPrefHeight(200);
        
        listeFilmAPlacer.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable,
                    String oldValue, String newValue) {
                refreshTable();
            }
        });
        
        
        listeFilmAPlacer.setOnDragDropped((DragEvent event) -> {
            boolean success;
            TablePosition tp = (TablePosition)table.getSelectionModel().getSelectedCells().get(0);
            if(affichage.getSelectionModel().getSelectedIndex() == 0)
                success = planningHelper.supprimerProjection(jcb2.getSelectionModel().getSelectedIndex()+1, tp.getColumn()-1, tp.getRow());
            else
                success = planningHelper.supprimerProjection(tp.getColumn(), jcb2.getSelectionModel().getSelectedIndex(), tp.getRow());
            if (success && event.getGestureSource() != listeFilmAPlacer && event.getGestureSource() != listeFilmPlacable &&
                    event.getDragboard().hasString()) {
                refreshTable();
                refreshList();
                success = true;
                Notifications.create()
                    .position(Pos.BOTTOM_RIGHT)
                    .title("Projection suppprimé")
                    .text("La projection du film " + event.getDragboard().getString() + " a été supprimé !")
                    .showInformation();
            } else {
                Notifications.create()
                            .position(Pos.BOTTOM_RIGHT)
                            .title("Projection non supprimée")
                            .text("La projection du film " + event.getDragboard().getString()
                                    + " n'a pas été supprimée : supprimez d'abord la séance du lendemain associée à ce film !")
                            .showError();
            }
            event.setDropCompleted(success);
            event.consume();
        });
        
        listeFilmAPlacer.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(item);
                        if(!empty)
                        {   
                            Film film = null;
                
                            List<Film> liste = planningHelper.getListeFilm();

                            for (Film f : liste)
                                if (item.equals(f.getTitre()))
                                {
                                    film = f;
                                    break;
                                }
                            
                            Concours c = film.getConcours();
                            List<Salle_Concours> sc = planningHelper.getlisteSalle_Concours();
                            List<Salle> ls = new ArrayList<Salle>();

                            for (Salle_Concours s : sc)
                                if (s.getConcours() == c)
                                    ls.add(s.getSalle());

                            String msg = "";
                            for (int i = 0; i < ls.size(); i++)
                                msg += i == 0 ? ls.get(i).getNom() : ", " + ls.get(i).getNom();

                            Tooltip tooltip = new Tooltip("Plaçable dans les salles : " + msg);
                            hackTooltipStartTiming(tooltip);
                            setTooltip(tooltip);
                        }
                    }
                    
                    public void hackTooltipStartTiming(Tooltip tooltip) {
                        try {
                            Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
                            fieldBehavior.setAccessible(true);
                            Object objBehavior = fieldBehavior.get(tooltip);

                            Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
                            fieldTimer.setAccessible(true);
                            Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

                            objTimer.getKeyFrames().clear();
                            objTimer.getKeyFrames().add(new KeyFrame(Duration.seconds(8)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                };
            }
        });
        
        listeFilmAPlacer.setOnDragOver((DragEvent event) -> {
            if (event.getGestureSource() != listeFilmAPlacer &&
                    event.getGestureSource() != listeFilmPlacable &&
                    event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        
        listeFilmAPlacer.setOnDragDetected((MouseEvent event) -> {
            ObservableList selected = listeFilmAPlacer.getSelectionModel().getSelectedItems();
            String s = (String) selected.get(0);
            // String val = (String) tablePosition.getTableColumn().getCellData(tablePosition.getRow());
            
            if(s != null && !s.equals("")){
                Dragboard db = listeFilmAPlacer.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                content.putString(s);
                db.setContent(content);
            }
        });
        
        listeFilmAPlacer.setOnDragDone((DragEvent event) -> {
            listeFilmAPlacer.getSelectionModel().clearSelection();
            refreshTable();
            event.consume();
        });
        
        VBox v1 = new VBox();
        v1.setSpacing(3);
        
        v1.getChildren().addAll(labelL1, listeFilmAPlacer);
        
        return v1;
    }
    
    private VBox generateListFilmPlacable() {
        
        Label labelL2 = new Label("Film placables dans cet écran :");
        
        listeFilmPlacable = new ListView<>();
        ObservableList<String> items = FXCollections.observableArrayList ();
        listeFilmPlacable.setItems(items);
        listeFilmPlacable.setPrefWidth(180);
        listeFilmPlacable.setPrefHeight(200);
        
        listeFilmPlacable.getSelectionModel().selectedItemProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> observable,
                    String oldValue, String newValue) {
                refreshTable();
            }
        });
        
        
        listeFilmPlacable.setCellFactory(new Callback<ListView<String>, ListCell<String>>() {
            @Override
            public ListCell<String> call(ListView<String> param) {
                return new ListCell<String>() {
                    @Override
                    public void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        setText(item);
                        if(!empty)
                        {   
                            Film film = null;
                
                            List<Film> liste = planningHelper.getListeFilm();

                            for (Film f : liste)
                                if (item.equals(f.getTitre()))
                                {
                                    film = f;
                                    break;
                                }
                            
                            Concours c = film.getConcours();
                            List<Salle_Concours> sc = planningHelper.getlisteSalle_Concours();
                            List<Salle> ls = new ArrayList<Salle>();

                            for (Salle_Concours s : sc)
                                if (s.getConcours() == c)
                                    ls.add(s.getSalle());

                            String msg = "";
                            for (int i = 0; i < ls.size(); i++)
                                msg += i == 0 ? ls.get(i).getNom() : ", " + ls.get(i).getNom();

                            Tooltip tooltip = new Tooltip("Plaçable dans les salles : " + msg);
                            hackTooltipStartTiming(tooltip);
                            setTooltip(tooltip);
                        }
                    }
                    
                    public void hackTooltipStartTiming(Tooltip tooltip) {
                        try {
                            Field fieldBehavior = tooltip.getClass().getDeclaredField("BEHAVIOR");
                            fieldBehavior.setAccessible(true);
                            Object objBehavior = fieldBehavior.get(tooltip);

                            Field fieldTimer = objBehavior.getClass().getDeclaredField("activationTimer");
                            fieldTimer.setAccessible(true);
                            Timeline objTimer = (Timeline) fieldTimer.get(objBehavior);

                            objTimer.getKeyFrames().clear();
                            objTimer.getKeyFrames().add(new KeyFrame(new Duration(250)));
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                };
            }
        });
        
        listeFilmPlacable.setOnDragDropped((DragEvent event) -> {
            boolean success;
            TablePosition tp = (TablePosition)table.getSelectionModel().getSelectedCells().get(0);
            if(affichage.getSelectionModel().getSelectedIndex() == 0)
                success = planningHelper.supprimerProjection(jcb2.getSelectionModel().getSelectedIndex()+1, tp.getColumn()-1, tp.getRow());
            else
                success = planningHelper.supprimerProjection(tp.getColumn(), jcb2.getSelectionModel().getSelectedIndex(), tp.getRow());
            if (success && event.getGestureSource() != listeFilmAPlacer &&
                    event.getDragboard().hasString()) {
                success = true;
                refreshTable();
                refreshList();
                Notifications.create()
                    .position(Pos.BOTTOM_RIGHT)
                    .title("Projection suppprimée")
                    .text("La projection du film " + event.getDragboard().getString() + " a été supprimée !")
                    .showInformation();
            } else {
                Notifications.create()
                    .position(Pos.BOTTOM_RIGHT)
                    .title("Projection non suppprimée")
                    .text("La projection du film " + event.getDragboard().getString() + " n'a pas été supprimée."
                            + " Essayez de supprimer la projection du lendemain associée avant !")
                    .showError();
            }
            event.setDropCompleted(success);
            event.consume();
        });
        
        listeFilmPlacable.setOnDragOver((DragEvent event) -> {
            if (event.getGestureSource() != listeFilmPlacable &&
                    event.getGestureSource() != listeFilmAPlacer &&
                    event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            event.consume();
        });
        
        listeFilmPlacable.setOnDragDetected((MouseEvent event) -> {
            ObservableList selected = listeFilmPlacable.getSelectionModel().getSelectedItems();
            String s = (String) selected.get(0);
            // String val = (String) tablePosition.getTableColumn().getCellData(tablePosition.getRow());
            
            if(s != null && !s.equals("")){
                Dragboard db = listeFilmPlacable.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                content.putString(s);
                db.setContent(content);
            }
        });
        
        listeFilmPlacable.setOnDragDone((DragEvent event) -> {
            listeFilmPlacable.getSelectionModel().clearSelection();
            refreshTable();
            event.consume();
        });
        
        VBox v2 = new VBox();
        v2.setSpacing(3);
        
        v2.getChildren().addAll(labelL2, listeFilmPlacable);
        
        return v2;
    }
    
    private VBox generateHBListFilm(VBox l1, VBox l2) {
        VBox VBFL = new VBox();
        VBFL.setSpacing(10);
        
        VBFL.getChildren().addAll(l1, l2);
        
        return VBFL;
    }

    private TableView generateTableProjection() {
        table = new TableView();
        table.setId("table");
        table.setEditable(true);
        table.setPrefSize(750, 300);
        table.setMaxWidth(750);
        table.getSelectionModel().setCellSelectionEnabled(true);
        
        refreshTable();
        
        table.setOnDragDetected((MouseEvent event) -> {
            ObservableList selected = table.getSelectionModel().getSelectedCells();
            TablePosition tablePosition = (TablePosition) selected.get(0);
            String val = (String) tablePosition.getTableColumn().getCellData(tablePosition.getRow());
            
            if(/*selected != null && */!val.equals("")){
                Dragboard db = table.startDragAndDrop(TransferMode.ANY);
                ClipboardContent content = new ClipboardContent();
                content.putString(val);
                db.setContent(content);
            }
        });
        
        table.setOnDragOver((DragEvent event) -> {
            if (event.getGestureSource() != table &&
                    event.getDragboard().hasString()) {
                event.acceptTransferModes(TransferMode.COPY_OR_MOVE);
            }
            
            event.consume();
        });
        
        table.setOnDragDropped((DragEvent event) -> {
            TableCell targetCell = getCell(event.getPickResult().getIntersectedNode());
            int x = table.getColumns().indexOf(targetCell.getTableColumn());
            int y = targetCell.getTableRow().getIndex();
            
            int success = 0;
            
            if (x!= 0 && event.getGestureSource() != table &&
                    event.getDragboard().hasString()) {
                if(affichage.getSelectionModel().getSelectedIndex() == 0)
                {
                    success = planningHelper.newProjection(event.getDragboard().getString(), jcb2.getSelectionModel().getSelectedIndex()+1, x-1, y);
                } else {
                    success = planningHelper.newProjection(event.getDragboard().getString(),x, jcb2.getSelectionModel().getSelectedIndex(), y);
                }
            }
            
            switch(success){
                    case -1:
                           Notifications.create()
                            .position(Pos.BOTTOM_RIGHT)
                            .title("Projection non créée")
                            .text("Cette projection du lendemain aurait été mal placée par "
                                    + "rapport à la projection officielle associée !")
                            .showError();
                    break;
                    case -2:
                    case -7:
                    case -8:
                           Notifications.create()
                            .position(Pos.BOTTOM_RIGHT)
                            .title("Projection non créée")
                            .text("Il y déjà une projection pour ce film et les films "
                                    + "de ce concours ne sont pas projetables plusieurs fois !")
                            .showError();
                    break;
                    case -3:
                           Notifications.create()
                            .position(Pos.BOTTOM_RIGHT)
                            .title("Projection non créée")
                            .text("Il faut d'abord placer une projection officielle avant "
                                    + "d'en placer une du lendemain !")
                            .showError();
                    break;
                    case -4:
                           Notifications.create()
                            .position(Pos.BOTTOM_RIGHT)
                            .title("Projection non créée")
                            .text("Ce film ne se projette pas dans cette salle !")
                            .showError();
                    break;
                    case -5:
                           Notifications.create()
                            .position(Pos.BOTTOM_RIGHT)
                            .title("Projection non créée")
                            .text("Il y a déjà une projection prévue au créneau choisi !")
                            .showError();
                    break;
                    case -6:
                           Notifications.create()
                            .position(Pos.BOTTOM_RIGHT)
                            .title("Projection non créée")
                            .text("Cette projection officielle aurait été mal placée par "
                                    + "rapport à la projection du lendemain associée !")
                            .showError();
                    break;
                    case 1:
                    case 2:
                    case 3:
                        Notifications.create()
                            .position(Pos.BOTTOM_RIGHT)
                            .title("Projection créée")
                            .text("La projection du film " + event.getDragboard().getString()
                                    + " a été créée avec succès !")
                            .showConfirm();
                }
            
            refreshList();
            event.setDropCompleted(success > 0);
            event.consume();
        });
        
        return table;
    }

    private HBox generateCBBar() {
        
        Label label1 = new Label("Affichage par");
        
        ObservableList<String> options = FXCollections.observableArrayList("Jour", "Salle");
        affichage = new ComboBox(options);
        affichage.getSelectionModel().select(0);              
        
        HBox HBJCB1 = new HBox();
        HBJCB1.setSpacing(10);
        
        HBJCB1.getChildren().addAll(label1, affichage);
        
        jcb2Text = new Label("Texte jcb2");
        
        jcb2List = FXCollections.observableArrayList("Choix");
        jcb2 = new ComboBox(jcb2List);
        jcb2.setPrefWidth(190);
        
        HBox HBJCB2 = new HBox();
        HBJCB2.setSpacing(10);
        
        HBox HBJCB3 = new HBox();
        HBJCB3.setSpacing(10);
        
        HBJCB2.getChildren().addAll(jcb2Text, jcb2);
        
        Button plann = new Button("Enregistrer");
        Tooltip tooltip = new Tooltip("Permet d'enregistrer ce planning en base de données et de générer le planning en PDF");
        plann.setTooltip(tooltip);
        
        HBJCB3.getChildren().add(plann);
        
        plann.setOnAction((ActionEvent event) -> {
            boolean succes = planningHelper.enregistrerPlanning();
            if(succes)
            {
                Notifications.create()
                    .position(Pos.BOTTOM_RIGHT)
                    .title("Planning enregistré")
                    .text("Le planning a été enregistré en base de données. Il était valide !")
                    .showInformation();
            } else {
                Notifications.create()
                    .position(Pos.BOTTOM_RIGHT)
                    .title("Planning enregistré")
                    .text("Le planning a été enregistré en base de données mais attention, il n'était pas terminé !")
                    .showInformation();
            }
        });
        
        HBox HBJCB4 = new HBox();
        HBJCB4.setSpacing(10);
        Button rege = new Button("Regénérer");
        Tooltip tooltip2 = new Tooltip("Régénère automatiquement un planning valide");
        rege.setTooltip(tooltip2);
        HBJCB4.getChildren().add(rege);
        
        rege.setOnAction((ActionEvent event) -> {
            planningHelper.generatePlanning();
            refreshList();
            refreshTable();
            Notifications.create()
                .position(Pos.BOTTOM_RIGHT)
                .title("Planning régénéré")
                .text("Un planning a été régénéré automatiquement !")
                .showInformation();
        });
        
        HBox HBJCB5 = new HBox();
        HBJCB5.setSpacing(10);
        Button supp = new Button("Supprimer");
        
        Tooltip tooltip3 = new Tooltip("Supprime le planning présent en base de données");
        supp.setTooltip(tooltip3);
        HBJCB5.getChildren().add(supp);
        
        supp.setOnAction((ActionEvent event) -> {
            planningHelper.deletePlanningFromBD();
            Notifications.create()
                .position(Pos.BOTTOM_RIGHT)
                .title("Planning supprimé")
                .text("Le planning enregistré en base de données a été supprimé !")
                .showInformation();
        });
        
        HBox hboxBouton = new HBox();
        hboxBouton.setPadding(new Insets(5, 12, 5, 12));
        hboxBouton.setSpacing(30);
 
        hboxBouton.getChildren().addAll(HBJCB1, HBJCB2, HBJCB5, HBJCB4, HBJCB3);
        updatecb2();
        
        affichage.setOnAction((Event ev) -> {
            listeFilmAPlacer.getSelectionModel().clearSelection();
            listeFilmPlacable.getSelectionModel().clearSelection();
            updatecb2();
            refreshList();
        });
        
        jcb2.setOnAction((Event ev) -> {
            listeFilmAPlacer.getSelectionModel().clearSelection();
            listeFilmPlacable.getSelectionModel().clearSelection();
            refreshTable();
            refreshList();
        });
        
        return hboxBouton;
    }

    private BorderPane generateBorderPane() {
        BorderPane border = new BorderPane();
        border.setTop(generateCBBar());
        border.setCenter(generateTableProjection());
        
        return border;
    }

    private HBox assemblyAll(VBox list, BorderPane border) {
        HBox hbox = new HBox();
        hbox.setPadding(new Insets(15, 12, 15, 12));
        hbox.setSpacing(10);
 
        hbox.getChildren().addAll(list, border);
        
        return hbox;
    }
    
    private void refreshTable() {
        Projection[][][] structureProjection = planningHelper.getStructureProjection();
        
        List<Horaire> listeHoraire = planningHelper.getListeHoraire();
         table.getColumns().removeAll(table.getColumns());
        
        if(affichage.getSelectionModel().getSelectedItem().toString().equals("Jour"))
        {
            table.getColumns().removeAll(table.getColumns());
            
            List<Salle> listeSalle = planningHelper.getListeSalle();
            
            for(int j = 0; j < listeSalle.size() + 1; j++)
            {
                final int p = j;
                TableColumn tc;
                if(p == 0)
                {
                    tc = new TableColumn();
                } else {
                    tc = new TableColumn(listeSalle.get(p-1).getNom());
                }
                
                tc.setCellValueFactory(new Callback<CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<ObservableList, String> param) {
                        
                        int taille = Integer.parseInt((String)param.getValue().get(param.getValue().size()-1));
                        tc.setId(String.valueOf(taille));
                        return new SimpleStringProperty(param.getValue().get(p).toString());
                    }
                });
                
                tc.setCellFactory(new Callback<TableColumn, TableCell>() {
                    
                    @Override
                    public TableCell call(TableColumn param) {
                        
                        return new TableCell<ObservableList, String>() {

                            @Override
                            public void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(item);
                                if(p - 1 != -1)
                                {
                                    if(listeFilmAPlacer.isFocused())
                                    {
                                        if(listeFilmAPlacer.getSelectionModel().getSelectedIndex() != -1)
                                        {
                                            int ind = Integer.parseInt(tc.getId());

                                            if(planningHelper.isPossible(listeFilmAPlacer.getSelectionModel().getSelectedItem(), p-1, jcb2.getSelectionModel().getSelectedIndex(), 
                                                    ind, affichage.getSelectionModel().getSelectedIndex()) > 0)
                                                this.setStyle("-fx-background-color: rgba(14,174,27,0.29);");
                                            else
                                                this.setStyle("-fx-background-color: rgba(221,31,28,0.40);");
                                        }
                                    } else if (listeFilmPlacable.isFocused()) 
                                    {
                                        if(listeFilmPlacable.getSelectionModel().getSelectedIndex() != -1)
                                        {
                                            int ind = Integer.parseInt(tc.getId());

                                            if(planningHelper.isPossible(listeFilmPlacable.getSelectionModel().getSelectedItem(), p-1, jcb2.getSelectionModel().getSelectedIndex(), 
                                                    ind, affichage.getSelectionModel().getSelectedIndex()) > 0)
                                                this.setStyle("-fx-background-color: rgba(14,174,27,0.29);");
                                            else
                                                this.setStyle("-fx-background-color: rgba(221,31,28,0.40);");
                                        }
                                    }
                                    
                                    if(!empty)
                                    {   
                                        Projection[][][] structureProjection = planningHelper.getStructureProjection();
                                        Projection pro = structureProjection[jcb2.getSelectionModel().getSelectedIndex()][p-1][Integer.parseInt(tc.getId())];
                                        
                                        if(pro != null)
                                        {
                                            Tooltip tooltip = new Tooltip(pro.getInfo());
                                            setTooltip(tooltip);
                                        }
                                    }
                                }
                            }
                        };
                    }
                });
                
                tc.setMinWidth(150);
                tc.setSortable(false);
                table.getColumns().add(tc);
            }
            
            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();
            
            int numJour = -1;
            
            if(jcb2.getSelectionModel().getSelectedItem() != null)
                numJour = Integer.parseInt((String)jcb2.getSelectionModel().getSelectedItem())-1;
            
            numJour = (numJour == -1) ? 0 : numJour;
            
            for(int i = 0; i < listeHoraire.size(); i++)
            {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(listeHoraire.get(i).getHoraire());
                
                for(int j = 0; j < listeSalle.size(); j++)
                {
                    if(structureProjection[numJour][j][i] != null)
                    {
                        row.add(structureProjection[numJour][j][i].getFilm().getTitre());
                    } else
                        row.add("");
                }
                row.add(String.valueOf(i));
                data.add(row);
            }
            
            table.setItems(data);
        } else {
            table.getColumns().removeAll(table.getColumns());

            List<Integer> listeJour = new ArrayList<>();
            
            for(int i = 1; i < 12; i++)
                listeJour.add(i);

            for(int j = 0; j < listeJour.size() + 1; j++)
            {
                final int p = j;
                TableColumn tc;
                if(p == 0)
                {
                    tc = new TableColumn();
                } else {
                    tc = new TableColumn("Jour " + listeJour.get(p-1));
                }
                tc.setCellValueFactory(new Callback<CellDataFeatures<ObservableList, String>, ObservableValue<String>>() {
                    @Override
                    public ObservableValue<String> call(CellDataFeatures<ObservableList, String> param) {
                        int taille = Integer.parseInt((String)param.getValue().get(param.getValue().size()-1));
                        tc.setId(String.valueOf(taille));
                        return new SimpleStringProperty(param.getValue().get(p).toString());
                    }
                });
                
                tc.setCellFactory(new Callback<TableColumn, TableCell>() {
                    
                    @Override
                    public TableCell call(TableColumn param) {
                        
                        return new TableCell<ObservableList, String>() {

                            @Override
                            public void updateItem(String item, boolean empty) {
                                super.updateItem(item, empty);
                                setText(item);
                                
                                if(p - 1 != -1)
                                {
                                    if(listeFilmAPlacer.isFocused())
                                    {
                                        if(listeFilmAPlacer.getSelectionModel().getSelectedIndex() != -1)
                                        {
                                            int ind = Integer.parseInt(tc.getId());
                                            if(planningHelper.isPossible(listeFilmAPlacer.getSelectionModel().getSelectedItem(), p-1, jcb2.getSelectionModel().getSelectedIndex(), 
                                                    ind, affichage.getSelectionModel().getSelectedIndex()) > 0)
                                                this.setStyle("-fx-background-color: rgba(14,174,27,0.29);");
                                            else
                                                this.setStyle("-fx-background-color: rgba(221,31,28,0.40);");
                                        }
                                    } else if (listeFilmPlacable.isFocused())
                                    {
                                        if(listeFilmPlacable.getSelectionModel().getSelectedIndex() != -1)
                                        {
                                            int ind = Integer.parseInt(tc.getId());
                                            if(planningHelper.isPossible(listeFilmPlacable.getSelectionModel().getSelectedItem(), p-1, jcb2.getSelectionModel().getSelectedIndex(), 
                                                    ind, affichage.getSelectionModel().getSelectedIndex()) > 0)
                                                this.setStyle("-fx-background-color: rgba(14,174,27,0.29);");
                                            else
                                                this.setStyle("-fx-background-color: rgba(221,31,28,0.40);");
                                        }
                                    }
                                    
                                    if(!empty)
                                    {   
                                        Projection[][][] structureProjection = planningHelper.getStructureProjection();
                                        Projection pro = structureProjection[p-1][jcb2.getSelectionModel().getSelectedIndex()][Integer.parseInt(tc.getId())];
                                        
                                        if(pro != null)
                                        {
                                            Tooltip tooltip = new Tooltip(pro.getInfo());
                                            setTooltip(tooltip);
                                        }
                                        
                                    }
                                }
                            }
                            
                        };
                    }
                });
                
                
                tc.setMinWidth(150);
                tc.setSortable(false);
                table.getColumns().add(tc);
            }

            ObservableList<ObservableList<String>> data = FXCollections.observableArrayList();

            int numSalle = -1;

            if(jcb2.getSelectionModel().getSelectedItem() != null)
                numSalle = jcb2.getSelectionModel().getSelectedIndex();

            numSalle = (numSalle == -1) ? 0 : numSalle;

            for(int i = 0; i < listeHoraire.size(); i++)
            {
                ObservableList<String> row = FXCollections.observableArrayList();
                row.add(listeHoraire.get(i).getHoraire());

                for(int j = 0; j < listeJour.size(); j++)
                {
                    if(structureProjection[j][numSalle][i] != null)
                    {
                        row.add(structureProjection[j][numSalle][i].getFilm().getTitre());
                    } else
                        row.add("");
                }
                row.add(String.valueOf(i));
                data.add(row);
            }
            table.setItems(data);
        }
    }
    
    private void refreshList() {
        
        ObservableList<String> itemFilmAPlacer = FXCollections.observableArrayList ();
        itemFilmAPlacer.addAll(planningHelper.generateListeFilmAPlacer());
        listeFilmAPlacer.setItems(itemFilmAPlacer);
        listeFilmAPlacer.refresh();

        ObservableList<String> itemFilmPlacable = FXCollections.observableArrayList ();
        itemFilmPlacable.addAll(planningHelper.generateListeFilmPlacable(
                affichage.getSelectionModel().getSelectedIndex() != 0, 
                jcb2.getSelectionModel().getSelectedIndex()));
        listeFilmPlacable.setItems(itemFilmPlacable);
        listeFilmPlacable.refresh();
    }

    private void connectionInit() {
        Connection connection = null;
        
        try {
            connection = DataSource.getMysqlDataSource().getConnection();
        } catch (SQLException | IOException e) {
            e.printStackTrace();
        }
        
        planningHelper = new PlanningHelper(connection);
    }

    private void updatecb2() {
        String modeA = affichage.getSelectionModel().getSelectedItem().toString();

        if(modeA.equals("Jour"))
        {
            jcb2Text.setText("Jour");
            jcb2List.removeAll(jcb2List);
            for(int i = 1; i<12; i++)
                jcb2List.add(String.valueOf(i));
        } else {
            List<Salle> listeSalle = planningHelper.getListeSalle();
            jcb2Text.setText("Salle");
            jcb2List.removeAll(jcb2List);
            for(int i = 0; i < listeSalle.size(); i++)
                jcb2List.add(listeSalle.get(i).getNom());
        }
        jcb2.getSelectionModel().select(0);
    }
    
    private static TableCell getCell(Node node) {
        while (node != null && !(node instanceof TableCell))
            node = node.getParent();
        return (TableCell) node;
    }
    
}
