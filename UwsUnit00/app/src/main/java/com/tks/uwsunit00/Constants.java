package com.tks.uwsunit00;

import android.bluetooth.BluetoothGatt;

public class Constants {
	/* エラーコード */
	public final static int UWS_NG_SUCCESS				= 0;	/* OK */
	public final static int UWS_NG_RECONNECT_OK			= -1;	/* 再接続OK */
	public final static int UWS_NG_SERVICE_NOTFOUND		= -2;	/* サービスが見つからない(=Bluetooth未サポ－ト) */
	public final static int UWS_NG_ADAPTER_NOTFOUND		= -3;	/* BluetoothAdapterがnull(=Bluetooth未サポ－ト) */
	public final static int UWS_NG_BT_OFF				= -4;	/* Bluetooth機能がOFF */
	public final static int UWS_NG_ALREADY_SCANNED		= -5;	/* 既にscan中 */
	public final static int UWS_NG_PERMISSION_DENIED	= -6;	/* 権限なし */
	public final static int UWS_NG_ALREADY_SCANSTOPEDNED= -7;	/* 既にscan停止中 */
	public final static int UWS_NG_ILLEGALARGUMENT		= -8;	/* 引数不正 */
	public final static int UWS_NG_DEVICE_NOTFOUND		= -9;	/* デバイスが見つからない。 */
	public final static int UWS_NG_GATT_SUCCESS			= BluetoothGatt.GATT_SUCCESS;
}
