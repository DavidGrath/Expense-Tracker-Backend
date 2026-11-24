package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.entities.StatisticsFilterDb
import com.davidgrath.expensetrackerbackend.entities.dtos.ItemSumByCategory
import com.davidgrath.expensetrackerbackend.entities.dtos.TransactionAndItemCount
import com.davidgrath.expensetrackerbackend.generatedapi.model.*
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.service.exceptions.EntityNotFoundException
import com.davidgrath.expensetrackerbackend.service.exceptions.InvalidCurrencyCodeException
import com.davidgrath.expensetrackerbackend.service.exceptions.InvalidDateRangeException
import com.davidgrath.expensetrackerbackend.service.exceptions.StatisticsHeterogeneousCurrencyCodeException
import com.davidgrath.expensetrackerbackend.utils.*
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.util.*

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
    @Autowired
    lateinit var statisticsRepository: StatisticsRepository
    @Autowired
    lateinit var transactionRepository: TransactionRepository
    @Autowired
    lateinit var imageRepository: ImageRepository

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

        val foreignAccounts = statisticsFilter.accounts.toSet().subtract(userProfileAccounts.toSet())
        if(foreignAccounts.isNotEmpty()) {
            LOGGER.info("Filter accounts contains IDs that don't belong: {}", foreignAccounts)
            throw EntityNotFoundException()
        }
        val foreignCategories = (statisticsFilter.categories.toSet().subtract(userProfileCategories.toSet()))
        if(foreignCategories.isNotEmpty()) {
            LOGGER.info("Filter categories contains IDs that don't belong: {}", foreignCategories)
            throw EntityNotFoundException()
        }
        val foreignSellers = (statisticsFilter.sellers.toSet().subtract(userProfileSellers.toSet()))
        if(foreignSellers.isNotEmpty()) {
            LOGGER.info("Filter sellers contains IDs that don't belong: {}", foreignSellers)
            throw EntityNotFoundException()
        }
        val foreignSellerLocations = (statisticsFilter.sellerLocations.toSet().subtract(userProfileSellerLocations.toSet()))
        if(foreignSellerLocations.isNotEmpty()) {
            LOGGER.info("Filter seller locations contains IDs that don't belong: {}", foreignSellerLocations)
            throw EntityNotFoundException()
        }
        if(statisticsFilter.from != null && statisticsFilter.to != null) {
            if(statisticsFilter.from!!.compareTo(statisticsFilter.to!!) < 0) {
                throw InvalidDateRangeException()
            }
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
                throw InvalidCurrencyCodeException()
            } else {
                LOGGER.info("Currency code will override account list")
                ValidateStatisticsFilterResult.Valid
            }
        } else {
            LOGGER.info("Currency code is null")
            if(statisticsFilter.accounts.isEmpty()) {
                LOGGER.info("Account list is empty. Still valid because default will be picked")
                ValidateStatisticsFilterResult.Valid
            } else {
                val countDistinct = accountRepository.countDistinctCurrencyCodes(profileId, statisticsFilter.accounts)
                if (countDistinct > 1) {
                    LOGGER.info("More than 1 distinct currency code in account set")
                    throw StatisticsHeterogeneousCurrencyCodeException()
                } else {
                    ValidateStatisticsFilterResult.Valid
                }
            }

        }
        LOGGER.info("Validate filter: {}", ret)
        return ret
    }

    fun getStatisticsTransactions(profileId: Long, statisticsFilter: StatisticsFilter, baseUrl: Triple<String, String, Int>): List<StatisticsTransaction> {
        validateFilter(profileId, statisticsFilter)
        val converted = statisticsFilterToStatisticsFilterDb(statisticsFilter)
        val items = statisticsRepository.getItems(profileId, converted)
        val imageIds = items.filter { it.firstImageId != null }.map { it.firstImageId!! }.distinct()
        val categoryIds = items.map { it.categoryId }.distinct()

        val images = imageRepository.getAllByIds(profileId, imageIds).associateBy { it.id }
        val categories = categoryRepository.getAllByIds(profileId, categoryIds).associateBy { it.id }
        val itemsGroupedByTransactionId = items.groupBy { it.transactionId }
        val transactionIds = items.map { it.transactionId }.distinct()
        val transactions = transactionRepository.getAllByIds(profileId, transactionIds)
        val accountIds = transactionRepository.getDistinctAccountIdsByIds(profileId, transactionIds)
        val accounts = accountRepository.getAllByIds(profileId, accountIds)
        val accountsGroupedByOrdinal = accounts.associateBy { it.ordinal }
        val exclusionStats = transactionRepository.getExcludedTransactionItemStats(profileId, items.map { it.id }, false)
        val exclusionStatsGroupedByTransactionId = exclusionStats.associateBy { it.id }

        return transactions.map { t ->
            val items = itemsGroupedByTransactionId[t.id!!]!!.map { ti ->
                val category = categoryPojoToCategory(categories[ti.categoryId]!!)
                val firstImage = images[ti.firstImageId]
                val firstImageApi = if(firstImage != null) {
                    imagePojoToImage(firstImage, baseUrl)
                } else {
                    null
                }
                val weight: TransactionItemWeight? = if(ti.weight != null) {
                    val prefix: UnitPrefix? = if(ti.weightPrefix != null) {
                        UnitPrefix.fromValue(ti.weightPrefix)
                    } else {
                        null
                    }
                    TransactionItemWeight(ti.weight, WeightUnit.fromValue(ti.weightUnit), prefix)
                } else {
                    null
                }

                val volume: TransactionItemVolume? = if(ti.volume != null) {
                    val prefix: UnitPrefix? = if(ti.volumePrefix != null) {
                        UnitPrefix.fromValue(ti.volumePrefix)
                    } else {
                        null
                    }
                    TransactionItemVolume(ti.volume, VolumeUnit.fromValue(ti.volumeUnit), prefix)
                } else {
                    null
                }
                TransactionItemSummary(ti.id, ti.amount, ti.description, category, ti.quantity, ti.isReduction, ti.brand, ti.variation, weight, volume, firstImageApi)
            }
            val account = accountsGroupedByOrdinal[t.accountOrdinal!!]!!
            val apiAccount = accountPojoToAccount(account)
            val exclusionStats = exclusionStatsGroupedByTransactionId[t.id]
            StatisticsTransaction(t.id, apiAccount, t.amount!!, t.debitOrCredit!!.toBoolean(), t.datedAt!!, t.time?.toString(), t.mode.toApiMode(), exclusionStats?.excludedItemCount?:0, exclusionStats?.excludedItemAmount?: BigDecimal.ZERO,
                exclusionStats?.excludedChargesAmount?: BigDecimal.ZERO, items)
        }
    }

    fun getTransactionDebitSum(profileId: Long, statisticsFilter: StatisticsFilter): BigDecimal {
        val statisticsFilterDb = statisticsFilterToStatisticsFilterDb(statisticsFilter)
        return statisticsRepository.getTransactionItemDebitSum(profileId, statisticsFilterDb)
    }
    fun getTransactionCreditSum(profileId: Long, statisticsFilter: StatisticsFilter): BigDecimal {
        val statisticsFilterDb = statisticsFilterToStatisticsFilterDb(statisticsFilter)
        return statisticsRepository.getTransactionItemCreditSum(profileId, statisticsFilterDb)
    }
    fun getTransactionDebitSumByCategory(profileId: Long, statisticsFilter: StatisticsFilter): List<ItemSumByCategory> {
        val statisticsFilterDb = statisticsFilterToStatisticsFilterDb(statisticsFilter)
        val list = statisticsRepository.getSumByCategory(profileId, true, statisticsFilterDb)
        LOGGER.info("List size: {}", list.size)
        return list
    }
    fun getTransactionCreditSumByCategory(profileId: Long, statisticsFilter: StatisticsFilter): List<ItemSumByCategory> {
        val statisticsFilterDb = statisticsFilterToStatisticsFilterDb(statisticsFilter)
        val list = statisticsRepository.getSumByCategory(profileId, false, statisticsFilterDb)
        LOGGER.info("List size: {}", list.size)
        return list
    }
    fun getTransactionAndItemCount(profileId: Long, statisticsFilter: StatisticsFilterDb): TransactionAndItemCount {
        return statisticsRepository.getTransactionAndItemCount(profileId, statisticsFilter)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(StatisticsService::class.java)
    }
}