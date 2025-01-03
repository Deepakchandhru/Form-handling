module com.example.formapp {
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.web;
    requires org.mongodb.driver.sync.client;
    requires org.mongodb.bson;
    requires org.mongodb.driver.core;

    // Other required modules...

    opens com.example to javafx.fxml;
    exports com.example;
}
