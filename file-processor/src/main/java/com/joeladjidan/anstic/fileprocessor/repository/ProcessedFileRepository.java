package com.joeladjidan.anstic.fileprocessor.repository;

import com.joeladjidan.anstic.fileprocessor.model.ProcessedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedFileRepository extends JpaRepository<ProcessedFile, Long> {
}

