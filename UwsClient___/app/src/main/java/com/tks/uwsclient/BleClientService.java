package com.tks.uwsclient;

import static com.tks.uwsclient.Constants.UWS_NG_SUCCESS;

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

public class BleClientService extends Service {
	private IBleClientServiceCallback	mCb;	/* 常に後発優先 */
	private BluetoothLeAdvertiser		mBluetoothLeAdvertiser;

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
		public int initBle() throws RemoteException {
			return BctInit();
		}

		@Override
		public void startAdvertising(int seekerid) throws RemoteException {
			BctStartAdvertising(seekerid);
		}

		@Override
		public void stopAdvertising() throws RemoteException {
			BctStopAdvertising();;
		}
	};

	/** *******
	 * BLE初期化
	 * ********/
	private int BctInit() {
		/* Bluetooth権限なし */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			TLog.d( "Bluetoothの権限がありません");
			return Constants.UWS_NG_PERMISSION_DENIED;
		}
		TLog.d( "Bluetooth権限OK.");

		/* Bluetoothサービス取得 */
		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null) {
			TLog.d( "Bluetooth未サポートです");
			return Constants.UWS_NG_SERVICE_NOTFOUND;
		}
		TLog.d( "Bluetoothサービス取得OK.");

		/* Bluetoothアダプタ取得 */
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			TLog.d( "Bluetooth未サポートです2");
			return Constants.UWS_NG_ADAPTER_NOTFOUND;
		}

		mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
		if (mBluetoothLeAdvertiser == null) {
			TLog.d( "Bluetooth未サポートです3");
			return Constants.UWS_NG_ADAPTER_NOTFOUND;
		}

		return UWS_NG_SUCCESS;
	}

	/** ************
	 * アドバタイズ開始
	 * ************/
	private void BctStartAdvertising(int seekerid) {
		TLog.d("アドバタイズ開始");
		AdvertiseSettings	settings	= buildAdvertiseSettings();
		AdvertiseData		data		= buildAdvertiseData(seekerid);
		if (mBluetoothLeAdvertiser != null) {
			mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
		}
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







	private AdvertiseData buildAdvertiseData(int seekerid) {
		AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
		dataBuilder.addServiceUuid(ParcelUuid.fromString(Constants.createServiceUuid(seekerid)));
		dataBuilder.setIncludeDeviceName(true);
//        dataBuilder.addManufacturerData(0xffff, new byte[]{'F','I','R','E','_','F','I','G','H','T','E','R'});
		/* ↑これを載っけるとscanで引っかからなくなる。 */
		return dataBuilder.build();
	}

	private AdvertiseSettings buildAdvertiseSettings() {
		AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
		settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
		settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		settingsBuilder.setTimeout(0);  /* タイムアウトは自前で管理する。 */
		return settingsBuilder.build();
	}
}
