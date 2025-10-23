package com.doculens.pdfbox.service;

import com.doculens.pdfbox.model.Paragraph;
import com.doculens.pdfbox.model.WordBoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the ParagraphExtractor service.
 *
 * <p>Tests the paragraph grouping algorithm with various scenarios:</p>
 * <ul>
 *   <li>Empty input handling</li>
 *   <li>Single line text</li>
 *   <li>Multiple lines forming paragraphs</li>
 *   <li>Large vertical gaps creating paragraph breaks</li>
 * </ul>
 *
 * @author PDFBox Service
 * @version 1.0
 */
@DisplayName("Paragraph Extractor Tests")
class ParagraphExtractorTest {

    private ParagraphExtractor paragraphExtractor;

    @BeforeEach
    void setUp() {
        paragraphExtractor = new ParagraphExtractor();
    }

    /**
     * Helper method to create a word with a bounding box.
     *
     * @param text the word text
     * @param x    X coordinate
     * @param y    Y coordinate
     * @param w    width
     * @param h    height
     * @return a WordBoundingBox object
     */
    private WordBoundingBox createWord(String text, float x, float y, float w, float h) {
        WordBoundingBox.BoundingBox bbox = new WordBoundingBox.BoundingBox(x, y, w, h);
        return new WordBoundingBox(text, bbox);
    }

    @Test
    @DisplayName("Should handle empty word list")
    void testEmptyWordList() {
        // Arrange
        List<WordBoundingBox> words = new ArrayList<>();

        // Act
        List<Paragraph> paragraphs = paragraphExtractor.extractParagraphs(words);

        // Assert
        assertNotNull(paragraphs, "Result should not be null");
        assertTrue(paragraphs.isEmpty(), "Should return empty list for empty input");
    }

    @Test
    @DisplayName("Should throw exception for null input")
    void testNullInput() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class,
            () -> paragraphExtractor.extractParagraphs(null),
            "Should throw exception for null input");
    }

    @Test
    @DisplayName("Should create one paragraph from words on same line")
    void testSingleLineParagraph() {
        // Arrange - words on the same line (same Y coordinate)
        List<WordBoundingBox> words = new ArrayList<>();
        words.add(createWord("The", 10, 100, 20, 12));
        words.add(createWord("quick", 35, 100, 30, 12));
        words.add(createWord("brown", 70, 100, 30, 12));
        words.add(createWord("fox", 105, 100, 20, 12));

        // Act
        List<Paragraph> paragraphs = paragraphExtractor.extractParagraphs(words);

        // Assert
        assertEquals(1, paragraphs.size(), "Should create one paragraph");

        Paragraph para = paragraphs.get(0);
        assertEquals(0, para.getParagraphIndex(), "First paragraph should have index 0");
        assertEquals(4, para.getWords().size(), "Paragraph should contain all 4 words");
        assertEquals("The quick brown fox", para.getText(),
            "Paragraph text should be concatenated words");
        assertNotNull(para.getBoundingBox(), "Paragraph should have bounding box");
    }

    @Test
    @DisplayName("Should create one paragraph from words on adjacent lines")
    void testMultipleLinesSingleParagraph() {
        // Arrange - two lines with small vertical gap (same paragraph)
        List<WordBoundingBox> words = new ArrayList<>();

        // First line at Y=100
        words.add(createWord("The", 10, 100, 20, 12));
        words.add(createWord("quick", 35, 100, 30, 12));

        // Second line at Y=115 (15 points below, normal line spacing)
        words.add(createWord("brown", 10, 115, 30, 12));
        words.add(createWord("fox", 45, 115, 20, 12));

        // Act
        List<Paragraph> paragraphs = paragraphExtractor.extractParagraphs(words);

        // Assert
        assertEquals(1, paragraphs.size(),
            "Should create one paragraph for closely spaced lines");

        Paragraph para = paragraphs.get(0);
        assertEquals(4, para.getWords().size(),
            "Paragraph should contain all words from both lines");
        assertTrue(para.getText().contains("The") && para.getText().contains("fox"),
            "Paragraph should contain words from both lines");
    }

    @Test
    @DisplayName("Should create multiple paragraphs with large vertical gaps")
    void testMultipleParagraphsWithGaps() {
        // Arrange - two groups of words with large vertical gap
        List<WordBoundingBox> words = new ArrayList<>();

        // First paragraph at Y=100
        words.add(createWord("First", 10, 100, 30, 12));
        words.add(createWord("paragraph", 45, 100, 50, 12));

        // Second paragraph at Y=150 (50 points below, large gap)
        words.add(createWord("Second", 10, 150, 40, 12));
        words.add(createWord("paragraph", 55, 150, 50, 12));

        // Act
        List<Paragraph> paragraphs = paragraphExtractor.extractParagraphs(words);

        // Assert
        assertEquals(2, paragraphs.size(),
            "Should create two paragraphs due to large vertical gap");

        // First paragraph
        Paragraph para1 = paragraphs.get(0);
        assertEquals(0, para1.getParagraphIndex(), "First paragraph index");
        assertEquals(2, para1.getWords().size(), "First paragraph word count");
        assertTrue(para1.getText().contains("First"), "First paragraph content");

        // Second paragraph
        Paragraph para2 = paragraphs.get(1);
        assertEquals(1, para2.getParagraphIndex(), "Second paragraph index");
        assertEquals(2, para2.getWords().size(), "Second paragraph word count");
        assertTrue(para2.getText().contains("Second"), "Second paragraph content");
    }

    @Test
    @DisplayName("Should calculate paragraph bounding boxes correctly")
    void testParagraphBoundingBoxCalculation() {
        // Arrange - words with known positions
        List<WordBoundingBox> words = new ArrayList<>();
        words.add(createWord("Word1", 10, 100, 30, 12));  // leftmost
        words.add(createWord("Word2", 50, 100, 30, 12));  // rightmost

        // Act
        List<Paragraph> paragraphs = paragraphExtractor.extractParagraphs(words);

        // Assert
        assertEquals(1, paragraphs.size(), "Should have one paragraph");

        WordBoundingBox.BoundingBox bbox = paragraphs.get(0).getBoundingBox();
        assertNotNull(bbox, "Bounding box should exist");

        // Verify bounding box encompasses all words
        assertEquals(10, bbox.getX(), 0.1, "Bounding box X (leftmost)");
        assertEquals(100, bbox.getY(), 0.1, "Bounding box Y (topmost)");

        // Width should span from leftmost to rightmost word
        assertTrue(bbox.getWidth() >= 70, "Bounding box width should span all words");

        // Height should be at least the word height
        assertTrue(bbox.getHeight() >= 12, "Bounding box height should cover words");
    }
}
