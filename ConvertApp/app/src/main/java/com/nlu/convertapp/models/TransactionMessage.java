package com.nlu.convertapp.models;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class TransactionMessage {
    private String senderName;
    private String accountNumber;
    private double amount;
    private String message;
    private Date transactionDate;
    private String bankName;

    public TransactionMessage(String senderName, String accountNumber, double amount, 
                             String message, Date transactionDate, String bankName) {
        this.senderName = senderName;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.message = message;
        this.transactionDate = transactionDate;
        this.bankName = bankName;
    }

    public String getSenderName() {
        return senderName;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public double getAmount() {
        return amount;
    }

    public String getMessage() {
        return message;
    }

    public Date getTransactionDate() {
        return transactionDate;
    }

    public String getBankName() {
        return bankName;
    }
    
    public String getFormattedDate() {
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        return dateFormat.format(transactionDate);
    }
    
    public String getFormattedAmount() {
        return String.format(Locale.getDefault(), "%,.0f VND", amount);
    }
} 