package com.example.extractor.handler;

import com.example.extractor.domain.Product;
import com.example.extractor.repository.ProductRepository;
import com.example.extractor.util.TextUtil;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.util.Set;

@Component
public class ProductMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(ProductMessageHandler.class);

    private final ObjectMapper objectMapper;

    private final ProductRepository productRepository;

    private final TextUtil textUtil;

    private final Validator validator;

    public ProductMessageHandler(
            ObjectMapper objectMapper,
            ProductRepository productRepository,
            TextUtil textUtil,
            Validator validator
    ) {
        this.objectMapper = objectMapper;
        this.productRepository = productRepository;
        this.textUtil = textUtil;
        this.validator = validator;
    }

    public void handle(ConsumerRecord<String, String> record) {
        try {
            Product product = parse(record.value());

            String sanitizedName = textUtil.sanitizeProductName(product.getName());
            product.setName(sanitizedName);

            validate(product);
            save(product);
        } catch (Exception ex) {
            log.error("Exception while handling message: {}", ex.getMessage());
            ex.printStackTrace();
        }
    }

    private Product parse(String message) throws JsonProcessingException {
        return objectMapper.readValue(message, Product.class);
    }

    private void validate(Product product) throws RuntimeException {
        Set<ConstraintViolation<Product>> violations = validator.validate(product);

        if (!violations.isEmpty()) {
            violations.forEach(v ->
                    log.warn("{}: {}", v.getPropertyPath(), v.getMessage()));

            throw new RuntimeException("Validation failed on one or more fields");
        }
    }

    private void save(Product product) {
        String productId = product.getId();
        boolean isExistingProduct = productRepository.existsById(productId);
        productRepository.save(product);

        if (isExistingProduct) {
            log.info("Product: {} updated successfully", productId);
        } else {
            log.info("New Product saved with id: {}", productId);
        }
    }

}
