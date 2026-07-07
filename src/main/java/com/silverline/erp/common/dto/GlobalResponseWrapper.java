package com.silverline.erp.common.dto;

import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@RestControllerAdvice(basePackages = "com.silverline.erp")
public class GlobalResponseWrapper implements ResponseBodyAdvice<Object> {

    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;
    private static final com.fasterxml.jackson.databind.ObjectMapper FALLBACK_MAPPER = new com.fasterxml.jackson.databind.ObjectMapper();

    public GlobalResponseWrapper(@org.springframework.beans.factory.annotation.Autowired(required = false) com.fasterxml.jackson.databind.ObjectMapper objectMapper) {
        this.objectMapper = objectMapper != null ? objectMapper : FALLBACK_MAPPER;
    }

    @Override
    public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
        Class<?> paramType = returnType.getParameterType();
        
        // Skip wrapping if it is already an ApiResponse
        if (ApiResponse.class.isAssignableFrom(paramType)) {
            return false;
        }
        
        // Skip wrapping for file downloads, raw streams, or system endpoints
        if (paramType == byte[].class || 
            org.springframework.web.servlet.mvc.method.annotation.SseEmitter.class.isAssignableFrom(paramType) ||
            org.springframework.core.io.Resource.class.isAssignableFrom(paramType)) {
            return false;
        }

        // Skip wrapping if the message converter writes raw byte arrays or resources directly
        if (org.springframework.http.converter.ByteArrayHttpMessageConverter.class.isAssignableFrom(converterType) ||
            org.springframework.http.converter.ResourceHttpMessageConverter.class.isAssignableFrom(converterType)) {
            return false;
        }
        
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        // If body is already ApiResponse, return as-is
        if (body instanceof ApiResponse) {
            return body;
        }

        // Handle raw String returns specially to prevent ClassCastException inside Spring's StringHttpMessageConverter
        if (body instanceof String) {
            try {
                return objectMapper.writeValueAsString(
                        ApiResponse.success("Operation completed successfully", body)
                );
            } catch (Exception e) {
                return body;
            }
        }

        return ApiResponse.success("Operation completed successfully", body);
    }
}
