package com.yuqixue.common.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Loans 服务的 Feign 客户端接口
 * 用于 accounts 或其他服务调用 loans 服务
 */
@FeignClient(name = "loans", path = "/api")
public interface LoansFeignClient {
    
    /**
     * 根据手机号查询贷款详情
     * @param mobileNumber 手机号（10位数字）
     * @return 贷款详情的 ResponseEntity
     */
    @GetMapping("/fetch")
    ResponseEntity<Object> fetchLoanDetails(@RequestParam("mobileNumber") String mobileNumber);
}
