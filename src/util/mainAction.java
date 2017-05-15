package util;

public enum mainAction {
	ChangePort,
	ServerStatus,
	startAll,
	startOne,
	stopAll,
	stopOne,
	quit,
	FAULT,
	UNDEFINED;
	
	public static final short firstIndex = 1;
	
	public static mainAction fromInteger(final int val){
		mainAction ret;
		
		switch(val){
			case firstIndex:
				ret = ChangePort;
				break;
			case firstIndex + 1:
				ret = ServerStatus;
				break;
			case firstIndex + 2:
				ret = startAll;
				break;
			case firstIndex + 3:
				ret = startOne;
				break;
			case firstIndex + 4:
				ret = stopAll;
				break;
			case firstIndex + 5:
				ret = stopOne;
				break;
			case firstIndex + 6:
				ret = quit;
				break;
			case -1:
				ret = FAULT;
				break;
			default:
				ret = UNDEFINED;
				break;	
		}
		
		return ret;
	}
}