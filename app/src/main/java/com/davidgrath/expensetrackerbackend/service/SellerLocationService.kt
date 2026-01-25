package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.repository.SellerLocationRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SellerLocationService {
    @Autowired
    lateinit var sellerLocationRepository: SellerLocationRepository

    fun isSellerLocationInProfile(profileId: Long, sellerLocationId: Long): Boolean {
        val seller = sellerLocationRepository.getByProfileIdAndId(profileId, sellerLocationId)
        val value = seller != null
        LOGGER.info("Seller in profile: {}", value)
        return value
    }

    fun isSellerLocationForUser(userId: Long, sellerLocationId: Long): Boolean {
        val sellerLocation = sellerLocationRepository.getByUserIdAndId(userId, sellerLocationId)
        val value = sellerLocation != null
        LOGGER.info("Seller location for user: {}", value)
        return value
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellerLocationService::class.java)
    }
}