package com.hk.voting.services

import com.hk.voting.models.AuthUser
import com.hk.voting.utility.eq
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.data.mongodb.core.MongoTemplate
import org.springframework.data.mongodb.core.query.Query
import org.springframework.stereotype.Repository


@Repository
class AuthUserRepo(@Autowired private val mongo: MongoTemplate) {

    fun singleOrNull(username: String): AuthUser? {
        return mongo.findOne(Query.query(AuthUser::username eq username), AuthUser::class.java)
    }
}

