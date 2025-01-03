package com.example;

import com.example.mongodb.FormDataManager;
import com.example.mongodb.MongoDBHelper;
import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.bson.Document;

import java.util.ArrayList;
import java.util.List;

public class FormResponseApp extends Application {

    private VBox questionsDisplayArea;
    private FormDataManager formDataManager;
    private FormDataManager responseDataManager;
    private List<Document> userResponses;
    private List<Document> questions;
    private String formName; // Replace with actual form Object ID
    private String gmail;

    public FormResponseApp(String gmail,String formName){
        this.gmail = gmail;
        this.formName = formName;
    }

    @Override
    public void start(Stage primaryStage) {
        MongoDBHelper mongoDBHelper = new MongoDBHelper("mongodb://localhost:27017", "formB");
        formDataManager = new FormDataManager(mongoDBHelper,"question");
        responseDataManager = new FormDataManager(mongoDBHelper,"response");

        // Create a VBox to display form questions
        questionsDisplayArea = new VBox(10);
        questionsDisplayArea.setPadding(new Insets(20));

        // List to store user responses
        userResponses = new ArrayList<>();

        // Load the form from the database
        loadForm();

        // Create submit button
        Button submitButton = new Button("Submit Answers");
        submitButton.setOnAction(e -> submitAndEvaluateAnswers());

        // Layout for the form page
        VBox mainLayout = new VBox(20, questionsDisplayArea, submitButton);
        mainLayout.setPadding(new Insets(20));

        Scene scene = new Scene(mainLayout, 400, 300);
        primaryStage.setTitle("Form Response Page");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    private void loadForm() {
        Document form = formDataManager.getFormByTitle(formName);
        if (form != null) {
            String formTitle = form.getString("formTitle");
            Label formTitleLabel = new Label("Form: " + formTitle);
            formTitleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            questionsDisplayArea.getChildren().add(formTitleLabel);

            questions = (List<Document>) form.get("questions");

            // Display all questions with their options
            if (questions != null && !questions.isEmpty()) {
                for (int i = 0; i < questions.size(); i++) {
                    displayQuestion(questions.get(i), i + 1);
                }
            } else {
                showAlert("No questions found in the form.");
            }
        } else {
            showAlert("Form not found!");
        }
    }

    private void displayQuestion(Document questionDoc, int questionNumber) {
        String questionText = questionDoc.getString("questionText");
        if (questionText == null) {
            showAlert("Question text is null for question number " + questionNumber);
            return;
        }

        Label questionLabel = new Label("Q" + questionNumber + ": " + questionText);
        questionLabel.setStyle("-fx-font-size: 16px;");
        questionsDisplayArea.getChildren().add(questionLabel);

        String questionType = questionDoc.getString("questionType");
        switch (questionType) {
            case "text area":
                displayTextareaQuestion(questionDoc, questionNumber);
                break;
            case "radio button":
                displayRadioQuestion(questionDoc, questionNumber);
                break;
            case "checkbox":
                displayCheckboxQuestion(questionDoc, questionNumber);
                break;
            default:
                showAlert("Unknown question type: " + questionType);
        }
    }

    private void displayTextareaQuestion(Document questionDoc, int questionNumber) {
        TextField textArea = new TextField();
        textArea.setPromptText("Type your answer here...");
        questionsDisplayArea.getChildren().add(textArea);

        // Save user's response when they type in the textarea
        textArea.setOnKeyReleased(e -> {
            Document userResponse = new Document("questionNumber", questionNumber)
                    .append("questionText", questionDoc.getString("questionText"))
                    .append("answerText", textArea.getText());
            saveUserResponse(questionNumber, userResponse);
        });
    }

    private void displayRadioQuestion(Document questionDoc, int questionNumber) {
        List<String> options = (List<String>) questionDoc.get("options");

        ToggleGroup toggleGroup = new ToggleGroup();
        for (String option : options) {
            RadioButton radioButton = new RadioButton(option);
            radioButton.setToggleGroup(toggleGroup);
            questionsDisplayArea.getChildren().add(radioButton);

            // Save user's response when they select an option
            radioButton.setOnAction(e -> {
                Document userResponse = new Document("questionNumber", questionNumber)
                        .append("questionText", questionDoc.getString("questionText"))
                        .append("selectedOption", radioButton.getText());
                saveUserResponse(questionNumber, userResponse);
            });
        }
    }

    private void displayCheckboxQuestion(Document questionDoc, int questionNumber) {
        List<String> options = (List<String>) questionDoc.get("options");

        List<String> selectedOptions = new ArrayList<>();
        for (String option : options) {
            CheckBox checkBox = new CheckBox(option);
            questionsDisplayArea.getChildren().add(checkBox);

            // Save user's selected options
            checkBox.setOnAction(e -> {
                if (checkBox.isSelected()) {
                    selectedOptions.add(checkBox.getText());
                } else {
                    selectedOptions.remove(checkBox.getText());
                }

                Document userResponse = new Document("questionNumber", questionNumber)
                        .append("questionText", questionDoc.getString("questionText"))
                        .append("selectedOptions", selectedOptions);
                saveUserResponse(questionNumber, userResponse);
            });
        }
    }

    private void saveUserResponse(int questionNumber, Document userResponse) {
        if (userResponses.size() >= questionNumber) {
            userResponses.set(questionNumber - 1, userResponse);  // Update response if already exists
        } else {
            userResponses.add(userResponse);  // Add new response
        }
    }

    private void submitAndEvaluateAnswers() {
        if (!userResponses.isEmpty()) {
            double totalScore = 0.0;

            // Evaluate answers
            for (int i = 0; i < questions.size(); i++) {
                Document questionDoc = questions.get(i);
                List<String> correctAnswers = (List<String>) questionDoc.get("correctAnswer");
                if (correctAnswers == null || correctAnswers.isEmpty()) {
                    showAlert("Correct answer not defined for question number " + (i + 1));
                    continue;
                }

                String questionType = questionDoc.getString("questionType");
                double questionScore = 0.0;

                switch (questionType) {
                    case "text area":
                    case "radio button":
                        String userAnswer = (String) userResponses.get(i).get("answerText");
                        if (userAnswer == null) {
                            userAnswer = (String) userResponses.get(i).get("selectedOption");
                        }

                        // For text area or radio button, check against correctAnswer[0]
                        if (correctAnswers.get(0).equals(userAnswer)) {
                            questionScore = 1.0;
                        }
                        break;

                    case "checkbox":
                        List<String> selectedOptions = (List<String>) userResponses.get(i).get("selectedOptions");
                        if (selectedOptions == null) {
                            selectedOptions = new ArrayList<>();
                        }

                        // If the user selected any incorrect option, score is 0
                        boolean hasIncorrectAnswer = false;
                        for (String option : selectedOptions) {
                            if (!correctAnswers.contains(option)) {
                                hasIncorrectAnswer = true;
                                break;
                            }
                        }

                        if (!hasIncorrectAnswer) {
                            // Calculate partial score based on the number of correct answers selected
                            int correctSelections = selectedOptions.size();
                            int totalCorrectAnswers = correctAnswers.size();
                            if (correctSelections <= totalCorrectAnswers) {
                                questionScore = (double) correctSelections / totalCorrectAnswers;
                            }
                        }
                        break;

                    default:
                        showAlert("Unknown question type for question number " + (i + 1));
                }

                userResponses.set(i,userResponses.get(i).append("Score",String.format("%.2f", questionScore)));

                totalScore += questionScore;
            }

            // Save user's responses to the database
            responseDataManager.saveResponse(formName, gmail ,userResponses,String.format("%.2f", totalScore));
            updateRespondersInForm(String.format("%.2f", totalScore));

            // Display total score, rounding to 2 decimal places
            showAlert("Your total score: " + String.format("%.2f", totalScore) + "/" + questions.size());
        } else {
            showAlert("Please answer all questions before submitting.");
        }
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void updateRespondersInForm(String totalScore) {
        // Retrieve the form document by formId
        Document form = formDataManager.getFormByTitle(formName);

        if (form != null) {
            // Get the current list of responders, or initialize a new list if it doesn't exist
            List<Document> responders = (List<Document>) form.get("responders");
            if (responders == null) {
                responders = new ArrayList<>();
            }

            // Check if the user's Gmail is already in the responders list to avoid duplicates
            boolean responderExists = false;
            for (Document responderDoc : responders) {
                if (responderDoc.getString("responder").equals(gmail)) {
                    responderExists = true;
                    break;
                }
            }

// If the responder doesn't exist, add them along with their mark
            if (!responderExists) {
                Document newResponder = new Document("responder", gmail)
                        .append("Total Score", totalScore);  // Assuming 'mark' is already calculated
                responders.add(newResponder);  // Add the new responder with mark
            }

            // Update the form document in the database
            form.put("responders", responders);
            formDataManager.updateForm(formName, form);  // Update the form in the database with the new responders list
        } else {
            showAlert("Form not found, unable to update responders.");
        }
    }


    public static void main(String[] args) {
        launch(args);
    }
}
