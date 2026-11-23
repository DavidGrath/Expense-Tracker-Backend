package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import com.davidgrath.expensetrackerbackend.utils.uuidToBytes
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.util.*

@Service
class ProfileService {

    @Autowired
    lateinit var profileRepository: ProfileRepository

    fun getAllByUserId(userId: Long): List<ProfilePojo> {
        return profileRepository.findAllByUserId(userId)
    }

    fun getByUserIdAndUuid(userId: Long, uuidString: String): ProfilePojo? {
        val uuid = UUID.fromString(uuidString)
        val uuidBytes = uuidToBytes(uuid)
        return profileRepository.findByUuid(userId, uuidBytes)
    }
}