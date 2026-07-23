package fr.esgi.foodflow.order_service.config;

import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Gestion des messages empoisonnés côté consommateur Java.
 *
 * Spring Boot détecte automatiquement un bean {@link DefaultErrorHandler}
 * (un CommonErrorHandler) et le câble sur le container factory auto-configuré
 * des @KafkaListener. Quand le listener lève une exception :
 *   - on retente selon le BackOff ;
 *   - une fois les tentatives épuisées, le recoverer republie le message sur
 *     le topic mort {@code <topic>.DLT} (convention Spring Kafka).
 */
@Configuration
public class KafkaErrorHandlingConfig {

    private static final Logger log = LoggerFactory.getLogger(KafkaErrorHandlingConfig.class);

    @Bean
    public DefaultErrorHandler kafkaErrorHandler(KafkaTemplate<String, String> kafkaTemplate) {
        // Après épuisement des tentatives : republication sur <topic>.DLT, même
        // partition. Résolveur explicite car en spring-kafka 4.1 le suffixe par
        // défaut est "-dlt" ; on force ".DLT" (convention du TP + topic créé).
        DeadLetterPublishingRecoverer recoverer = new DeadLetterPublishingRecoverer(
                kafkaTemplate,
                (record, exception) -> new TopicPartition(record.topic() + ".DLT", record.partition()));

        // 1 tentative initiale + 2 retries = 3 au total, intervalle 1 s.
        FixedBackOff backOff = new FixedBackOff(1000L, 2L);

        DefaultErrorHandler handler = new DefaultErrorHandler(recoverer, backOff);

        // Log lisible de chaque tentative échouée (pour la démo).
        handler.setRetryListeners((record, ex, deliveryAttempt) ->
                log.warn("Tentative {} échouée (topic={}, partition={}, offset={}): {}",
                        deliveryAttempt, record.topic(), record.partition(), record.offset(),
                        ex.getMessage()));

        return handler;
    }
}
