package com.doculens.pdfbox.service;

import com.doculens.pdfbox.model.Paragraph;
import com.doculens.pdfbox.model.WordBoundingBox;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Service for grouping words into paragraphs based on spatial relationships.
 *
 * <p>This class analyzes word positions and groups them into logical paragraphs
 * similar to how Google Document AI and Tesseract OCR organize text hierarchically.
 * The grouping algorithm considers:</p>
 *
 * <ul>
 *   <li><b>Vertical spacing:</b> Large vertical gaps indicate paragraph breaks</li>
 *   <li><b>Line breaks:</b> Y-coordinate changes indicate new lines</li>
 *   <li><b>Horizontal alignment:</b> Words with similar Y-coordinates are on the same line</li>
 * </ul>
 *
 * <p><b>Algorithm Overview:</b></p>
 * <ol>
 *   <li>Sort words by Y-coordinate (top to bottom) then X-coordinate (left to right)</li>
 *   <li>Group words into lines based on Y-coordinate similarity</li>
 *   <li>Group lines into paragraphs based on vertical spacing thresholds</li>
 *   <li>Calculate bounding boxes for each paragraph</li>
 * </ol>
 *
 * @author PDFBox Service
 * @version 1.0
 * @see Paragraph
 * @see WordBoundingBox
 */
@Component
public class ParagraphExtractor {

    private static final Logger logger = LoggerFactory.getLogger(ParagraphExtractor.class);

    /**
     * Threshold for considering two words to be on the same line (in points).
     * If the Y-coordinate difference is less than this value, words are on the same line.
     * Default: 3 points (~0.04 inches or ~1mm)
     */
    private static final float SAME_LINE_THRESHOLD = 3.0f;

    /**
     * Multiplier for determining paragraph breaks based on average line height.
     * A vertical gap larger than (average_line_height * PARAGRAPH_BREAK_MULTIPLIER)
     * indicates a new paragraph.
     * Default: 1.5 (i.e., 1.5x the normal line spacing)
     */
    private static final double PARAGRAPH_BREAK_MULTIPLIER = 1.5;

    /**
     * Minimum vertical gap (in points) to consider a paragraph break.
     * Even if calculated threshold is smaller, this minimum is enforced.
     * Default: 8 points (~0.11 inches or ~2.8mm)
     */
    private static final float MIN_PARAGRAPH_GAP = 8.0f;

    /**
     * Groups a list of words into paragraphs based on their spatial layout.
     *
     * <p>This method implements a multi-step algorithm:</p>
     * <ol>
     *   <li>Pre-process: Filter out null or invalid words</li>
     *   <li>Group words into lines based on Y-coordinate proximity</li>
     *   <li>Calculate average line height from all detected lines</li>
     *   <li>Group lines into paragraphs using vertical spacing analysis</li>
     *   <li>Post-process: Build text and calculate bounding boxes</li>
     * </ol>
     *
     * @param words the list of words to group (should be from a single page)
     * @return a list of paragraphs, each containing related words
     * @throws IllegalArgumentException if words list is null
     */
    public List<Paragraph> extractParagraphs(List<WordBoundingBox> words) {
        if (words == null) {
            throw new IllegalArgumentException("Words list cannot be null");
        }

        List<Paragraph> paragraphs = new ArrayList<>();

        // Handle empty input
        if (words.isEmpty()) {
            logger.debug("No words to group into paragraphs");
            return paragraphs;
        }

        logger.debug("Grouping {} words into paragraphs", words.size());

        // Step 1: Group words into lines
        List<List<WordBoundingBox>> lines = groupWordsIntoLines(words);
        logger.debug("Grouped words into {} lines", lines.size());

        if (lines.isEmpty()) {
            return paragraphs;
        }

        // Step 2: Calculate average line height for paragraph detection
        float avgLineHeight = calculateAverageLineHeight(lines);
        logger.debug("Average line height: {} points", avgLineHeight);

        // Step 3: Calculate dynamic paragraph break threshold
        // Use the larger of: (avgLineHeight * multiplier) or minimum gap
        float paragraphBreakThreshold = Math.max(
            avgLineHeight * (float) PARAGRAPH_BREAK_MULTIPLIER,
            MIN_PARAGRAPH_GAP
        );
        logger.debug("Paragraph break threshold: {} points", paragraphBreakThreshold);

        // Step 4: Group lines into paragraphs
        paragraphs = groupLinesIntoParagraphs(lines, paragraphBreakThreshold);
        logger.debug("Created {} paragraphs", paragraphs.size());

        return paragraphs;
    }

    /**
     * Groups words into lines based on their Y-coordinates.
     *
     * <p>Words with Y-coordinates within {@link #SAME_LINE_THRESHOLD} of each other
     * are considered to be on the same line. The algorithm maintains the original
     * left-to-right order of words within each line.</p>
     *
     * @param words the list of words to group into lines
     * @return a list of lines, where each line is a list of words
     */
    private List<List<WordBoundingBox>> groupWordsIntoLines(List<WordBoundingBox> words) {
        List<List<WordBoundingBox>> lines = new ArrayList<>();

        // Sort words by Y coordinate (top to bottom), then X coordinate (left to right)
        List<WordBoundingBox> sortedWords = new ArrayList<>(words);
        sortedWords.sort((w1, w2) -> {
            WordBoundingBox.BoundingBox b1 = w1.getBoundingBox();
            WordBoundingBox.BoundingBox b2 = w2.getBoundingBox();

            // Primary sort: Y coordinate (top to bottom)
            int yCompare = Float.compare(b1.getY(), b2.getY());
            if (yCompare != 0) {
                return yCompare;
            }

            // Secondary sort: X coordinate (left to right)
            return Float.compare(b1.getX(), b2.getX());
        });

        // Group words into lines
        List<WordBoundingBox> currentLine = new ArrayList<>();
        float currentLineY = -1;

        for (WordBoundingBox word : sortedWords) {
            WordBoundingBox.BoundingBox bbox = word.getBoundingBox();
            if (bbox == null) {
                continue; // Skip words without bounding boxes
            }

            float wordY = bbox.getY();

            // Check if this word belongs to the current line
            if (currentLine.isEmpty() || Math.abs(wordY - currentLineY) < SAME_LINE_THRESHOLD) {
                // Same line or first word
                currentLine.add(word);
                if (currentLine.size() == 1) {
                    currentLineY = wordY; // Set baseline for this line
                }
            } else {
                // New line detected
                if (!currentLine.isEmpty()) {
                    lines.add(currentLine);
                }
                currentLine = new ArrayList<>();
                currentLine.add(word);
                currentLineY = wordY;
            }
        }

        // Add the last line
        if (!currentLine.isEmpty()) {
            lines.add(currentLine);
        }

        return lines;
    }

    /**
     * Calculates the average height of lines for paragraph detection.
     *
     * <p>The line height is determined by finding the maximum height of all
     * bounding boxes within each line, then averaging across all lines.
     * This value is used to set the paragraph break threshold.</p>
     *
     * @param lines the list of lines (each line is a list of words)
     * @return the average line height in points, or 12.0 if calculation fails
     */
    private float calculateAverageLineHeight(List<List<WordBoundingBox>> lines) {
        float totalHeight = 0;
        int validLines = 0;

        for (List<WordBoundingBox> line : lines) {
            float maxHeight = 0;

            // Find the maximum height in this line
            for (WordBoundingBox word : line) {
                WordBoundingBox.BoundingBox bbox = word.getBoundingBox();
                if (bbox != null) {
                    maxHeight = Math.max(maxHeight, bbox.getHeight());
                }
            }

            if (maxHeight > 0) {
                totalHeight += maxHeight;
                validLines++;
            }
        }

        // Return average, or default to 12 points (typical text height) if no valid lines
        return validLines > 0 ? (totalHeight / validLines) : 12.0f;
    }

    /**
     * Groups lines into paragraphs based on vertical spacing.
     *
     * <p>A new paragraph is started when the vertical gap between consecutive lines
     * exceeds the paragraph break threshold. This mimics natural paragraph breaks
     * in documents.</p>
     *
     * @param lines                    the list of lines to group
     * @param paragraphBreakThreshold  the minimum gap (in points) to start a new paragraph
     * @return a list of paragraphs with all fields populated
     */
    private List<Paragraph> groupLinesIntoParagraphs(
            List<List<WordBoundingBox>> lines,
            float paragraphBreakThreshold) {

        List<Paragraph> paragraphs = new ArrayList<>();
        Paragraph currentParagraph = new Paragraph(0);
        float previousLineBottomY = -1;

        for (int lineIndex = 0; lineIndex < lines.size(); lineIndex++) {
            List<WordBoundingBox> line = lines.get(lineIndex);

            if (line.isEmpty()) {
                continue;
            }

            // Calculate the top Y of this line (minimum Y among all words)
            float lineTopY = Float.MAX_VALUE;
            for (WordBoundingBox word : line) {
                WordBoundingBox.BoundingBox bbox = word.getBoundingBox();
                if (bbox != null) {
                    lineTopY = Math.min(lineTopY, bbox.getY());
                }
            }

            // Check if we should start a new paragraph
            if (previousLineBottomY > 0) {
                float gap = lineTopY - previousLineBottomY;

                if (gap > paragraphBreakThreshold) {
                    // Large gap detected - start new paragraph
                    finalizeParagraph(currentParagraph);
                    paragraphs.add(currentParagraph);

                    currentParagraph = new Paragraph(paragraphs.size());
                    logger.trace("New paragraph #{} started due to gap of {} points",
                               paragraphs.size(), gap);
                }
            }

            // Add all words from this line to the current paragraph
            for (WordBoundingBox word : line) {
                currentParagraph.addWord(word);
            }

            // Update bottom Y for next iteration
            float lineBottomY = -1;
            for (WordBoundingBox word : line) {
                WordBoundingBox.BoundingBox bbox = word.getBoundingBox();
                if (bbox != null) {
                    float bottom = bbox.getY() + bbox.getHeight();
                    lineBottomY = Math.max(lineBottomY, bottom);
                }
            }
            previousLineBottomY = lineBottomY;
        }

        // Add the last paragraph
        if (!currentParagraph.getWords().isEmpty()) {
            finalizeParagraph(currentParagraph);
            paragraphs.add(currentParagraph);
        }

        return paragraphs;
    }

    /**
     * Finalizes a paragraph by building its text and calculating its bounding box.
     *
     * <p>This method should be called before adding a paragraph to the final list.
     * It performs the following operations:</p>
     * <ul>
     *   <li>Concatenates all word texts with spaces</li>
     *   <li>Calculates the encompassing bounding box</li>
     * </ul>
     *
     * @param paragraph the paragraph to finalize
     */
    private void finalizeParagraph(Paragraph paragraph) {
        paragraph.buildText();
        paragraph.calculateBoundingBox();

        logger.trace("Finalized paragraph #{} with {} words: '{}'",
                   paragraph.getParagraphIndex(),
                   paragraph.getWords().size(),
                   paragraph.getText().length() > 50 ?
                       paragraph.getText().substring(0, 50) + "..." :
                       paragraph.getText());
    }
}
