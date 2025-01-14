# Report on Spring Batch Job Implementation for Order Processing

## 1. Objective
The goal of this project is to process orders from a CSV file, apply a 10% discount to each order amount, and store the transformed data in an HSQLDB database. The batch job consists of the following components:

- A reader to extract data from the CSV file.
- A processor to apply the discount.
- A writer to save the data to the database.
- Logging to track the job execution at each stage.

## 2. Components of the Batch Job

### 2.1 FlatFileItemReader
This component reads the `orders.csv` file and maps its contents to an `Order` object. The reader skips the first line (header) and reads fields such as `orderId`, `customerName`, and `amount`.

**Configuration:**
```java
@Bean
public FlatFileItemReader<Order> reader() {
    return new FlatFileItemReaderBuilder<Order>()
            .name("orderItemReader")
            .resource(new ClassPathResource("orders.csv"))
            .delimited()
            .names("orderId", "customerName", "amount")
            .linesToSkip(1)
            .targetType(Order.class)
            .build();
}
```

### 2.2 OrderItemProcessor
The processor applies a 10% discount to the `amount` field of each order. Logging is added to track each processed order.

**Key Functionality:**
```java
@Override
public Order process(Order order) {
    final Double originalAmount = order.getAmount();
    final Double discountedAmount = originalAmount * 0.90; // Applying a 10% discount
    order.setAmount(discountedAmount);
    log.info("Processing order: {} -> Discounted amount: {}", order.getOrderId(), discountedAmount);
    return order;
}
```

### 2.3 JdbcBatchItemWriter
This component writes the processed `Order` data to an HSQLDB table. The writer uses a SQL query to insert the data.

**Configuration:**
```java
@Bean
public JdbcBatchItemWriter<Order> writer(DataSource dataSource) {
    return new JdbcBatchItemWriterBuilder<Order>()
            .sql("INSERT INTO orders (order_id, customer_name, amount) VALUES (:orderId, :customerName, :amount)")
            .dataSource(dataSource)
            .beanMapped()
            .build();
}
```

### 2.4 JobCompletionNotificationListener
This listener logs a message when the job completes and retrieves the inserted data from the database for verification.

**Key Functionality:**
```java
@Override
public void afterJob(JobExecution jobExecution) {
    if (jobExecution.getStatus() == BatchStatus.COMPLETED) {
        log.info("!!! JOB COMPLETED! Displaying results...");
        jdbcTemplate.query("SELECT order_id, customer_name, amount FROM orders",
                (rs, row) -> new Order(rs.getLong(1), rs.getString(2), rs.getDouble(3)))
                .forEach(order -> log.info("Inserted order: {}", order));
    }
}
```

## 3. Job Configuration
The job is defined with a single step that includes the reader, processor, and writer. A `RunIdIncrementer` ensures that the job can be re-run with a unique identifier.

**Job and Step Configuration:**
```java
@Bean
public Step step1(JobRepository jobRepository, DataSourceTransactionManager transactionManager, JdbcBatchItemWriter<Order> writer) {
    return new StepBuilder("step1", jobRepository)
            .<Order, Order>chunk(3, transactionManager)
            .reader(reader())
            .processor(processor())
            .writer(writer)
            .build();
}

@Bean
public Job importOrderJob(JobRepository jobRepository, Step step1, JobCompletionNotificationListener listener) {
    return new JobBuilder("importOrderJob", jobRepository)
            .incrementer(new RunIdIncrementer())
            .listener(listener)
            .flow(step1)
            .end()
            .build();
}
```

## 4. Logging for Debugging and Monitoring
Logging is implemented at every stage of the job to provide insights and facilitate debugging:

- **Reader Initialization**: Logs when the reader starts.
- **Processor Execution**: Logs the original and discounted amounts for each order.
- **Writer Completion**: Logs the orders inserted into the database.
- **Job Completion**: Logs the final status and inserted data.

## 5. Sample Execution Output

**Reader Initialization:**
```bash
INFO  ma.enset.tp6springbatch.BatchConfig - Initializing FlatFileItemReader...
```

**Processor Log for Each Order:**
```bash
INFO  ma.enset.tp6springbatch.OrderItemProcessor - Processing order: 1 -> Discounted amount: 90.0
INFO  ma.enset.tp6springbatch.OrderItemProcessor - Processing order: 2 -> Discounted amount: 180.0
```

**Job Completion Log:**
```bash
INFO  ma.enset.tp6springbatch.JobCompletionNotificationListener - !!! JOB COMPLETED! Displaying results...
INFO  ma.enset.tp6springbatch.JobCompletionNotificationListener - Inserted order: Order{orderId=1, customerName='Kaoutar Tamouche', amount=90.0}
```

## 6. Conclusion
This Spring Batch job successfully processes and transforms order data while maintaining a robust logging mechanism for monitoring and debugging. The implementation demonstrates how to build scalable and maintainable batch processing systems using Spring Batch.
