package com.doculens.pdfbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.ArrayList;

/**
 * Represents a single page in a PDF document with extracted text data.
 *
 * <p>This class contains all the text extraction results for a page, including:
 * <ul>
 *   <li>Page dimensions (width and height in points)</li>
 *   <li>Individual words with their bounding boxes</li>
 *   <li>Paragraphs grouping related words</li>
 *   <li>Full text content of the page</li>
 * </ul>
 *
 * <p>The hierarchical structure mirrors Google Document AI format:
 * Page → Paragraphs → Words</p>
 *
 * @author PDFBox Service
 * @version 1.0
 */
@Schema(description = "Data for a single page including text, words, and paragraphs")
public class PageData {

    /**
     * The page number (1-indexed). First page = 1.
     */
    @JsonProperty("pageNumber")
    @Schema(description = "Page number (1-indexed)", example = "1")
    private int pageNumber;

    /**
     * Width of the page in points (1 point = 1/72 inch).
     * Standard US Letter is 612 points.
     */
    @JsonProperty("width")
    @Schema(description = "Page width in points", example = "612.0")
    private float width;

    /**
     * Height of the page in points (1 point = 1/72 inch).
     * Standard US Letter is 792 points.
     */
    @JsonProperty("height")
    @Schema(description = "Page height in points", example = "792.0")
    private float height;

    /**
     * List of all words on the page with their bounding boxes.
     * Words are ordered as they appear in the document (left-to-right, top-to-bottom).
     */
    @JsonProperty("words")
    @Schema(description = "All words on the page with bounding boxes")
    private List<WordBoundingBox> words;

    /**
     * List of paragraphs on the page.
     * Each paragraph contains a group of related words.
     * Paragraphs are ordered as they appear in the document.
     */
    @JsonProperty("paragraphs")
    @Schema(description = "Paragraphs grouping related words")
    private List<Paragraph> paragraphs;

    /**
     * Complete text content of the page as a single string.
     * This is a convenience field for quick text access without parsing words/paragraphs.
     */
    @JsonProperty("textContent")
    @Schema(description = "Complete text content of the page", example = "This is the full page text...")
    private String textContent;

    /**
     * Default constructor initializing empty collections.
     */
    public PageData() {
        this.words = new ArrayList<>();
        this.paragraphs = new ArrayList<>();
    }

    /**
     * Constructs a PageData with page number and dimensions.
     *
     * @param pageNumber the page number (1-indexed)
     * @param width      the page width in points
     * @param height     the page height in points
     */
    public PageData(int pageNumber, float width, float height) {
        this.pageNumber = pageNumber;
        this.width = width;
        this.height = height;
        this.words = new ArrayList<>();
        this.paragraphs = new ArrayList<>();
    }

    public int getPageNumber() {
        return pageNumber;
    }

    public void setPageNumber(int pageNumber) {
        this.pageNumber = pageNumber;
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

    public List<WordBoundingBox> getWords() {
        return words;
    }

    public void setWords(List<WordBoundingBox> words) {
        this.words = words;
    }

    public String getTextContent() {
        return textContent;
    }

    public void setTextContent(String textContent) {
        this.textContent = textContent;
    }

    public List<Paragraph> getParagraphs() {
        return paragraphs;
    }

    public void setParagraphs(List<Paragraph> paragraphs) {
        this.paragraphs = paragraphs;
    }

    /**
     * Adds a word to the page's word list.
     *
     * @param word the word to add
     */
    public void addWord(WordBoundingBox word) {
        this.words.add(word);
    }

    /**
     * Adds a paragraph to the page's paragraph list.
     *
     * @param paragraph the paragraph to add
     */
    public void addParagraph(Paragraph paragraph) {
        this.paragraphs.add(paragraph);
    }
}
