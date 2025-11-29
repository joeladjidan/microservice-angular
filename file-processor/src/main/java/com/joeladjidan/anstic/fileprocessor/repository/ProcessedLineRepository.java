package com.joeladjidan.anstic.fileprocessor.repository;

import com.joeladjidan.anstic.fileprocessor.model.ProcessedLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProcessedLineRepository extends JpaRepository<ProcessedLine, Long> {
}

