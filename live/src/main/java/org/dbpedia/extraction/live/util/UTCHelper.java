package org.dbpedia.extraction.live.util;

import org.apache.commons.lang3.time.DateFormatUtils;

import java.util.Date;

public class UTCHelper
{
    public static String transformToUTC(Date date)
    {
        return DateFormatUtils.formatUTC(
                date, DateFormatUtils.ISO_DATETIME_FORMAT.getPattern())+"Z";
    }

    public static String transformToUTC(long l)
    {
        Date now = new Date(l);
        return DateFormatUtils.formatUTC(
                now, DateFormatUtils.ISO_DATETIME_FORMAT.getPattern())+"Z";
    }
}
