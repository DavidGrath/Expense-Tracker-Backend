package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.dtos.*
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.generatedapi.model.StatisticsTransaction
import com.davidgrath.expensetrackerbackend.generatedapi.model.SuggestionsFieldType
import com.davidgrath.expensetrackerbackend.generatedapi.model.Transaction
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionItemSummary
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionItemSummaryVolume
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionItemSummaryWeight
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionItemVolume
import com.davidgrath.expensetrackerbackend.generatedapi.model.TransactionItemWeight
import com.davidgrath.expensetrackerbackend.generatedapi.model.UnitPrefix
import com.davidgrath.expensetrackerbackend.generatedapi.model.VolumeUnit
import com.davidgrath.expensetrackerbackend.generatedapi.model.WeightUnit
import com.davidgrath.expensetrackerbackend.repository.*
import com.davidgrath.expensetrackerbackend.service.exceptions.CountLimitExceededException
import com.davidgrath.expensetrackerbackend.utils.*
import com.davidgrath.expensetrackerbackend.utils.Constants.Companion.FALSE_BYTE
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.*

@Service
class TransactionService {

    @Autowired
    lateinit var transactionRepository: TransactionRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository
    @Autowired
    lateinit var categoryRepository: CategoryRepository
    @Autowired
    lateinit var genericRepository: GenericRepository
    @Autowired
    lateinit var timeHandler: TimeHandler
    @Autowired
    lateinit var imageRepository: ImageRepository
    
    fun addTransaction(profileId: Long, accountId: Long, debitOrCredit: Boolean, amount: BigDecimal, description: String, categoryId: Long, zoneId: ZoneId): Long {
        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val localDate = utcDate.toLocalDate()
        val userTime = date.withZoneSameInstant(zoneId).toLocalTime()
        var transactionIdRet = -1L
        genericRepository.transactionContext { trx ->
            val trxContext = trx.dsl()
            val account = accountRepository.getByProfileIdAndIdTrx(trxContext, profileId, accountId)!!
            val ordinalSortKeyOccupancySlot = transactionRepository.getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext, profileId, accountId)
            val nextOrdinal = ((ordinalSortKeyOccupancySlot.ordinal?: 0) + 1).toShort()
            val allSlots = transactionRepository.getAllOccupancySlotsTrx(trxContext, profileId, accountId)
//            val nextOccupancySlot = ((ordinalSortKeyOccupancySlot.occupancySlot?: 0) + 1).toShort()
            val nextOccupancySlot = findEarliestOccupancySlot(allSlots.toShortArray(), Constants.Constraints.MAX_TRANSACTIONS_PER_PROFILE)
            if(nextOccupancySlot == null) {
                throw CountLimitExceededException(Constants.Constraints.MAX_TRANSACTIONS_PER_PROFILE)
            }

            val idsToKeys = transactionRepository.getIdsAndSortKeysByProfileIdAndAccountIdAndDateTrx(trxContext, profileId, accountId, localDate)
            val idToSortKeyMap = idsToKeys.associate { it.id!! to it.sortKey!! }
            val mover = object: Mover<Long> {
                override fun moveSortKey(id: Long, sortKey: Short) {
                    transactionRepository.moveSortKeyTrx(trxContext, profileId, id, accountId, localDate,sortKey)
                }
            }

            val nextSortKey = getNextSortKey(ordinalSortKeyOccupancySlot.sortKey?:0, idToSortKeyMap, mover)

            val transaction = TransactionPojo(
                null,
                profileId,
                amount,
                debitOrCredit.toByte(),
                localDate,
                userTime,
                TransactionMode.Other,
                null,
                account.ordinal,
                null,
                null,
                nextOrdinal,
                nextSortKey,
                nextOccupancySlot,
                utcDate.toLocalDateTime()
            )
            val transactionId = transactionRepository.insertTransactionTrx(trxContext, transaction)
            val category = categoryRepository.findByProfileIdAndIdTrx(trxContext, profileId, categoryId)
            val item = TransactionItemPojo(null, profileId, account.ordinal, nextOrdinal,
                description, amount, category!!.ordinal, 1, 1_000, 1, 1,  null,null, null, null, null, null, null, null,
                FALSE_BYTE, FALSE_BYTE,utcDate.toLocalDateTime())
            transactionItemRepository.insertTransactionItemTrx(trxContext, item)
            transactionIdRet = transactionId
        }

        return transactionIdRet
    }

    /*fun getTotalAmountByDateOld(
        profileId: Long, debitOrCredit: Boolean, fromDate: LocalDate?, toDate: LocalDate? = null,
        accountIds: List<Long>, dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<DateAmountSummary> {
        val originalSummary =
            transactionRepository.getTransactionSumByDate(
                profileId, debitOrCredit, fromDate, toDate,
                accountIds, dates, categories, modes, sellers
            )

        var zeroCount = 0
        val start = if (fromDate == null) {
            LocalDate.now(timeHandler.getClock())
        } else {
            fromDate
        }
        val end = if (toDate == null) {
            LocalDate.now(timeHandler.getClock())
        } else {
            toDate
        }
        var currentDate = start

        val newList = arrayListOf<DateAmountSummary>()

        val listSize = originalSummary.size
        var existingDateIndex = if (listSize > 0) 0 else -1
        while (currentDate <= end) {
            if (existingDateIndex != -1) {
                if (existingDateIndex < listSize) {
                    val summary = originalSummary[existingDateIndex]
                    if (summary.aggregateDate == currentDate) {
                        newList.add(summary)
                        existingDateIndex += 1
                    } else {
                        newList.add(DateAmountSummary(currentDate, BigDecimal.ZERO))
                        zeroCount++
                    }
                } else {
                    newList.add(DateAmountSummary(currentDate, BigDecimal.ZERO))
                    zeroCount++
                }
            } else {
                newList.add(DateAmountSummary(currentDate, BigDecimal.ZERO))
                zeroCount++
            }
            currentDate = currentDate.plusDays(1)
        }
        if (zeroCount > 0) {
            LOGGER.info("Filled {} empty dates with zeroes", zeroCount)
        }
        LOGGER.info("Item Count: {}", newList.size)
        val filledSummary = newList.toList()

        return filledSummary
    }*/


    fun getByProfileIdAndId(profileId: Long, id: Long): TransactionPojo? {
        return transactionRepository.getByProfileIdAndId(profileId, id)
    }

    /*fun getByUserIdAndId(userId: Long, id: Long): TransactionPojo? {
        return transactionRepository.getByUserIdAndId(userId, id)
    }*/


    fun getTransactionBasics(
        profileId: Long,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>, baseUrl: Triple<String, String, Int>
    ): List<Transaction> {
        val start = System.nanoTime()
        val transactions = transactionRepository.getTransactionBasics(profileId, fromDate, toDate, accountIds)
        val end = System.nanoTime()
        val duration = Duration.ofNanos(end - start)
        LOGGER.info("Time taken: {}", duration)
        LOGGER.info("getTransactions: Fetched {} items", transactions.size)

        val transactionIds = transactions.map { it.id!! }
        val items = transactionItemRepository.getAllByTransactionIds(profileId, transactionIds)

        val imageIds = items.filter { it.firstImageId != null }.map { it.firstImageId!! }.distinct()
        val categoryIds = items.map { it.categoryId }.distinct()

        val images = imageRepository.getAllByIds(profileId, imageIds).associateBy { it.id }
        val categories = categoryRepository.getAllByIds(profileId, categoryIds).associateBy { it.id }
        val itemsGroupedByTransactionId = items.groupBy { it.transactionId }
        val accountIds = transactionRepository.getDistinctAccountIdsByIds(profileId, transactionIds)
        val accounts = accountRepository.getAllByIds(profileId, accountIds)
        val accountsGroupedByOrdinal = accounts.associateBy { it.ordinal }


        val mapped = transactions.map { t ->
            val items = itemsGroupedByTransactionId[t.id!!]!!.map { ti ->
                val category = categoryPojoToCategory(categories[ti.categoryId]!!)
                val firstImage = images[ti.firstImageId]
                val firstImageApi = if(firstImage != null) {
                    imagePojoToImage(firstImage, baseUrl)
                } else {
                    null
                }
                val weight: TransactionItemSummaryWeight? = if(ti.weight != null) {
                    val prefix: UnitPrefix? = if(ti.weightPrefix != null) {
                        UnitPrefix.fromValue(ti.weightPrefix)
                    } else {
                        null
                    }
                    TransactionItemSummaryWeight(ti.weight, WeightUnit.fromValue(ti.weightUnit), prefix)
                } else {
                    null
                }

                val volume: TransactionItemSummaryVolume? = if(ti.volume != null) {
                    val prefix: UnitPrefix? = if(ti.volumePrefix != null) {
                        UnitPrefix.fromValue(ti.volumePrefix)
                    } else {
                        null
                    }
                    TransactionItemSummaryVolume(ti.volume, VolumeUnit.fromValue(ti.volumeUnit), prefix)
                } else {
                    null
                }
                TransactionItemSummary(ti.id, ti.amount, ti.description, category, ti.quantity, ti.isReduction, ti.brand, ti.variation, weight, volume, firstImageApi)
            }
            val account = accountsGroupedByOrdinal[t.accountOrdinal!!]!!
            val apiAccount = accountPojoToAccount(account)

            Transaction(t.id, apiAccount, t.amount!!, t.debitOrCredit!!.toBoolean(), t.datedAt!!, t.time?.toString(), t.mode.toApiMode(), items)
        }


        return mapped
    }

    fun getItemsForTransaction(profileId: Long, transactionId: Long): List<TransactionItemPojo> {
        val list = transactionItemRepository.getAllByTransactionId(profileId, transactionId)
        LOGGER.info("getItemsForTransaction: Fetched {} items", list.size)
        return list
    }



    fun getEarliestDate(profileId: Long, accountIds: List<Long>): LocalDate? {
        return transactionRepository.getEarliestTransactionDate(profileId, accountIds)
    }

    /**
     * Main idea gotten from
     * https://udn.realityripple.com/docs/Mozilla/Tech/Places/Frecency_algorithm
     */
    fun getGenericSuggestions(profileId: Long, field: SuggestionsFieldType, prefixString: String): List<String> {
        if(prefixString.isBlank()) {
            LOGGER.warn("getGenericSuggestions: Blank prefix, returning empty list")
            return emptyList()
        }
        val start = System.currentTimeMillis()

        val now = LocalDateTime.now(timeHandler.getClock())
        val bucket1 = 0..4;     val bucket1Weight = 100
        val bucket2 = 5..14;    val bucket2Weight = 70
        val bucket3 = 15..31;   val bucket3Weight = 50
        val bucket4 = 32..90;   val bucket4Weight = 30
        val bucket5Weight = 10
        val stringFields = when (field) {
            SuggestionsFieldType.DESCRIPTION -> {
                transactionItemRepository.getDescriptionsAndDates(profileId, prefixString)
            }

            SuggestionsFieldType.BRAND -> {
                transactionItemRepository.getBrandsAndDates(profileId, prefixString)
            }

            SuggestionsFieldType.VARIATION -> {
                transactionItemRepository.getVariationsAndDates(profileId, prefixString)
            }
        }
        LOGGER.info("getGenericSuggestions: Loaded {} suggestions", stringFields.size)
        val groupedByDesc = stringFields.groupBy { it.stringField }
        val _groupedByDesc = stringFields.groupBy { it.stringField.lowercase() }
        LOGGER.info("getGenericSuggestions: Grouped suggestions into {} lists", groupedByDesc.size)
        LOGGER.info(
            "getGenericSuggestions: Suggestions are grouped into {} lists when ignoring case",
            _groupedByDesc.size
        )
        val scoredDescriptions = arrayListOf<Pair<Double, String>>()
        for ((stringField, list) in groupedByDesc) {
            val sampled = list.take(10)
            var sum = 0.0
            for (date in sampled) {
                val descriptionDate = date.createdAt
                val difference = Duration.between(descriptionDate, now).toDays()
                if (difference in bucket1) {
                    sum += bucket1Weight
                } else if (difference in bucket2) {
                    sum += bucket2Weight
                } else if (difference in bucket3) {
                    sum += bucket3Weight
                } else if (difference in bucket4) {
                    sum += bucket4Weight
                } else {
                    sum += bucket5Weight
                }
            }
            val score = list.size * sum / sampled.size
            scoredDescriptions.add(score to stringField)
        }
        val sorted = scoredDescriptions.sortedByDescending { it.first }
        LOGGER.info("Scores: {}", sorted.take(5))

        val end = System.currentTimeMillis()
        val duration = end - start
        LOGGER.info("getGenericSuggestions time: {} ms", duration)
        return sorted.map { it.second }.take(5)
    }

    /*fun getCumSumByDateOld(
        profileId: Long, debitOrCredit: Boolean, fromDate: LocalDate?, toDate: LocalDate? = null,
        accountIds: List<Long>, dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<DateAmountSummary> {
        val originalSummary =
            transactionRepository.getTransactionCumSumByDate(
                profileId, debitOrCredit, fromDate, toDate,
                accountIds, dates, categories, modes, sellers
            )

        var emptyCount = 0
        var lastAmount = BigDecimal.ZERO
        val start = if (fromDate == null) {
            LocalDate.now(timeHandler.getClock())
        } else {
            fromDate
        }
        val end = if (toDate == null) {
            LocalDate.now(timeHandler.getClock())
        } else {
            toDate
        }
        var currentDate = start

        val newList = arrayListOf<DateAmountSummary>()

        val listSize = originalSummary.size
        var existingDateIndex = if (listSize > 0) 0 else -1
        while (currentDate <= end) {
            if (existingDateIndex != -1) {
                if (existingDateIndex < listSize) {
                    val summary = originalSummary[existingDateIndex]
                    if (summary.aggregateDate == currentDate) {
                        newList.add(summary)
                        lastAmount = summary.sum
                        existingDateIndex += 1
                    } else {
                        newList.add(DateAmountSummary(currentDate, lastAmount))
                        emptyCount++
                    }
                } else {
                    newList.add(DateAmountSummary(currentDate, lastAmount))
                    emptyCount++
                }
            } else {
                newList.add(DateAmountSummary(currentDate, lastAmount))
                emptyCount++
            }
            currentDate = currentDate.plusDays(1)
        }
        if (emptyCount > 0) {
            LOGGER.info("Filled {} empty dates", emptyCount)
        }
        LOGGER.info("Item Count: {}", newList.size)
        val filledSummary = newList.toList()

        return filledSummary
    }*/




    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionService::class.java)
    }
}