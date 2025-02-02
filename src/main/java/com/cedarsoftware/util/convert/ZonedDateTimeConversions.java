package com.cedarsoftware.util.convert;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import static com.cedarsoftware.util.convert.MapConversions.ZONED_DATE_TIME;

/**
 * @author Kenny Partlow (kpartlow@gmail.com)
 *         <br>
 *         Copyright (c) Cedar Software LLC
 *         <br><br>
 *         Licensed under the Apache License, Version 2.0 (the "License");
 *         you may not use this file except in compliance with the License.
 *         You may obtain a copy of the License at
 *         <br><br>
 *         <a href="http://www.apache.org/licenses/LICENSE-2.0">License</a>
 *         <br><br>
 *         Unless required by applicable law or agreed to in writing, software
 *         distributed under the License is distributed on an "AS IS" BASIS,
 *         WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *         See the License for the specific language governing permissions and
 *         limitations under the License.
 */
final class ZonedDateTimeConversions {

    private ZonedDateTimeConversions() {}

    static long toLong(Object from, Converter converter) {
        return ((ZonedDateTime) from).toInstant().toEpochMilli();   // speed over shorter code.
    }

    static double toDouble(Object from, Converter converter) {
        ZonedDateTime zdt = (ZonedDateTime) from;
        return InstantConversions.toDouble(zdt.toInstant(), converter);
    }

    static Instant toInstant(Object from, Converter converter) {
        return ((ZonedDateTime) from).toInstant();
    }

    private static ZonedDateTime toDifferentZone(Object from, Converter converter) {
        return ((ZonedDateTime)from).withZoneSameInstant(converter.getOptions().getZoneId());
    }
    
    static LocalDateTime toLocalDateTime(Object from, Converter converter) {
        ZonedDateTime zdt = (ZonedDateTime) from;
        ZonedDateTime adjustedZonedDateTime = zdt.withZoneSameInstant(converter.getOptions().getZoneId());
        return adjustedZonedDateTime.toLocalDateTime();
    }

    static LocalDate toLocalDate(Object from, Converter converter) {
        return toDifferentZone(from, converter).toLocalDate();  // shorter code over speed
    }

    static LocalTime toLocalTime(Object from, Converter converter) {
        return toDifferentZone(from, converter).toLocalTime();  // shorter code over speed
    }

    static OffsetDateTime toOffsetDateTime(Object from, Converter converter) {
        ZonedDateTime zdt = (ZonedDateTime) from;
        return zdt.toOffsetDateTime();
    }

    static AtomicLong toAtomicLong(Object from, Converter converter) {
       return new AtomicLong(toLong(from, converter));
    }

    static Timestamp toTimestamp(Object from, Converter converter) {
        ZonedDateTime zdt = (ZonedDateTime) from;
        return Timestamp.from(zdt.toInstant());
    }

    static Calendar toCalendar(Object from, Converter converter) {
        ZonedDateTime zdt = (ZonedDateTime) from;
        Calendar cal = Calendar.getInstance(converter.getOptions().getTimeZone());
        cal.setTimeInMillis(zdt.toInstant().toEpochMilli());
        return cal;
    }

    static java.sql.Date toSqlDate(Object from, Converter converter) {
        return new java.sql.Date(toLong(from, converter));
    }

    static Date toDate(Object from, Converter converter) {
        return new Date(toLong(from, converter));
    }

    static BigInteger toBigInteger(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return InstantConversions.toBigInteger(instant, converter);
    }

    static BigDecimal toBigDecimal(Object from, Converter converter) {
        Instant instant = toInstant(from, converter);
        return InstantConversions.toBigDecimal(instant, converter);
    }

    static String toString(Object from, Converter converter) {
        ZonedDateTime zonedDateTime = (ZonedDateTime) from;
        return zonedDateTime.format(DateTimeFormatter.ISO_DATE_TIME);
    }

    static Map<String, Object> toMap(Object from, Converter converter) {
        String zdtStr = toString(from, converter);
        Map<String, Object> target = new LinkedHashMap<>();
        target.put(ZONED_DATE_TIME, zdtStr);
        return target;
    }
}
