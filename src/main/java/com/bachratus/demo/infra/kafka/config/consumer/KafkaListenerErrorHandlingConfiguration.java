package com.bachratus.demo.infra.kafka.config.consumer;

import com.bachratus.demo.infra.kafka.config.AppKafkaProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.autoconfigure.kafka.ConcurrentKafkaListenerContainerFactoryConfigurer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

import java.time.Duration;

/**
 * Configures Kafka listener retry handling and dead-letter publishing for consumer failures.
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnProperty(prefix = "app.kafka", name = "enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class KafkaListenerErrorHandlingConfiguration {

    private final AppKafkaProperties kafkaProperties;

    @Bean
    public DefaultErrorHandler kafkaDefaultErrorHandler(
            KafkaTemplate<String, Object> kafkaTemplate,
            KafkaDeadLetterTopicResolver deadLetterTopicResolver,
            KafkaDeadLetterHeadersFactory deadLetterHeadersFactory
    ) {
        LoggingDeadLetterPublishingRecoverer recoverer = new LoggingDeadLetterPublishingRecoverer(
                kafkaTemplate,
                deadLetterTopicResolver
        );
        recoverer.addHeadersFunction(deadLetterHeadersFactory::create);
        recoverer.setFailIfSendResultIsError(true);
        recoverer.setWaitForSendResultTimeout(Duration.ofMillis(
                kafkaProperties.producer().sendResultTimeoutMs()
        ));
        recoverer.setLogRecoveryRecord(true);
        recoverer.setThrowIfNoDestinationReturned(true);

        long retries = Math.max(0, kafkaProperties.listener().maxAttempts() - 1L);

        return new DefaultErrorHandler(
                recoverer,
                new FixedBackOff(kafkaProperties.listener().retryIntervalMs(), retries)
        );
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<Object, Object> kafkaListenerContainerFactory(
            ConcurrentKafkaListenerContainerFactoryConfigurer configurer,
            ConsumerFactory<Object, Object> consumerFactory,
            DefaultErrorHandler kafkaDefaultErrorHandler
    ) {
        ConcurrentKafkaListenerContainerFactory<Object, Object> factory =
                new ConcurrentKafkaListenerContainerFactory<>();

        configurer.configure(factory, consumerFactory);
        factory.setCommonErrorHandler(kafkaDefaultErrorHandler);
        return factory;
    }
}
