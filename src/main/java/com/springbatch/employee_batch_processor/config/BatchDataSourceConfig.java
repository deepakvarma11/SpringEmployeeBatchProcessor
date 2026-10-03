package com.springbatch.employee_batch_processor.config;

import org.springframework.batch.core.configuration.support.JdbcDefaultBatchConfiguration;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchDataSourceConfig extends JdbcDefaultBatchConfiguration {

    private final DataSource dataSource;
    private final PlatformTransactionManager transactionManager;

    public BatchDataSourceConfig(DataSource dataSource, PlatformTransactionManager transactionManager) {
        this.dataSource = dataSource;
        this.transactionManager = transactionManager;
    }

    @Override
    protected DataSource getDataSource() {
        return this.dataSource;
    }

    @Override
    protected PlatformTransactionManager getTransactionManager() {
        return this.transactionManager;
    }
}

