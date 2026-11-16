package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.entities.TransactionMode
import com.davidgrath.expensetrackerbackend.entities.dtos.ImageDocumentStats
import com.davidgrath.expensetrackerbackend.entities.dtos.ImageWithItemId
import com.davidgrath.expensetrackerbackend.entities.dtos.ImageWithStats
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.repository.ImageRepository
import com.davidgrath.expensetrackerbackend.repository.ItemImagesRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.LocalDate

@Service
class ImageService {

    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository
    @Autowired
    lateinit var imageRepository: ImageRepository

    fun getImagesForItem(profileId: Long, itemId: Long): List<ImagePojo> {
        val images = itemImagesRepository.findByItemId(profileId, itemId)
        LOGGER.info("Images size: {}", images.size)
        return images
    }

    fun getImagesFiltered(
        profileId: Long,
        fromDate: LocalDate? = null, toDate: LocalDate? = null,
        accountIds: List<Long>,
        dates: List<LocalDate>, categories: List<Long>, modes: List<TransactionMode>, sellers: List<Long>): List<ImageWithItemId> {
        val images = itemImagesRepository.findAllFiltered(profileId, fromDate, toDate, accountIds, dates, categories, modes, sellers)
        LOGGER.info("Images size: {}", images.size)
        return images
    }

    fun getAllImages(profileId: Long): List<ImageWithStats> {
        val images = imageRepository.findAll(profileId)
        LOGGER.info("Images size: {}", images.size)
        return images
    }

    //TODO Generic ownership check framework
    fun isImageForProfile(profileId: Long, imageId: Long): Boolean {
        val image = imageRepository.findByProfileIdAndId(profileId, imageId)
        val value = image != null
        LOGGER.info("Image for profile: {}", value)
        return value
    }

    fun isImageForUser(userId: Long, imageId: Long): Boolean {
        val image = imageRepository.findByUserIdAndId(userId, imageId)
        val value = image != null
        LOGGER.info("Image for user: {}", value)
        return value
    }

    fun getByProfileIdAndId(profileId: Long, imageId: Long): ImageWithStats? {
        return imageRepository.findByProfileIdAndId(profileId, imageId)
    }

    fun getImageStats(profileId: Long): ImageDocumentStats {
        return imageRepository.getOverallStats(profileId)
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ImageService::class.java)
    }
}