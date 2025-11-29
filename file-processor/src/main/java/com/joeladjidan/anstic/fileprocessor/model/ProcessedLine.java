package com.joeladjidan.anstic.fileprocessor.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_lines")
public class ProcessedLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "processed_at")
    private Instant processedAt;

    public ProcessedLine() {}

    public ProcessedLine(String fileName, String content, Instant processedAt) {
        this.fileName = fileName;
        this.content = content;
        this.processedAt = processedAt;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
}

