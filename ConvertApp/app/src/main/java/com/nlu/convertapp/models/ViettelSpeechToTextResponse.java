package com.nlu.convertapp.models;

import com.google.gson.annotations.SerializedName;

public class ViettelSpeechToTextResponse {
    @SerializedName("code")
    private int code;
    
    @SerializedName("message")
    private String message;
    
    @SerializedName("response")
    private ViettelResponse response;
    
    public int getCode() {
        return code;
    }
    
    public String getMessage() {
        return message;
    }
    
    public ViettelResponse getResponse() {
        return response;
    }
    
    public static class ViettelResponse {
        @SerializedName("text")
        private String text;
        
        public String getText() {
            return text;
        }
    }
} 