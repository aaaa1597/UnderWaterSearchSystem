package com.tks.uwsclientwearos;

public class Constants {
	public final static int		NOTIFICATION_ID_FOREGROUND_SERVICE = 1231234;
	public final static String	NOTIFICATION_CHANNEL_STARTSTOP = "NOTIFICATION_CHANNEL_STARTSTOP";

	public static class ACTION {
		public final static String INITIALIZE = "uws.action.initialize";
		public final static String FINALIZE = "uws.action.finalize";
		public final static String STARTLOC = "uws.action.startloc";
		public final static String STOPLOC = "uws.action.stoploc";
	}

	public static class STATE_SERVICE {
		public static final int CONNECTED = 10;
		public static final int NOT_CONNECTED = 0;
	}
}
