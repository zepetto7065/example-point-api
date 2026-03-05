package com.musinsa.point.batch.job;

import com.musinsa.point.domain.Ledger;
import com.musinsa.point.domain.Wallet;
import com.musinsa.point.repository.LedgerRepository;
import com.musinsa.point.repository.WalletRepository;
import jakarta.persistence.EntityManagerFactory;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.launch.support.RunIdIncrementer;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.item.ItemProcessor;
import org.springframework.batch.item.ItemWriter;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Configuration
public class ExpireJobConfig {

    private static final int CHUNK_SIZE = 100;

    private final JobRepository jobRepository;
    private final PlatformTransactionManager transactionManager;
    private final EntityManagerFactory entityManagerFactory;
    private final LedgerRepository ledgerRepository;
    private final WalletRepository walletRepository;

    public ExpireJobConfig(JobRepository jobRepository,
                                PlatformTransactionManager transactionManager,
                                EntityManagerFactory entityManagerFactory,
                                LedgerRepository ledgerRepository,
                                WalletRepository walletRepository) {
        this.jobRepository = jobRepository;
        this.transactionManager = transactionManager;
        this.entityManagerFactory = entityManagerFactory;
        this.ledgerRepository = ledgerRepository;
        this.walletRepository = walletRepository;
    }

    @Bean
    public Job pointExpireJob() {
        return new JobBuilder("pointExpireJob", jobRepository)
                .incrementer(new RunIdIncrementer())
                .start(expireStep())
                .build();
    }

    @Bean
    public Step expireStep() {
        return new StepBuilder("expireStep", jobRepository)
                .<Ledger, Ledger>chunk(CHUNK_SIZE, transactionManager)
                .reader(expiredLedgerReader())
                .processor(expireProcessor())
                .writer(expireWriter())
                .build();
    }

    @Bean
    @StepScope
    public JpaPagingItemReader<Ledger> expiredLedgerReader() {
        return new JpaPagingItemReaderBuilder<Ledger>()
                .name("expiredLedgerReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString("SELECT l FROM Ledger l WHERE l.status = 'ACTIVE' AND l.expireAt <= :now AND l.balance > 0")
                .parameterValues(Map.of("now", LocalDateTime.now()))
                .pageSize(CHUNK_SIZE)
                .build();
    }

    @Bean
    public ItemProcessor<Ledger, Ledger> expireProcessor() {
        return ledger -> {
            if (ledger.getBalance() <= 0) {
                return null;
            }
            return ledger;
        };
    }

    @Bean
    public ItemWriter<Ledger> expireWriter() {
        return items -> {
            for (Ledger ledger : items) {
                long remainingBalance = ledger.getBalance();

                Wallet wallet = walletRepository.findByMemberId(ledger.getMemberId())
                        .orElse(null);
                if (wallet != null) {
                    wallet.subtractBalance(remainingBalance);
                    walletRepository.save(wallet);
                }

                ledger.expire();
                ledgerRepository.save(ledger);

                log.info("포인트 만료 처리: ledgerId={}, memberId={}, amount={}",
                        ledger.getLedgerId(), ledger.getMemberId(), remainingBalance);
            }
        };
    }
}
