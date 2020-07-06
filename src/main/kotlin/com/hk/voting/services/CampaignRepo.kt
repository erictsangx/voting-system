package com.hk.voting.services

import com.hk.voting.models.Campaign
import com.hk.voting.models.Vote
import com.hk.voting.models.VoteCount
import com.hk.voting.utility.*
import com.mongodb.client.model.Filters
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.dao.DuplicateKeyException
import org.springframework.data.mongodb.core.FindAndReplaceOptions
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Service
import java.time.Instant


@Service
class CampaignRepo @Autowired constructor(
        private val mongo: MongoTemplate
) {

    private val logger = LoggerFactory.getLogger(CampaignRepo::class.java)


    fun create(campaign: Campaign): Campaign {
        val result = mongo.insert(campaign)
        val zeroCount = result.candidates.map {
            VoteCount(it.id)
        }
        mongo.insertAll(zeroCount)
        return result
    }


    fun listAvailable(): List<Campaign> {
        val now = Instant.now()
        val query = Query().addCriteria((Campaign::startTime lte now) and (Campaign::endTime gte now))
        return mongo.find(query, Campaign::class.java).toList()
    }

    fun listExpired(): List<Campaign> {
        val now = Instant.now()
        val query = Query().addCriteria((Campaign::endTime lte now)).with(Campaign::endTime.desc())
        return mongo.find(query, Campaign::class.java).toList()
    }


    fun singleOrNull(campaignId: String): Campaign? {
        val query = Query().addCriteria(Campaign::id eq campaignId)
        return mongo.findOne(query, Campaign::class.java)
    }

    fun getVoteCount(candidateIds: List<String>): List<VoteCount> {
        return mongo.find(Query().addCriteria(VoteCount::candidateId inside candidateIds), VoteCount::class.java)
    }

    fun countVote(candidateId: String): Long {
        return mongo.getCollection("vote").countDocuments(Filters.eq(Vote::candidateId.name, candidateId))
    }

    fun listVote(hkId: String): List<Vote> {
        return mongo.find(Query.query(Vote::hkId eq hkId), Vote::class.java)
    }

    /**
     * Update VoteCount
     * Insert if not exist [VoteCount.candidateId]
     */
    fun upsertVoteCount(voteCount: VoteCount) {
        mongo.findAndReplace(
                Query.query(VoteCount::candidateId eq voteCount.candidateId),
                voteCount, FindAndReplaceOptions.options().upsert()
        )
    }

    /**
     * Mongo UNIQUE COMPOUND INDEX(campaignId, hkId)
     * Ignore duplicate votes
     */
    fun vote(vote: Vote): Boolean {
        try {
            mongo.insert(vote)
        } catch (ex: DuplicateKeyException) {
            logger.warn(ex.localizedMessage)
            return false
        } catch (e: Exception) {
            return false
        }
        return true
    }


}
