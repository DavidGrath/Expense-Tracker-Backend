package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.generatedapi.model.StatisticsFilter
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.Currency

@Service
class StatisticsService {

    @Autowired
    lateinit var accountRepository: AccountRepository


    /**
     * TODO maybe IDOR checks should happen here
     */
    fun validateFilter(profileId: Long, statisticsFilter: StatisticsFilter): Boolean {
        //TODO IDOR Checks on next commit
        statisticsFilter.accounts
        statisticsFilter.sellers
        statisticsFilter.sellerLocations
        statisticsFilter.categories
        val ret = if(statisticsFilter.currencyCode != null) {
            val exception: IllegalArgumentException? = try {
                Currency.getInstance(statisticsFilter.currencyCode!!)
                null
            } catch (e: IllegalArgumentException) {
                e
            }
            if(exception != null) {
                false
            } else {
                LOGGER.info("Currency code will override account list")
                true
            }
        } else {
            if(statisticsFilter.accounts.isEmpty()) {
                false
            } else {
                val countDistinct = accountRepository.countDistinctCurrencyCodes(profileId, statisticsFilter.accounts)
                if (countDistinct > 1) {
                    false
                } else {
                    true
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