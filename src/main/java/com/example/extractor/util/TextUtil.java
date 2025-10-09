package com.example.extractor.util;

import org.apache.commons.text.StringEscapeUtils;
import org.apache.commons.text.WordUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TextUtil {

    private static final Logger log = LoggerFactory.getLogger(TextUtil.class);

    public String capitalizeWords(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        return WordUtils.capitalizeFully(text);
    }

    public String escapeHtml(String text) {
        if (text == null) {
            return null;
        }
        return StringEscapeUtils.escapeHtml4(text);
    }

    public String sanitizeProductName(String productName) {
        if (productName == null || productName.isEmpty()) {
            log.warn("Product name is null or empty");
            return productName;
        }

        String capitalized = capitalizeWords(productName);
        String sanitized = escapeHtml(capitalized);

        log.debug("Sanitized product name: {} -> {}", productName, sanitized);
        return sanitized;
    }

}
