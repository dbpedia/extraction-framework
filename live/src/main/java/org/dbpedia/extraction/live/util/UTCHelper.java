package org.dbpedia.extraction.live.util;

import java.util.Date;

import org.apache.commons.lang.time.DateFormatUtils;

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
