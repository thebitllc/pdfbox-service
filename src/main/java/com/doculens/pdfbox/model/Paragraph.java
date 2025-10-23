package com.doculens.pdfbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a paragraph in a PDF document.
 *
 * <p>A paragraph is a logical grouping of words that are spatially related
 * and represent a coherent block of text. Paragraphs are detected based on
 * line breaks, vertical spacing, and text alignment.</p>
 *
 * <p>Similar to Google Document AI and Tesseract OCR output, this structure
 * provides hierarchical text organization: Page → Paragraph → Word</p>
 *
 * @author PDFBox Service
 * @version 1.0
 */
@Schema(description = "A paragraph containing a group of related words")
public class Paragraph {

    /**
     * Unique identifier for the paragraph within the page (0-indexed).
     * First paragraph on a page will have paragraphIndex = 0.
     */
    @JsonProperty("paragraphIndex")
    @Schema(description = "Zero-based index of the paragraph within the page", example = "0")
    private int paragraphIndex;

    /**
     * The complete text content of the paragraph, constructed by joining
     * all words with spaces.
     */
    @JsonProperty("text")
    @Schema(description = "Complete text content of the paragraph", example = "This is a sample paragraph.")
    private String text;

    /**
     * List of words that make up this paragraph.
     * Each word includes its text content and bounding box coordinates.
     */
    @JsonProperty("words")
    @Schema(description = "List of words in the paragraph with their bounding boxes")
    private List<WordBoundingBox> words;

    /**
     * Bounding box that encompasses the entire paragraph.
     * Calculated as the union of all word bounding boxes within the paragraph.
     */
    @JsonProperty("boundingBox")
    @Schema(description = "Bounding box coordinates for the entire paragraph")
    private WordBoundingBox.BoundingBox boundingBox;

    /**
     * Confidence score for the paragraph (currently always 1.0 for extracted text).
     * In OCR scenarios, this would represent the confidence of text recognition.
     */
    @JsonProperty("confidence")
    @Schema(description = "Confidence score for the paragraph (1.0 for PDF text extraction)", example = "1.0")
    private double confidence;

    /**
     * Default constructor initializing an empty paragraph.
     */
    public Paragraph() {
        this.words = new ArrayList<>();
        this.confidence = 1.0;
    }

    /**
     * Constructs a paragraph with the specified index.
     *
     * @param paragraphIndex the zero-based index of the paragraph within the page
     */
    public Paragraph(int paragraphIndex) {
        this.paragraphIndex = paragraphIndex;
        this.words = new ArrayList<>();
        this.confidence = 1.0;
    }

    // Getters and Setters

    public int getParagraphIndex() {
        return paragraphIndex;
    }

    public void setParagraphIndex(int paragraphIndex) {
        this.paragraphIndex = paragraphIndex;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public List<WordBoundingBox> getWords() {
        return words;
    }

    public void setWords(List<WordBoundingBox> words) {
        this.words = words;
    }

    public WordBoundingBox.BoundingBox getBoundingBox() {
        return boundingBox;
    }

    public void setBoundingBox(WordBoundingBox.BoundingBox boundingBox) {
        this.boundingBox = boundingBox;
    }

    public double getConfidence() {
        return confidence;
    }

    public void setConfidence(double confidence) {
        this.confidence = confidence;
    }

    /**
     * Adds a word to this paragraph.
     *
     * @param word the word to add
     */
    public void addWord(WordBoundingBox word) {
        this.words.add(word);
    }

    /**
     * Calculates and sets the bounding box for this paragraph based on all
     * contained words. The bounding box is the minimum rectangle that encompasses
     * all word bounding boxes.
     */
    public void calculateBoundingBox() {
        if (words.isEmpty()) {
            return;
        }

        float minX = Float.MAX_VALUE;
        float minY = Float.MAX_VALUE;
        float maxX = Float.MIN_VALUE;
        float maxY = Float.MIN_VALUE;

        // Find the extremes of all word bounding boxes
        for (WordBoundingBox word : words) {
            WordBoundingBox.BoundingBox bbox = word.getBoundingBox();
            if (bbox != null) {
                minX = Math.min(minX, bbox.getX());
                minY = Math.min(minY, bbox.getY());
                maxX = Math.max(maxX, bbox.getX() + bbox.getWidth());
                maxY = Math.max(maxY, bbox.getY() + bbox.getHeight());
            }
        }

        // Create the paragraph bounding box
        this.boundingBox = new WordBoundingBox.BoundingBox(
            minX,
            minY,
            maxX - minX,
            maxY - minY
        );
    }

    /**
     * Builds the paragraph text by concatenating all words with spaces.
     * This should be called after all words have been added to the paragraph.
     */
    public void buildText() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            sb.append(words.get(i).getText());
            if (i < words.size() - 1) {
                sb.append(" ");
            }
        }
        this.text = sb.toString();
    }
}
