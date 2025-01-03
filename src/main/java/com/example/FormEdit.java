package com.example;

import com.example.mongodb.FormDataManager;
import com.example.mongodb.MongoDBHelper;
import javafx.application.Application;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.util.ArrayList;
import java.util.List;

public class FormEdit extends Application {

    private TextField formTitleField;
    private TextField questionField;
    private ComboBox<String> answerTypeComboBox;
    private Button addQuestionButton;
    private Button updateFormButton;
    private VBox questionsDisplayArea;
    private List<Document> questionsList;
    private FormDataManager formDataManager;
    private String gmail;
    private String formId;
    private String formTitle;

    public FormEdit(String gmail, String formId) {
        this.gmail = gmail;
        this.formTitle = formId;

        System.out.println(gmail+"+"+formTitle);
    }

    @Override
    public void start(Stage primaryStage) {
        MongoDBHelper mongoDBHelper = new MongoDBHelper("mongodb://localhost:27017", "formB");
        formDataManager = new FormDataManager(mongoDBHelper, "question");
        questionsList = new ArrayList<>();
        formId=formDataManager.getFormIdByTitle(formTitle);
        formTitleField = new TextField();
        formTitleField.setPromptText("Enter Form Title");
        formTitleField.setStyle("-fx-font-size: 20px; -fx-padding: 10px; -fx-background-color: #f1f3f4;");

        // Question Input
        questionField = new TextField();
        questionField.setPromptText("Enter Question");
        questionField.setStyle("-fx-font-size: 16px; -fx-padding: 10px; -fx-background-color: #f1f3f4;");

        // Answer Type ComboBox
        answerTypeComboBox = new ComboBox<>();
        answerTypeComboBox.getItems().addAll("Text Area", "Radio Button", "Checkbox");
        answerTypeComboBox.setValue("Text Area");
        answerTypeComboBox.setStyle("-fx-font-size: 16px; -fx-padding: 10px; -fx-background-color: #f1f3f4;");

        // Buttons
        addQuestionButton = new Button("Add Question");
        addQuestionButton.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold;");
        updateFormButton = new Button("Save Form");
        updateFormButton.setStyle("-fx-background-color: #1a73e8; -fx-text-fill: white; -fx-font-weight: bold;");

        // Questions Display Area
        questionsDisplayArea = new VBox();
        questionsDisplayArea.setSpacing(10);
        questionsDisplayArea.setStyle("-fx-padding: 10;");

        // Add action to Home Button

        // Layouts
        VBox inputPanel = new VBox(10, new Label("Form Title:"), formTitleField,
                new Label("Question:"), questionField,
                new Label("Answer Type:"), answerTypeComboBox,
                addQuestionButton, updateFormButton);
        inputPanel.setStyle("-fx-padding: 20; -fx-background-color: #ffffff; -fx-border-radius: 10; -fx-border-color: #e0e0e0; -fx-border-width: 1;");

        Button logoutButton = new Button("Logout");
        logoutButton.setId("logout-button"); // Set CSS ID for styling
        logoutButton.setOnAction(e -> {
            new LoginApp().start(primaryStage);
        });

        Button backButton = new Button("Back");
        backButton.setId("back-button"); // Set CSS ID for styling
        backButton.setOnAction(e -> {new Dashboard(gmail).start(primaryStage);});

        HBox buttonLayout = new HBox(10, backButton, logoutButton); // Use HBox for horizontal layout
        buttonLayout.setAlignment(Pos.TOP_RIGHT); // Align buttons to the top right
        buttonLayout.setPadding(new Insets(20)); // Add padding

        HBox layout = new HBox(20, inputPanel, questionsDisplayArea,buttonLayout);
        layout.setPadding(new javafx.geometry.Insets(20));

        Scene scene = new Scene(layout, 800, 600);

        loadExistingQuestions();
        primaryStage.setTitle("Form Builder");
        primaryStage.setScene(scene);
        primaryStage.show();

        // Set action listeners
        addQuestionButton.setOnAction(e -> addQuestion());
        updateFormButton.setOnAction(e -> updateForm());
    }

    private void loadExistingQuestions() {
        System.out.print("load");
        Document form = formDataManager.getFormById(formId);
        if (form != null) {
            questionsList = (List<Document>) form.get("questions");
            for (Document questionDoc : questionsList) {
                displayQuestion(questionDoc);
            }
            formTitleField.setText(form.getString("title"));
        } else {
            showAlert("No form found with the provided ID.");
        }
    }

    private void addQuestion() {
        System.out.print("add");
        String questionText = questionField.getText();
        String selectedAnswerType = answerTypeComboBox.getValue();

        if (!questionText.isEmpty()) {
            List<String> options = new ArrayList<>();
            List<String> correctAnswers = new ArrayList<>();

            if (!selectedAnswerType.equals("Text Area")) {
                // Handle Radio Button and Checkbox options
                TextInputDialog numOptionsDialog = new TextInputDialog();
                numOptionsDialog.setHeaderText("Enter the number of options:");
                int numOptions;
                try {
                    numOptions = Integer.parseInt(numOptionsDialog.showAndWait().orElse("0"));
                } catch (NumberFormatException e) {
                    showAlert("Invalid number of options.");
                    return;
                }

                for (int i = 1; i <= numOptions; i++) {
                    TextInputDialog optionDialog = new TextInputDialog();
                    optionDialog.setHeaderText("Enter option " + i + ":");
                    String option = optionDialog.showAndWait().orElse("");
                    options.add(option);

                    if (selectedAnswerType.equals("Checkbox")) {
                        // Ask if this option is correct for checkbox questions
                        Alert correctOptionAlert = new Alert(Alert.AlertType.CONFIRMATION);
                        correctOptionAlert.setTitle("Correct Option");
                        correctOptionAlert.setHeaderText("Is this option correct?");
                        correctOptionAlert.setContentText(option);

                        ButtonType yesButton = new ButtonType("Yes", ButtonBar.ButtonData.YES);
                        ButtonType noButton = new ButtonType("No", ButtonBar.ButtonData.NO);
                        correctOptionAlert.getButtonTypes().setAll(yesButton, noButton);

                        correctOptionAlert.showAndWait().ifPresent(button -> {
                            if (button == yesButton) {
                                correctAnswers.add(option);
                            }
                        });
                    }
                }

                if (selectedAnswerType.equals("Radio Button")) {
                    boolean correctAnswerSelected = false;
                    while (!correctAnswerSelected) {
                        TextInputDialog correctAnswerDialog = new TextInputDialog();
                        correctAnswerDialog.setHeaderText("Enter the correct answer for the radio button:");
                        String correctAnswer = correctAnswerDialog.showAndWait().orElse("");

                        if (options.contains(correctAnswer)) {
                            correctAnswers.add(correctAnswer);
                            correctAnswerSelected = true;
                        } else {
                            showAlert("Please enter a valid option from the list.");
                        }
                    }
                }
            } else {
                // Handle Text Area correct answer
                TextInputDialog correctAnswerDialog = new TextInputDialog();
                correctAnswerDialog.setHeaderText("Enter the correct answer for this Text Area question:");
                String correctAnswer = correctAnswerDialog.showAndWait().orElse("");
                correctAnswers.add(correctAnswer);
            }

            Document questionDoc = new Document("questionText", questionText)
                    .append("questionType", selectedAnswerType.toLowerCase())
                    .append("options", options) // Empty if Text Area
                    .append("correctAnswer", correctAnswers)
                    .append("answers", new ArrayList<>());

            questionsList.add(questionDoc);
            displayQuestion(questionDoc);
            questionField.clear();
        } else {
            showAlert("Please enter a question.");
        }
    }

    private void displayQuestion(Document questionDoc) {
        System.out.print("display");
        VBox questionBox = new VBox();
        questionBox.setSpacing(10);

        // Create question label
        Label questionLabel = new Label(questionDoc.getString("questionText") + " (Type: " + questionDoc.getString("questionType") + ")");
        questionBox.getChildren().add(questionLabel);


        // Display the options based on question type
        String questionType = questionDoc.getString("questionType");
        VBox optionsBox = new VBox();
        List<String> options = (List<String>) questionDoc.get("options");
        if (questionType.equals("text area")) {
            TextArea answerField = new TextArea();
            answerField.setPromptText("Your answer...");
            answerField.setPrefHeight(50);
            questionBox.getChildren().add(answerField);
        } else if (questionType.equals("radio button")) {
            ToggleGroup toggleGroup = new ToggleGroup();
            for (String option : options) {
                RadioButton radioButton = new RadioButton(option);
                radioButton.setToggleGroup(toggleGroup);
                optionsBox.getChildren().add(radioButton);
            }
        } else if (questionType.equals("checkbox")) {
            for (String option : options) {
                CheckBox checkBox = new CheckBox(option);
                optionsBox.getChildren().add(checkBox);
            }
        }
        questionBox.getChildren().add(optionsBox);

        // Add separator
        questionBox.getChildren().add(new Separator());

        questionsDisplayArea.getChildren().add(questionBox);
    }

    private void editQuestion(Document questionDoc) {
        Dialog<ButtonType> dialog = new Dialog<>();
        dialog.setTitle("Edit Question");
        dialog.setHeaderText("Edit your question details");

        VBox dialogPane = new VBox(10);
        TextField editQuestionField = new TextField(questionDoc.getString("questionText"));
        ComboBox<String> editAnswerTypeComboBox = new ComboBox<>();
        editAnswerTypeComboBox.getItems().addAll("Text Area", "Radio Button", "Checkbox");
        editAnswerTypeComboBox.setValue(questionDoc.getString("questionType").substring(0, 1).toUpperCase() + questionDoc.getString("questionType").substring(1));

        // Create a VBox for options
        VBox optionsBox = new VBox();
        List<String> options = (List<String>) questionDoc.get("options");
        for (String option : options) {
            TextField optionField = new TextField(option);
            optionsBox.getChildren().add(optionField);
        }

        // Display answers if any
        VBox answersBox = new VBox();
        List<String> answers = (List<String>) questionDoc.get("answers");
        for (String answer : answers) {
            TextField answerField = new TextField(answer);
            answersBox.getChildren().add(answerField);
        }

        dialogPane.getChildren().addAll(new Label("Question:"), editQuestionField, new Label("Answer Type:"), editAnswerTypeComboBox,
                new Label("Options:"), optionsBox, new Label("Answers:"), answersBox);
        dialog.getDialogPane().setContent(dialogPane);

        // Add buttons
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                String updatedQuestionText = editQuestionField.getText();
                String updatedAnswerType = editAnswerTypeComboBox.getValue();
                List<String> updatedOptions = new ArrayList<>();

                for (javafx.scene.Node node : optionsBox.getChildren()) {
                    if (node instanceof TextField) {
                        updatedOptions.add(((TextField) node).getText());
                    }
                }

                List<String> updatedAnswers = new ArrayList<>();
                for (javafx.scene.Node node : answersBox.getChildren()) {
                    if (node instanceof TextField) {
                        updatedAnswers.add(((TextField) node).getText());
                    }
                }

                questionDoc.put("questionText", updatedQuestionText);
                questionDoc.put("questionType", updatedAnswerType.toLowerCase());
                questionDoc.put("options", updatedOptions);
                questionDoc.put("answers", updatedAnswers); // Update answers
                questionDoc.put("correctAnswer", questionDoc.getString("correctAnswer")); // Keep the correct answer
                return ButtonType.OK;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(result -> {
            if (result == ButtonType.OK) {
                // Update the display
                questionsDisplayArea.getChildren().clear();
                for (Document q : questionsList) {
                    displayQuestion(q);
                }
            }
        });
    }

    private String showInputDialog(String title, String existingText) {
        TextInputDialog dialog = new TextInputDialog(existingText);
        dialog.setTitle(title);
        dialog.setHeaderText(null);
        dialog.setContentText("Modify your question:");
        return dialog.showAndWait().orElse(null);
    }

    private void updateForm() {
        System.out.print("update");
        if (!formTitle.isEmpty() && !questionsList.isEmpty()) {
            formDataManager.updateForm(formId, formTitle, questionsList);
            showAlert("Form updated successfully!");
            // Clear fields and refresh
            formTitleField.clear();
            questionsDisplayArea.getChildren().clear();
            questionsList.clear();
        } else {
            showAlert("Please add a form title and at least one question.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
