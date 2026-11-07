package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.generatedapi.model.StatisticsFilter
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.CategoryRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Currency

@Service
class StatisticsService {

    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository

    enum class ValidateStatisticsFilterResult {
        Valid,
        EntityDoesNotBelongToOwner,
        CurrencyOrAccountIssue
    }


    fun validateFilter(profileId: Long, statisticsFilter: StatisticsFilter): ValidateStatisticsFilterResult {

        val userProfileAccounts = accountRepository.getAllByProfileId(profileId).map { it.id!! }
        val userProfileCategories = categoryRepository.getAllByProfileId(profileId).map { it.id!! }
        val userProfileSellers = sellerRepository.getAllByProfileId(profileId).map { it.id!! }
        val userProfileSellerLocations = sellerLocationRepository.getAllByProfileId(profileId).map { it.id!! }

        if((statisticsFilter.accounts.toSet().subtract(userProfileAccounts.toSet())).isNotEmpty()) {
            LOGGER.info("Filter accounts contains IDs that don't belong")
            return ValidateStatisticsFilterResult.EntityDoesNotBelongToOwner
        }
        if((statisticsFilter.categories.toSet().subtract(userProfileCategories.toSet())).isNotEmpty()) {
            LOGGER.info("Filter categories contains IDs that don't belong")
            return ValidateStatisticsFilterResult.EntityDoesNotBelongToOwner
        }
        if((statisticsFilter.sellers.toSet().subtract(userProfileSellers.toSet())).isNotEmpty()) {
            LOGGER.info("Filter sellers contains IDs that don't belong")
            return ValidateStatisticsFilterResult.EntityDoesNotBelongToOwner
        }
        if((statisticsFilter.sellerLocations.toSet().subtract(userProfileSellerLocations.toSet())).isNotEmpty()) {
            LOGGER.info("Filter seller locations contains IDs that don't belong")
            return ValidateStatisticsFilterResult.EntityDoesNotBelongToOwner
        }
        val ret = if(statisticsFilter.currencyCode != null) {
            LOGGER.info("Currency code is not null")
            val exception: IllegalArgumentException? = try {
                Currency.getInstance(statisticsFilter.currencyCode!!)
                null
            } catch (e: IllegalArgumentException) {
                e
            }
            if(exception != null) {
                LOGGER.info("Currency parse exception")
                ValidateStatisticsFilterResult.CurrencyOrAccountIssue
            } else {
                LOGGER.info("Currency code will override account list")
                ValidateStatisticsFilterResult.Valid
            }
        } else {
            LOGGER.info("Currency code is null")
            if(statisticsFilter.accounts.isEmpty()) {
                LOGGER.info("Account list is empty")
                ValidateStatisticsFilterResult.CurrencyOrAccountIssue
            } else {
                val countDistinct = accountRepository.countDistinctCurrencyCodes(profileId, statisticsFilter.accounts)
                if (countDistinct > 1) {
                    LOGGER.info("More than 1 distinct currency code in account set")
                    ValidateStatisticsFilterResult.CurrencyOrAccountIssue
                } else {
                    ValidateStatisticsFilterResult.Valid
                }
            }

        }
        LOGGER.info("Validate filter: {}", ret)
        return ret
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsService::class.java)
    }
}