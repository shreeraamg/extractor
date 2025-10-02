package com.example.extractor.listener;

import com.example.extractor.handler.ProductMessageHandler;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class ProductKafkaListener {

    private static final Logger log = LoggerFactory.getLogger(ProductKafkaListener.class);

    private final ProductMessageHandler handler;

    @Autowired
    public ProductKafkaListener(ProductMessageHandler productMessageHandler) {
        handler = productMessageHandler;
    }

    @KafkaListener(topics = "product-topic")
    public void listen(ConsumerRecord<String, String> record) {
        try {
            log.info("Reading message from kafka topic: {}, partition: {}, offset: {}",
                    record.topic(), record.partition(), record.offset());

            handler.handle(record);
        } catch (Exception ex) {
            log.error("Exception while processing record topic: {}, partition: {}, offset: {}, error: {}",
                    record.topic(), record.partition(), record.offset(), ex.getMessage(), ex);
        }
    }
}
