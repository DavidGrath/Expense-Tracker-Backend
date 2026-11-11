package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.repository.GenericRepository
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.service.exceptions.CountLimitExceededException
import com.davidgrath.expensetrackerbackend.utils.Constants
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.ZoneId
import java.time.ZonedDateTime

@Service
class SellerService {

    @Autowired
    lateinit var sellerRepository: SellerRepository
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository
    @Autowired
    lateinit var profileRepository: ProfileRepository
    @Autowired
    lateinit var genericRepository: GenericRepository
    @Autowired
    lateinit var timeHandler: TimeHandler

    fun getAll(profileId: Long): List<SellerPojo> {
        val sellers = sellerRepository.getAllByProfileId(profileId)
        LOGGER.info("Sellers size: {}", sellers.size)
        return sellers
    }


    fun countAllSellers(profileId: Long): Int {
        val count = sellerRepository.countAll(profileId)
        LOGGER.info("Sellers size: {}", count)
        return count
    }


    fun countAllLocations(profileId: Long, sellerId: Long): Int {
        val count = sellerLocationRepository.countByProfileIdAndSellerId(profileId, sellerId)
        LOGGER.info("Locations size: {}", count)
        return count
    }

    fun getAllSellerLocations(profileId: Long, sellerId: Long): List<SellerLocationPojo> {
        val locations = sellerLocationRepository.getByProfileIdAndSellerId(profileId, sellerId)
        LOGGER.info("Seller locations size: {}", locations.size)
        return locations
    }

    fun isSellerInProfile(profileId: Long, sellerId: Long): Boolean {
        val seller = sellerRepository.getByProfileIdAndId(profileId, sellerId)
        val value = seller != null
        LOGGER.info("Seller in profile: {}", value)
        return value
    }

    fun isSellerForUser(userId: Long, sellerId: Long): Boolean {
        val seller = sellerRepository.getByUserIdAndId(userId, sellerId)
        val value = seller != null
        LOGGER.info("Seller for user: {}", value)
        return value
    }

    fun isLocationInProfile(profileId: Long, locationId: Long): Boolean {
        val sellerLocation = sellerLocationRepository.getByProfileIdAndId(profileId, locationId)
        val value = sellerLocation != null
        LOGGER.info("Seller location in profile: {}", value)
        return value
    }

    fun isLocationForUser(userId: Long, locationId: Long): Boolean {
        val seller = sellerLocationRepository.getByUserIdAndId(userId, locationId)
        val value = seller != null
        LOGGER.info("Seller location for user: {}", value)
        return value
    }

    fun getByProfileIdAndId(profileId: Long, sellerId: Long): SellerPojo? {
        return sellerRepository.getByProfileIdAndId(profileId, sellerId)
    }


    fun getSellerLocation(profileId: Long, locationId: Long): SellerLocationPojo? {
        return sellerLocationRepository.getByProfileIdAndId(profileId, locationId)
    }

    fun createSeller(profileId: Long, name: String): Long {
        var retId = -1L
        genericRepository.transactionContext { trx ->
            val trxContext = trx.dsl()
            val profile = profileRepository.getByIdForUpdateTrx(trxContext, profileId)
            LOGGER.info("Acquired lock for profile {}", profile.id)

            val sellerCount = sellerRepository.countAllTrx(trxContext, profile.id!!)
            if(sellerCount >= Constants.Constraints.MAX_SELLERS_PER_PROFILE) {
                val exception = CountLimitExceededException(Constants.Constraints.MAX_SELLERS_PER_PROFILE)
                LOGGER.error("Max sellers reached", exception)
                throw exception
            }
            val ordinalSortKeyOccupancySlot = sellerRepository.getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext, profileId)
            val nextOrdinal = ((ordinalSortKeyOccupancySlot.ordinal?: 0) + 1).toShort()
            val nextSortKey = ((ordinalSortKeyOccupancySlot.sortKey?: 0) + 1_000).toShort()
            val nextOccupancySlot = ((ordinalSortKeyOccupancySlot.occupancySlot?: 0) + 1).toShort()
            //TODO This is an oversimplified way of getting the max values, handle the case of slots or sort keys being taken
            val date = ZonedDateTime.now(timeHandler.getClock())
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
            val sellerPojo = SellerPojo(null, profileId, name, nextOrdinal, nextSortKey, nextOccupancySlot, utcDate)
            val id = sellerRepository.insertSellerTrx(trxContext, sellerPojo)
            LOGGER.info("ID: {}", id)
        }
        return retId
    }


    fun createSellerLocation(profileId: Long, sellerId: Long, name: String): Long {
        var retId = -1L
        genericRepository.transactionContext { trx ->
            val trxContext = trx.dsl()
            val seller = sellerRepository.getByIdForUpdateTrx(trxContext, profileId, sellerId)
            LOGGER.info("Acquired lock for seller {}", seller.id)

            val sellerCount = sellerLocationRepository.countByProfileIdAndSellerIdTrx(trxContext, profileId, sellerId)
            if(sellerCount >= Constants.Constraints.MAX_LOCATIONS_PER_SELLER) {
                val exception = CountLimitExceededException(Constants.Constraints.MAX_LOCATIONS_PER_SELLER)
                LOGGER.error("Max sellers reached", exception)
                throw exception
            }
            val date = ZonedDateTime.now(timeHandler.getClock())
            val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()


            val ordinalSortKeyOccupancySlot = sellerLocationRepository.getMaxOrdinalSortKeyAndOccupancySlotTrx(trxContext, profileId, sellerId)
            val nextOrdinal = ((ordinalSortKeyOccupancySlot.ordinal?: 0) + 1).toShort()
            val nextSortKey = ((ordinalSortKeyOccupancySlot.sortKey?: 0) + 1_000).toShort() //TODO I should eventually rework all of my fetch queries to actually sort by sort key. Was sort key actually necessary for every table? Not really, but I decided I might as well go all out since I've started this massive rework
            val nextOccupancySlot = ((ordinalSortKeyOccupancySlot.occupancySlot?: 0) + 1).toShort()
            //TODO This is an oversimplified way of getting the max values, handle the case of slots or sort keys being taken

            val sellerLocationPojo = SellerLocationPojo(null, sellerId, seller.ordinal!!, name, null, null, null, nextOrdinal, nextSortKey, nextOccupancySlot, utcDate)
            val id = sellerLocationRepository.insertSellerLocation(sellerLocationPojo)
            LOGGER.info("ID: {}", id)
            retId = id
        }
        return retId
    }

    fun updateSeller(profileId: Long, id: Long, name: String): Int {
        val count = sellerRepository.updateSeller(profileId, id, name)
        return count
    }

    fun updateSellerLocation(profileId: Long, id: Long, name: String): Int {
        val count = sellerLocationRepository.updateSellerLocation(profileId, id, name)
        return count
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellerService::class.java)
    }
}