package com.nlu.convertapp.models;

public class TransactionMessage {
    private String message;

    public TransactionMessage(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
} 