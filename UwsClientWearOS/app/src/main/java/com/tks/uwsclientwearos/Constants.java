package com.tks.uwsclientwearos;

import java.util.Locale;

public class Constants {
	/* *****/
	/* 通知 */
	/*******/
	public final static int NOTIFICATION_ID_FOREGROUND_SERVICE_HB = 1122;
	public final static int NOTIFICATION_ID_FOREGROUND_SERVICE_BLE= 2233;
	public final static String	NOTIFICATION_CHANNEL_ID = "com.tks.NOTIFICATION_CHANNEL_ID";

	public static class ACTION {
		public final static String INITIALIZE	= "uws.action.initialize";
		public final static String FINALIZE		= "uws.action.finalize";
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

	public static String d2Str(double val) {
		return String.format(Locale.JAPAN, "%1$.12f", val);
//		return String.format(Locale.JAPAN, "%.10f", val);
	}

}
