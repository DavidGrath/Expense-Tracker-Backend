package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.views.DateAmountSummary
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionItemPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.TransactionPojo
import com.davidgrath.expensetrackerbackend.repository.AccountRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionItemRepository
import com.davidgrath.expensetrackerbackend.repository.TransactionRepository
import com.davidgrath.expensetrackerbackend.utils.toByte
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

@Service
class TransactionService {

    @Autowired
    lateinit var transactionRepository: TransactionRepository
    @Autowired
    lateinit var accountRepository: AccountRepository
    @Autowired
    lateinit var transactionItemRepository: TransactionItemRepository
    @Autowired
    lateinit var timeHandler: TimeHandler
    
    fun addTransaction(accountId: Long, debitOrCredit: Boolean, amount: BigDecimal, description: String, categoryId: Long, zoneId: ZoneId): Long {
        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC"))
        val userTime = date.withZoneSameInstant(zoneId).toLocalTime()

        val account = accountRepository.findById(accountId)!!
        val maxOrdinal = transactionRepository.getMaxOrdinalInDayForAccount(accountId, utcDate.toLocalDate())?: 0
        val ordinal = maxOrdinal + 1
        val transaction = TransactionPojo(
            null,
            accountId,
            amount,
            account.currencyCode,
            null,
            debitOrCredit.toByte(),
            utcDate.toLocalDate(),
            ordinal,
            TransactionMode.Other,
            null,
            null,
            null,
            utcDate.toLocalDateTime(), userTime
        )
        val transactionId = transactionRepository.insertTransaction(transaction)

        val item = TransactionItemPojo(null, transactionId, description, amount, categoryId, 1, 1, null, null,null, false.toByte(), utcDate.toLocalDateTime())
        transactionItemRepository.insertTransactionItem(item)
        return transactionId
    }

    fun getTotalAmountByDate(
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
            LOGGER.info("getTotalAmountByDate: Filled {} empty dates with zeroes", zeroCount)
        }
        LOGGER.info("getTotalAmountByDate: Item Count: {}", newList.size)
        val filledSummary = newList.toList()

        return filledSummary
    }

//    fun getByProfileIdAndId(): TransactionPojo? {
//
//    }

    fun getByUserIdAndId(userId: Long, id: Long): TransactionPojo? {
        return transactionRepository.getByUserIdAndId(userId, id)
    }

    fun getTransactions(
        profileId: Long,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>
    ): List<TransactionPojo> {
        val list = transactionRepository.getTransactions(profileId, fromDate, toDate, accountIds, dates, categories, modes, sellers)
        LOGGER.info("getTransactions: Fetched {} items", list.size)
        return list
    }

    fun getItemsForTransaction(transactionId: Long): List<TransactionItemPojo> {
        val list = transactionItemRepository.getAllById(transactionId)
        LOGGER.info("getItemsForTransaction: Fetched {} items", list.size)
        return list
    }

    fun getTransactionDebitSum(profileId: Long, fromDate: LocalDate?, toDate: LocalDate?,
                               accountIds: List<Long>,
                               dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>): BigDecimal {
        return transactionRepository.getTransactionSum(profileId, true, fromDate, toDate, accountIds, dates, categories, modes, sellers)
    }

    fun getTransactionCreditSum(profileId: Long, fromDate: LocalDate?, toDate: LocalDate?,
                                accountIds: List<Long>,
                                dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>): BigDecimal {
        return transactionRepository.getTransactionSum(profileId, false, fromDate, toDate, accountIds, dates, categories, modes, sellers)
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(TransactionService::class.java)
    }
}