package com.springbatch.employee_batch_processor.dto;

public class BatchJobResponse {

    private Long executionId;
    private String jobName;
    private String jobStatus;


    public BatchJobResponse(Long executionId, String jobName, String jobStatus) {
        this.executionId = executionId;
        this.jobName = jobName;
        this.jobStatus = jobStatus;
    }

    public Long getExecutionId() {
        return executionId;
    }

    public String getJobName() {
        return jobName;
    }

    public String getJobStatus() {
        return jobStatus;
    }
}
