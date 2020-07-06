package com.hk.voting.services

import com.hk.voting.models.Vote
import com.hk.voting.models.VoteCount
import com.hk.voting.utility.CacheKey
import com.hk.voting.utility.toData
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Service


/**
 * A simplified MQ consumer for demonstration purposes
 * Usually a MQ consumer will be separated for scaling,decoupling,etc.
 */
@Service
class MqConsumer @Autowired constructor(
        private val campaignRepo: CampaignRepo,
        private val redis: RedisService
) {
    private val logger = LoggerFactory.getLogger(MqConsumer::class.java)

    private val timeout = 10L

    /**
     * Consume using redis [brpop]
     */
    @Scheduled(fixedDelay = 1000)
    fun consume() {
        var received = redis.consume(timeout)
        while (received != null) {
            val vote = received.toData<Vote>()
            logger.info("Consuming message: $vote")

            val inserted = campaignRepo.vote(vote)

            if (inserted) {
                logger.info("Saved to Mongo")
                logger.info("Inc Redis")
                val incremented = redis.inc(CacheKey.CANDIDATE_COUNT, vote.candidateId, 1)
                campaignRepo.upsertVoteCount(VoteCount(vote.candidateId, incremented))
                val key = CacheKey.LIST_VOTE + vote.hkId
                logger.info("Evict cache: $key")
                redis.del(key)
            }
            received = redis.consume(timeout)
        }
    }
}
