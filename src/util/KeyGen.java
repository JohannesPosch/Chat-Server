package util;

import java.util.UUID;

public class KeyGen {

	public static String generateSessionKey(final int length){
		String randomStr = UUID.randomUUID().toString().replaceAll("-", "");
		while(randomStr.length() < length)
			randomStr += UUID.randomUUID().toString().replaceAll("-", "");
		
		return randomStr.substring(0, length);
	}
}