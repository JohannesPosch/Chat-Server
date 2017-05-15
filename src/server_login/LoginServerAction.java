package server_login;

import java.util.Objects;

public enum LoginServerAction {

	LOGIN("LOGIN"),
	LOGOFF("LOGOFF"),
	REGISTER("REGISTER");
	
	private String text;

	LoginServerAction(String text) throws NullPointerException{
		this.text = Objects.requireNonNull(text);;
	}

	public String getText() {
		return this.text;
	}
	
	public static LoginServerAction fromString(final String name)throws NullPointerException{
		Objects.requireNonNull(name);
		
		for (LoginServerAction b : LoginServerAction.values()) {
			if (b.text.equalsIgnoreCase(name)) {
				return b;
			}
	    }
	    return null;
	}
}
