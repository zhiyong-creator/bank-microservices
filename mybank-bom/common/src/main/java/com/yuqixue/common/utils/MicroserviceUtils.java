package com.yuqixue.common.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;

import java.util.function.Function;

/**
 * 微服务调用工具类
 * 提供统一的 Feign Client 调用、数据转换和异常处理
 */
@Slf4j
public class MicroserviceUtils {
    
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    
    /**
     * 调用微服务并转换结果（带默认值）
     * 
     * @param call 微服务调用函数
     * @param converter 数据转换函数
     * @param defaultValue 失败时的默认值
     * @param serviceName 微服务名称（用于日志）
     * @param <T> 返回类型
     * @return 转换后的结果或默认值
     */
    public static <T> T callMicroserviceWithFallback(
            MicroserviceCall call,
            Function<Object, T> converter,
            T defaultValue,
            String serviceName) {
        try {
            ResponseEntity<Object> response = call.execute();
            if (response != null && response.getBody() != null) {
                return converter.apply(response.getBody());
            }
            return defaultValue;
        } catch (Exception e) {
            log.warn("调用微服务 [{}] 失败: {}", serviceName, e.getMessage());
            log.debug("详细错误信息", e);
            return defaultValue;
        }
    }
    
    /**
     * 调用微服务并转换为指定类型（无默认值，失败返回 null）
     * 
     * @param call 微服务调用函数
     * @param targetType 目标类型
     * @param serviceName 微服务名称（用于日志）
     * @param <T> 返回类型
     * @return 转换后的结果或 null
     */
    public static <T> T callMicroserviceAndConvert(
            MicroserviceCall call,
            Class<T> targetType,
            String serviceName) {
        return callMicroserviceWithFallback(
                call,
                body -> OBJECT_MAPPER.convertValue(body, targetType),
                null,
                serviceName
        );
    }
    
    /**
     * 微服务调用函数式接口
     */
    @FunctionalInterface
    public interface MicroserviceCall {
        ResponseEntity<Object> execute() throws Exception;
    }
}
