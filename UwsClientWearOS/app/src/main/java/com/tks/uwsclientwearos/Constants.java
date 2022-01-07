package com.tks.uwsclientwearos;

import android.bluetooth.BluetoothGatt;
import java.text.MessageFormat;
import java.util.UUID;

public class Constants {
	/* ベース位置(小城消防署(33.29333107719108, 130.19189394973347)) */
	public final static double	UWS_LOC_BASE_LONGITUDE	= 130.19189394973347;
	public final static double	UWS_LOC_BASE_LATITUDE	= 33.29333107719108;
	public final static int 	UWS_OWNDATA_KEY			= 0xffff;

	/* エラーコード */
	public final static int UWS_NG_SUCCESS				= 0;	/* OK */
	public final static int UWS_NG_GATT_SUCCESS			= BluetoothGatt.GATT_SUCCESS;
	public final static int UWS_NG_ALREADY_CONNECTED	= -1;	/* 既に接続済 */
	public final static int UWS_NG_SERVICE_NOTFOUND		= -2;	/* サービスが見つからない(=Bluetooth未サポ－ト) */
	public final static int UWS_NG_ADAPTER_NOTFOUND		= -3;	/* BluetoothAdapterがnull(=Bluetooth未サポ－ト) */
	public final static int UWS_NG_ADVERTISER_NOTFOUND	= -4;	/* BluetoothAdvertiserがnull(=Bluetooth未サポ－ト) */
	public final static int UWS_NG_BT_OFF				= -5;	/* Bluetooth機能がOFF */
	public final static int UWS_NG_ALREADY_SCANNED		= -6;	/* 既にscan中 */
	public final static int UWS_NG_PERMISSION_DENIED	= -7;	/* 権限なし */
	public final static int UWS_NG_ALREADY_ADVERTISED	= -8;	/* 既にアドバタイズ中 */
	public final static int UWS_NG_ALREADY_SCANSTOPEDNED= -9;	/* 既にscan停止中 */
	public final static int UWS_NG_ILLEGALARGUMENT		= -10;	/* 引数不正 */
	public final static int UWS_NG_DEVICE_NOTFOUND		= -11;	/* デバイスが見つからない。 */
	public final static int UWS_NG_AIDL_REMOTE_ERROR	= -12;	/* AIDL-RemoteException発生 */
	public final static int UWS_NG_GATTSERVER_NOTFOUND	= -13;	/* Gattサーバがない */

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
}
