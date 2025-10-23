# pdfbox-service

A REST API service for PDF analysis using Apache PDFBox. This service can detect if a PDF is scanned and extract text with word-level bounding boxes, similar to Google Document AI.

## Features

- **Scanned PDF Detection**: Automatically detects if a PDF is a scanned document or contains selectable text
- **Text Extraction**: Extracts all text from PDF documents
- **Word-Level Bounding Boxes**: Provides precise bounding box coordinates for each word
- **Paragraph Grouping**: Intelligently groups words into paragraphs like Tesseract OCR and Google Document AI
- **Hierarchical Structure**: Page → Paragraphs → Words organization
- **PDF Metadata**: Extracts document metadata (author, title, etc.)
- **Docker Support**: Fully containerized application
- **RESTful API**: Simple HTTP endpoints for easy integration
- **Swagger/OpenAPI Documentation**: Interactive API documentation with try-it-out functionality
- **Comprehensive Tests**: Includes test suite with sample PDFs

## API Documentation

Once the service is running, you can access the interactive API documentation:

- **Swagger UI**: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)
- **OpenAPI JSON**: [http://localhost:8080/api-docs](http://localhost:8080/api-docs)

The Swagger UI provides:
- Interactive API testing with file upload
- Complete request/response examples
- Detailed schema documentation
- Try-it-out functionality for all endpoints

## API Endpoints

### Analyze PDF
```
POST /api/pdf/analyze
Content-Type: multipart/form-data
```

**Request:**
- Form parameter: `file` (PDF file, max 50MB)

**Response:**
```json
{
  "isScanned": false,
  "fileName": "document.pdf",
  "totalPages": 2,
  "pages": [
    {
      "pageNumber": 1,
      "width": 612.0,
      "height": 792.0,
      "textContent": "Sample text...",
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
      ],
      "paragraphs": [
        {
          "paragraphIndex": 0,
          "text": "Sample text from first paragraph.",
          "boundingBox": {
            "x": 72.0,
            "y": 720.0,
            "width": 400.0,
            "height": 14.0
          },
          "words": [...],
          "confidence": 1.0
        }
      ]
    }
  ],
  "metadata": {
    "author": "John Doe",
    "title": "Sample Document",
    "subject": null,
    "creator": "Microsoft Word",
    "producer": "Adobe PDF Library"
  }
}
```

### Health Check
```
GET /api/pdf/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "PDFBox Analysis Service"
}
```

## Quick Start

### Using Docker Compose (Recommended)

1. Clone the repository:
```bash
git clone https://github.com/yourusername/pdfbox-service.git
cd pdfbox-service
```

2. Build and run with Docker Compose:
```bash
docker-compose up --build
```

3. The service will be available at `http://localhost:8080`

### Using Docker

Build the image:
```bash
docker build -t pdfbox-service .
```

Run the container:
```bash
docker run -p 8080:8080 pdfbox-service
```

### Local Development

Prerequisites:
- Java 17 or higher
- Maven 3.6+

1. Build the project:
```bash
mvn clean package
```

2. Run the application:
```bash
java -jar target/pdfbox-service-1.0.0.jar
```

Or run directly with Maven:
```bash
mvn spring-boot:run
```

## Usage Examples

### cURL
```bash
curl -X POST http://localhost:8080/api/pdf/analyze \
  -F "file=@/path/to/your/document.pdf" \
  -H "Content-Type: multipart/form-data"
```

### Python
```python
import requests

url = "http://localhost:8080/api/pdf/analyze"
files = {"file": open("document.pdf", "rb")}

response = requests.post(url, files=files)
result = response.json()

print(f"Is Scanned: {result['isScanned']}")
print(f"Total Pages: {result['totalPages']}")

for page in result['pages']:
    print(f"Page {page['pageNumber']}: {len(page['words'])} words")
```

### JavaScript (Node.js)
```javascript
const FormData = require('form-data');
const fs = require('fs');
const axios = require('axios');

const form = new FormData();
form.append('file', fs.createReadStream('document.pdf'));

axios.post('http://localhost:8080/api/pdf/analyze', form, {
  headers: form.getHeaders()
})
.then(response => {
  console.log('Is Scanned:', response.data.isScanned);
  console.log('Total Pages:', response.data.totalPages);
})
.catch(error => console.error('Error:', error));
```

## Configuration

Configuration can be modified in `src/main/resources/application.properties`:

```properties
# Server port
server.port=8080

# Max file upload size
spring.servlet.multipart.max-file-size=50MB
spring.servlet.multipart.max-request-size=50MB

# Logging level
logging.level.com.doculens.pdfbox=INFO
```

## Scanned PDF Detection Logic

The service uses multiple heuristics to detect scanned PDFs:

1. **Text Density**: Calculates average characters per page
   - Below 50 characters/page → likely scanned

2. **Image Coverage**: Analyzes image-to-page area ratio
   - Images covering >70% of page with minimal text → likely scanned

3. **Combined Analysis**: Uses both metrics for accurate detection

## Bounding Box Coordinate System

Bounding boxes use the PDF coordinate system:
- Origin (0, 0) is at the **bottom-left** corner
- X increases to the right
- Y increases upward
- All measurements are in points (1/72 inch)

```
(0, height) -------- (width, height)
     |                      |
     |                      |
     |      Page            |
     |                      |
     |                      |
  (0, 0) ----------- (width, 0)
```

## Technology Stack

- **Spring Boot 3.2.0**: Web framework
- **Apache PDFBox 3.0.1**: PDF processing
- **Java 17**: Programming language
- **Maven**: Build tool
- **Docker**: Containerization

## Project Structure

```
pdfbox-service/
├── src/main/java/com/doculens/pdfbox/
│   ├── PdfBoxApplication.java              # Main application
│   ├── controller/
│   │   └── PdfAnalysisController.java      # REST endpoints
│   ├── service/
│   │   ├── PdfAnalysisService.java         # Core analysis logic
│   │   └── WordBoundingBoxExtractor.java   # Custom text stripper
│   └── model/
│       ├── AnalysisResponse.java           # Response model
│       ├── PageData.java                   # Page data model
│       └── WordBoundingBox.java            # Word bbox model
├── src/main/resources/
│   └── application.properties              # Configuration
├── Dockerfile                              # Docker image definition
├── docker-compose.yml                      # Docker Compose config
└── pom.xml                                # Maven dependencies
```

## Performance Considerations

- **Memory**: Allocate sufficient memory for large PDFs (default: 512MB max)
- **File Size**: Current limit is 50MB (configurable)
- **Processing Time**: Varies based on PDF size and complexity
  - Simple text PDF: ~1-2 seconds per page
  - Complex/scanned PDF: ~2-5 seconds per page

## Troubleshooting

### Out of Memory Errors
Increase memory allocation:
```bash
docker run -p 8080:8080 -e JAVA_OPTS="-Xmx1024m" pdfbox-service
```

### Font Rendering Issues
The Docker image includes DejaVu fonts. For additional fonts:
```dockerfile
RUN apk add --no-cache ttf-liberation ttf-linux-libertine
```

### Large File Processing
Adjust timeout and file size limits in `application.properties`

## Testing

The project includes comprehensive unit and integration tests.

### Running Tests

```bash
# Run all tests
mvn test

# Run with coverage
mvn test jacoco:report
```

### Test Structure

Tests are located in `src/test/java/com/doculens/pdfbox/`:
- `PdfAnalysisServiceTest` - Integration tests for PDF analysis
- `ParagraphExtractorTest` - Unit tests for paragraph grouping

### Sample PDFs

Place test PDF files in the `samples/` directory:
- `text-sample.pdf` - Regular text-based PDF for testing text extraction
- `scanned-sample.pdf` - Scanned document for testing scanned detection

The tests verify:
- ✓ Text PDF processing with word extraction
- ✓ Paragraph grouping and bounding boxes
- ✓ Scanned PDF detection accuracy
- ✓ Metadata extraction
- ✓ Page dimension calculations

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributing

Contributions are welcome! Please submit pull requests or open issues.

## Support

For issues or questions, please open an issue on GitHub.
