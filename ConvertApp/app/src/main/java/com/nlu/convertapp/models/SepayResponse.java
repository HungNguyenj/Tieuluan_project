package com.nlu.convertapp.models;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class SepayResponse {
    @SerializedName("status")
    private int status;
    
    @SerializedName("error")
    private String error;
    
    @SerializedName("messages")
    private Messages messages;
    
    @SerializedName("transactions")
    private List<SepayTransaction> transactions;
    
    public int getStatus() { return status; }
    public String getError() { return error; }
    public Messages getMessages() { return messages; }
    public List<SepayTransaction> getTransactions() { return transactions; }
    
    public static class Messages {
        @SerializedName("success")
        private boolean success;
        
        public boolean isSuccess() { return success; }
    }
} 