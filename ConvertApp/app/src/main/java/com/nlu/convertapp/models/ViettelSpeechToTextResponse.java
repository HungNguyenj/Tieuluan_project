package com.nlu.convertapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

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
        @SerializedName("result")
        private List<TranscriptResult> result;
        
        public List<TranscriptResult> getResult() {
            return result;
        }
    }
    
    public static class TranscriptResult {
        @SerializedName("transcript")
        private String transcript;
        
        @SerializedName("confidence")
        private double confidence;
        
        @SerializedName("segment")
        private List<Double> segment;
        
        @SerializedName("word_alignment")
        private List<WordAlignment> wordAlignment;
        
        public String getTranscript() {
            return transcript;
        }
        
        public double getConfidence() {
            return confidence;
        }
        
        public List<Double> getSegment() {
            return segment;
        }
        
        public List<WordAlignment> getWordAlignment() {
            return wordAlignment;
        }
    }
    
    public static class WordAlignment {
        @SerializedName("beg")
        private double beg;
        
        @SerializedName("end")
        private double end;
        
        @SerializedName("word")
        private String word;
        
        @SerializedName("confidence")
        private double confidence;
        
        public double getBeg() {
            return beg;
        }
        
        public double getEnd() {
            return end;
        }
        
        public String getWord() {
            return word;
        }
        
        public double getConfidence() {
            return confidence;
        }
    }

    // Thêm method để debug
    @Override
    public String toString() {
        return "ViettelSpeechToTextResponse{" +
                "code=" + code +
                ", message='" + message + '\'' +
                ", response=" + (response != null ? 
                    "{result=" + response.result + "}" : "null") +
                '}';
    }
} 