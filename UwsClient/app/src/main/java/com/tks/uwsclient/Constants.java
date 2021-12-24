package com.tks.uwsclient;

import android.bluetooth.BluetoothGatt;

import java.text.MessageFormat;
import java.util.UUID;

public class Constants {
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
	public final static int UWS_NG_ALREADY_SCANSTOPEDNED= -8;	/* 既にscan停止中 */
	public final static int UWS_NG_ILLEGALARGUMENT		= -9;	/* 引数不正 */
	public final static int UWS_NG_DEVICE_NOTFOUND		= -10;	/* デバイスが見つからない。 */
	public final static int UWS_NG_AIDL_REMOTE_ERROR	= -11;	/* AIDL-RemoteException発生 */
	public final static int UWS_NG_GATTSERVER_NOTFOUND	= -16;	/* Gattサーバがない */

	/* ServiceUUIDは0000xxxx-0000-1000-8000-00805f9b34fbの形を守る必要がある。CharacteristicUUIDはなんでもOK.*/
//	public static final UUID UWS_SERVICE_UUID							= UUID.fromString("00002c00-0000-1000-8000-00805f9b34fb");
	public static final UUID UWS_CHARACTERISTIC_SAMLE_UUID				= UUID.fromString("29292c2c-728c-4a2b-81cb-7b4d884adb04");

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
}
