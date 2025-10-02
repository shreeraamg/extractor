package com.example.extractor;

import com.example.extractor.domain.Product;
import com.example.extractor.handler.ProductMessageHandler;
import com.example.extractor.repository.ProductRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.Validator;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExtractorApplicationTests {

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private ProductRepository productRepository;

    private Validator validator;

    @InjectMocks
    private ProductMessageHandler handler;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        // Real validator to enforce javax.validation annotations
        LocalValidatorFactoryBean factory = new LocalValidatorFactoryBean();
        factory.afterPropertiesSet();
        validator = factory.getValidator();

        // Create handler with mocks + real validator
        handler = new ProductMessageHandler(objectMapper, productRepository, validator);
    }

    @Test
    void shouldParseValidateAndSaveProductSuccessfully() throws Exception {
        // given
        String message = "{\"id\":\"p1\",\"name\":\"Laptop\",\"brand\":\"Dell\",\"category\":\"Electronics\",\"price\":59999.0,\"isAvailable\":true,\"quantity\":10,\"tags\":[\"tech\",\"computers\"]}";
        Product product = new Product();
        product.setId("p1");
        product.setName("Laptop");
        product.setBrand("Dell");
        product.setCategory("Electronics");
        product.setPrice(59999.0);
        product.setAvailable(true);
        product.setQuantity(10);
        product.setTags(Collections.singletonList("tech"));

        when(objectMapper.readValue(message, Product.class)).thenReturn(product);
        when(productRepository.existsById("p1")).thenReturn(false);
        when(productRepository.save(product)).thenReturn(product);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("product-topic", 0, 0L, null, message);

        // when
        handler.handle(record);

        // then
        verify(objectMapper).readValue(message, Product.class);
        verify(productRepository).existsById("p1");
        verify(productRepository).save(product);
    }

    // @Test
    void shouldThrowExceptionWhenValidationFails() throws Exception {
        // given: Product with empty name to trigger @NotBlank
        String message = "{\"id\":\"p2\",\"name\":\"\",\"brand\":\"Dell\",\"category\":\"Electronics\",\"price\":59999.0,\"isAvailable\":true,\"quantity\":10}";
        Product invalidProduct = new Product();
        invalidProduct.setId("p2");
        invalidProduct.setName("");  // triggers @NotBlank violation
        invalidProduct.setBrand("Dell");
        invalidProduct.setCategory("Electronics");
        invalidProduct.setPrice(59999.0);
        invalidProduct.setAvailable(true);
        invalidProduct.setQuantity(10);

        when(objectMapper.readValue(message, Product.class)).thenReturn(invalidProduct);

        ConsumerRecord<String, String> record = new ConsumerRecord<>("product-topic", 0, 0L, null, message);

        // when & then
        assertThatThrownBy(() -> handler.handle(record))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Validation failed");

        verify(objectMapper).readValue(message, Product.class);
        verify(productRepository, never()).save(any());
    }

    @Test
    void shouldHandleJsonProcessingExceptionGracefully() throws Exception {
        // given: invalid JSON
        String badMessage = "{invalid-json}";
        when(objectMapper.readValue(anyString(), eq(Product.class)))
                .thenThrow(new JsonProcessingException("Invalid JSON") {});

        ConsumerRecord<String, String> record = new ConsumerRecord<>("product-topic", 0, 0L, null, badMessage);

        // when
        handler.handle(record);

        // then: productRepository should never be called
        verify(productRepository, never()).save(any());
    }
}
