package com.ratel.domain;

/**
 * @author zhangxn
 * @date 2021/11/20  17:13
 */
public class PhraseSuggestParam {
    private int maxErrors;
    private float confidence;
    private String analyzer;
    private String suggestMode;

    public PhraseSuggestParam(int maxErrors, float confidence, String analyzer, String suggestMode) {
        this.maxErrors = maxErrors;
        this.confidence = confidence;
        this.analyzer = analyzer;
        this.suggestMode = suggestMode;
    }

    public int getMaxErrors() {
        return maxErrors;
    }

    public void setMaxErrors(int maxErrors) {
        this.maxErrors = maxErrors;
    }

    public float getConfidence() {
        return confidence;
    }

    public void setConfidence(float confidence) {
        this.confidence = confidence;
    }

    public String getAnalyzer() {
        return analyzer;
    }

    public void setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
    }

    public String getSuggestMode() {
        return suggestMode;
    }

    public void setSuggestMode(String suggestMode) {
        this.suggestMode = suggestMode;
    }

}
