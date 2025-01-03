package com.example;

import com.example.mongodb.MongoDBHelper;
import com.mongodb.client.MongoCollection;
import org.bson.Document;

import java.util.HashMap;
import java.util.Map;

public class UserDataManager {

    private MongoCollection<Document> userCollection;

    public UserDataManager(MongoDBHelper mongoDBHelper) {
        // Get the users collection from the database
        this.userCollection = mongoDBHelper.getCollection("users");
    }

    // Add a new user to the database
    public void addUser(String username, String password) {
        Document newUser = new Document("gmail", username)
                .append("password", password);
        userCollection.insertOne(newUser);
    }

    // Check if a user exists in the database
    public boolean userExists(String gmail) {
        Document query = new Document("gmail", gmail);
        Document result = userCollection.find(query).first();
        return result != null;
    }

    // Authenticate a user based on username and password
    public boolean authenticateUser(String gmail, String password) {
        Document query = new Document("gmail", gmail)
                .append("password", password);
        Document result = userCollection.find(query).first();
        return result != null;
    }
}