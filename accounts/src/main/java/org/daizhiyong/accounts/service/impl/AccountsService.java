package org.daizhiyong.accounts.service.impl;

import com.daizhiyong.common.exception.BusinessException;
import com.daizhiyong.common.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.daizhiyong.accounts.constants.AccountsConstants;
import org.daizhiyong.accounts.dto.AccountsDto;
import org.daizhiyong.accounts.dto.CustomerDto;
import org.daizhiyong.accounts.entity.Accounts;
import org.daizhiyong.accounts.entity.Customer;
import org.daizhiyong.accounts.mapper.AccountsMapper;
import org.daizhiyong.accounts.mapper.CustomerMapper;
import org.daizhiyong.accounts.repository.AccountsRepository;
import org.daizhiyong.accounts.repository.CustomerRepository;
import org.daizhiyong.accounts.service.IAccountsService;

import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
@Slf4j
public class AccountsService implements IAccountsService {

    private AccountsRepository accountsRepo;
    private CustomerRepository customerRepo;


    /**
     * 创建账户
     * @param customerDto 客户DTO
     */
    @Override
    public void createAccount(CustomerDto customerDto) {
        log.info("创建新客户账户, mobileNumber: {}", customerDto.getMobileNumber());
        
        Customer customer = CustomerMapper.mapToCustomer(customerDto, new Customer());
        Optional<Customer> optionalCustomer = customerRepo.findByMobileNumber(customerDto.getMobileNumber());

        if(optionalCustomer.isPresent()){
            log.warn("客户已存在, mobileNumber: {}", customerDto.getMobileNumber());
            throw BusinessException.alreadyExists("Customer", customerDto.getMobileNumber());
        }

        Customer savedCustomer = customerRepo.save(customer);
        Accounts savedAccount = accountsRepo.save(createNewAccount(savedCustomer));
        
        log.info("账户创建成功, customerId: {}, accountNumber: {}", 
                savedCustomer.getCustomerId(), savedAccount.getAccountNumber());
    }

    /**
     * 查询账户信息
     * @param mobileNumber 手机号
     * @return 客户DTO
     */
    @Override
    public CustomerDto fetchAccount(String mobileNumber) {
        log.debug("查询账户信息, mobileNumber: {}", mobileNumber);
        
        Customer customer = customerRepo.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber));

        Accounts accounts = accountsRepo.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () -> new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString())
        );
        
        CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer, new CustomerDto());
        customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));
        
        log.info("账户信息查询成功, mobileNumber: {}, accountNumber: {}", 
                mobileNumber, accounts.getAccountNumber());
        return customerDto;
    }

    /**
     * @param customerDto - CustomerDto Object
     * @return boolean indicating if the update of Account detiails is successful or not
     */
    @Override
    public boolean updateAccount(CustomerDto customerDto) {
        boolean isUpdated = false;
        AccountsDto accountsDto = customerDto.getAccountsDto();
        if(accountsDto !=null ){
            Accounts accounts = accountsRepo.findById(accountsDto.getAccountNumber()).orElseThrow(
                    () -> new ResourceNotFoundException("Account", "AccountNumber", accountsDto.getAccountNumber().toString())
            );
            AccountsMapper.mapToAccounts(accountsDto, accounts);
            accounts = accountsRepo.save(accounts);

            Long customerId = accounts.getCustomerId();
            Customer customer = customerRepo.findById(customerId).orElseThrow(
                    () -> new ResourceNotFoundException("Customer", "CustomerID", customerId.toString())
            );
            CustomerMapper.mapToCustomer(customerDto,customer);
            customerRepo.save(customer);
            isUpdated = true;
        }
        return  isUpdated;
    }

    /**
     * @param mobileNumber
     * @return
     */
    @Override
    public boolean deleteAccount(String mobileNumber) {
        Customer customer = customerRepo.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Customer", "mobileNumber", mobileNumber)
        );
        accountsRepo.deleteByCustomerId(customer.getCustomerId());
        customerRepo.deleteById(customer.getCustomerId());
        return true;
    }


    private Accounts createNewAccount(Customer customer){
       Accounts newAccount = new Accounts();
       newAccount.setCustomerId(customer.getCustomerId());
       long randomAccNumber = 1000000000L + new Random().nextInt(900000000);

       newAccount.setAccountNumber(randomAccNumber);
       newAccount.setAccountType(AccountsConstants.SAVINGS);
       newAccount.setBranchAddress(AccountsConstants.ADDRESS);
       // createdAt, createdBy 等审计字段由 JPA 自动填充，无需手动设置
       return newAccount;
    }
}
