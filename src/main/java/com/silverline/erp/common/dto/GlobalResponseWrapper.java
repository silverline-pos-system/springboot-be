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
        
        return true;
    }

    @Override
    public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
                                  Class<? extends HttpMessageConverter<?>> selectedConverterType,
                                  ServerHttpRequest request, ServerHttpResponse response) {
        
        String path = request.getURI().getPath();
        
        // Skip wrapping for Swagger docs, Actuator, or resource endpoints
        if (path.contains("/v3/api-docs") || path.contains("/swagger-ui") || path.contains("/actuator")) {
            return body;
        }

        // If body is already ApiResponse, return as-is
        if (body instanceof ApiResponse) {
            return body;
        }

        // Handle raw String returns specially to prevent ClassCastException inside Spring's StringHttpMessageConverter
        if (body instanceof String) {
            try {
                return new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(
                        ApiResponse.success("Operation completed successfully", body)
                );
            } catch (Exception e) {
                return body;
            }
        }

        return ApiResponse.success("Operation completed successfully", body);
    }
}
