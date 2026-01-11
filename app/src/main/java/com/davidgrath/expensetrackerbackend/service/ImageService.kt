package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ImagePojo
import com.davidgrath.expensetrackerbackend.repository.ImageRepository
import com.davidgrath.expensetrackerbackend.repository.ItemImagesRepository
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ImageService {

    @Autowired
    lateinit var itemImagesRepository: ItemImagesRepository

    fun getImages(profileId: Long, itemId: Long): List<ImagePojo> {
        val images = itemImagesRepository.findByItemId(profileId, itemId)
        LOGGER.info("Images size: {}", images.size)
        return images
    }

    companion object {
        private val LOGGER = LoggerFactory.getLogger(ImageService::class.java)
    }
}