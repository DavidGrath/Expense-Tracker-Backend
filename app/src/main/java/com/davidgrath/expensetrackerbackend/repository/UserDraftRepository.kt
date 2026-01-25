package com.davidgrath.expensetrackerbackend.repository

import com.davidgrath.expensetrackerbackend.entities.UserDraftType
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.pojos.UserDraftPojo
import com.davidgrath.expensetrackerbackend.generated.jooq.tables.references.USER_DRAFT
import org.jooq.DSLContext
import org.jooq.JSON
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Repository

@Repository
class UserDraftRepository @Autowired constructor(open var dslContext: DSLContext) {

    // region Create

    fun insertDraft(userDraftPojo: UserDraftPojo): Long {
        val userDraft = dslContext.newRecord(USER_DRAFT, userDraftPojo)
        userDraft.store()
        return userDraft.id!!
    }

    // endregion

    // region Read

    fun getDraftByProfile(profileId: Long): UserDraftPojo? {
        val selectStep = dslContext.selectFrom(USER_DRAFT)
            .where(USER_DRAFT.DRAFT_TYPE.eq(UserDraftType.Create))
            .and(USER_DRAFT.PROFILE_ID.eq(profileId))
        return selectStep.fetchOne()?.into(UserDraftPojo::class.java)
    }

    // endregion

    // region Update

    fun updateDraft(profileId: Long, id: Long, json: JSON): Int {
        val updateStep = dslContext.update(USER_DRAFT)
            .set(USER_DRAFT.DRAFT, json)
            .where(USER_DRAFT.ID.eq(id))
            .and(USER_DRAFT.PROFILE_ID.eq(profileId))

        val updatedCount = updateStep.execute()
        LOGGER.info("Updated rows: {}", updatedCount)
        if(updatedCount != 1) {
            LOGGER.warn("Not exactly 1 row updated")
        }
        return updatedCount
    }
    // endregion

    //region Delete
    fun deleteDraft(profileId: Long, id: Long): Int {
        val deleteStep = dslContext.deleteFrom(USER_DRAFT)
            .where(USER_DRAFT.ID.eq(id))
            .and(USER_DRAFT.PROFILE_ID.eq(profileId))
        val deletedCount = deleteStep.execute()
        LOGGER.info("Deleted rows: {}", deletedCount)
        if(deletedCount != 1) {
            LOGGER.warn("Not exactly 1 row deleted")
        }
        return deletedCount
    }


    companion object {
        private val LOGGER = LoggerFactory.getLogger(UserDraftRepository::class.java)
    }

}