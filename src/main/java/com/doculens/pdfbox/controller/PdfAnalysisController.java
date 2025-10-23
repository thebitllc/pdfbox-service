package com.doculens.pdfbox.controller;

import com.doculens.pdfbox.model.AnalysisResponse;
import com.doculens.pdfbox.service.PdfAnalysisService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/pdf")
@Tag(name = "PDF Analysis", description = "APIs for PDF document analysis and text extraction")
public class PdfAnalysisController {

    private static final Logger logger = LoggerFactory.getLogger(PdfAnalysisController.class);

    @Autowired
    private PdfAnalysisService pdfAnalysisService;

    @Operation(
        summary = "Analyze PDF document",
        description = "Analyzes a PDF document to detect if it's scanned and extracts text with word-level bounding boxes. " +
                      "Returns structured data similar to Google Document AI format including page dimensions, word coordinates, " +
                      "and document metadata."
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "PDF analyzed successfully",
            content = @Content(
                mediaType = "application/json",
                schema = @Schema(implementation = AnalysisResponse.class),
                examples = @ExampleObject(
                    name = "Example Analysis Response",
                    value = """
                    {
                      "isScanned": false,
                      "fileName": "document.pdf",
                      "totalPages": 1,
                      "pages": [
                        {
                          "pageNumber": 1,
                          "width": 612.0,
                          "height": 792.0,
                          "textContent": "Sample text",
                          "words": [
                            {
                              "text": "Sample",
                              "boundingBox": {
                                "x": 72.0,
                                "y": 720.0,
                                "width": 45.5,
                                "height": 12.0
                              },
                              "confidence": 1.0
                            }
                          ]
                        }
                      ],
                      "metadata": {
                        "author": "John Doe",
                        "title": "Sample Document",
                        "creator": "Microsoft Word",
                        "producer": "Adobe PDF"
                      }
                    }
                    """
                )
            )
        ),
        @ApiResponse(
            responseCode = "400",
            description = "Invalid request (empty file or not a PDF)",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"error\": \"File must be a PDF\"}")
            )
        ),
        @ApiResponse(
            responseCode = "500",
            description = "Internal server error during PDF processing",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"error\": \"Error processing PDF: ...\"}")
            )
        )
    })
    @PostMapping(value = "/analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> analyzePdf(
        @Parameter(
            description = "PDF file to analyze (max 50MB)",
            required = true,
            content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE)
        )
        @RequestParam("file") MultipartFile file) {
        logger.info("Received PDF analysis request for file: {}", file.getOriginalFilename());

        // Validate file
        if (file.isEmpty()) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("File is empty"));
        }

        if (!file.getContentType().equals("application/pdf")) {
            return ResponseEntity.badRequest()
                .body(createErrorResponse("File must be a PDF"));
        }

        try {
            AnalysisResponse response = pdfAnalysisService.analyzePdf(file);
            logger.info("Successfully analyzed PDF: {}", file.getOriginalFilename());
            return ResponseEntity.ok(response);

        } catch (IOException e) {
            logger.error("Error processing PDF: {}", e.getMessage(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(createErrorResponse("Error processing PDF: " + e.getMessage()));
        }
    }

    @Operation(
        summary = "Health check endpoint",
        description = "Returns the health status of the PDF analysis service"
    )
    @ApiResponses(value = {
        @ApiResponse(
            responseCode = "200",
            description = "Service is healthy",
            content = @Content(
                mediaType = "application/json",
                examples = @ExampleObject(value = "{\"status\": \"UP\", \"service\": \"PDFBox Analysis Service\"}")
            )
        )
    })
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "UP");
        response.put("service", "PDFBox Analysis Service");
        return ResponseEntity.ok(response);
    }

    private Map<String, String> createErrorResponse(String message) {
        Map<String, String> error = new HashMap<>();
        error.put("error", message);
        return error;
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleException(Exception e) {
        logger.error("Unhandled exception: {}", e.getMessage(), e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
            .body(createErrorResponse("Internal server error: " + e.getMessage()));
    }
}
