package org.yiqixue.accounts.service.impl;

import com.yuqixue.common.exception.BusinessException;
import com.yuqixue.common.exception.ResourceNotFoundException;
import com.yuqixue.common.feign.CardsFeignClient;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.yiqixue.accounts.constants.AccountsConstants;
import org.yiqixue.accounts.dto.AccountsDto;
import org.yiqixue.accounts.dto.CustomerDto;
import org.yiqixue.accounts.entity.Accounts;
import org.yiqixue.accounts.entity.Customer;
import org.yiqixue.accounts.mapper.AccountsMapper;
import org.yiqixue.accounts.mapper.CustomerMapper;
import org.yiqixue.accounts.repository.AccountsRepository;
import org.yiqixue.accounts.repository.CustomerRepository;
import org.yiqixue.accounts.service.IAccountsService;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
public class AccountsService implements IAccountsService {

    //private final CustomerRepository customerRepository;
    private AccountsRepository accountsRepo;
    private CustomerRepository customerRepo;


    /**
     * @param customerDto - CustomerDto object
     */
    @Override
    public void createAccount(CustomerDto customerDto) {
        Customer customer = CustomerMapper.mapToCustomer(customerDto, new Customer());
        Optional<Customer> optionalCustomer = customerRepo.findByMobileNumber(customerDto.getMobileNumber());

        if(optionalCustomer.isPresent()){
            throw BusinessException.alreadyExists("Customer", customerDto.getMobileNumber());
        }

//        customer.setCreatedAt(LocalDateTime.now());
//        customer.setCreatedBy("Anonymous");

        Customer savedCustomer = customerRepo.save(customer);
        accountsRepo.save(createNewAccount(savedCustomer));

    }

    /**
     * @param mobileNumber
     * @return
     */

    @Autowired
    private CardsFeignClient cardsFeignClient;
    @Override
    public CustomerDto fetchAccount(String mobileNumber) {

        Customer customer = customerRepo.findByMobileNumber(mobileNumber).orElseThrow(
                ()-> new ResourceNotFoundException("Customer", "mobileNumaber", mobileNumber));

        Accounts accounts = accountsRepo.findByCustomerId(customer.getCustomerId()).orElseThrow(
                () ->new ResourceNotFoundException("Account", "customerId", customer.getCustomerId().toString()
                )
        );
        CustomerDto customerDto = CustomerMapper.mapToCustomerDto(customer,new CustomerDto());
        customerDto.setAccountsDto(AccountsMapper.mapToAccountsDto(accounts, new AccountsDto()));
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


    /**
     *
     * @param customer - Customer Object
     * @return the account object
     */

    private Accounts createNewAccount(Customer customer){
       Accounts newAccount = new Accounts();
       newAccount.setCustomerId(customer.getCustomerId());
       long randomAccNumber = 1000000000L+new Random().nextInt(900000000);

       newAccount.setAccountNumber(randomAccNumber);
       newAccount.setAccountType(AccountsConstants.SAVINGS);
       newAccount.setBranchAddress(AccountsConstants.ADDRESS);
//       newAccount.setCreatedAt(LocalDateTime.now());
//       newAccount.setCreatedBy("Anonymous");

       return newAccount;
    }
}
