package com.tks.uwsserverunit00;

import android.bluetooth.BluetoothGatt;

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
	public final static int UWS_NG_ALREADY_ADVERTISED	= -8;	/* 既にアドバタイズ中 */
	public final static int UWS_NG_ALREADY_SCANSTOPEDNED= -9;	/* 既にscan停止中 */
	public final static int UWS_NG_ILLEGALARGUMENT		= -10;	/* 引数不正 */
	public final static int UWS_NG_DEVICE_NOTFOUND		= -11;	/* デバイスが見つからない。 */
	public final static int UWS_NG_AIDL_REMOTE_ERROR	= -12;	/* AIDL-RemoteException発生 */
	public final static int UWS_NG_GATTSERVER_NOTFOUND	= -13;	/* Gattサーバがない */

}
