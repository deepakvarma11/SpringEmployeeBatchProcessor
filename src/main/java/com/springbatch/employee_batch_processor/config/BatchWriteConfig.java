package com.springbatch.employee_batch_processor.config;

import com.springbatch.employee_batch_processor.model.Employee;
import org.springframework.batch.infrastructure.item.database.JdbcBatchItemWriter;
import org.springframework.batch.infrastructure.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;

@Configuration
public class BatchWriteConfig {

    @Bean
    public JdbcBatchItemWriter<Employee> employeeWriter(DataSource dataSource){
        return new JdbcBatchItemWriterBuilder<Employee>()
                .dataSource(dataSource)
                .sql("""
                        INSERT INTO employees
                        (
                            id,
                            name,
                            department,
                            salary,
                            annual_salary,
                            salary_grade,
                            bonus_percentage,
                            bonus_amount
                        )
                        VALUES
                        (
                            :id,
                            :name,
                            :department,
                            :salary,
                            :annualSalary,
                            :salaryGrade,
                            :bonusPercentage,
                            :bonusAmount
                        )
                        """)
                .beanMapped()
                .build();
    }
}
