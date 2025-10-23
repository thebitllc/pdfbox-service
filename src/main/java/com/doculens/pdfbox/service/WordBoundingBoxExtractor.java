package com.doculens.pdfbox.service;

import com.doculens.pdfbox.model.WordBoundingBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

/**
 * Custom PDFBox text stripper for extracting words with bounding box coordinates.
 *
 * <p>This class extends {@link PDFTextStripper} to provide word-level text extraction
 * with precise bounding box coordinates. Unlike the default PDFBox text extraction,
 * this implementation:</p>
 * <ul>
 *   <li>Tracks individual character positions using {@link TextPosition}</li>
 *   <li>Groups characters into words based on whitespace</li>
 *   <li>Calculates bounding boxes for each complete word</li>
 *   <li>Converts coordinates to PDF standard (bottom-left origin)</li>
 * </ul>
 *
 * <p><b>Coordinate System:</b></p>
 * <p>PDF uses a bottom-left origin coordinate system where:</p>
 * <ul>
 *   <li>(0, 0) is at the bottom-left corner of the page</li>
 *   <li>X increases to the right</li>
 *   <li>Y increases upward</li>
 *   <li>Units are in points (1 point = 1/72 inch)</li>
 * </ul>
 *
 * <p><b>Usage Example:</b></p>
 * <pre>{@code
 * WordBoundingBoxExtractor extractor = new WordBoundingBoxExtractor();
 * List<WordBoundingBox> words = extractor.extractWords(document, 1);
 * }</pre>
 *
 * @author PDFBox Service
 * @version 1.0
 * @see PDFTextStripper
 * @see TextPosition
 * @see WordBoundingBox
 */
public class WordBoundingBoxExtractor extends PDFTextStripper {

    /**
     * List of extracted words with their bounding boxes.
     * Populated during the text extraction process.
     */
    private List<WordBoundingBox> words;

    /**
     * Temporary storage for characters that make up the current word.
     * Accumulated until a word separator (whitespace) is encountered.
     */
    private List<TextPosition> wordCharacters;

    /**
     * Height of the current page in points.
     * Used for coordinate system conversion (PDFBox Y vs PDF standard Y).
     */
    private float pageHeight;

    /**
     * Constructs a new WordBoundingBoxExtractor.
     *
     * @throws IOException if there's an error initializing the PDFTextStripper
     */
    public WordBoundingBoxExtractor() throws IOException {
        super();
        this.words = new ArrayList<>();
        this.wordCharacters = new ArrayList<>();
    }

    /**
     * Extracts all words with bounding boxes from a specific page.
     *
     * <p>This method processes a single page of a PDF document and extracts
     * all words along with their precise bounding box coordinates. The extraction
     * process involves:</p>
     * <ol>
     *   <li>Retrieving the page dimensions for coordinate conversion</li>
     *   <li>Processing text character by character via PDFBox</li>
     *   <li>Grouping characters into words</li>
     *   <li>Calculating bounding boxes for each word</li>
     * </ol>
     *
     * @param document   the PDF document to extract from
     * @param pageNumber the page number to extract (1-indexed, first page = 1)
     * @return a list of words with their bounding boxes, ordered as they appear in the document
     * @throws IOException if there's an error reading the PDF or extracting text
     */
    public List<WordBoundingBox> extractWords(PDDocument document, int pageNumber) throws IOException {
        // Initialize/reset state for this page
        this.words = new ArrayList<>();
        this.wordCharacters = new ArrayList<>();

        // Get page dimensions for coordinate conversion
        // PDFBox uses 0-indexed pages, but our API uses 1-indexed
        PDPage page = document.getPage(pageNumber - 1);
        this.pageHeight = page.getMediaBox().getHeight();

        // Configure text stripper for single page extraction
        this.setStartPage(pageNumber);
        this.setEndPage(pageNumber);

        // Trigger text extraction - this will call writeString() for each text segment
        StringWriter writer = new StringWriter();
        this.writeText(document, writer);

        // Process any remaining characters at end of page
        // (in case page doesn't end with whitespace)
        processWord();

        return words;
    }

    /**
     * Overridden method called by PDFBox for each text segment.
     *
     * <p>This method is automatically invoked by PDFBox during text extraction.
     * It receives text strings along with position information for each character.
     * We use this to accumulate characters into words, breaking on whitespace.</p>
     *
     * <p><b>Algorithm:</b></p>
     * <ul>
     *   <li>Iterate through each character position</li>
     *   <li>If character is whitespace → process accumulated word</li>
     *   <li>If character is not whitespace → add to current word buffer</li>
     * </ul>
     *
     * @param text          the text string (may contain multiple characters)
     * @param textPositions position information for each character in the text
     * @throws IOException if there's an error during processing
     */
    @Override
    protected void writeString(String text, List<TextPosition> textPositions) throws IOException {
        for (TextPosition position : textPositions) {
            String character = position.getUnicode();

            // Check if this character is a word separator (whitespace, newline, etc.)
            if (character.matches("\\s+") || character.isEmpty()) {
                // Whitespace found - process the accumulated word
                processWord();
            } else {
                // Regular character - add to current word buffer
                wordCharacters.add(position);
            }
        }
    }

    /**
     * Processes accumulated characters into a complete word with bounding box.
     *
     * <p>This method is called when a word boundary is detected (whitespace) or
     * at the end of page extraction. It performs the following steps:</p>
     * <ol>
     *   <li>Concatenates all buffered characters into a word string</li>
     *   <li>Calculates the bounding box by finding min/max coordinates</li>
     *   <li>Converts coordinates from PDFBox format to PDF standard format</li>
     *   <li>Creates a WordBoundingBox object and adds it to the results</li>
     *   <li>Clears the character buffer for the next word</li>
     * </ol>
     *
     * <p><b>Coordinate Conversion:</b></p>
     * <p>PDFBox TextPosition uses a top-left origin where Y increases downward.
     * We convert this to PDF standard (bottom-left origin, Y increases upward)
     * using: {@code standardY = pageHeight - pdfboxY}</p>
     *
     * <p>Empty words (whitespace only) are skipped.</p>
     */
    private void processWord() {
        // Nothing to process if no characters accumulated
        if (wordCharacters.isEmpty()) {
            return;
        }

        // Step 1: Build the word text from accumulated characters
        StringBuilder wordText = new StringBuilder();
        for (TextPosition pos : wordCharacters) {
            wordText.append(pos.getUnicode());
        }

        // Skip empty words or pure whitespace
        // This can happen with special characters or formatting codes
        String word = wordText.toString().trim();
        if (word.isEmpty()) {
            wordCharacters.clear();
            return;
        }

        // Step 2: Calculate bounding box by finding extremes of all character positions
        // Initialize with extreme values for comparison
        float minX = Float.MAX_VALUE;  // Leftmost edge
        float minY = Float.MAX_VALUE;  // Topmost edge (in PDFBox coordinates)
        float maxX = Float.MIN_VALUE;  // Rightmost edge
        float maxY = Float.MIN_VALUE;  // Bottommost edge (in PDFBox coordinates)

        // Find the bounding rectangle that encompasses all characters
        for (TextPosition pos : wordCharacters) {
            // Get character position and dimensions
            float x = pos.getX();           // Left edge of character
            float y = pos.getY();           // Baseline of character
            float width = pos.getWidth();   // Character width
            float height = pos.getHeight(); // Character height

            // Update bounding box extremes
            minX = Math.min(minX, x);                // Leftmost point
            minY = Math.min(minY, y - height);       // Top edge (baseline - height)
            maxX = Math.max(maxX, x + width);        // Rightmost point
            maxY = Math.max(maxY, y);                // Bottom edge (baseline)
        }

        // Step 3: Convert to PDF standard coordinate system (bottom-left origin)
        // PDFBox Y-coordinate increases downward, PDF standard increases upward
        float boxX = minX;                          // X stays the same
        float boxY = pageHeight - maxY;             // Flip Y coordinate
        float boxWidth = maxX - minX;               // Width of bounding box
        float boxHeight = maxY - minY;              // Height of bounding box

        // Step 4: Create the bounding box object
        WordBoundingBox.BoundingBox bbox = new WordBoundingBox.BoundingBox(
            boxX, boxY, boxWidth, boxHeight
        );

        // Step 5: Create the word object with its bounding box
        WordBoundingBox wordBox = new WordBoundingBox(word, bbox);
        words.add(wordBox);

        // Step 6: Clear character buffer for the next word
        wordCharacters.clear();
    }
}
