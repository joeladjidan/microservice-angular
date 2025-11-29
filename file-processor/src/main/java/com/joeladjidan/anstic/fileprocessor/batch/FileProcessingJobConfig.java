package com.joeladjidan.anstic.fileprocessor.batch;

import com.joeladjidan.anstic.fileprocessor.model.ProcessedFile;
import com.joeladjidan.anstic.fileprocessor.model.ProcessedLine;
import com.joeladjidan.anstic.fileprocessor.repository.ProcessedFileRepository;
import com.joeladjidan.anstic.fileprocessor.repository.ProcessedLineRepository;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.JobBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepBuilderFactory;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.listener.JobExecutionListenerSupport;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemReader;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.mapping.DefaultLineMapper;
import org.springframework.batch.item.file.transform.PassThroughLineTokenizer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.FileSystemResource;

import java.nio.file.Paths;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Configuration
public class FileProcessingJobConfig {

    @Bean
    @StepScope
    public ItemReader<String> fileItemReader(@Value("#{jobParameters['filePath']}") String filePath) {
        FlatFileItemReader<String> reader = new FlatFileItemReader<>();
        reader.setResource(new FileSystemResource(filePath));
        reader.setLineMapper(new DefaultLineMapper<>() {{
            setLineTokenizer(new PassThroughLineTokenizer());
            setFieldSetMapper(fieldSet -> fieldSet.readString(0));
        }});
        return reader;
    }

    @Bean
    public ItemProcessor<String, String> lineProcessor() {
        return item -> item == null ? null : item.trim();
    }

    @Bean
    @StepScope
    public ItemWriter<String> lineWriter(ProcessedLineRepository lineRepo, @Value("#{jobParameters['filePath']}") String filePath) {
        String fileName = filePath == null ? null : Paths.get(filePath).getFileName().toString();
        return items -> {
            List<ProcessedLine> lines = items.stream()
                    .map(s -> new ProcessedLine(fileName, s, Instant.now()))
                    .collect(Collectors.toList());
            lineRepo.saveAll(lines);
        };
    }

    @Bean
    public Step processFileStep(StepBuilderFactory steps, ItemReader<String> reader, ItemProcessor<String,String> processor, ItemWriter<String> writer) {
        return steps.get("processFileStep")
                .<String,String>chunk(1000)
                .reader(reader)
                .processor(processor)
                .writer(writer)
                .build();
    }

    @Bean
    public Job fileProcessingJob(JobBuilderFactory jobs, Step processFileStep, ProcessedFileRepository repo) {
        return jobs.get("fileProcessingJob")
                .start(processFileStep)
                .listener(new JobExecutionListenerSupport() {
                    @Override
                    public void afterJob(JobExecution jobExecution) {
                        long totalRead = jobExecution.getStepExecutions().stream().mapToLong(se -> se.getReadCount()).sum();
                        String filePath = jobExecution.getJobParameters().getString("filePath");
                        String fileSha = jobExecution.getJobParameters().getString("fileSha", "");
                        String fileName = filePath == null ? null : Paths.get(filePath).getFileName().toString();
                        ProcessedFile pf = new ProcessedFile(fileName, totalRead, fileSha == null ? "" : fileSha, Instant.now(), jobExecution.getEndTime().getTime() - jobExecution.getCreateTime().getTime());
                        repo.save(pf);
                    }
                })
                .build();
    }

    /**
     * Implémentation locale minimale de LineTokenizer qui renvoie la ligne entière
     * comme un seul champ. Remplace la dépendance absente à une classe externe.
     */
    private static class PassThroughLineTokenizer implements LineTokenizer {
        @Override
        public FieldSet tokenize(String line) {
            // retourne la ligne complète comme unique champ index 0
            return new DefaultFieldSet(new String[] { line });
        }
    }
}
