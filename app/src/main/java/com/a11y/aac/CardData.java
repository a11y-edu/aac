package com.a11y.aac;

public class CardData {
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getTextToSpeak() {
        return textToSpeak;
    }

    public void setTextToSpeak(String textToSpeak) {
        this.textToSpeak = textToSpeak;
    }

    private String name;
    private String textToSpeak;
}