package org.yiqixue.accounts.service.impl;

import com.yuqixue.common.exception.ResourceNotFoundException;
import com.yuqixue.common.feign.CardsFeignClient;
import com.yuqixue.common.feign.LoansFeignClient;
import com.yuqixue.common.utils.MicroserviceUtils;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.yiqixue.accounts.dto.AccountsDto;
import org.yiqixue.accounts.dto.CardsDto;
import org.yiqixue.accounts.dto.CustomerDetailsDto;
import org.yiqixue.accounts.dto.LoansDto;
import org.yiqixue.accounts.entity.Accounts;
import org.yiqixue.accounts.entity.Customer;
import org.yiqixue.accounts.mapper.AccountsMapper;
import org.yiqixue.accounts.mapper.CustomerMapper;
import org.yiqixue.accounts.repository.AccountsRepository;
import org.yiqixue.accounts.repository.CustomerRepository;
import org.yiqixue.accounts.service.ICustomerService;

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
        log.debug("开始查询客户详情, mobileNumber: {}", mobileNumber);

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
            CardsDto cardsDto = MicroserviceUtils.callMicroserviceAndConvert(
                    () -> cardsFeignClient.fetchCardDetails(mobileNumber),
                    CardsDto.class,
                    "Cards Service"
            );
            if (cardsDto != null) {
                customerDetailsDto.setCardsDto(cardsDto);
                log.debug("成功获取卡片信息, mobileNumber: {}", mobileNumber);
            }
        });

        CompletableFuture<Void> loansFuture = CompletableFuture.runAsync(() -> {
            LoansDto loansDto = MicroserviceUtils.callMicroserviceAndConvert(
                    () -> loansFeignClient.fetchLoanDetails(mobileNumber),
                    LoansDto.class,
                    "Loans Service"
            );
            if (loansDto != null) {
                customerDetailsDto.setLoansDto(loansDto);
                log.debug("成功获取贷款信息, mobileNumber: {}", mobileNumber);
            }
        });

        // 4. 等待两个异步任务完成
        CompletableFuture.allOf(cardsFuture, loansFuture).join();
        
        log.info("客户详情查询完成, mobileNumber: {}, hasCards: {}, hasLoans: {}", 
                mobileNumber, 
                customerDetailsDto.getCardsDto() != null,
                customerDetailsDto.getLoansDto() != null);

        return customerDetailsDto;
    }
}