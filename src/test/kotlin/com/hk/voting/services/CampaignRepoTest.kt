package com.hk.voting.services

import com.hk.voting.models.Campaign
import com.hk.voting.models.Candidate
import com.hk.voting.models.Vote
import com.hk.voting.models.VoteCount
import com.hk.voting.test.*
import com.mongodb.client.model.IndexOptions
import com.mongodb.client.model.Indexes
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest
import org.springframework.context.annotation.Import
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.test.context.TestPropertySource

/**
 * Test db query with docker containers mongo:4.2.0-bionic
 */
@DataMongoTest
@Import(CampaignRepo::class)
@TestPropertySource(locations = ["classpath:application.test.properties"])
class CampaignRepoTest {


    @Autowired
    lateinit var repo: CampaignRepo

    @Autowired
    lateinit var mongo: MongoTemplate


    val campaignA = Campaign(
            randomStr,
            randomPast,
            randomFuture,
            (1..5).map {
                Candidate(randomStr, randomId)
            },
            randomId
    )
    val campaignB = Campaign(
            randomStr,
            randomPast,
            randomFuture,
            (1..5).map {
                Candidate(randomStr, randomId)
            },
            randomId
    )

    val expiredA = Campaign(
            randomStr,
            randomPast,
            randomPast,
            (1..5).map {
                Candidate(randomStr, randomId)
            },
            randomId
    )
    val expiredB = Campaign(
            randomStr,
            randomPast,
            randomPast,
            (1..5).map {
                Candidate(randomStr, randomId)
            },
            randomId
    )


    val voteCountList = (expiredA.candidates + expiredB.candidates).map {
        VoteCount(it.id, randomLong, randomInstant)
    }

    val hkA = randomStr
    val hkB = randomStr

    val voteList = listOf(
            Vote(expiredA.candidates[0].id, expiredA.id!!, hkA, randomInstant),
            Vote(expiredA.candidates[1].id, expiredA.id!!, hkB, randomInstant),

            Vote(expiredB.candidates[1].id, expiredB.id!!, hkA, randomInstant),
            Vote(expiredB.candidates[1].id, expiredB.id!!, hkB, randomInstant)
    ) + (1..10).map { Vote(expiredA.candidates[0].id, randomId, randomStr, randomInstant) }


    @BeforeAll
    fun setup() {
        mongo.getCollection("vote").createIndex(
                Indexes.compoundIndex(
                        Indexes.ascending("campaignId"),
                        Indexes.ascending("hkId")
                ),
                IndexOptions().unique(true)
        )
        mongo.insertAll(listOf(campaignA, campaignB, expiredA, expiredB))
        mongo.insertAll(voteCountList)
        mongo.insertAll(voteList)
    }

    @Test
    fun `create a campaign`() {
        val expected = Campaign(
                randomStr,
                randomInstant,
                randomInstant,
                listOf(Candidate(randomStr, randomId), Candidate(randomStr, randomId)),
                null)
        val result = mongo.insert(expected)
        assertNotNull(result.id)
        assertEquals(expected, result.copy(id = null))
    }

    @Test
    fun `list available`() {
        val campaigns = repo.listAvailable()
        assertEquals(listOf(campaignA, campaignB), campaigns)
    }

    @Test
    fun `list expired, order by endDate DESC`() {
        val campaigns = repo.listExpired()
        assertEquals(listOf(expiredB, expiredA).sortedByDescending { it.endTime }, campaigns)
    }

    @Test
    fun singleOrNull() {
        assertEquals(campaignA, repo.singleOrNull(campaignA.id!!))
        assertEquals(null, repo.singleOrNull(randomId))
    }

    @Test
    fun `vote counting of candidates`() {
        val expected = voteCountList.filter { it.candidateId == expiredA.candidates[0].id || it.candidateId == expiredB.candidates[0].id }
        val result = repo.getVoteCount(expected.map { it.candidateId })
        assertEquals(expected, result)
    }

    @Test
    fun `count all votes of a candidate`() {
        val expected = voteList.filter { it.candidateId == expiredA.candidates[0].id }.size.toLong()
        val result = repo.countVote(expiredA.candidates[0].id)
        assertEquals(expected, result)

        val result2 = repo.countVote(expiredB.candidates[1].id)
        assertEquals(2L, result2)

        val result3 = repo.countVote(randomId)
        assertEquals(0L, result3)
    }

    @Test
    fun `list vote of a HK ID`() {
        val expected = voteList.filter { it.hkId == hkA }
        val result = repo.listVote(hkA)
        assertEquals(expected, result)

        val vote = voteList.random()
        val expected2 = voteList.filter { it.hkId == vote.hkId }
        val result2 = repo.listVote(vote.hkId)
        assertEquals(expected2, result2)

        val result3 = repo.listVote(randomStr)
        assertTrue(result3.isEmpty())
    }

    @Test
    fun `upsert vote count`() {

        val voteCount = voteCountList.random()
        val updated = voteCount.copy(count = randomLong)
        repo.upsertVoteCount(updated)
        val result = repo.getVoteCount(listOf(voteCount.candidateId))
        assertTrue(result.size == 1)
        assertEquals(updated, result.first())


        val inserted = VoteCount(randomId, randomLong, randomInstant)
        repo.upsertVoteCount(inserted)
        val result2 = repo.getVoteCount(listOf(inserted.candidateId))
        assertTrue(result2.size == 1)
        assertEquals(inserted, result2.first())
    }


    @Test
    fun `insert a vote`() {
        val hkId = randomStr
        val expected = listOf(Vote(randomId, randomId, hkId, randomInstant), Vote(randomId, randomId, hkId, randomInstant))
        expected.forEach {
            repo.vote(it)
        }
        val result = repo.listVote(hkId)
        assertEquals(expected, result)

        //check duplicate(campaignId,hkId)
        val copy = expected.first().copy(candidateId = randomStr)
        repo.vote(copy)
        val result2 = repo.listVote(hkId)
        assertTrue(result2.size == 2)

    }

}
