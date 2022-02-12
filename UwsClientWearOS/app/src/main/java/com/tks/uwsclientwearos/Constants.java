package com.tks.uwsclientwearos;

import java.util.Locale;
import java.util.UUID;

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
	public final static int SERVICE_STATUS_LOC_BEAT		= 1;	/* 位置情報取得中かつ脈拍取得中 */
	public final static int SERVICE_STATUS_CONNECTING	= 2;	/* サーバ接続待ち... */
	public final static int SERVICE_STATUS_CON_LOC_BEAT	= 3;	/* BT接続中かつ位置情報取得中かつ脈拍取得中 */

	public enum Sender {
		App, Service,
	}

	/* ***********/
	/* Bluetooth */
	/* ***********/
	public static final UUID BT_CLASSIC_UUID = UUID.fromString("41eb5f39-6c3a-4067-8bb9-bad64e6e0908");

	/* ************/
	/* エラーコード */
	/* ************/
	public final static int ERR_OK	= 0;	/* OK */
	public final static int ERR_ALREADY_STARTED	= -1;	/* すでに実行中 */
	public final static int ERR_BT_DISABLE		= -2;	/* BT無効 */

	public static String d2Str(double val) {
		return String.format(Locale.JAPAN, "%1$.12f", val);
//		return String.format(Locale.JAPAN, "%.10f", val);
	}
}
