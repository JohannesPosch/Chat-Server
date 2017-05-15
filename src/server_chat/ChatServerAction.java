package server_chat;

import java.util.Objects;

import server_login.LoginServerAction;

public enum ChatServerAction {

	GET_USERS("GET_USERS"),
	GET_IP_USER("GET_IP_USER");
	
	private String text;

	ChatServerAction(String text) throws NullPointerException{
		this.text = Objects.requireNonNull(text);;
	}

	public String getText() {
		return this.text;
	}
	
	public static ChatServerAction fromString(final String name)throws NullPointerException{
		Objects.requireNonNull(name);
		
		for (ChatServerAction b : ChatServerAction.values()) {
			if (b.text.equalsIgnoreCase(name)) {
				return b;
			}
	    }
	    return null;
	}
}
