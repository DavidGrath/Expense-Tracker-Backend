package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.configuration.TimeHandler
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerLocationPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.SellerPojo
import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import com.davidgrath.expensetrackerbackend.utils.toByte
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
    lateinit var timeHandler: TimeHandler

    fun getAll(profileId: Long): List<SellerPojo> {
        val sellers = sellerRepository.getAll(profileId)
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
        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val sellerPojo = SellerPojo(null, profileId, name, utcDate)
        val id = sellerRepository.insertSeller(sellerPojo)
        LOGGER.info("ID: {}", id)
        return id
    }


    fun createSellerLocation(sellerId: Long, name: String): Long {
        val date = ZonedDateTime.now(timeHandler.getClock())
        val utcDate = date.withZoneSameInstant(ZoneId.of("UTC")).toLocalDateTime()
        val sellerLocationPojo = SellerLocationPojo(null, sellerId, name, false.toByte(), null, null, null, utcDate)
        val id = sellerLocationRepository.insertSellerLocation(sellerLocationPojo)
        LOGGER.info("ID: {}", id)
        return id
    }

    fun updateSeller(profileId: Long, id: Long, name: String): Int {
        val count = sellerRepository.updateSeller(profileId, id, name)
        LOGGER.info("Update result: {}", count)
        return count
    }

    fun updateSellerLocation(profileId: Long, id: Long, location: String): Int {
        val count = sellerLocationRepository.updateSellerLocation(profileId, id, location)
        LOGGER.info("Update result: {}", count)
        return count
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellerService::class.java)
    }
}