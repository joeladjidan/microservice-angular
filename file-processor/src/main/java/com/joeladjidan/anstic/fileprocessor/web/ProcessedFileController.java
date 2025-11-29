package com.joeladjidan.anstic.fileprocessor.web;

import com.joeladjidan.anstic.fileprocessor.model.ProcessedFile;
import com.joeladjidan.anstic.fileprocessor.repository.ProcessedFileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/processor/files")
@RequiredArgsConstructor
public class ProcessedFileController {

    private final ProcessedFileRepository repo;

    @GetMapping
    public ResponseEntity<List<ProcessedFile>> all() {
        return ResponseEntity.ok(repo.findAll());
    }
}

