package com.davidgrath.expensetrackerbackend.service

import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.ProfilePojo
import com.davidgrath.expensetrackerbackend.repository.ProfileRepository
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
class ProfileService {

    @Autowired
    lateinit var profileRepository: ProfileRepository

    fun getAllByUserId(userId: Long): List<ProfilePojo> {
        return profileRepository.findAllByUserId(userId)
    }

    fun getByUserIdAndStringId(userId: Long, stringId: String): ProfilePojo? {
        return profileRepository.findByStringId(userId, stringId)
    }
}