package com.gayadi.android.data

import com.gayadi.android.data.remote.travel.jsonArrayFromBody
import org.junit.Assert.assertEquals
import org.junit.Test

class TravelJsonListParseTest {
    @Test
    fun readsBareArray() {
        val array = jsonArrayFromBody("""[{"id":1},{"id":2}]""")
        assertEquals(2, array.length())
    }

    @Test
    fun readsItemsEnvelope() {
        val array = jsonArrayFromBody("""{"items":[{"id":31}],"hasNext":false}""")
        assertEquals(1, array.length())
        assertEquals(31, array.getJSONObject(0).getInt("id"))
    }
}
