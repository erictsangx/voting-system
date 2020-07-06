package com.hk.voting.utility

import com.hk.voting.test.Foo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

class JsonMapperTest {

    @Test
    fun `deserialize string to object`() {
        val str = """
            
{
"id":123,
"str":"abc  ",
"time":"2018-05-30T21:12:15.42919Z"
}

        """.trimIndent()

        val foo = Foo(
                id = 123,
                str = "abc",
                time = Instant.parse("2018-05-30T21:12:15.42919Z")
        )

        val singleFoo: Foo = str.toData()
        assertEquals(foo, singleFoo)

        val listFoo: List<Foo> = "[$str]".toData()
        assertEquals(listOf(foo), listFoo)
    }

}
