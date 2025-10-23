package com.doculens.pdfbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WordBoundingBox {

    @JsonProperty("text")
    private String text;

    @JsonProperty("boundingBox")
    private BoundingBox boundingBox;

    @JsonProperty("confidence")
    private double confidence;

    public WordBoundingBox() {
        this.confidence = 1.0; // PDFBox extracts actual text, so confidence is high
    }

    public WordBoundingBox(String text, BoundingBox boundingBox) {
        this.text = text;
        this.boundingBox = boundingBox;
        this.confidence = 1.0;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    public static class BoundingBox {
        @JsonProperty("x")
        private float x;

        @JsonProperty("y")
        private float y;

        @JsonProperty("width")
        private float width;

        @JsonProperty("height")
        private float height;

        public BoundingBox() {}

        public BoundingBox(float x, float y, float width, float height) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
        }

        public float getX() {
            return x;
        }

        public void setX(float x) {
            this.x = x;
        }

        public float getY() {
            return y;
        }

        public void setY(float y) {
            this.y = y;
        }

        public float getWidth() {
            return width;
        }

        public void setWidth(float width) {
            this.width = width;
        }

        public float getHeight() {
            return height;
        }

        public void setHeight(float height) {
            this.height = height;
        }
    }
}
