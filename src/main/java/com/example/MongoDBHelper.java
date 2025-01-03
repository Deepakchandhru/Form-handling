package com.example.mongodb;

import com.mongodb.ConnectionString;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.FindIterable;
import org.bson.Document;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MongoDBHelper {

    private MongoClient mongoClient;
    private MongoDatabase database;

    public MongoDBHelper(String uri, String dbName) {
        ConnectionString connectionString = new ConnectionString(uri);
        this.mongoClient = MongoClients.create(connectionString);
        this.database = mongoClient.getDatabase(dbName);
    }

    // Method to get a specific collection by name
    public MongoCollection<Document> getCollection(String collectionName) {
        return database.getCollection(collectionName);
    }

    // Method to retrieve forms by the creator's Gmail
    public List<Document> getFormsByCreator(String gmail) {
        MongoCollection<Document> collection = getCollection("question");
        FindIterable<Document> documents = collection.find(new Document("creater", gmail));

        List<Document> forms = new ArrayList<>();
        for (Document doc : documents) {
            forms.add(doc);
        }
        return forms;
    }
    public List<Document> getSortedResponders(String formTitle, Document sortCriteria) {
        MongoCollection<Document> formCollection = database.getCollection("question");

        // Aggregation pipeline to match the form, unwind the responders, sort, and project
        return formCollection.aggregate(Arrays.asList(
                new Document("$match", new Document("formTitle", formTitle)), // Match the form title
                new Document("$unwind", "$responders"), // Unwind the responders array
                new Document("$sort", new Document("responders." + sortCriteria.keySet().iterator().next(),
                        sortCriteria.getInteger(sortCriteria.keySet().iterator().next()))), // Sort by the provided criteria
                new Document("$project", new Document("responder", "$responders.responder") // Project only the needed fields
                        .append("Total Score", "$responders.Total Score"))
        )).into(new java.util.ArrayList<>());
    }

    // Method to get responders without sorting
    public List<Document> getResponders(String formTitle) {
        MongoCollection<Document> formCollection = database.getCollection("question");

        // Similar to the sorted version but without the $sort stage
        return formCollection.aggregate(Arrays.asList(
                new Document("$match", new Document("formTitle", formTitle)), // Match the form title
                new Document("$unwind", "$responders"), // Unwind the responders array
                new Document("$project", new Document("responder", "$responders.responder") // Project only the needed fields
                        .append("Total Score", "$responders.Total Score"))
        )).into(new java.util.ArrayList<>());
    }


    // Close the MongoDB connection
    public void close() {
        mongoClient.close();
    }
}