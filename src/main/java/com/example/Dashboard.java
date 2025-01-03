package com.example;

import javafx.geometry.Pos;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;  // Import HBox
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import com.example.mongodb.MongoDBHelper;
import org.bson.Document;

import java.util.List;

public class Dashboard {

    private String gmail;  // Store Gmail information
    private MongoDBHelper mongoDBHelper;

    // Constructor to accept Gmail
    public Dashboard(String gmail) {
        this.gmail = gmail;
        this.mongoDBHelper = new MongoDBHelper("mongodb://localhost:27017", "formB");
    }

    public void start(Stage primaryStage) {
        primaryStage.setTitle("Dashboard");

        // Create a box that represents the "Create Form" button (plus symbol)
        Rectangle createFormBox = new Rectangle(100, 100);
        createFormBox.setId("plus-box"); // Set CSS ID for styling
        createFormBox.setArcWidth(20);
        createFormBox.setArcHeight(20);

        // Create a label for the plus symbol
        Text plusSymbol = new Text("+");
        plusSymbol.setId("plus-symbol"); // Set CSS ID for styling
        plusSymbol.setFill(Color.WHITE); // Set text color

        // Layout for the plus symbol inside the box
        StackPane plusBox = new StackPane(createFormBox, plusSymbol);
        plusBox.setOnMouseClicked(e -> openFormApp(primaryStage));  // Navigate to FormApp on click

        // Create a GridPane to display form buttons
        GridPane formsGrid = new GridPane();
        formsGrid.setId("grid-pane"); // Set CSS ID for styling

        // Add the Create Form box to the grid
        formsGrid.add(plusBox, 0, 0);  // Add the create form button at the first position
        GridPane.setMargin(plusBox, new Insets(10)); // Add margin for spacing

        List<Document> forms = mongoDBHelper.getFormsByCreator(gmail);
        int col = 1;  // Start after the "Create Form" icon
        int row = 0;  // Row index

        // Create a button for each form name styled like the Create Form button
        for (Document form : forms) {
            // Create a rectangle for each form button with the same size as the create button
            Rectangle formButtonBox = new Rectangle(100, 100);
            formButtonBox.setId("form-button-box"); // Set CSS ID for styling
            formButtonBox.setArcWidth(20);
            formButtonBox.setArcHeight(20);

            // Create a button for the form name
            Button formButton = new Button(form.getString("formTitle"));
            formButton.setId("form-button"); // Set CSS ID for styling
            formButton.setOnAction(e -> showFormOptions(form)); // Show options when clicked

            // StackPane for the button and rectangle
            StackPane stackPane = new StackPane(formButtonBox, formButton);

            // Add the StackPane to the GridPane
            formsGrid.add(stackPane, col, row); // Add to grid
            GridPane.setMargin(stackPane, new Insets(10)); // Add margin for spacing

            col++;

            // Move to the next row after 3 buttons
            if (col == 4) {
                col = 0;
                row++;
            }
        }

        // Create Logout and Back buttons
        Button logoutButton = new Button("Logout");
        logoutButton.setId("logout-button"); // Set CSS ID for styling
        logoutButton.setOnAction(e -> {
            // Logic to go back to the login page
            openLoginPage(primaryStage);
        });

        Button backButton = new Button("Back");
        backButton.setId("back-button"); // Set CSS ID for styling
        backButton.setOnAction(e -> {
            // Logic to go back to the previous page (you can customize as needed)
            openLoginPage(primaryStage);
        });

        // Arrange Logout and Back buttons in an HBox
        HBox buttonLayout = new HBox(10, backButton, logoutButton); // Use HBox for horizontal layout
        buttonLayout.setAlignment(Pos.TOP_RIGHT); // Align buttons to the top right
        buttonLayout.setPadding(new Insets(20)); // Add padding

        // Arrange components in a VBox
        VBox layout = new VBox(20, buttonLayout, formsGrid);
        layout.setAlignment(Pos.CENTER);
        layout.setId("vbox"); // Set CSS ID for styling

        // Create a Scene and set to full screen
        Scene scene = new Scene(layout);
        primaryStage.setScene(scene);
        primaryStage.setFullScreen(true); // Set to full screen
        primaryStage.show();

        // Load CSS
        scene.getStylesheets().add(getClass().getResource("/styles1.css").toExternalForm()); // Load CSS
    }

    // Method to open the Login page
    private void openLoginPage(Stage primaryStage) {
        new LoginApp().start(primaryStage);  // Replace this with the actual login page class
    }

    // Method to open FormApp when the plus symbol is clicked
    private void openFormApp(Stage primaryStage) {
        new FormApp(gmail).start(primaryStage);  // Replace this with the actual form creation app
    }

    private void openFormEdit(Stage primaryStage,String formTitle) {
        new FormEdit(gmail,formTitle).start(primaryStage);
    }

    // Show options for the selected form
    private void showFormOptions(Document form) {
        Stage optionsStage = new Stage();
        optionsStage.setTitle("Form Options");

        // Create buttons for Update and Responses
        Button updateButton = new Button("Update");
        updateButton.setId("options-button"); // Set CSS ID for styling
        updateButton.setOnAction(e -> openFormEdit(optionsStage,form.getString("formTitle")));

        Button responseButton = new Button("Responses");
        responseButton.setId("options-button"); // Set CSS ID for styling
        responseButton.setOnAction(e -> {
            // Open the form details page
            FormDetails formDetails = new FormDetails(form,gmail);
            formDetails.start(new Stage());
            optionsStage.close(); // Close the options window
        });

        // Create a VBox for the options layout
        VBox optionsLayout = new VBox(10, updateButton, responseButton);
        optionsLayout.setAlignment(Pos.CENTER);
        optionsLayout.setPadding(new Insets(20));
        optionsLayout.setId("options-layout"); // Set CSS ID for styling

        Scene optionsScene = new Scene(optionsLayout, 200, 150);
        optionsStage.setScene(optionsScene);
        optionsStage.show();
    }
}
