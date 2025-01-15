package org.yiqixue.accounts.service.client;

import com.yuqixue.common.feign.CardsFeignClient;
import com.yuqixue.common.feign.LoansFeignClient;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * 微服务客户端服务类
 * 用于调用其他微服务（cards、loans）的 API
 */
@Service
@AllArgsConstructor
public class MicroserviceClientService {
    
    private final CardsFeignClient cardsFeignClient;
    private final LoansFeignClient loansFeignClient;
    
    /**
     * 查询客户的卡片详情
     * @param mobileNumber 手机号
     * @return 卡片详情
     */
    public Object getCardDetails(String mobileNumber) {
        return cardsFeignClient.fetchCardDetails(mobileNumber);
    }
    
    /**
     * 查询客户的贷款详情
     * @param mobileNumber 手机号
     * @return 贷款详情
     */
    public Object getLoanDetails(String mobileNumber) {
        return loansFeignClient.fetchLoanDetails(mobileNumber);
    }
    
    /**
     * 查询客户的完整信息（包括卡片和贷款）
     * @param mobileNumber 手机号
     * @return 包含卡片和贷款信息的对象
     */
    public CustomerFullDetails getCustomerFullDetails(String mobileNumber) {
        Object cardDetails = null;
        Object loanDetails = null;
        
        try {
            cardDetails = cardsFeignClient.fetchCardDetails(mobileNumber);
        } catch (Exception e) {
            // 如果调用失败，记录日志但不影响整体流程
            System.err.println("Failed to fetch card details: " + e.getMessage());
        }
        
        try {
            loanDetails = loansFeignClient.fetchLoanDetails(mobileNumber);
        } catch (Exception e) {
            // 如果调用失败，记录日志但不影响整体流程
            System.err.println("Failed to fetch loan details: " + e.getMessage());
        }
        
        return new CustomerFullDetails(cardDetails, loanDetails);
    }
    
    /**
     * 内部类：客户完整信息
     */
    public static class CustomerFullDetails {
        private Object cardDetails;
        private Object loanDetails;
        
        public CustomerFullDetails(Object cardDetails, Object loanDetails) {
            this.cardDetails = cardDetails;
            this.loanDetails = loanDetails;
        }
        
        public Object getCardDetails() {
            return cardDetails;
        }
        
        public void setCardDetails(Object cardDetails) {
            this.cardDetails = cardDetails;
        }
        
        public Object getLoanDetails() {
            return loanDetails;
        }
        
        public void setLoanDetails(Object loanDetails) {
            this.loanDetails = loanDetails;
        }
    }
}
