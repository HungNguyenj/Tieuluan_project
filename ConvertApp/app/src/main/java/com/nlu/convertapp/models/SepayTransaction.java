package com.nlu.convertapp.models;

import com.google.gson.annotations.SerializedName;

public class SepayTransaction {
    @SerializedName("id")
    private String id;
    
    @SerializedName("bank_brand_name")
    private String bankBrandName;
    
    @SerializedName("account_number")
    private String accountNumber;
    
    @SerializedName("transaction_date")
    private String transactionDate;
    
    @SerializedName("amount_out")
    private String amountOut;
    
    @SerializedName("amount_in")
    private String amountIn;
    
    @SerializedName("accumulated")
    private String accumulated;
    
    @SerializedName("transaction_content")
    private String transactionContent;
    
    @SerializedName("reference_number")
    private String referenceNumber;
    
    @SerializedName("code")
    private String code;
    
    @SerializedName("sub_account")
    private String subAccount;
    
    @SerializedName("bank_account_id")
    private String bankAccountId;

    // Getters
    public String getId() { return id; }
    public String getBankBrandName() { return bankBrandName; }
    public String getAccountNumber() { return accountNumber; }
    public String getTransactionDate() { return transactionDate; }
    public String getAmountOut() { return amountOut; }
    public String getAmountIn() { return amountIn; }
    public String getAccumulated() { return accumulated; }
    public String getTransactionContent() { return transactionContent; }
    public String getReferenceNumber() { return referenceNumber; }
    public String getCode() { return code; }
    public String getSubAccount() { return subAccount; }
    public String getBankAccountId() { return bankAccountId; }
} 