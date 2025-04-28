package com.nlu.convertapp.models;

public class ViettelTtsRequest {
    private String text;
    private String voice;
    private float speed;
    private int tts_return_option;
    private String token;
    private boolean without_filter;

    public ViettelTtsRequest(String text, String voice, float speed, int tts_return_option, 
                           String token, boolean without_filter) {
        this.text = text;
        this.voice = voice;
        this.speed = speed;
        this.tts_return_option = tts_return_option;
        this.token = token;
        this.without_filter = without_filter;
    }
} 