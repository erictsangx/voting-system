package com.hk.voting.utility

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.core.Version
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.jacksonTypeRef


/**
 * Trim strings for deserialization
 */
class TrimStringModule : SimpleModule("TrimStringModule", Version(1, 0, 0, null, "TrimStringModule", "TrimStringModule")) {

    private class StringDeserializer : JsonDeserializer<String?>() {
        override fun deserialize(jp: JsonParser, ctxt: DeserializationContext): String? {
            return jp.valueAsString?.trim()
        }
    }

    init {
        addDeserializer(String::class.java, StringDeserializer())
    }
}

@Suppress("ObjectPropertyName")
val _mapper: ObjectMapper = ObjectMapper()
//        .findAndRegisterModules()
        .registerModule(KotlinModule())
        .registerModule(JavaTimeModule())
        .registerModule(TrimStringModule())
        .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)

fun <T> T.toJsonString(): String = _mapper.writeValueAsString(this)

inline fun <reified T> String.toData(): T = _mapper.readValue(this, jacksonTypeRef<T>())
