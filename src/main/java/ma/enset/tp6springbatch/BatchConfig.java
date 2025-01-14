package ma.enset.tp6springbatch;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.EnableBatchProcessing;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.database.JdbcBatchItemWriter;
import org.springframework.batch.item.database.builder.JdbcBatchItemWriterBuilder;
import org.springframework.batch.item.file.FlatFileItemReader;
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;

import javax.sql.DataSource;

@Configuration
public class BatchConfig {
    private static final Logger log = LoggerFactory.getLogger(BatchConfig.class);

    @Bean
    public FlatFileItemReader<Order> reader() {
        log.info("Initializing FlatFileItemReader...");
        return new FlatFileItemReaderBuilder<Order>()
                .name("orderItemReader")
                .resource(new ClassPathResource("orders.csv"))
                .delimited()
                .names("orderId", "customerName", "amount")
                .linesToSkip(1)
                .targetType(Order.class)
                .build();
    }

    @Bean
    public OrderItemProcessor processor() {
        log.info("Initializing OrderItemProcessor...");
        return new OrderItemProcessor();
    }

    @Bean
    public JdbcBatchItemWriter<Order> writer(DataSource dataSource) {
        log.info("Initializing JdbcBatchItemWriter...");
        return new JdbcBatchItemWriterBuilder<Order>()
                .sql("INSERT INTO orders (order_id, customer_name, amount) VALUES (:orderId, :customerName, :amount)")
                .dataSource(dataSource)
                .beanMapped()
                .build();
    }

    @Bean
    public Step step1(JobRepository jobRepository,
                      DataSourceTransactionManager transactionManager,
                      JdbcBatchItemWriter<Order> writer) {
        log.info("Configuring Step1...");
        return new StepBuilder("step1", jobRepository)
                .<Order, Order>chunk(3, transactionManager)
                .reader(reader())
                .processor(processor())
                .writer(writer)
                .build();
    }

    @Bean
    public Job importOrderJob(JobRepository jobRepository,
                              Step step1,
                              JobCompletionNotificationListener listener) {
        log.info("Creating importOrderJob...");
        return new JobBuilder("importOrderJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .listener(listener)
                .flow(step1)
                .end()
                .build();
    }
}
