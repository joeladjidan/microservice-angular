package com.joeladjidan.anstic.fileprocessor.model;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "processed_files")
public class ProcessedFile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "total_lines")
    private Long totalLines;

    @Column(name = "sha256", length = 64)
    private String sha256;

    @Column(name = "processed_at")
    private Instant processedAt;

    @Column(name = "duration_ms")
    private Long durationMs;

    public ProcessedFile() {}

    public ProcessedFile(String fileName, Long totalLines, String sha256, Instant processedAt, Long durationMs) {
        this.fileName = fileName;
        this.totalLines = totalLines;
        this.sha256 = sha256;
        this.processedAt = processedAt;
        this.durationMs = durationMs;
    }

    // getters and setters

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFileName() { return fileName; }
    public void setFileName(String fileName) { this.fileName = fileName; }
    public Long getTotalLines() { return totalLines; }
    public void setTotalLines(Long totalLines) { this.totalLines = totalLines; }
    public String getSha256() { return sha256; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public Instant getProcessedAt() { return processedAt; }
    public void setProcessedAt(Instant processedAt) { this.processedAt = processedAt; }
    public Long getDurationMs() { return durationMs; }
    public void setDurationMs(Long durationMs) { this.durationMs = durationMs; }
}

