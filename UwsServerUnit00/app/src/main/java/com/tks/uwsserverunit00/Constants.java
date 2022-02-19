package com.tks.uwsserverunit00;

import android.bluetooth.BluetoothGatt;

import java.text.MessageFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;

public class Constants {
	/* ベース位置(小城消防署(33.29333107719108, 130.19189394973347)) */
	public final static double	UWS_LOC_BASE_LONGITUDE	= 130.19153989612116;
	public final static double	UWS_LOC_BASE_LATITUDE	= 33.29307995564407;
	public final static double	UWS_LOC_BASE_DISTANCE_X = 40000*1000/*4万*1000m*/ * Math.cos(UWS_LOC_BASE_LATITUDE*Math.PI/180) / 360;	/* 小城消防署付近の経度1°当たりの距離[m] */
	public final static double	UWS_LOC_BASE_DISTANCE_Y = 40000*1000/*4万*1000m*/ / 360.0;												/* 赤道     付近の経度1°当たりの距離[m] */

	/* ***********/
	/* Bluetooth */
	/* ***********/
	public static final UUID BT_CLASSIC_UUID = UUID.fromString("41eb5f39-6c3a-4067-8bb9-bad64e6e0908");
	public static final String BT_NORTIFY_SEEKERID	= "com.tsk.setSeekerid";
	public static final String BT_NORTIFY_CLOSE		= "com.tsk.close";

//	/* ServiceUUIDは0000xxxx-0000-1000-8000-00805f9b34fbの形を守る必要がある。CharacteristicUUIDはなんでもOK.*/
//	public static final UUID UWS_UUID_SERVICE					= UUID.fromString("00002c2c-0000-1000-8000-00805f9b34fb");
//	public static final UUID UWS_UUID_CHARACTERISTIC_HRATBEAT	= UUID.fromString("29292c2c-728c-4a2b-81cb-7b4d884adb04");
//
//	public static String createServiceUuid(int seqno) {
//		String ret = MessageFormat.format("00002c{0}-0000-1000-8000-00805f9b34fb", String.format("%02x", (byte)(seqno & 0xff)));
//		TLog.d("UUID文字列={0} seqno={1}({2})", ret, seqno, (byte)(seqno & 0xff));
//		return ret;
//	}
//
//	/* 定義済UUIDに変換する "0000xxxx-0000-1000-8000-00805f9b34fb" */
//	private static UUID convertFromInteger(int i) {
//		final long MSB = 0x0000000000001000L;
//		final long LSB = 0x800000805f9b34fbL;
//		long value = i & 0xFFFFFFFF;
//		return new UUID(MSB | (value << 32), LSB);
//	}
//	public static String getShortUuid(String uuid) {
//		return uuid.substring(4,8);
//	}
//
//	public static String getShortUuid(UUID uuid) {
//		return uuid.toString().substring(4,8);
//	}

	/* 状態(サービス) */
	public static final int UWS_SERVICE_STATUS_GATT_DISCONNECT = -1;	/* GATT切断 */

	/* Log文字列 */
	public static String d2Str(double val) {
		return String.format(Locale.JAPAN, "%1$.12f", val);
//		return String.format(Locale.JAPAN, "%.10f", val);
	}
	private final static SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSXXX", Locale.JAPAN);
	public static String d2Str(Date val) {
		return df.format(val);
	}
	public static String d2Str(Long val) {
		return df.format(new Date(val));
	}
}
