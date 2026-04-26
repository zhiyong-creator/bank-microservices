package org.daizhiyong.cards.service.impl;

import com.daizhiyong.common.exception.BusinessException;
import com.daizhiyong.common.exception.ResourceNotFoundException;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.daizhiyong.cards.constants.CardsConstants;
import org.daizhiyong.cards.dto.CardsDto;
import org.daizhiyong.cards.entity.Cards;
import org.daizhiyong.cards.mapper.CardsMapper;
import org.daizhiyong.cards.repository.CardsRepository;
import org.daizhiyong.cards.service.ICardsService;

import java.util.Optional;
import java.util.Random;

@Service
@AllArgsConstructor
@Slf4j
public class CardsServiceImpl implements ICardsService {

    private CardsRepository cardsRepository;

    /**
     * 创建卡片
     * @param mobileNumber 客户手机号
     */
    @Override
    public void createCard(String mobileNumber) {
        log.info("创建新卡片, mobileNumber: {}", mobileNumber);
        
        Optional<Cards> optionalCards = cardsRepository.findByMobileNumber(mobileNumber);
        if(optionalCards.isPresent()){
            log.warn("卡片已存在, mobileNumber: {}", mobileNumber);
            throw BusinessException.alreadyExists("Card", mobileNumber);
        }
        
        Cards savedCard = cardsRepository.save(createNewCard(mobileNumber));
        log.info("卡片创建成功, cardId: {}, cardNumber: {}", 
                savedCard.getCardId(), savedCard.getCardNumber());
    }

    /**
     * 创建新卡片对象
     * @param mobileNumber 客户手机号
     * @return 卡片实体
     */
    private Cards createNewCard(String mobileNumber) {
        Cards newCard = new Cards();
        long randomCardNumber = 100000000000L + new Random().nextInt(900000000);
        newCard.setCardNumber(Long.toString(randomCardNumber));
        newCard.setMobileNumber(mobileNumber);
        newCard.setCardType(CardsConstants.CREDIT_CARD);
        newCard.setTotalLimit(CardsConstants.NEW_CARD_LIMIT);
        newCard.setAmountUsed(0);
        newCard.setAvailableAmount(CardsConstants.NEW_CARD_LIMIT);
        // createdAt, createdBy 等审计字段由 JPA 自动填充，无需手动设置
        return newCard;
    }

    /**
     * 查询卡片信息
     * @param mobileNumber 客户手机号
     * @return 卡片DTO
     */
    @Override
    public CardsDto fetchCard(String mobileNumber) {
        log.debug("查询卡片信息, mobileNumber: {}", mobileNumber);
        
        Cards cards = cardsRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Card", "mobileNumber", mobileNumber)
        );
        
        log.info("卡片信息查询成功, mobileNumber: {}, cardNumber: {}", 
                mobileNumber, cards.getCardNumber());
        return CardsMapper.mapToCardsDto(cards, new CardsDto());
    }

    /**
     *
     * @param cardsDto - CardsDto Object
     * @return boolean indicating if the update of card details is successful or not
     */
    @Override
    public boolean updateCard(CardsDto cardsDto) {
        Cards cards = cardsRepository.findByCardNumber(cardsDto.getCardNumber()).orElseThrow(
                () -> new ResourceNotFoundException("Card", "CardNumber", cardsDto.getCardNumber()));
        CardsMapper.mapToCards(cardsDto, cards);
        cardsRepository.save(cards);
        return  true;
    }

    /**
     * @param mobileNumber - Input MobileNumber
     * @return boolean indicating if the delete of card details is successful or not
     */
    @Override
    public boolean deleteCard(String mobileNumber) {
        Cards cards = cardsRepository.findByMobileNumber(mobileNumber).orElseThrow(
                () -> new ResourceNotFoundException("Card", "mobileNumber", mobileNumber)
        );
        cardsRepository.deleteById(cards.getCardId());
        return true;
    }


}