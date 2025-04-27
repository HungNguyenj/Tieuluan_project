package com.nlu.convertapp.models;

public class TextToSpeechRequest {
    private String text;
    private String model_id;

    public TextToSpeechRequest(String text, String model_id) {
        this.text = text;
        this.model_id = model_id;
    }
} 