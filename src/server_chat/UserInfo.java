package server_chat;

import java.util.HashMap;
import java.util.Map;

public class UserInfo {

	public String user;
	public boolean active;
	
	static public Map toMap(final UserInfo obj){
		Map elem = new HashMap();
		elem.put("user", obj.user);
		elem.put("active", obj.active);
		return elem;
	}
}
