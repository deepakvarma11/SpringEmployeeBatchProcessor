package com.springbatch.employee_batch_processor.controller;


import com.springbatch.employee_batch_processor.dto.BatchJobResponse;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.JobExecution;
import org.springframework.batch.core.job.parameters.InvalidJobParametersException;
import org.springframework.batch.core.job.parameters.JobParameters;
import org.springframework.batch.core.job.parameters.JobParametersBuilder;
import org.springframework.batch.core.launch.*;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/batch")
public class BatchController {

    private  final JobOperator jobLauncher;
    private final Job employeeJob;
    public BatchController(JobOperator jobLauncher, Job employeeJob){
        this.employeeJob = employeeJob;
        this.jobLauncher = jobLauncher;
    }

    @PostMapping("/employees")
    public ResponseEntity<BatchJobResponse> runEmployeeJob(
            @RequestParam String fileName
    ) throws JobInstanceAlreadyCompleteException, InvalidJobParametersException, JobExecutionAlreadyRunningException, JobRestartException {
        JobParameters jobParameters = new JobParametersBuilder()
                .addString("fileName", fileName)
                .toJobParameters();

        JobExecution jobExecution = jobLauncher.start(employeeJob, jobParameters);

        BatchJobResponse response = new BatchJobResponse(jobExecution.getId()
        ,employeeJob.getName(), jobExecution.getStatus().toString());

        return ResponseEntity.ok(response);
    }

}
