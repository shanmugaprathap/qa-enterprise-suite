package com.enterprise.qa.core.ai.datagen;

import com.enterprise.qa.core.config.ConfigManager;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * AI-powered test data generator using OpenAI API.
 * Generates realistic, context-aware test data based on natural language descriptions.
 */
@Slf4j
public class AITestDataGenerator {

    private final LLMClient llmClient;
    private final ObjectMapper objectMapper;
    private final Map<String, String> cache;
    private final boolean enabled;

    public AITestDataGenerator() {
        this.llmClient = new LLMClient();
        this.objectMapper = new ObjectMapper();
        this.cache = new ConcurrentHashMap<>();
        this.enabled = ConfigManager.getInstance().isAiDataGenerationEnabled();
    }

    /**
     * Generates test data based on a natural language description.
     *
     * @param description a description of the data to generate
     * @return the generated data as a JSON string
     */
    public String generate(String description) {
        if (!enabled) {
            log.warn("AI data generation is disabled. Set OPENAI_API_KEY and enable ai.datagen.enabled");
            return generateFallbackData(description);
        }

        // Check cache first
        String cacheKey = description.toLowerCase().trim();
        if (cache.containsKey(cacheKey)) {
            log.debug("Returning cached data for: {}", description);
            return cache.get(cacheKey);
        }

        String prompt = buildPrompt(description);
        String result = llmClient.complete(prompt);

        if (result != null) {
            cache.put(cacheKey, result);
        } else {
            result = generateFallbackData(description);
        }

        return result;
    }

    /**
     * Generates user data based on a persona description.
     *
     * @param persona a description of the user persona
     * @return JSON string with user data
     */
    public String generateUserData(String persona) {
        String description = String.format(
                "Generate realistic user data for: %s. Include: firstName, lastName, email, phone, " +
                "address (street, city, state, zip, country), dateOfBirth (YYYY-MM-DD format).",
                persona
        );
        return generate(description);
    }

    /**
     * Generates credit card test data.
     *
     * @param cardType the type of card (visa, mastercard, amex)
     * @return JSON string with card data
     */
    public String generateCreditCard(String cardType) {
        String description = String.format(
                "Generate test credit card data for %s (use standard test card numbers). " +
                "Include: cardNumber, expiryMonth, expiryYear, cvv, cardholderName.",
                cardType
        );
        return generate(description);
    }

    /**
     * Generates product data.
     *
     * @param category the product category
     * @param count    number of products to generate
     * @return JSON string with product array
     */
    public String generateProducts(String category, int count) {
        String description = String.format(
                "Generate %d realistic product entries for category '%s'. " +
                "Each product should have: id, name, description, price, sku, category, inStock.",
                count, category
        );
        return generate(description);
    }

    /**
     * Generates form data for a specific form type.
     *
     * @param formType the type of form (registration, checkout, contact, etc.)
     * @return JSON string with form data
     */
    public String generateFormData(String formType) {
        String description = String.format(
                "Generate realistic form data for a %s form. Include all typical fields with valid values.",
                formType
        );
        return generate(description);
    }

    /**
     * Generates a list of search queries for testing.
     *
     * @param domain  the domain/context (e.g., "e-commerce", "travel")
     * @param count   number of queries to generate
     * @return list of search queries
     */
    public List<String> generateSearchQueries(String domain, int count) {
        String description = String.format(
                "Generate %d realistic search queries a user might type when searching for " +
                "products/services in the %s domain. Return as a JSON array of strings.",
                count, domain
        );

        String result = generate(description);
        return parseStringArray(result);
    }

    /**
     * Generates edge case test data for a field.
     *
     * @param fieldName the name of the field
     * @param fieldType the type of field (text, email, phone, number, date)
     * @return list of edge case values
     */
    public List<String> generateEdgeCases(String fieldName, String fieldType) {
        String description = String.format(
                "Generate edge case test values for a %s field named '%s'. " +
                "Include: valid boundary values, invalid values, special characters, " +
                "SQL injection attempts (safe), XSS attempts (safe), empty values, " +
                "very long values. Return as a JSON array of strings.",
                fieldType, fieldName
        );

        String result = generate(description);
        return parseStringArray(result);
    }

    /**
     * Generates localized test data for a specific locale.
     *
     * @param dataType the type of data to generate
     * @param locale   the locale (e.g., "en-US", "de-DE", "ja-JP")
     * @return JSON string with localized data
     */
    public String generateLocalizedData(String dataType, String locale) {
        String description = String.format(
                "Generate realistic %s data localized for %s. Use appropriate " +
                "formatting, names, addresses, and conventions for that locale.",
                dataType, locale
        );
        return generate(description);
    }

    /**
     * Builds the prompt for the LLM.
     */
    private String buildPrompt(String description) {
        return String.format("""
                You are a test data generator for software testing. Generate realistic test data based on this description:

                %s

                Requirements:
                - Return ONLY valid JSON (no markdown, no explanation)
                - Use realistic but fake data (don't use real PII)
                - Use standard test values where appropriate (e.g., test credit card numbers)
                - Ensure all data is properly formatted
                - If generating multiple items, use a JSON array

                Generate the data now:""", description);
    }

    /**
     * Generates fallback data when AI is not available.
     */
    private String generateFallbackData(String description) {
        log.debug("Using fallback data generation for: {}", description);

        String descLower = description.toLowerCase();

        if (descLower.contains("user")) {
            return """
                    {
                        "firstName": "John",
                        "lastName": "Doe",
                        "email": "john.doe@example.com",
                        "phone": "+1-555-123-4567",
                        "address": {
                            "street": "123 Test Street",
                            "city": "Test City",
                            "state": "CA",
                            "zip": "90210",
                            "country": "USA"
                        },
                        "dateOfBirth": "1990-01-15"
                    }""";
        }

        if (descLower.contains("credit card") || descLower.contains("card")) {
            return """
                    {
                        "cardNumber": "4111111111111111",
                        "expiryMonth": "12",
                        "expiryYear": "2025",
                        "cvv": "123",
                        "cardholderName": "John Doe"
                    }""";
        }

        if (descLower.contains("product")) {
            return """
                    [{
                        "id": "PROD-001",
                        "name": "Test Product",
                        "description": "A product for testing purposes",
                        "price": 29.99,
                        "sku": "SKU-12345",
                        "category": "Test Category",
                        "inStock": true
                    }]""";
        }

        // Default generic data
        return """
                {
                    "name": "Test Name",
                    "value": "Test Value",
                    "description": "Test Description",
                    "timestamp": "2024-01-15T10:30:00Z"
                }""";
    }

    /**
     * Parses a JSON array of strings.
     */
    private List<String> parseStringArray(String json) {
        try {
            JsonNode node = objectMapper.readTree(json);
            List<String> result = new ArrayList<>();
            if (node.isArray()) {
                for (JsonNode item : node) {
                    result.add(item.asText());
                }
            }
            return result;
        } catch (Exception e) {
            log.error("Failed to parse string array: {}", e.getMessage());
            return Collections.singletonList(json);
        }
    }

    /**
     * Clears the data cache.
     */
    public void clearCache() {
        cache.clear();
    }

    /**
     * Checks if AI data generation is enabled.
     */
    public boolean isEnabled() {
        return enabled;
    }
}
