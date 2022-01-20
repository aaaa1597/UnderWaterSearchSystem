package com.tks.uwsclientwearos;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Constants {
	/* *****/
	/* 通知 */
	/*******/
	public final static int		NOTIFICATION_ID_FOREGROUND_SERVICE = 1231234;
	public final static String	NOTIFICATION_CHANNEL_STARTSTOP = "NOTIFICATION_CHANNEL_STARTSTOP";

	public static class ACTION {
		public final static String INITIALIZE	= "uws.action.initialize";
		public final static String FINALIZE		= "uws.action.finalize";
		public final static String FINALIZEFROMS= "uws.action.finalizefromservice";
		public final static String STARTLOC		= "uws.action.startloc";
		public final static String STOPLOC		= "uws.action.stoploc";
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

	/* *****/
	/* BLE */
	/* *****/
	public final static int 	UWS_OWNDATA_KEY			= 0xffff;
	/* ServiceUUIDは0000xxxx-0000-1000-8000-00805f9b34fbの形を守る必要がある。CharacteristicUUIDはなんでもOK.*/
//	public static final UUID UWS_UUID_SERVICE					= UUID.fromString("00002c2c-0000-1000-8000-00805f9b34fb");
	public static final UUID UWS_UUID_CHARACTERISTIC_HRATBEAT	= UUID.fromString("29292c2c-728c-4a2b-81cb-7b4d884adb04");

	public static String createServiceUuid(int seqno) {
		String ret = MessageFormat.format("00002c{0}-0000-1000-8000-00805f9b34fb", String.format("%02x", (byte)(seqno & 0xff)));
		TLog.d("UUID文字列={0} seqno={1}({2})", ret, seqno, (byte)(seqno & 0xff));
		return ret;
	}

	/* 定義済UUIDに変換する "0000xxxx-0000-1000-8000-00805f9b34fb" */
	private static UUID convertFromInteger(int i) {
		final long MSB = 0x0000000000001000L;
		final long LSB = 0x800000805f9b34fbL;
		long value = i & 0xFFFFFFFF;
		return new UUID(MSB | (value << 32), LSB);
	}
//	public static String getShortUuid(String uuid) {
//		return uuid.substring(4,8);
//	}
//
//	public static String getShortUuid(UUID uuid) {
//		return uuid.toString().substring(4,8);
//	}

	public static String d2Str(double val) {
		return String.format(Locale.JAPAN, "%1$.12f", val);
//		return String.format(Locale.JAPAN, "%.10f", val);
	}

	private final static SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSXXX", Locale.JAPAN);
	public static String d2Str(Date val) {
		return df.format(val);
	}
}
