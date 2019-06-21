package org.dbpedia.extraction.live.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Date;

import org.apache.commons.lang3.time.DateFormatUtils;

/**
 * Created with IntelliJ IDEA.
 * User: Dimitris Kontokostas
 * Date: 8/16/12
 * Time: 11:18 AM
 * Util / Abstraction class for Date functions
 */
public class DateUtil {

    public static DateTimeFormatter ISO_INSTANT_NO_NANO = new DateTimeFormatterBuilder().parseCaseInsensitive().appendInstant(0).toFormatter();

    public static long getDuration1MonthMillis() {
        return 30 * getDuration1DayMillis();
    }

    public static long getDuration1DayMillis() {
        return 24 * getDuration1HourMillis();
    }

    public static long getDuration1HourMillis() {
        return 60 * getDuration1MinMillis();
    }

    public static long getDuration1MinMillis() {
        return 60 * 1000;
    }

    public static String formatMillisWithPattern(long millis, String pattern) {
        return DateFormatUtils.format(millis, pattern);
    }

    // imported from UTCHelper
    public static String transformToUTC(Date date) {
        return DateFormatUtils.formatUTC(
                date, DateFormatUtils.ISO_DATETIME_FORMAT.getPattern()) + "Z";
    }

    public static String transformToUTC(long millis) {
        return DateFormatUtils.formatUTC(
                millis, DateFormatUtils.ISO_DATETIME_FORMAT.getPattern()) + "Z";
    }

    public static String transformUnixTimestampToUTC(long seconds) {
        return ISO_INSTANT_NO_NANO.format(ZonedDateTime.ofInstant(Instant.ofEpochSecond(seconds), ZoneId.systemDefault()));
    }


    public static long transformUTCtoLong(String UTC) {
        return ZonedDateTime.parse(UTC).toInstant().toEpochMilli();
    }
}
