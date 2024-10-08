package com.usktea.mongo_test.config.reading

import com.usktea.mongo_test.config.CustomMongoConverter
import org.springframework.data.convert.ReadingConverter
import java.time.ZoneId
import java.time.ZonedDateTime
import java.util.Date

@ReadingConverter
object DateToZonedDateTimeConverter : CustomMongoConverter<Date, ZonedDateTime> {

    private val ZONE_ID_ASIA_SEOUL = ZoneId.of("Asia/Seoul")

    override fun convert(source: Date): ZonedDateTime {
        return source.toInstant().atZone(ZONE_ID_ASIA_SEOUL)
    }
}
