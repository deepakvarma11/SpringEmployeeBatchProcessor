# Employee Batch Processor

A Spring Batch application that reads employee data from CSV files,
processes the records with business rules, and writes the transformed
data into MySQL. The application also exposes a REST API to trigger
batch jobs.

## Tech Stack

-   Java 21
-   Spring Boot 4.1.1
-   Spring Batch 6.0.5
-   Spring Web MVC
-   Spring JDBC
-   Spring Data JPA / Hibernate
-   MySQL
-   Maven
-   IntelliJ IDEA / macOS
-   Postman or cURL for API testing

## Project Goal

The application demonstrates a production-style Spring Batch workflow:

``` text
CSV File
   |
   v
ItemReader
   |
   v
ItemProcessor
   |
   v
ItemWriter
   |
   v
MySQL
```

The batch job uses chunk processing and maintains Spring Batch metadata
in the `BATCH_*` tables.

The current application also demonstrates:

-   Job parameters
-   Job and step execution
-   Job restart behavior
-   Filtering
-   Chunk processing
-   Step listeners
-   Item process listeners
-   Shared `ExecutionContext`
-   Conditional job flow
-   Error handling
-   REST-based job launching
-   JSON API responses using `ResponseEntity`

## Current Project Structure

``` text
src/
├── main/
│   ├── java/
│   │   └── com/springbatch/employee_batch_processor/
│   │       ├── EmployeeBatchProcessorApplication.java
│   │       │
│   │       ├── config/
│   │       │   ├── BatchDataSourceConfig.java
│   │       │   ├── BatchJobConfig.java
│   │       │   ├── BatchProcessorConfig.java
│   │       │   ├── BatchReaderConfig.java
│   │       │   └── BatchWriteConfig.java
│   │       │
│   │       ├── controller/
│   │       │   └── BatchController.java
│   │       │
│   │       └── model/
│   │           ├── BatchJobResponse.java
│   │           └── Employee.java
│   │
│   └── resources/
│       ├── application.properties
│       ├── Queries/
│       │   ├── StandardbatchjobTables.sql
│       │   └── employees-restart.sql
│       └── employee CSV input files
│
└── test/
    └── java/
        └── com/springbatch/employee_batch_processor/
            └── EmployeeBatchProcessorApplicationTests.java
```

> The project is intentionally being developed toward a structure that
> can support multiple batch jobs. As the application grows,
> job-specific readers, processors, writers, and listeners can be
> grouped under job/domain packages rather than keeping one large flat
> configuration package.

## Batch Job

The current job is named:

``` text
employeeJob
```

The main flow is:

``` text
employeeStep
      |
      +-- COMPLETED --> summaryStep
      |
      +-- FAILED -----> errorStep
```

### employeeStep

`employeeStep` is a chunk-oriented step.

The current chunk size is:

``` text
2
```

This means Spring Batch processes and commits records in chunks of two.

For example, eight input records are processed approximately as:

``` text
Chunk 1: records 1-2
Chunk 2: records 3-4
Chunk 3: records 5-6
Chunk 4: records 7-8
```

A chunk is a transaction boundary; it does not mean that the job can
process only two records.

### summaryStep

The summary step reads values placed into the job `ExecutionContext` by
the employee step listener and prints a summary.

The current summary includes:

-   Processed/write count
-   Filtered count

### errorStep

The error step is used when `employeeStep` fails.

It explicitly sets its exit status to `FAILED` so that the overall job
remains failed instead of appearing successful after error handling.

## CSV Reader

The reader uses Spring Batch's `FlatFileItemReader`.

The CSV format is:

``` csv
id,name,department,salary
101,Ravi,IT,65000
102,Anil,HR,45000
103,John,IT,85000
```

The header is skipped.

The reader receives the input file through a job parameter:

``` text
fileName
```

The parameter is resolved with:

``` text
#{jobParameters['fileName']}
```

The reader is step-scoped because job parameters are available when the
step starts.

Input files are currently stored under:

``` text
src/main/resources/
```

and loaded using a classpath resource.

## Processor

The processor applies employee business rules.

Current processing includes:

### Name

The employee name is trimmed.

### Department

The department is trimmed and converted to uppercase.

### Filtering

Employees with salary below `40000` are filtered:

``` text
salary < 40000
```

The processor returns `null` for these records.

In Spring Batch:

``` text
processor returns null
        |
        v
record is FILTERED
```

Filtering is different from skipping.

### Annual Salary

``` text
annualSalary = salary * 12
```

### Salary Grade

Current rules:

``` text
40,000 - 59,999  -> C
60,000 - 100,000  -> B
> 100,000         -> A
< 40,000          -> filtered
```

### Bonus Percentage

Current department rules:

``` text
IT       -> 10%
Finance  -> 7%
HR       -> 5%
Other    -> 3%
```

### Bonus Amount

``` text
bonusAmount = salary * bonusPercentage / 100
```

## Database

The business table is:

``` sql
CREATE TABLE employees (
    id BIGINT PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    department VARCHAR(50) NOT NULL,
    salary DECIMAL(12,2) NOT NULL,
    annual_salary DECIMAL(14,2),
    salary_grade VARCHAR(10),
    bonus_percentage DECIMAL(5,2),
    bonus_amount DECIMAL(12,2)
);
```

Spring Batch maintains its own metadata tables, such as:

``` text
BATCH_JOB_INSTANCE
BATCH_JOB_EXECUTION
BATCH_JOB_EXECUTION_PARAMS
BATCH_STEP_EXECUTION
BATCH_STEP_EXECUTION_CONTEXT
BATCH_JOB_EXECUTION_CONTEXT
```

The business table and Spring Batch metadata tables have different
purposes.

## JDBC Writer

The writer uses:

``` text
JdbcBatchItemWriter<Employee>
```

and writes the processed employee object into the `employees` table.

The SQL uses bean property names such as:

``` text
:id
:name
:department
:salary
:annualSalary
:salaryGrade
:bonusPercentage
:bonusAmount
```

## Job Parameters

The current job accepts:

``` text
fileName
```

Example:

``` text
fileName=employee-summary7.csv
```

The parameter is used by the reader to determine which CSV file to
process.

The `fileName` parameter is also identifying for the Job Instance.

Therefore:

``` text
employee-summary6.csv
```

and:

``` text
employee-summary7.csv
```

represent different Job Instances.

A completed Job Instance cannot normally be launched again with the
exact same identifying parameters.

A failed Job Instance can be restarted depending on the state and
restartability of its steps.

## ExecutionContext

The employee step listener stores information in the job-level
`ExecutionContext`.

The current values include:

``` text
processedCount
filteredCount
```

Because the values are stored in the Job Execution Context, a later step
such as `summaryStep` can read them.

Conceptually:

``` text
employeeStep
     |
     | writes
     v
Job ExecutionContext
     |
     | reads
     v
summaryStep
```

## Filtering vs Skipping vs Retrying

These are intentionally different concepts in Spring Batch.

### Filtering

Processor returns:

``` java
return null;
```

Result:

``` text
FILTER_COUNT increases
```

The item is not sent to the writer.

### Skipping

An exception occurs and the step is configured with skip behavior.

Result:

``` text
item is skipped
```

### Retrying

An exception occurs and the step is configured with retry behavior.

Result:

``` text
item can be attempted again
```

### Normal failure

If an exception is not handled by retry or skip configuration:

``` text
step -> FAILED
job flow -> FAILED path
```

## Listeners

The application has explored multiple Spring Batch listener levels.

### StepExecutionListener

Used for the whole step.

It can access:

``` text
StepExecution
JobExecution
ExecutionContext
```

The current step listener stores:

``` text
processedCount
filteredCount
```

### ItemProcessListener

Used around processing of individual items.

Typical lifecycle:

``` text
beforeProcess()
      |
      v
processor
      |
      +---- success ----> afterProcess()
      |
      +---- exception --> onProcessError()
```

## REST API

Automatic job execution at application startup is disabled:

``` properties
spring.batch.job.enabled=false
```

The batch job is launched through the REST controller.

### Start Employee Job

``` http
POST /api/batch/employees?fileName=employee-summary7.csv
```

cURL:

``` bash
curl -X POST "http://localhost:8080/api/batch/employees?fileName=employee-summary7.csv"
```

### JSON Response

The API uses `ResponseEntity` and returns a response object such as:

``` json
{
  "executionId": 25,
  "jobName": "employeeJob",
  "status": "COMPLETED"
}
```

The response contains:

-   `executionId` - Spring Batch Job Execution ID
-   `jobName` - name of the launched job
-   `status` - current execution status

## Multiple Jobs

The current controller explicitly launches `employeeJob`.

If the application grows to multiple jobs, for example:

``` text
employeeJob
salaryJob
departmentJob
```

the controller should not become a large collection of hard-coded job
references.

A scalable design can introduce a service that resolves jobs by job
name:

``` text
REST Controller
      |
      v
BatchJobService
      |
      +---- employeeJob
      |
      +---- salaryJob
      |
      +---- departmentJob
```

Job-specific components can then be grouped by domain/job.

A possible future structure is:

``` text
batch/
├── employee/
│   ├── EmployeeJobConfig.java
│   ├── EmployeeReader.java
│   ├── EmployeeProcessor.java
│   ├── EmployeeWriter.java
│   └── EmployeeJobListener.java
│
├── salary/
│   ├── SalaryJobConfig.java
│   ├── SalaryReader.java
│   ├── SalaryProcessor.java
│   └── SalaryWriter.java
│
└── department/
    ├── DepartmentJobConfig.java
    ├── DepartmentReader.java
    ├── DepartmentProcessor.java
    └── DepartmentWriter.java
```

This is a future scalability direction rather than a requirement for the
current implementation.

## Configuration

The application uses MySQL as its data source.

Typical configuration:

``` properties
spring.application.name=employee-batch-processor

spring.batch.job.enabled=false

spring.datasource.url=jdbc:mysql://localhost:3306/spring_batch_demo
spring.datasource.username=root
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=update
spring.jpa.show-sql=true
spring.jpa.properties.hibernate.format_sql=true

spring.datasource.hikari.maximum-pool-size=10

spring.batch.jdbc.initialize-schema=always
```

Do not commit real database passwords or credentials to source control.

## Running the Application

### 1. Start MySQL

Make sure MySQL is running and the database exists:

``` sql
CREATE DATABASE spring_batch_demo;
```

### 2. Verify the input CSV

For example:

``` text
src/main/resources/employee-summary7.csv
```

### 3. Build

``` bash
mvn clean package
```

### 4. Start the application

From IntelliJ, run:

``` text
EmployeeBatchProcessorApplication
```

or use:

``` bash
mvn spring-boot:run
```

The application starts on:

``` text
http://localhost:8080
```

### 5. Trigger the job

``` bash
curl -X POST "http://localhost:8080/api/batch/employees?fileName=employee-summary7.csv"
```

## Verify Results

Check the business data:

``` sql
SELECT *
FROM employees
ORDER BY id;
```

Check Spring Batch job executions:

``` sql
SELECT
    JOB_EXECUTION_ID,
    STATUS,
    EXIT_CODE,
    START_TIME,
    END_TIME
FROM BATCH_JOB_EXECUTION
ORDER BY JOB_EXECUTION_ID DESC;
```

Check step executions:

``` sql
SELECT
    STEP_EXECUTION_ID,
    JOB_EXECUTION_ID,
    STEP_NAME,
    STATUS,
    EXIT_CODE,
    READ_COUNT,
    WRITE_COUNT,
    FILTER_COUNT
FROM BATCH_STEP_EXECUTION
ORDER BY STEP_EXECUTION_ID DESC;
```

## Learning Topics Covered

This project is being used to learn Spring Batch concepts progressively:

-   Job
-   JobInstance
-   JobExecution
-   Step
-   StepExecution
-   JobParameters
-   ExecutionContext
-   ItemReader
-   ItemProcessor
-   ItemWriter
-   Chunk processing
-   Transactions
-   Filtering
-   Skip
-   Retry
-   Listeners
-   Conditional job flows
-   Restartability
-   Spring Batch metadata
-   JDBC batch writing
-   REST-based job launching
-   JSON API responses
-   `ResponseEntity`
-   Multi-job architecture

## Future Improvements

Potential next stages include:

-   Job status API using Execution ID
-   Generic multi-job API
-   Job launch service
-   Better exception handling
-   Validation of input files
-   Job parameter validation
-   REST error responses
-   More comprehensive tests
-   Integration tests with MySQL/Testcontainers
-   Multiple independent batch jobs
-   Job-specific package organization
-   Monitoring and operational endpoints
-   Production-ready logging
-   Authentication and authorization for job-triggering APIs

## Author / Learning Project

This project is a hands-on Spring Batch learning application focused on
understanding how batch jobs work internally and how a batch processing
system can be exposed through REST APIs.
