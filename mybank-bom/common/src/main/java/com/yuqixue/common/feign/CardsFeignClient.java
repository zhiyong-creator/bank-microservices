package com.yuqixue.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Cards 服务的 Feign 客户端接口
 * 用于 accounts 或其他服务调用 cards 服务
 */
@FeignClient(name = "cards", path = "/api")
public interface CardsFeignClient {
    
    /**
     * 根据手机号查询卡片详情
     * @param mobileNumber 手机号（10位数字）
     * @return 卡片详情的 ResponseEntity
     */
    @GetMapping("/fetch")
    ResponseEntity<Object> fetchCardDetails(@RequestParam("mobileNumber") String mobileNumber);
}
