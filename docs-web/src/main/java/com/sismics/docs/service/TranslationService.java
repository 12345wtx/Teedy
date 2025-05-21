package com.sismics.docs.service;

public class TranslationService {
    public String translate(String text, String sourceLanguage, String targetLanguage) {
        if ("zh".equals(targetLanguage)) {
            return "这是翻译后的中文文本: " + text;
        } else if ("en".equals(targetLanguage)) {
            return "This is the translated English text: " + text;
        } else {
            return "Translated text: " + text;
        }
    }
} 
