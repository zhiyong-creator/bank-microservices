package com.yuqixue.common.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * 通用业务异常类
 * 用于替代各微服务中的特定业务异常（如 CustomerAlreadyExistsException 等）
 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class BusinessException extends RuntimeException {
    
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
    
    /**
     * 创建资源已存在的异常
     * @param resourceName 资源名称（如 Customer、Card、Loan）
     * @param identifier 标识符（如 mobileNumber）
     * @return BusinessException
     */
    public static BusinessException alreadyExists(String resourceName, String identifier) {
        return new BusinessException(String.format("%s already exists with identifier: %s", resourceName, identifier));
    }
}
