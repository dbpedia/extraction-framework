package org.dbpedia.extraction.live.util;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.apache.log4j.Logger;

public class MD5Util
{
	private static Logger logger = Logger.getLogger(MD5Util.class);
	
	public static String generateMD5(String str)
	{
		String result = null;
		try {
			result = _generateMD5(str);
		} catch(Exception e) {
			logger.error(ExceptionUtil.toString(e));
		}
		
		return result;
	}
	
	private static String _generateMD5(String str)
		throws NoSuchAlgorithmException
	{
		MessageDigest md5 = MessageDigest.getInstance("MD5");
		
		md5.reset();
		md5.update(str.getBytes());
		byte[] result = md5.digest();

		StringBuffer hexString = new StringBuffer();
		for (int i = 0; i < result.length; i++) {
			hexString.append(Integer.toHexString(0xFF & result[i]));
		}
		return hexString.toString();
	}
}
