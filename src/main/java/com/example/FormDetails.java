package com.example;

import com.example.mongodb.FormDataManager;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.example.mongodb.MongoDBHelper;
import org.bson.Document;

import java.util.List;

public class FormDetails extends Application {

    private Document form;
    private MongoDBHelper mongoDBHelper;
    private String gmail;

    // Constructor to accept the form document
    public FormDetails(Document form, String gmail) {
        this.form = form;
        this.gmail = gmail;
        this.mongoDBHelper = new MongoDBHelper("mongodb://localhost:27017", "formB");
    }

    @Override
    public void start(Stage primaryStage) {
        primaryStage.setTitle("Form Details");

        VBox layout = new VBox(10);
        layout.setAlignment(Pos.CENTER);
        layout.setStyle("-fx-padding: 20;");

        String title = form.getString("formTitle");

        // Display the form title
        Label titleLabel = new Label("Form Title: " + title);
        layout.getChildren().add(titleLabel);

        FormDataManager formDataManager = new FormDataManager(mongoDBHelper, "question");

        Label responderLabel = new Label("Total No.of Responses : " + formDataManager.getResponseCount(title));
        layout.getChildren().add(responderLabel);

        // Create a TableView to display responders and scores
        TableView<ResponderScore> tableView = new TableView<>();

        // Create columns for the table
        TableColumn<ResponderScore, String> responderColumn = new TableColumn<>("Responder");
        responderColumn.setMinWidth(150);
        responderColumn.setCellValueFactory(new PropertyValueFactory<>("responder"));

        TableColumn<ResponderScore, String> scoreColumn = new TableColumn<>("Total Score");
        scoreColumn.setMinWidth(150);
        scoreColumn.setCellValueFactory(new PropertyValueFactory<>("totalScore"));

        // Add columns to the table
        tableView.getColumns().addAll(responderColumn, scoreColumn);

        // Create sorting buttons
        Button sortByNameButton = new Button("Sort by Name");
        Button sortByScoreButton = new Button("Sort by Score");

        // Load data without sorting initially
        loadResponders(tableView, title, null);

        // Sort by Responder name (alphabetically)
        sortByNameButton.setOnAction(e -> {
            loadResponders(tableView, title, "responder");
        });

        // Sort by Total Score (in decreasing order)
        sortByScoreButton.setOnAction(e -> {
            loadResponders(tableView, title, "totalScore");
        });

        // Add sorting buttons to the layout
        layout.getChildren().addAll(tableView, sortByNameButton, sortByScoreButton);

        // Create a Back button
        Button backButton = new Button("Back");
        backButton.setPrefWidth(100);
        backButton.setOnAction(e -> openDashboard(gmail, primaryStage)); // Go back to dashboard
        layout.getChildren().add(backButton);

        // Create a Scene and set it to full screen
        Scene scene = new Scene(layout, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true); // Set to full screen
        primaryStage.show();
    }

    // Method to load responders from MongoDB with optional sorting
    private void loadResponders(TableView<ResponderScore> tableView, String title, String sortBy) {
        tableView.getItems().clear();  // Clear existing items

        // Define sort order
        Document sortCriteria = null;
        if ("responder".equals(sortBy)) {
            sortCriteria = new Document("responder", 1); // Sort by responder name (ascending)
        } else if ("totalScore".equals(sortBy)) {
            sortCriteria = new Document("Total Score", -1); // Sort by score (descending)
        }

        // Query MongoDB for sorted responders
        List<Document> responders;
        if (sortCriteria != null) {
            responders = mongoDBHelper.getSortedResponders(title, sortCriteria);
        } else {
            responders = mongoDBHelper.getResponders(title);
        }

        // Populate TableView
        ObservableList<ResponderScore> data = FXCollections.observableArrayList();
        for (Document responder : responders) {
            String responderName = responder.getString("responder");
            String totalScore = responder.getString("Total Score");
            data.add(new ResponderScore(responderName, totalScore));
        }
        tableView.setItems(data);
    }

    // Method to go back to the Dashboard
    private void openDashboard(String gmail, Stage primaryStage) {
        new Dashboard(gmail).start(primaryStage); // Pass the appropriate Gmail or identifier
    }

    // Class to hold responder data
    public static class ResponderScore {
        private final String responder;
        private final String totalScore;

        public ResponderScore(String responder, String totalScore) {
            this.responder = responder;
            this.totalScore = totalScore;
        }

        public String getResponder() {
            return responder;
        }

        public String getTotalScore() {
            return totalScore;
        }
    }
}
