package com.doculens.pdfbox.service;

import com.doculens.pdfbox.model.AnalysisResponse;
import com.doculens.pdfbox.model.PageData;
import com.doculens.pdfbox.model.Paragraph;
import com.doculens.pdfbox.model.WordBoundingBox;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDDocumentInformation;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDResources;
import org.apache.pdfbox.pdmodel.graphics.PDXObject;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Main service for analyzing PDF documents.
 *
 * <p>This service provides comprehensive PDF analysis including:</p>
 * <ul>
 *   <li>Scanned document detection</li>
 *   <li>Word-level text extraction with bounding boxes</li>
 *   <li>Paragraph grouping and hierarchical structure</li>
 *   <li>PDF metadata extraction</li>
 * </ul>
 *
 * <p><b>Scanned PDF Detection:</b></p>
 * <p>The service uses multiple heuristics to determine if a PDF is scanned:</p>
 * <ol>
 *   <li><b>Text Density:</b> Calculates average characters per page</li>
 *   <li><b>Image Coverage:</b> Analyzes the ratio of image area to page area</li>
 *   <li><b>Combined Analysis:</b> Uses both metrics for accurate detection</li>
 * </ol>
 *
 * <p><b>Output Format:</b></p>
 * <p>The analysis results are structured similar to Google Document AI, providing
 * a hierarchical view: Document → Pages → Paragraphs → Words</p>
 *
 * @author PDFBox Service
 * @version 1.0
 * @see AnalysisResponse
 * @see WordBoundingBoxExtractor
 * @see ParagraphExtractor
 */
@Service
public class PdfAnalysisService {

    private static final Logger logger = LoggerFactory.getLogger(PdfAnalysisService.class);

    /**
     * Threshold for text density (characters per page).
     * If average is below this value, the PDF is likely scanned.
     * Default: 50 characters per page
     */
    private static final int TEXT_THRESHOLD_PER_PAGE = 50;

    /**
     * Threshold for image coverage ratio (0.0 to 1.0).
     * If images cover more than this percentage of the page, it may be scanned.
     * Default: 0.7 (70% coverage)
     */
    private static final double IMAGE_COVERAGE_THRESHOLD = 0.7;

    /**
     * Combined threshold: text per page for documents with significant images.
     * If document has large images AND text is below this, consider it scanned.
     * Default: 200 characters per page
     */
    private static final int TEXT_THRESHOLD_WITH_IMAGES = 200;

    /**
     * Service for grouping words into paragraphs.
     * Auto-wired by Spring dependency injection.
     */
    @Autowired
    private ParagraphExtractor paragraphExtractor;

    public AnalysisResponse analyzePdf(MultipartFile file) throws IOException {
        logger.info("Starting PDF analysis for file: {}", file.getOriginalFilename());

        AnalysisResponse response = new AnalysisResponse();
        response.setFileName(file.getOriginalFilename());

        try (InputStream inputStream = file.getInputStream();
             PDDocument document = PDDocument.load(inputStream)) {

            int totalPages = document.getNumberOfPages();
            response.setTotalPages(totalPages);

            // Extract metadata
            response.setMetadata(extractMetadata(document));

            // Analyze each page
            int totalTextLength = 0;
            boolean hasSignificantImages = false;

            for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                PDPage page = document.getPage(pageNum - 1);
                PageData pageData = new PageData(
                    pageNum,
                    page.getMediaBox().getWidth(),
                    page.getMediaBox().getHeight()
                );

                // Step 1: Extract words with bounding boxes from the page
                WordBoundingBoxExtractor extractor = new WordBoundingBoxExtractor();
                List<WordBoundingBox> words = extractor.extractWords(document, pageNum);
                pageData.setWords(words);
                logger.debug("Extracted {} words from page {}", words.size(), pageNum);

                // Step 2: Group words into paragraphs
                List<Paragraph> paragraphs = paragraphExtractor.extractParagraphs(words);
                pageData.setParagraphs(paragraphs);
                logger.debug("Grouped words into {} paragraphs on page {}", paragraphs.size(), pageNum);

                // Step 3: Calculate full page text content
                StringBuilder pageText = new StringBuilder();
                for (WordBoundingBox word : words) {
                    pageText.append(word.getText()).append(" ");
                }
                String textContent = pageText.toString().trim();
                pageData.setTextContent(textContent);
                totalTextLength += textContent.length();

                // Step 4: Check for large images (for scanned detection)
                if (hasLargeImages(page)) {
                    hasSignificantImages = true;
                }

                response.addPage(pageData);
                logger.debug("Completed page {} - {} words, {} paragraphs",
                           pageNum, words.size(), paragraphs.size());
            }

            // Determine if PDF is scanned using our heuristics
            double avgTextPerPage = (double) totalTextLength / totalPages;

            // A PDF is considered "scanned" if:
            // 1. Very low text density (< 50 chars/page), OR
            // 2. Has large images AND low-ish text density (< 200 chars/page)
            boolean isScanned = avgTextPerPage < TEXT_THRESHOLD_PER_PAGE ||
                              (hasSignificantImages && avgTextPerPage < TEXT_THRESHOLD_WITH_IMAGES);

            response.setScanned(isScanned);
            logger.info("PDF analysis complete. IsScanned: {}, AvgTextPerPage: {:.1f}, HasLargeImages: {}",
                       isScanned, avgTextPerPage, hasSignificantImages);

        } catch (Exception e) {
            logger.error("Error analyzing PDF: {}", e.getMessage(), e);
            throw new IOException("Failed to analyze PDF: " + e.getMessage(), e);
        }

        return response;
    }

    /**
     * Checks if a page contains large images that might indicate a scanned document.
     *
     * <p>This method analyzes the embedded images on a page and calculates the
     * ratio of image area to page area. Large images covering most of the page
     * are a strong indicator of scanned documents.</p>
     *
     * <p><b>Algorithm:</b></p>
     * <ol>
     *   <li>Calculate total page area (width × height)</li>
     *   <li>Sum up areas of all embedded images</li>
     *   <li>Compare ratio to {@link #IMAGE_COVERAGE_THRESHOLD}</li>
     * </ol>
     *
     * <p><b>Note:</b> This is a simplified estimation. It doesn't account for
     * image positioning or overlapping images, just the total pixel count.</p>
     *
     * @param page the PDF page to analyze
     * @return true if images cover more than the threshold percentage of the page
     */
    private boolean hasLargeImages(PDPage page) {
        try {
            // Get page resources (images, fonts, etc.)
            PDResources resources = page.getResources();
            if (resources == null) {
                return false; // No resources means no images
            }

            // Calculate total page area in points²
            float pageArea = page.getMediaBox().getWidth() * page.getMediaBox().getHeight();
            float totalImageArea = 0;

            // Iterate through all XObjects (external objects, including images)
            for (var xObjectName : resources.getXObjectNames()) {
                PDXObject xObject = resources.getXObject(xObjectName);

                // Check if this XObject is an image
                if (xObject instanceof PDImageXObject) {
                    PDImageXObject image = (PDImageXObject) xObject;

                    // Accumulate image area (width × height in pixels)
                    // Note: This is a simplified calculation that doesn't account
                    // for image scaling or positioning on the page
                    totalImageArea += image.getWidth() * image.getHeight();
                }
            }

            // Calculate coverage ratio
            float coverageRatio = totalImageArea / pageArea;

            // Return true if images cover more than the threshold
            boolean hasLargeImages = coverageRatio > IMAGE_COVERAGE_THRESHOLD;

            if (hasLargeImages) {
                logger.debug("Page has large images - coverage ratio: {:.2f}", coverageRatio);
            }

            return hasLargeImages;

        } catch (Exception e) {
            // Log warning but don't fail the analysis
            logger.warn("Error checking for images on page: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Extracts metadata from the PDF document.
     *
     * <p>Retrieves standard PDF metadata including:</p>
     * <ul>
     *   <li>Author - document author</li>
     *   <li>Title - document title</li>
     *   <li>Subject - document subject/description</li>
     *   <li>Creator - application that created the original document</li>
     *   <li>Producer - application that produced the PDF</li>
     * </ul>
     *
     * <p>All fields are optional and may be null if not present in the PDF.</p>
     *
     * @param document the PDF document to extract metadata from
     * @return a PdfMetadata object containing all available metadata fields
     */
    private AnalysisResponse.PdfMetadata extractMetadata(PDDocument document) {
        AnalysisResponse.PdfMetadata metadata = new AnalysisResponse.PdfMetadata();

        // Get document information dictionary
        PDDocumentInformation info = document.getDocumentInformation();

        if (info != null) {
            // Extract standard metadata fields
            metadata.setAuthor(info.getAuthor());
            metadata.setTitle(info.getTitle());
            metadata.setSubject(info.getSubject());
            metadata.setCreator(info.getCreator());     // e.g., "Microsoft Word"
            metadata.setProducer(info.getProducer());   // e.g., "Adobe PDF Library"

            logger.debug("Extracted metadata - Title: {}, Author: {}, Creator: {}",
                       info.getTitle(), info.getAuthor(), info.getCreator());
        } else {
            logger.debug("No metadata found in PDF document");
        }

        return metadata;
    }
}
