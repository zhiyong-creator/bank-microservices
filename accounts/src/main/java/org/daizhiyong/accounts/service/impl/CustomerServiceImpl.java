package org.daizhiyong.accounts.service.impl;

import com.daizhiyong.common.exception.ResourceNotFoundException;
import com.daizhiyong.common.feign.CardsFeignClient;
import com.daizhiyong.common.feign.LoansFeignClient;
import com.daizhiyong.common.utils.MicroserviceUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.daizhiyong.accounts.dto.AccountsDto;
import org.daizhiyong.accounts.dto.CardsDto;
import org.daizhiyong.accounts.dto.CustomerDetailsDto;
import org.daizhiyong.accounts.dto.LoansDto;
import org.daizhiyong.accounts.entity.Accounts;
import org.daizhiyong.accounts.entity.Customer;
import org.daizhiyong.accounts.mapper.AccountsMapper;
import org.daizhiyong.accounts.mapper.CustomerMapper;
import org.daizhiyong.accounts.repository.AccountsRepository;
import org.daizhiyong.accounts.repository.CustomerRepository;
import org.daizhiyong.accounts.service.ICustomerService;

import java.util.concurrent.CompletableFuture;

@Service
@AllArgsConstructor
@Slf4j
public class CustomerServiceImpl implements ICustomerService {

    private AccountsRepository accountsRepository;
    private CustomerRepository customerRepository;
    private CardsFeignClient cardsFeignClient;
    private LoansFeignClient loansFeignClient;
    
    @Override
    public CustomerDetailsDto fetchCustomerDetails(String mobileNumber) {
        long startTime = System.currentTimeMillis();
        log.info("开始查询客户完整详情, mobileNumber: {}", mobileNumber);

        try {
            // 1. 从本地数据库查询 customer 和 account 信息
            Customer customer = customerRepository.findByMobileNumber(mobileNumber).orElseThrow(
                    () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
            );
            
            Accounts accounts = accountsRepository.findByCustomerId(customer.getCustomerId()).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
            );

            // 2. 组装基础客户信息
            CustomerDetailsDto customerDetailsDto = CustomerMapper.mapToCustomerDetailsDto(customer, new CustomerDetailsDto());
            customerDetailsDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));

            // 3. 并行调用 cards 和 loans 微服务，使用通用工具类处理
            CompletableFuture<Void> cardsFuture = CompletableFuture.runAsync(() -> {
                long cardsStartTime = System.currentTimeMillis();
                CardsDto cardsDto = MicroserviceUtils.callMicroserviceAndConvert(
                        () -> cardsFeignClient.fetchCardDetails(mobileNumber),
                        CardsDto.class,
                        "Cards Service"
                );
                long cardsDuration = System.currentTimeMillis() - cardsStartTime;
                
                if (cardsDto != null) {
                    customerDetailsDto.setCardsDto(cardsDto);
                    log.debug("成功获取卡片信息, mobileNumber: {}, 耗时: {}ms", mobileNumber, cardsDuration);
                } else {
                    log.warn("未获取到卡片信息, mobileNumber: {}", mobileNumber);
                }
            });

            CompletableFuture<Void> loansFuture = CompletableFuture.runAsync(() -> {
                long loansStartTime = System.currentTimeMillis();
                LoansDto loansDto = MicroserviceUtils.callMicroserviceAndConvert(
                        () -> loansFeignClient.fetchLoanDetails(mobileNumber),
                        LoansDto.class,
                        "Loans Service"
                );
                long loansDuration = System.currentTimeMillis() - loansStartTime;
                
                if (loansDto != null) {
                    customerDetailsDto.setLoansDto(loansDto);
                    log.debug("成功获取贷款信息, mobileNumber: {}, 耗时: {}ms", mobileNumber, loansDuration);
                } else {
                    log.warn("未获取到贷款信息, mobileNumber: {}", mobileNumber);
                }
            });

            // 4. 等待两个异步任务完成
            CompletableFuture.allOf(cardsFuture, loansFuture).join();
            
            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("客户详情查询完成, mobileNumber: {}, hasCards: {}, hasLoans: {}, 总耗时: {}ms", 
                    mobileNumber, 
                    customerDetailsDto.getCardsDto() != null,
                    customerDetailsDto.getLoansDto() != null,
                    totalDuration);

            return customerDetailsDto;
            
        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            log.error("查询客户详情失败, mobileNumber: {}, 耗时: {}ms, 错误: {}", 
                    mobileNumber, duration, e.getMessage(), e);
            throw e;
        }
    }
}