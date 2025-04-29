package com.nlu.convertapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SpeechToTextResponse {
    @SerializedName("language_code")
    private String languageCode;
    
    @SerializedName("language_probability")
    private double languageProbability;
    
    @SerializedName("text")
    private String text;
    
    @SerializedName("words")
    private List<Word> words;
    
    @SerializedName("additional_formats")
    private List<AdditionalFormat> additionalFormats;
    
    public String getText() {
        return text;
    }
    
    public static class Word {
        @SerializedName("text")
        private String text;
        
        @SerializedName("type")
        private String type;
        
        @SerializedName("start")
        private double start;
        
        @SerializedName("end")
        private double end;
        
        @SerializedName("speaker_id")
        private String speakerId;
        
        @SerializedName("characters")
        private List<Character> characters;
    }
    
    public static class Character {
        @SerializedName("text")
        private String text;
        
        @SerializedName("start")
        private double start;
        
        @SerializedName("end")
        private double end;
    }
    
    public static class AdditionalFormat {
        @SerializedName("requested_format")
        private String requestedFormat;
        
        @SerializedName("file_extension")
        private String fileExtension;
        
        @SerializedName("content_type")
        private String contentType;
        
        @SerializedName("is_base64_encoded")
        private boolean isBase64Encoded;
        
        @SerializedName("content")
        private String content;
    }
} 