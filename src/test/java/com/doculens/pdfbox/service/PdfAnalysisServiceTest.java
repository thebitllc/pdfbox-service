package com.doculens.pdfbox.service;

import com.doculens.pdfbox.model.AnalysisResponse;
import com.doculens.pdfbox.model.PageData;
import com.doculens.pdfbox.model.Paragraph;
import com.doculens.pdfbox.model.WordBoundingBox;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive test suite for PDF analysis functionality.
 *
 * <p>Tests cover:</p>
 * <ul>
 *   <li>Text PDF processing (word and paragraph extraction)</li>
 *   <li>Scanned PDF detection</li>
 *   <li>Bounding box accuracy</li>
 *   <li>Metadata extraction</li>
 *   <li>Edge cases and error handling</li>
 * </ul>
 *
 * <p><b>Test Data:</b></p>
 * <p>Tests use sample PDFs located in the {@code samples/} directory:</p>
 * <ul>
 *   <li>{@code sample_text_pdf1.pdf} - Regular text-based PDF</li>
 *   <li>{@code sample_scanned_pdf1.pdf} - Scanned document image</li>
 * </ul>
 *
 * @author PDFBox Service
 * @version 1.0
 */
@SpringBootTest
@DisplayName("PDF Analysis Service Tests")
class PdfAnalysisServiceTest {

    @Autowired
    private PdfAnalysisService pdfAnalysisService;

    private static final String SAMPLES_DIR = "samples";
    private static final String TEXT_PDF = "sample_text_pdf1.pdf";
    private static final String SCANNED_PDF = "sample_scanned_pdf1.pdf";

    /**
     * Setup method run before each test.
     * Verifies that required test files exist.
     */
    @BeforeEach
    void setUp() {
        // Verify test samples directory exists
        File samplesDir = new File(SAMPLES_DIR);
        if (!samplesDir.exists()) {
            fail("Samples directory not found: " + SAMPLES_DIR +
                 ". Please create it and add test PDF files.");
        }
    }

    /**
     * Helper method to create a MultipartFile from a file path.
     *
     * @param fileName the name of the file in the samples directory
     * @return a MultipartFile representing the PDF
     * @throws IOException if the file cannot be read
     */
    private MultipartFile createMultipartFile(String fileName) throws IOException {
        File file = new File(SAMPLES_DIR, fileName);

        if (!file.exists()) {
            fail("Test file not found: " + file.getAbsolutePath() +
                 ". Please add this file to the samples directory.");
        }

        FileInputStream input = new FileInputStream(file);
        return new MockMultipartFile(
            "file",
            fileName,
            "application/pdf",
            input
        );
    }

    // ========== TEXT PDF TESTS ==========

    @Test
    @DisplayName("Should successfully analyze text-based PDF")
    void testAnalyzeTextPdf() throws IOException {
        // Arrange
        MultipartFile file = createMultipartFile(TEXT_PDF);

        // Act
        AnalysisResponse response = pdfAnalysisService.analyzePdf(file);

        // Assert - Basic response structure
        assertNotNull(response, "Response should not be null");
        assertEquals(TEXT_PDF, response.getFileName(), "File name should match");
        assertTrue(response.getTotalPages() > 0, "Should have at least one page");

        // Assert - PDF should NOT be detected as scanned
        assertFalse(response.isScanned(),
            "Text-based PDF should not be detected as scanned");

        // Assert - Pages should be populated
        assertNotNull(response.getPages(), "Pages list should not be null");
        assertEquals(response.getTotalPages(), response.getPages().size(),
            "Number of pages should match totalPages");

        System.out.println("Text PDF Analysis Results:");
        System.out.println("  Total Pages: " + response.getTotalPages());
        System.out.println("  Is Scanned: " + response.isScanned());
        System.out.println("  Total Words: " + countTotalWords(response));
        System.out.println("  Total Paragraphs: " + countTotalParagraphs(response));
    }

    @Test
    @DisplayName("Should extract words with valid bounding boxes from text PDF")
    void testExtractWordsFromTextPdf() throws IOException {
        // Arrange
        MultipartFile file = createMultipartFile(TEXT_PDF);

        // Act
        AnalysisResponse response = pdfAnalysisService.analyzePdf(file);

        // Assert
        assertTrue(response.getPages().size() > 0, "Should have pages");

        PageData firstPage = response.getPages().get(0);

        // Check words extraction
        assertNotNull(firstPage.getWords(), "Words list should not be null");
        assertTrue(firstPage.getWords().size() > 0,
            "Text PDF should have extracted words");

        // Verify each word has valid data
        for (WordBoundingBox word : firstPage.getWords()) {
            assertNotNull(word.getText(), "Word text should not be null");
            assertFalse(word.getText().trim().isEmpty(),
                "Word text should not be empty");

            WordBoundingBox.BoundingBox bbox = word.getBoundingBox();
            assertNotNull(bbox, "Bounding box should not be null");

            // Bounding box coordinates should be non-negative
            assertTrue(bbox.getX() >= 0, "X coordinate should be non-negative");
            assertTrue(bbox.getY() >= 0, "Y coordinate should be non-negative");
            assertTrue(bbox.getWidth() > 0, "Width should be positive");
            assertTrue(bbox.getHeight() > 0, "Height should be positive");

            // Confidence should be 1.0 for extracted text
            assertEquals(1.0, word.getConfidence(), 0.001,
                "Confidence should be 1.0 for PDF text extraction");
        }

        System.out.println("First page word count: " + firstPage.getWords().size());
        System.out.println("Sample words: " +
            firstPage.getWords().stream()
                .limit(10)
                .map(WordBoundingBox::getText)
                .reduce((a, b) -> a + " " + b)
                .orElse("(none)"));
    }

    @Test
    @DisplayName("Should group words into paragraphs from text PDF")
    void testExtractParagraphsFromTextPdf() throws IOException {
        // Arrange
        MultipartFile file = createMultipartFile(TEXT_PDF);

        // Act
        AnalysisResponse response = pdfAnalysisService.analyzePdf(file);

        // Assert
        PageData firstPage = response.getPages().get(0);

        // Check paragraphs extraction
        assertNotNull(firstPage.getParagraphs(), "Paragraphs list should not be null");
        assertTrue(firstPage.getParagraphs().size() > 0,
            "Text PDF should have extracted paragraphs");

        // Verify each paragraph has valid data
        for (Paragraph paragraph : firstPage.getParagraphs()) {
            assertNotNull(paragraph.getText(), "Paragraph text should not be null");
            assertFalse(paragraph.getText().trim().isEmpty(),
                "Paragraph text should not be empty");

            assertNotNull(paragraph.getWords(), "Paragraph words should not be null");
            assertTrue(paragraph.getWords().size() > 0,
                "Paragraph should contain words");

            assertNotNull(paragraph.getBoundingBox(),
                "Paragraph bounding box should not be null");

            // Verify paragraph index
            assertTrue(paragraph.getParagraphIndex() >= 0,
                "Paragraph index should be non-negative");
        }

        System.out.println("First page paragraph count: " +
            firstPage.getParagraphs().size());
        System.out.println("Sample paragraph text: " +
            firstPage.getParagraphs().get(0).getText().substring(0,
                Math.min(100, firstPage.getParagraphs().get(0).getText().length())) +
            "...");
    }

    @Test
    @DisplayName("Should extract page dimensions correctly from text PDF")
    void testExtractPageDimensionsFromTextPdf() throws IOException {
        // Arrange
        MultipartFile file = createMultipartFile(TEXT_PDF);

        // Act
        AnalysisResponse response = pdfAnalysisService.analyzePdf(file);

        // Assert
        PageData firstPage = response.getPages().get(0);

        // Page dimensions should be positive
        assertTrue(firstPage.getWidth() > 0, "Page width should be positive");
        assertTrue(firstPage.getHeight() > 0, "Page height should be positive");

        // Typical page dimensions (e.g., US Letter is 612 x 792 points)
        assertTrue(firstPage.getWidth() > 100, "Page width seems too small");
        assertTrue(firstPage.getHeight() > 100, "Page height seems too small");

        System.out.println("Page dimensions: " +
            firstPage.getWidth() + " x " + firstPage.getHeight() + " points");
    }

    @Test
    @DisplayName("Should extract text content from text PDF")
    void testExtractTextContentFromTextPdf() throws IOException {
        // Arrange
        MultipartFile file = createMultipartFile(TEXT_PDF);

        // Act
        AnalysisResponse response = pdfAnalysisService.analyzePdf(file);

        // Assert
        PageData firstPage = response.getPages().get(0);

        assertNotNull(firstPage.getTextContent(), "Text content should not be null");
        assertFalse(firstPage.getTextContent().trim().isEmpty(),
            "Text content should not be empty");

        // Text content should contain actual text
        assertTrue(firstPage.getTextContent().length() > 10,
            "Text content should have substantial length");

        System.out.println("Text content length: " +
            firstPage.getTextContent().length() + " characters");
        System.out.println("Text preview: " +
            firstPage.getTextContent().substring(0,
                Math.min(200, firstPage.getTextContent().length())) + "...");
    }

    // ========== SCANNED PDF TESTS ==========

    @Test
    @DisplayName("Should detect scanned PDF correctly")
    void testDetectScannedPdf() throws IOException {
        // Arrange
        MultipartFile file = createMultipartFile(SCANNED_PDF);

        // Act
        AnalysisResponse response = pdfAnalysisService.analyzePdf(file);

        // Assert - Basic response structure
        assertNotNull(response, "Response should not be null");
        assertEquals(SCANNED_PDF, response.getFileName(), "File name should match");

        // Assert - PDF SHOULD be detected as scanned
        assertTrue(response.isScanned(),
            "Scanned PDF should be detected as scanned");

        // Assert - Should have minimal or no text extracted
        int totalWords = countTotalWords(response);
        assertTrue(totalWords < 100,
            "Scanned PDF should have few or no extracted words (got " + totalWords + ")");

        System.out.println("Scanned PDF Analysis Results:");
        System.out.println("  Total Pages: " + response.getTotalPages());
        System.out.println("  Is Scanned: " + response.isScanned());
        System.out.println("  Total Words: " + totalWords);
    }

    @Test
    @DisplayName("Should have minimal text extraction from scanned PDF")
    void testMinimalTextFromScannedPdf() throws IOException {
        // Arrange
        MultipartFile file = createMultipartFile(SCANNED_PDF);

        // Act
        AnalysisResponse response = pdfAnalysisService.analyzePdf(file);

        // Assert
        PageData firstPage = response.getPages().get(0);

        // Scanned PDFs typically have no extractable text
        int wordCount = firstPage.getWords() != null ? firstPage.getWords().size() : 0;
        assertTrue(wordCount < 50,
            "Scanned PDF should have minimal words (got " + wordCount + ")");

        System.out.println("Words extracted from scanned PDF: " + wordCount);
    }

    // ========== METADATA TESTS ==========

    @Test
    @DisplayName("Should extract PDF metadata")
    void testExtractMetadata() throws IOException {
        // Arrange
        MultipartFile file = createMultipartFile(TEXT_PDF);

        // Act
        AnalysisResponse response = pdfAnalysisService.analyzePdf(file);

        // Assert
        assertNotNull(response.getMetadata(), "Metadata should not be null");

        // Note: Metadata fields may be null if not present in the PDF
        // We just verify the metadata object exists
        AnalysisResponse.PdfMetadata metadata = response.getMetadata();

        System.out.println("Metadata:");
        System.out.println("  Title: " + metadata.getTitle());
        System.out.println("  Author: " + metadata.getAuthor());
        System.out.println("  Creator: " + metadata.getCreator());
        System.out.println("  Producer: " + metadata.getProducer());
        System.out.println("  Subject: " + metadata.getSubject());
    }

    // ========== HELPER METHODS ==========

    /**
     * Counts total words across all pages in the response.
     *
     * @param response the analysis response
     * @return total word count
     */
    private int countTotalWords(AnalysisResponse response) {
        return response.getPages().stream()
            .mapToInt(page -> page.getWords() != null ? page.getWords().size() : 0)
            .sum();
    }

    /**
     * Counts total paragraphs across all pages in the response.
     *
     * @param response the analysis response
     * @return total paragraph count
     */
    private int countTotalParagraphs(AnalysisResponse response) {
        return response.getPages().stream()
            .mapToInt(page -> page.getParagraphs() != null ? page.getParagraphs().size() : 0)
            .sum();
    }
}
