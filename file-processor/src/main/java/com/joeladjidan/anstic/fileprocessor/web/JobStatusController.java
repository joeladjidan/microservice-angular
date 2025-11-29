package com.joeladjidan.anstic.fileprocessor.web;

import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.explore.JobExplorer;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/processor/status")
@RequiredArgsConstructor
public class JobStatusController {

    private final JobExplorer jobExplorer;

    @GetMapping
    public ResponseEntity<List<String>> listExecutions(@RequestParam(required = false) String jobName) {
        List<JobExecution> executions = jobName == null ? jobExplorer.getJobNames().stream().flatMap(n -> jobExplorer.findJobExecutions(n).stream())
                .collect(Collectors.toList()) : jobExplorer.findJobExecutions(jobName);
        List<String> statuses = executions.stream().map(e -> e.getId() + ":" + e.getStatus().toString()).collect(Collectors.toList());
        return ResponseEntity.ok(statuses);
    }
}

