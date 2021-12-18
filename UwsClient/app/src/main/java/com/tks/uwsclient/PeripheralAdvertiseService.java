package com.tks.uwsclient;

import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.os.Binder;
import android.os.IBinder;
import android.os.ParcelUuid;

import androidx.annotation.Nullable;

public class PeripheralAdvertiseService extends Service {
	private final IBinder mBinder = new LocalBinder();
	public class LocalBinder extends Binder {
		PeripheralAdvertiseService getService() {
			return PeripheralAdvertiseService.this;
		}
	}

	public final static String KEY_NO = "com.tks.uws.uwsmember.NO";
	private int mNo = -1;

	private BluetoothLeAdvertiser mBluetoothLeAdvertiser;
	private AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartFailure(int errorCode) {
			super.onStartFailure(errorCode);
			/* アドバタイズのサイズがデカすぎの時は、AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGEが発生する。 */
			TLog.d("Advertising failed error={0}", errorCode);
			stopSelf();
		}

		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			super.onStartSuccess(settingsInEffect);
			TLog.d("Advertising successfully started");
		}
	};

	@Override
	public void onCreate() {
		super.onCreate();
		TLog.d("");
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		mNo = intent.getIntExtra(KEY_NO, -999);
		TLog.d("{0}={1}", KEY_NO, mNo);
		initialize();
		startAdvertising();
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		TLog.d("");
		stopAdvertising();
		stopForeground(true);
		return super.onUnbind(intent);
	}

	private void initialize() {
		BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if (bluetoothManager != null) {
			BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
			if (bluetoothAdapter != null) {
				mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
			}
			else {
				TLog.d("BT初期化失敗2!!");
				stopSelf();;
			}
		}
		else {
			TLog.d("BT初期化失敗!!");
			stopSelf();;
		}
	}

	private void startAdvertising() {
		TLog.d("Service: Start Advertising");
		AdvertiseSettings	settings	= buildAdvertiseSettings();
		AdvertiseData		data		= buildAdvertiseData();

		if (mBluetoothLeAdvertiser != null) {
			mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
		}
	}

	private void stopAdvertising() {
		TLog.d("Service: Stop Advertising");
		if (mBluetoothLeAdvertiser != null) {
			mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
		}
	}

	private AdvertiseData buildAdvertiseData() {
		AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
		dataBuilder.addServiceUuid(ParcelUuid.fromString(Constants.createServiceUuid(mNo)));
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
