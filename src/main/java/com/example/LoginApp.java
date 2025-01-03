package com.example;

import com.example.mongodb.FormDataManager;
import com.example.mongodb.MongoDBHelper;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Screen;
import javafx.stage.Stage;
import org.bson.Document;

public class LoginApp extends Application {
    private TextField gmailField;
    private PasswordField passwordField;
    private Button loginButton;
    private Button signupButton;
    private UserDataManager userDataManager;
    private VBox mainLayout;

    @Override
    public void start(Stage primaryStage) {
        // Initialize MongoDB connection
        MongoDBHelper mongoDBHelper = new MongoDBHelper("mongodb://localhost:27017", "formB");
        userDataManager = new UserDataManager(mongoDBHelper);
        primaryStage.setTitle("Login & Signup");

        // Get the primary screen's bounds
        Screen screen = Screen.getPrimary();

        // Set full screen
        primaryStage.setFullScreen(true);

        // Gmail input field
        gmailField = new TextField();
        gmailField.setPromptText("Enter Gmail");
        gmailField.getStyleClass().add("input-field");
        gmailField.setPrefWidth(200);  // Set desired width (in pixels)
        gmailField.setPrefHeight(30);

        // Password input field
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter Password");
        passwordField.getStyleClass().add("input-field");

        // Login button
        loginButton = new Button("Login");
        loginButton.getStyleClass().add("button-primary");

        // Signup button
        signupButton = new Button("Sign Up");
        signupButton.getStyleClass().add("button-secondary");

        // Layout for login form
        VBox formLayout = new VBox(10, new Label("Gmail:"), gmailField, new Label("Password:"), passwordField, loginButton, signupButton);
        formLayout.setAlignment(Pos.CENTER);
        formLayout.getStyleClass().add("login-form");

        // Box wrapper for form
        StackPane boxLayout = new StackPane(formLayout);
        boxLayout.setAlignment(Pos.CENTER);
        boxLayout.getStyleClass().add("box-layout");

        // Main container with background
        mainLayout = new VBox(boxLayout);
        mainLayout.setAlignment(Pos.CENTER);
        mainLayout.getStyleClass().add("root");

        // Create login scene
        Scene loginScene = new Scene(mainLayout, screen.getVisualBounds().getWidth(), screen.getVisualBounds().getHeight());

        // Add the CSS stylesheet to the scene
        loginScene.getStylesheets().add(getClass().getResource("/styles.css").toExternalForm());
        primaryStage.setScene(loginScene);
        primaryStage.show();

        // Button actions
        loginButton.setOnAction(e -> login(primaryStage));
        signupButton.setOnAction(e -> signup());
    }

    private void login(Stage primaryStage) {
        String gmail = gmailField.getText();
        String password = passwordField.getText();
        // Authenticate user
        if (userDataManager.authenticateUser(gmail, password)) {
            showAlert("Login Successful!", primaryStage);
            // Ask user type (Teacher/Student)
            showUserTypeDialog(primaryStage, gmail);
        } else {
            showAlert("Invalid Gmail or Password.", primaryStage);
        }
    }

    private void signup() {
        String gmail = gmailField.getText();
        String password = passwordField.getText();
        if (gmail.isEmpty() || password.isEmpty()) {
            showAlert("Gmail and Password cannot be empty.", null);
            return;
        }
        if (userDataManager.userExists(gmail)) {
            showAlert("User already exists. Please log in.", null);
        } else {
            userDataManager.addUser(gmail, password);
            showAlert("Sign Up Successful! Please log in.", null);
        }
    }

    private void showAlert(String message, Stage owner) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        // Center the alert dialog on the owner stage
        if (owner != null) {
            alert.initOwner(owner);
            alert.setTitle("Information");
            alert.setOnShown(event -> {
                alert.setX(owner.getX() + (owner.getWidth() - alert.getWidth()) / 2);
                alert.setY(owner.getY() + (owner.getHeight() - alert.getHeight()) / 2);
            });
        }
        alert.showAndWait();
    }

    private void showUserTypeDialog(Stage primaryStage, String gmail) {
        Button createButton = new Button("Manage Form");
        createButton.getStyleClass().add("button-primary");
        Button respondButton = new Button("Access Form");
        respondButton.getStyleClass().add("button-secondary");

        createButton.setOnAction(e -> {
            new Dashboard(gmail).start(primaryStage);
            mainLayout.getChildren().remove(1); // Remove user type options
        });

        respondButton.setOnAction(e -> {
            enterFormName(primaryStage, gmail);
            mainLayout.getChildren().remove(1); // Remove user type options
        });

        VBox userTypeLayout = new VBox(10, new Label("Select User Type:"), createButton, respondButton);
        userTypeLayout.setAlignment(Pos.CENTER);
        userTypeLayout.getStyleClass().add("vbox");
        userTypeLayout.setStyle("-fx-padding: 20;");

        if (mainLayout.getChildren().size() > 1) {
            mainLayout.getChildren().set(1, userTypeLayout);
        } else {
            mainLayout.getChildren().add(userTypeLayout);
        }
    }

    private void enterFormName(Stage primaryStage, String gmail) {
        TextInputDialog formNameDialog = new TextInputDialog();
        formNameDialog.setTitle("Access Form");
        formNameDialog.setHeaderText(null);
        formNameDialog.setContentText("Please Enter the Form Name:");
        MongoDBHelper mongoDBHelper = new MongoDBHelper("mongodb://localhost:27017", "formB");
        FormDataManager formDataManager = new FormDataManager(mongoDBHelper, "question");
        boolean formFound = false;
        while (!formFound) {
            formNameDialog.showAndWait();
            String formName = formNameDialog.getEditor().getText();
            if (formName.isEmpty()) {
                showAlert("Form name cannot be empty. Please try again.", null);
                continue;
            }
            Document form = formDataManager.getFormByTitle(formName);
            if (form != null) {
                formFound = true;
                new FormResponseApp(gmail, formName).start(primaryStage);
            } else {
                showAlert("Form not found. Please enter a valid form name.", null);
            }
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
