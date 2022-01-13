package com.tks.uwsclient;

public class Constants {
	/* *****/
	/* 通知 */
	/*******/
	public final static int		NOTIFICATION_ID_FOREGROUND_SERVICE = 1231234;
	public final static String	NOTIFICATION_CHANNEL_STARTSTOP = "NOTIFICATION_CHANNEL_STARTSTOP";

	public static class ACTION {
		public final static String INITIALIZE = "uws.action.initialize";
		public final static String FINALIZE = "uws.action.finalize";
		public final static String FINALIZEFROMS = "uws.action.finalizefromservice";
		public final static String STARTLOC = "uws.action.startloc";
		public final static String STOPLOC = "uws.action.stoploc";
	}

	public static class STATE_SERVICE {
		public static final int CONNECTED = 10;
		public static final int NOT_CONNECTED = 0;
	}

	/* ***************/
	/* 状態(Service) */
	/* ***************/
	public final static int SERVICE_STATUS_INITIALIZING	= 0;	/* 初期化中 */
	public final static int SERVICE_STATUS_IDLE			= 1;	/* IDLE */
	public final static int SERVICE_STATUS_AD_LOC_BEAT	= 2;	/* アドバタイズ中かつ位置情報取得中かつ脈拍取得中 */
	public final static int SERVICE_STATUS_CON_LOC_BEAT	= 3;	/* BLE接続中かつ位置情報取得中かつ脈拍取得中 */

	public enum Sender {
		App, Service,
	}
}
