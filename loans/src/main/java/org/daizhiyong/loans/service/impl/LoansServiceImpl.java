package org.daizhiyong.loans.service.impl;

import com.daizhiyong.common.exception.BusinessException;
import com.daizhiyong.common.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.daizhiyong.loans.constants.LoansConstants;
import org.daizhiyong.loans.dto.LoansDto;
import org.daizhiyong.loans.entity.Loans;
import org.daizhiyong.loans.mapper.LoansMapper;
import org.daizhiyong.loans.repository.LoansRepository;
import org.daizhiyong.loans.service.ILoanService;

import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
@Slf4j
public class LoansServiceImpl implements ILoanService.ILoansService {

    private LoansRepository loansRepository;

    /**
     * 创建贷款
     * @param mobileNumber 客户手机号
     */
    @Override
    public void createLoan(String mobileNumber) {
        log.info("创建新贷款, mobileNumber: {}", mobileNumber);
        
        Optional<Loans> optionalLoans = loansRepository.findByMobileNumber(mobileNumber);
        if(optionalLoans.isPresent()){
            log.warn("贷款已存在, mobileNumber: {}", mobileNumber);
            throw BusinessException.alreadyExists("Loan", mobileNumber);
        }
        
        Loans savedLoan = loansRepository.save(createNewLoan(mobileNumber));
        log.info("贷款创建成功, loanId: {}, loanNumber: {}", 
                savedLoan.getLoanId(), savedLoan.getLoanNumber());
    }

    /**
     * @param mobileNumber - Mobile Number of the Customer
     * @return the new loan details
     */
    private Loans createNewLoan(String mobileNumber) {
        Loans newLoan = new Loans();
        long randomLoanNumber = 100000000000L + new Random().nextInt(900000000);
        newLoan.setLoanNumber(Long.toString(randomLoanNumber));
        newLoan.setMobileNumber(mobileNumber);
        newLoan.setLoanType(LoansConstants.HOME_LOAN);
        newLoan.setTotalLoan(LoansConstants.NEW_LOAN_LIMIT);
        newLoan.setAmountPaid(0);
        newLoan.setOutstandingAmount(LoansConstants.NEW_LOAN_LIMIT);
        return newLoan;
    }

    /**
     * 查询贷款信息
     * @param mobileNumber 客户手机号
     * @return 贷款DTO
     */
    @Override
    public LoansDto fetchLoan(String mobileNumber) {
        log.debug("查询贷款信息, mobileNumber: {}", mobileNumber);
        
        Loans loans = loansRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Loan", "mobileNumber", mobileNumber)
        );
        
        log.info("贷款信息查询成功, mobileNumber: {}, loanNumber: {}", 
                mobileNumber, loans.getLoanNumber());
        return LoansMapper.mapToLoansDto(loans, new LoansDto());
    }

    /**
     *
     * @param loansDto - LoansDto Object
     * @return boolean indicating if the update of loan details is successful or not
     */
    @Override
    public boolean updateLoan(LoansDto loansDto) {
        Loans loans = loansRepository.findByLoanNumber(loansDto.getLoanNumber()).orElseThrow(
                () -> new ResourceNotFoundException("Loan", "LoanNumber", loansDto.getLoanNumber()));
        LoansMapper.mapToLoans(loansDto, loans);
        loansRepository.save(loans);
        return  true;
    }

    /**
     * @param mobileNumber - Input MobileNumber
     * @return boolean indicating if the delete of loan details is successful or not
     */
    @Override
    public boolean deleteLoan(String mobileNumber) {
        Loans loans = loansRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Loan", "mobileNumber", mobileNumber)
        );
        loansRepository.deleteById(loans.getLoanId());
        return true;
    }


}