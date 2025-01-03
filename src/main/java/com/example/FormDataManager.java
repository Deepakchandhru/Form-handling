package com.example.mongodb;

import com.mongodb.client.MongoCollection;
import org.bson.Document;
import static com.mongodb.client.model.Filters.eq;
import com.mongodb.client.MongoCursor;
import com.mongodb.client.model.Aggregates;
import com.mongodb.client.model.Accumulators;
import org.bson.types.ObjectId;

import java.util.Arrays;
import java.util.List;

public class FormDataManager {

    private MongoCollection<Document> formCollection;

    public FormDataManager(MongoDBHelper mongoDBHelper,String collectionName) {
        this.formCollection = mongoDBHelper.getCollection(collectionName);
    }

    public int getResponseCount(String title) {
        MongoCursor<Document> cursor = null;
        try {
            cursor = formCollection.aggregate(Arrays.asList(
                    Aggregates.match(new Document("formTitle", title)),
                    Aggregates.unwind("$responders"),
                    Aggregates.group(null, Accumulators.sum("totalResponders", 1))
            )).iterator();

            // Check if there are results and return the count
            if (cursor.hasNext()) {
                Document doc = cursor.next();
                return doc.getInteger("totalResponders", 0); // return 0 if not found
            } else {
                return 0; // No responders found
            }
        } catch (Exception e) {
            e.printStackTrace(); // Log the exception
            return -1; // Error occurred, return -1
        } finally {
            if (cursor != null) {
                cursor.close(); // Ensure the cursor is closed
            }
        }
    }

    public String getFormIdByTitle(String formTitle) {
        // Query to find the form with the specified title
        Document formDocument = formCollection.find(eq("formTitle", formTitle)).first();

        if (formDocument != null) {
            // Extract the _id field and return it as a string
            return formDocument.getObjectId("_id").toHexString();
        } else {
            System.out.println("Form not found for title: " + formTitle);
            return null;
        }
    }

    public Document getFormById(String id) {
        return formCollection.find(new Document("_id", new ObjectId(id))).first();
    }

    public void updateForm(String formId, String title, List<Document> questions) {
        Document updateData = new Document("title", title)
                .append("questions", questions);

        formCollection.updateOne(
                new Document("_id", new ObjectId(formId)), // Match the document to update
                new Document("$set", updateData) // Set the new values
        );
    }

    // Save a new form
    public void saveForm(String formTitle, List<Document> questions,String gmail) {
        Document formDocument = new Document("formTitle", formTitle)
                .append("questions", questions)
                .append("creater", gmail);
        formCollection.insertOne(formDocument);
    }

    // Retrieve all forms
    public List<Document> getAllForms() {
        return formCollection.find().into(new java.util.ArrayList<>());
    }

    // Retrieve a form by its ID
    public Document getFormByTitle(String formTitle) {
        return formCollection.find(eq("formTitle", formTitle)).first();
    }

    public void updateForm(String formName, Document updatedForm) {
        formCollection.replaceOne(eq("formTitle", formName), updatedForm);
    }


    // Save user responses
    public void saveResponse(String formName,String gmail, List<Document> responses, String score) {
        Document responseDocument = new Document("formName", formName)
                .append("User gmail",gmail)
                .append("responses", responses)
                .append("Total Score", score);
        formCollection.insertOne(responseDocument); // Save the response document to the collection
    }
}