package com.springbatch.employee_batch_processor.config;


import com.springbatch.employee_batch_processor.model.Employee;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.infrastructure.item.file.FlatFileItemReader;
import org.springframework.batch.infrastructure.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

@Configuration
public class BatchReaderConfig {

    @Bean
    @StepScope
    public FlatFileItemReader<Employee> employeeReader(
            @Value("#{jobParameters['fileName']}") String fileName
    ) {

        return new FlatFileItemReaderBuilder<Employee>()
                .name("employeeReader")
                .resource(new ClassPathResource(fileName))
                .linesToSkip(1)
                .delimited()
                .names("id","name","department","salary")
                .targetType(Employee.class)
                .build();
    }
}