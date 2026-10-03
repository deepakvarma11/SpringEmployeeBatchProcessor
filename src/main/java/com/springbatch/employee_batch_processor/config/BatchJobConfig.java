package com.springbatch.employee_batch_processor.config;


import com.springbatch.employee_batch_processor.model.Employee;
import org.jspecify.annotations.Nullable;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.job.Job;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.listener.StepExecutionListener;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.Step;
import org.springframework.batch.core.step.StepExecution;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.infrastructure.item.ExecutionContext;
import org.springframework.batch.infrastructure.item.ItemProcessor;
import org.springframework.batch.infrastructure.item.ItemReader;
import org.springframework.batch.infrastructure.item.ItemWriter;
import org.springframework.batch.infrastructure.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class BatchJobConfig {

    @Bean
    public Step employeeStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            ItemReader<Employee> employeeReader,
            ItemProcessor<Employee, Employee> employeeProcessor,
            ItemWriter<Employee> employeeItemWriter){

        return new StepBuilder("employeeStep", jobRepository)
                .<Employee, Employee>chunk(2)
                .transactionManager(transactionManager)
                .reader(employeeReader)
                .processor(employeeProcessor)
                .writer(employeeItemWriter)
                .listener(employeeStepListener())
                .build();
    }

    @Bean
    public Step summaryStep(
            JobRepository jobRepository, PlatformTransactionManager platformTransactionManager
    ){
        return new StepBuilder("summaryStep", jobRepository)
                .tasklet((contribution, chunkContext) ->
                {
                    ExecutionContext context = chunkContext.getStepContext()
                                    .getStepExecution()
                            .getJobExecution()
                            .getExecutionContext();

                    long processedCount = context.getLong("processedCount");
                    long filteredCount = context.getLong("filteredCount");
                    System.out.println("=================================");
                    System.out.println("Employee process completed");
                    System.out.println("Generating summary.........");
                    System.out.println("Process Records : " + processedCount);
                    System.out.println("Filtered records : " + filteredCount);
                    System.out.println("=================================");
                    return RepeatStatus.FINISHED;
                },platformTransactionManager).build();
    }

    @Bean
    public Step errorStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager) {

        return new StepBuilder("errorStep", jobRepository)
                .tasklet((contribution, chunkContext) -> {
                    ExecutionContext context = chunkContext.getStepContext()
                            .getStepExecution()
                            .getJobExecution()
                            .getExecutionContext();

                    long processedCount = context.getLong("processedCount");
                    long filteredCount = context.getLong("filteredCount");
                    System.out.println("=================================");
                    System.out.println("Employee process FAILED!");
                    System.out.println("Generating summary.........");
                    System.out.println("Process Records : " + processedCount);
                    System.out.println("Filtered records : " + filteredCount);
                    System.out.println("Running error handling...");
                    System.out.println("=================================");

                    contribution.setExitStatus(ExitStatus.FAILED);

                    return RepeatStatus.FINISHED;

                }, transactionManager)
                .build();
    }

    @Bean
    public StepExecutionListener employeeStepListener(){
        return new StepExecutionListener() {

            @Override
            public void beforeStep(StepExecution stepExecution) {
            }

            @Override
            public @Nullable ExitStatus afterStep(StepExecution stepExecution) {

                ExecutionContext context = stepExecution.getJobExecution().getExecutionContext();

                context.putLong("processedCount", stepExecution.getWriteCount());

                context.putLong("filteredCount",stepExecution.getFilterCount());

                return stepExecution.getExitStatus();
            }
        };
    }

    @Bean
    public Job employeeJob(
            JobRepository jobRepository,
            Step employeeStep,
            Step summaryStep,
            Step errorStep) {

        return new JobBuilder("employeeJob", jobRepository)
                .start(employeeStep)
                .on("COMPLETED").to(summaryStep)

                .from(employeeStep)
                .on("FAILED").to(errorStep)

                .end()
                .build();
    }


}
