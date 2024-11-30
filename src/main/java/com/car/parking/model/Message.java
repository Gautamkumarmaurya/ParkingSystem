package com.car.parking.model;

public class Message {
    private String message;

    public Message() {}
    public Message(String message) {
        this.message = message;
    }

    // Getters and setters
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
