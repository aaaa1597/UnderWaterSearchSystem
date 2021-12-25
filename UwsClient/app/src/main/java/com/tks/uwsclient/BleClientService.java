package com.tks.uwsclient;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import java.nio.ByteBuffer;
import java.text.MessageFormat;

import static com.tks.uwsclient.Constants.UWS_LOC_BASE_LATITUDE;
import static com.tks.uwsclient.Constants.UWS_LOC_BASE_LONGITUDE;
import static com.tks.uwsclient.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsclient.Constants.UWS_NG_ADAPTER_NOTFOUND;
import static com.tks.uwsclient.Constants.UWS_NG_PERMISSION_DENIED;
import static com.tks.uwsclient.Constants.UWS_NG_SERVICE_NOTFOUND;
import static com.tks.uwsclient.Constants.UWS_OWNDATA_KEY;

public class BleClientService extends Service {
	private IBleClientServiceCallback	mCb;	/* 常に後発優先 */

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		TLog.d("");
		return super.onUnbind(intent);
	}

	/** *****
	 * Binder
	 * ******/
	private Binder mBinder = new IBleClientService.Stub() {

		@Override
		public void setCallback(IBleClientServiceCallback callback) throws RemoteException {
			mCb = callback;
		}

		@Override
		public int initBle() {
			return BctInit();
		}

		@Override
		public void startAdvertising(int seekerid, float difflong, float difflat, int heartbeat) {
			BctStartAdvertising((short)seekerid, difflong, difflat, (short)heartbeat);
		}

		@Override
		public void stopAdvertising() {
			BctStopAdvertising();
		}
	};

	/** *******
	 * BLE初期化
	 * ********/
	private int BctInit() {
		/* Bluetooth権限なし */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			TLog.d( "Bluetoothの権限がありません");
			return UWS_NG_PERMISSION_DENIED;
		}
		TLog.d( "Bluetooth権限OK.");

		/* Bluetoothサービス取得 */
		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null) {
			TLog.d( "Bluetooth未サポートです");
			return UWS_NG_SERVICE_NOTFOUND;
		}
		TLog.d( "Bluetoothサービス取得OK.");

		/* Bluetoothアダプタ取得 */
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			TLog.d( "Bluetooth未サポートです2");
			return UWS_NG_ADAPTER_NOTFOUND;
		}

		TLog.d( "アドバタイズの最大サイズ={0}", bluetoothAdapter.getLeMaximumAdvertisingDataLength());
		mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
		if (mBluetoothLeAdvertiser == null) {
			TLog.d( "Bluetooth未サポートです3");
			return UWS_NG_ADAPTER_NOTFOUND;
		}

		return UWS_NG_SUCCESS;
	}

	/** **********************************************************************
	 * アドバタイズ
	 ** **********************************************************************/
	private BluetoothLeAdvertiser		mBluetoothLeAdvertiser;

	/** ************
	 * アドバタイズ開始
	 * ************/
	private void BctStartAdvertising(short seekerid, float difflong, float difflat, short heartbeat) {
		TLog.d("アドバタイズ開始 消防士{0} 緯度/経度=({1},{2}) 脈拍={3}", seekerid, difflong+UWS_LOC_BASE_LONGITUDE, difflat+UWS_LOC_BASE_LATITUDE, heartbeat);

		boolean ret = BluetoothAdapter.getDefaultAdapter().setName(MessageFormat.format("消防士{0}", seekerid));
		TLog.d("デバイス名変更 ret={0}", ret);
		AdvertiseSettings	settings	= buildAdvertiseSettings();
		AdvertiseData		data		= buildAdvertiseData(seekerid, difflong, difflat, heartbeat);
		if (mBluetoothLeAdvertiser != null)
			mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
	}

	private AdvertiseSettings buildAdvertiseSettings() {
		AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
		settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
		settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		settingsBuilder.setTimeout(0);  /* タイムアウトは自前で管理する。 */
		return settingsBuilder.build();
	}

	private AdvertiseData buildAdvertiseData(short seekerid, float difflong, float difflat, short heartbeat) {
		AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
		dataBuilder.addServiceUuid(ParcelUuid.fromString(Constants.createServiceUuid(seekerid)));
		dataBuilder.setIncludeDeviceName(true);

		/* 拡張データ生成 */
		byte[] sndBin = new byte[10];	/* 全部で12byteまでは送信可 */
		int spos = 0;
		/* (小城消防署からの)経度差分(4byte) */
		byte[] bdifflong = f2bs(difflong);
		System.arraycopy(bdifflong, 0, sndBin, spos, bdifflong.length);
		spos += bdifflong.length;
		/* (小城消防署からの)緯度差分(4byte) */
		byte[] bdifflat = f2bs(difflat);
		System.arraycopy(bdifflat, 0, sndBin, spos, bdifflat.length);
		spos += bdifflat.length;
		/* 脈拍(2byte) */
		byte[] bheartbeat = s2bs(heartbeat);
		System.arraycopy(bheartbeat, 0, sndBin, spos, bheartbeat.length);
		/* 拡張データ設定 */
		dataBuilder.addManufacturerData(UWS_OWNDATA_KEY, sndBin);

		return dataBuilder.build();
	}
	private byte[] s2bs(short value) {
		return ByteBuffer.allocate(2).putShort(value).array();
	}
	private byte[] f2bs(float value) {
		return ByteBuffer.allocate(4).putFloat(value).array();
	}

	private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			super.onStartSuccess(settingsInEffect);
			TLog.d("アドバタイズ開始OK.");
			try { mCb.notifyAdvertising(UWS_NG_SUCCESS); }
			catch (RemoteException e) { e.printStackTrace(); }
		}

		@Override
		public void onStartFailure(int errorCode) {
			super.onStartFailure(errorCode);
			TLog.d("アドバタイズ開始失敗 error={0}", errorCode);
			/* アドバタイズのサイズがデカすぎの時は、AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGEが発生する。 */
			try { mCb.notifyAdvertising(errorCode); }
			catch (RemoteException e) { e.printStackTrace(); }
		}
	};

	/** ************
	 * アドバタイズ終了
	 * ************/
	private void BctStopAdvertising() {
		TLog.d("アドバタイズ停止 {0}", mBluetoothLeAdvertiser);
		if (mBluetoothLeAdvertiser != null) {
			mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
		}
	}
}
