package com.doculens.pdfbox.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.ArrayList;

public class AnalysisResponse {

    @JsonProperty("isScanned")
    private boolean isScanned;

    @JsonProperty("fileName")
    private String fileName;

    @JsonProperty("totalPages")
    private int totalPages;

    @JsonProperty("pages")
    private List<PageData> pages;

    @JsonProperty("metadata")
    private PdfMetadata metadata;

    public AnalysisResponse() {
        this.pages = new ArrayList<>();
    }

    public boolean isScanned() {
        return isScanned;
    }

    public void setScanned(boolean scanned) {
        isScanned = scanned;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public void setTotalPages(int totalPages) {
        this.totalPages = totalPages;
    }

    public List<PageData> getPages() {
        return pages;
    }

    public void setPages(List<PageData> pages) {
        this.pages = pages;
    }

    public PdfMetadata getMetadata() {
        return metadata;
    }

    public void setMetadata(PdfMetadata metadata) {
        this.metadata = metadata;
    }

    public void addPage(PageData page) {
        this.pages.add(page);
    }

    public static class PdfMetadata {
        @JsonProperty("author")
        private String author;

        @JsonProperty("title")
        private String title;

        @JsonProperty("subject")
        private String subject;

        @JsonProperty("creator")
        private String creator;

        @JsonProperty("producer")
        private String producer;

        public PdfMetadata() {}

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getSubject() {
            return subject;
        }

        public void setSubject(String subject) {
            this.subject = subject;
        }

        public String getCreator() {
            return creator;
        }

        public void setCreator(String creator) {
            this.creator = creator;
        }

        public String getProducer() {
            return producer;
        }

        public void setProducer(String producer) {
            this.producer = producer;
        }
    }
}
