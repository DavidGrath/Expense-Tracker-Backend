package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.repository.SellerRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class SellerService {
    @Autowired
    lateinit var sellerRepository: SellerRepository

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

    companion object {
        private val LOGGER = LoggerFactory.getLogger(SellerService::class.java)
    }
}