package com.joeladjidan.anstic.fileprocessor.web;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/processor")
@RequiredArgsConstructor
public class JobController {

    private final JobLauncher jobLauncher;
    private final Job fileProcessingJob;

    @PostMapping("/upload")
    public ResponseEntity<Map<String,Object>> uploadAndStart(@RequestParam("file") MultipartFile file) throws Exception {
        // For simplicity we pass original filename as job parameter; actual implementation should store file first
        JobExecution exec = jobLauncher.run(fileProcessingJob, new JobParametersBuilder()
                .addString("fileName", file.getOriginalFilename())
                .addLong("time", System.currentTimeMillis())
                .toJobParameters());
        Map<String,Object> resp = new HashMap<>();
        resp.put("status", exec.getStatus().toString());
        resp.put("id", exec.getId());
        return ResponseEntity.ok(resp);
    }
}

