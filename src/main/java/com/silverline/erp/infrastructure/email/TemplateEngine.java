package com.silverline.erp.infrastructure.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
public final class TemplateEngine {

    private TemplateEngine() {
        // Utility class
    }

    /**
     * Load an HTML template from classpath resources under 'templates/' directory
     * and resolve variables of format ${variableName}.
     *
     * @param templateName the filename of the template (without .html extension)
     * @param variables    a map of variable names to replacement values
     * @return resolved HTML content string
     */
    public static String loadAndResolve(String templateName, Map<String, Object> variables) {
        try {
            ClassPathResource resource = new ClassPathResource("templates/" + templateName + ".html");
            String content = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
            for (Map.Entry<String, Object> entry : variables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue() != null ? String.valueOf(entry.getValue()) : "";
                content = content.replace("${" + key + "}", value);
            }
            return content;
        } catch (IOException e) {
            log.error("Failed to load and resolve HTML email template: {}", templateName, e);
            throw new RuntimeException("Failed to load email template: " + templateName, e);
        }
    }
}
