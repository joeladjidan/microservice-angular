package com.joeladjidan.anstic.fileprocessor.init;

import com.joeladjidan.anstic.fileprocessor.model.ProcessedFile;
import com.joeladjidan.anstic.fileprocessor.repository.ProcessedFileRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer {

    private final ProcessedFileRepository repo;

    @PostConstruct
    public void init() {
        // Only insert sample data if table is empty
        if (repo.count() == 0) {
            List<ProcessedFile> samples = List.of(
                    new ProcessedFile("sample1.csv", 10L, "d41d8cd98f00b204e9800998ecf8427e", Instant.now(), 1200L),
                    new ProcessedFile("sample2.csv", 250L, "e3b0c44298fc1c149afbf4c8996fb924", Instant.now(), 4500L)
            );
            repo.saveAll(samples);
        }
    }
}

