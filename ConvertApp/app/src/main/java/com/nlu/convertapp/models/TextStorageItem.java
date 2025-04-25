package com.nlu.convertapp.models;

public class TextStorageItem {
    private String date;
    private String content;
    private boolean starred;

    public TextStorageItem(String date, String content, boolean starred) {
        this.date = date;
        this.content = content;
        this.starred = starred;
    }

    public String getDate() {
        return date;
    }

    public void setDate(String date) {
        this.date = date;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isStarred() {
        return starred;
    }

    public void setStarred(boolean starred) {
        this.starred = starred;
    }
}
