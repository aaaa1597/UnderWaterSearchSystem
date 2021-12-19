package com.tks.uwsclient;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
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
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Date;
import java.util.Queue;

import static com.tks.uwsclient.Constants.UWS_CHARACTERISTIC_SAMLE_UUID;
import static com.tks.uwsclient.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsclient.Constants.UWS_NG_ADAPTER_NOTFOUND;
import static com.tks.uwsclient.Constants.UWS_NG_PERMISSION_DENIED;
import static com.tks.uwsclient.Constants.UWS_NG_SERVICE_NOTFOUND;
import static com.tks.uwsclient.Constants.UWS_NG_GATTSERVER_NOTFOUND;
import static com.tks.uwsclient.Constants.UWS_SERVICE_UUID;

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
		public int initBle() throws RemoteException {
			return BctInit();
		}

		@Override
		public void startAdvertising(int seekerid) throws RemoteException {
			BctStartAdvertising(seekerid);
		}

		@Override
		public void stopAdvertising() throws RemoteException {
			BctStopAdvertising();
		}

		@Override
		public void notifyOneShot() throws RemoteException {
			BctNotifyOneShot();
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

		mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
		if (mBluetoothLeAdvertiser == null) {
			TLog.d( "Bluetooth未サポートです3");
			return UWS_NG_ADAPTER_NOTFOUND;
		}

		mGattManager = bluetoothManager.openGattServer(this, mGattServerCallback);
		if(mGattManager == null)
			return UWS_NG_GATTSERVER_NOTFOUND;

		/* 自分自身のペリフェラル特性を定義 */
		mUwsCharacteristic = createOwnCharacteristic();

		return UWS_NG_SUCCESS;
	}

	/** **********************************************************************
	 * アドバタイズ
	 ** **********************************************************************/
	private BluetoothLeAdvertiser		mBluetoothLeAdvertiser;

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

	/** **********************************************************************
	 * Gattサーバ処理
	 ** **********************************************************************/
	private BluetoothGattServer			mGattManager;
	private BluetoothDevice				mServerDevice;
	private BluetoothGattCharacteristic	mUwsCharacteristic;
	private Queue<Runnable>				mNotifySender = new ArrayDeque<>();

	/** **********************
	 * 自身のペリフェラル特性を定義
	 * ***********************/
	private BluetoothGattCharacteristic createOwnCharacteristic() {
		/* 自身が提供するサービスを定義 */
		BluetoothGattService ownService = new BluetoothGattService(UWS_SERVICE_UUID, BluetoothGattService.SERVICE_TYPE_PRIMARY);

		/* 自身が提供するCharacteristic(特性)を定義 : 通知と読込みに対し、読込み許可 */
		BluetoothGattCharacteristic charac = new BluetoothGattCharacteristic(/*UUID*/UWS_CHARACTERISTIC_SAMLE_UUID,
				BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
				/*permissions*/BluetoothGattCharacteristic.PERMISSION_READ);

		charac.setValue(new byte[]{'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f',16,17,18,19,20,21,22,23,24,25,26,27});

		/* 定義したサービスにCharacteristic(特性)を付与 */
		ownService.addCharacteristic(charac);

		/* Gattサーバに定義したサービスを付与 */
		mGattManager.addService(ownService);

		return charac;
	}

	/** ************
	 * 1秒定義 通知
	 * ************/
	short mMsgIdCounter = 0;
	private void BctNotifyOneShot() {
		boolean indicate = (mUwsCharacteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) == BluetoothGattCharacteristic.PROPERTY_INDICATE;
		/* 通知データ取得 */
		double longitude = 0;
		double latitude = 0;
		int heartbeat  = 0;
		try {
			longitude = mCb.getLongitude();	/* 経度 */
			latitude = mCb.getLatitude();	/* 緯度 */
			heartbeat  = mCb.getHeartbeat();/* 脈拍 */
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
		double finalLongitude = longitude;
		double finalLatitude = latitude;
		int finalHeartbeat = heartbeat;

		/* 通知データ(先行分(msgid,seqno,日付,経度)) */
		mNotifySender.add(() -> {
			set1stValuetoCharacteristic(mUwsCharacteristic, mMsgIdCounter, new Date(), finalLongitude);
			TLog.d("1st送信");
			mGattManager.notifyCharacteristicChanged(mServerDevice, mUwsCharacteristic, indicate);
		});
		/* 通知データ(後発分(msgid,seqno,緯度,脈拍)) */
		mNotifySender.add(() -> {
			set2ndValuetoCharacteristic(mUwsCharacteristic, mMsgIdCounter++, finalLatitude, finalHeartbeat);
			TLog.d("2nd送信");
			mGattManager.notifyCharacteristicChanged(mServerDevice, mUwsCharacteristic, indicate);
		});
		/* 生成した通知データを1件処理(後は、onNotificationSent()で送信する) */
		mNotifySender.poll().run();
	}

	/** *****************
	 * GattサーバCallBack
	 * ******************/
	private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
		/** ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		 * 接続状態変化通知
		 * @param serverdevice	サーバ側デバイス
		 * @param status	int: Status of the connect or disconnect operation. BluetoothGatt.GATT_SUCCESS if the operation succeeds.
		 * @param newState	BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile#STATE_CONNECTED
		 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
		@Override
		public void onConnectionStateChange(BluetoothDevice serverdevice, final int status, int newState) {
			super.onConnectionStateChange(serverdevice, status, newState);
			TLog.d("status={0} newState={1}", status, newState);
			TLog.d("status-BluetoothGatt.GATT_SUCCESS({0}) newState-BluetoothGatt.STATE_xxxx(STATE_CONNECTED({1}),STATE_DISCONNECTED({2}))", BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_CONNECTED, BluetoothGatt.STATE_DISCONNECTED);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (newState == BluetoothGatt.STATE_CONNECTED) {
					mServerDevice = serverdevice;
					TLog.d("接続 to serverdevice: {0}", serverdevice.getAddress());
					try { mCb.notifyConnect(); }
					catch (RemoteException e) { e.printStackTrace();}
				}
				else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
					mServerDevice = null;
					TLog.d("切断 from serverdevice");
					try { mCb.notifyDisConnect(); }
					catch (RemoteException e) { e.printStackTrace(); }
				}
			}
			else {
				mServerDevice = null;
				try { mCb.notifyErrorConnect(status); }
				catch (RemoteException e) { e.printStackTrace(); }
			}
		}

		/* 通知/指示送信の結果 */
		@Override
		public void onNotificationSent(BluetoothDevice server, int status) {
			super.onNotificationSent(server, status);
			/* 積まれた通知データを送信 */
			Runnable runner = mNotifySender.poll();
			if(runner != null) runner.run();
			TLog.d("Notification sent. Status:{0} 残:{1}",status, mNotifySender.size());
		}

		/* Read要求受信 */
		@Override
		public void onCharacteristicReadRequest(BluetoothDevice server, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicReadRequest(server, requestId, offset, characteristic);
			/* 初回送信時のみ値設定 */
			if(requestId == 1) {
				try {
					double longitude = mCb.getLongitude();
					double latitude = mCb.getLatitude();
					int heartbeat = mCb.getHeartbeat();
					setValuetoCharacteristic(characteristic, new Date(),  longitude, latitude,  heartbeat);
//					TLog.d("arg(characteristic={0} mUwsCharacteristic={1} 両者は同じ{2}", characteristic, mUwsCharacteristic, characteristic==mUwsCharacteristic);
				}
				catch (RemoteException e) {
					e.printStackTrace();
					return;
				}
			}
			/* 分割送信処理対応 */
			byte[] resData = new byte[characteristic.getValue().length-offset];
			System.arraycopy(characteristic.getValue(), offset, resData, 0, resData.length);

			TLog.d("CentralからのRead要求({0}) 返却値:(UUID:{1},resData(byte数{2}:データ{3}) org(offset{4},val:{5}))", requestId, characteristic.getUuid(), resData.length, Arrays.toString(resData), offset, Arrays.toString(characteristic.getValue()));
			mGattManager.sendResponse(server, requestId, BluetoothGatt.GATT_SUCCESS, 0, resData);
		}

		/* Write要求受信 */
		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
			super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

			TLog.d("CentralからのWrite要求 受信値:(UUID:{0},vat:{1}))", mUwsCharacteristic.getUuid(), Arrays.toString(value));
			setValuetoCharacteristic(mUwsCharacteristic, new Date(), 555, 666,  123);
			if (responseNeeded)
				mGattManager.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
		}

		/* Read要求受信 */
		@Override
		public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
			super.onDescriptorReadRequest(device, requestId, offset, descriptor);

			TLog.d("CentralからのDescriptor_Read要求 返却値:(UUID:{0},vat:{1}))", descriptor.getUuid(), Arrays.toString(descriptor.getValue()));
			mGattManager.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
		}

		/* Write要求受信 */
		@Override
		public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
											 BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
											 int offset,
											 byte[] value) {
			super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

			TLog.d("CentralからのDescriptor_Write要求 受信値:(UUID:{0},vat:{1}))", descriptor.getUuid(), Arrays.toString(value));

//            int status = BluetoothGatt.GATT_SUCCESS;
//            if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
//                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
//                boolean supportsNotifications = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
//                boolean supportsIndications   = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
//
//                if (!(supportsNotifications || supportsIndications)) {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                }
//                else if (value.length != 2) {
//                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
//                }
//                else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsDisabled(characteristic);
//                    descriptor.setValue(value);
//                }
//                else if (supportsNotifications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */);
//                    descriptor.setValue(value);
//                }
//                else if (supportsIndications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */);
//                    descriptor.setValue(value);
//                }
//                else {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                }
//            }
//            else {
//                status = BluetoothGatt.GATT_SUCCESS;
//                descriptor.setValue(value);
//            }
			if (responseNeeded)
				mGattManager.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,0,null);

		}
	};

	private void set1stValuetoCharacteristic(BluetoothGattCharacteristic charac, short msgid, Date datetime, double longitude/*経度*/) {
		TLog.d("1st送信データ生成");
		byte[] ret = new byte[20];
		int spos = 0;
		/* メッセージID(2byte) */
		byte[] bmsgid = s2bs(msgid);
		System.arraycopy(bmsgid, 0, ret, spos, bmsgid.length);
		spos += bmsgid.length;
		/* Seq番号(2byte) */
		byte[] bseqno = s2bs((short)0);
		System.arraycopy(bseqno, 0, ret, spos, bseqno.length);
		spos += bseqno.length;
		/* 日付(8byte) */
		byte[] bdatetime = l2bs(datetime.getTime());
		System.arraycopy(bdatetime, 0, ret, spos, bdatetime.length);
		spos += bdatetime.length;
		/* 経度(8byte) */
		byte[] blongitude = d2bs(longitude);
		System.arraycopy(blongitude, 0, ret, spos, blongitude.length);
		/* 値 設定 */
		charac.setValue(ret);
	}
	private void set2ndValuetoCharacteristic(BluetoothGattCharacteristic charac, short msgid, double latitude/*緯度*/, int heartbeat) {
		TLog.d("2nd送信データ生成");
		byte[] ret = new byte[16];
		int spos = 0;
		/* メッセージID(2byte) */
		byte[] bmsgid = s2bs(msgid);
		System.arraycopy(bmsgid, 0, ret, spos, bmsgid.length);
		spos += bmsgid.length;
		/* Seq番号(2byte) */
		byte[] bseqno = s2bs((short)1);
		System.arraycopy(bseqno, 0, ret, spos, bseqno.length);
		spos += bseqno.length;
		/* 緯度(8byte) */
		byte[] blatitude = d2bs(latitude);
		System.arraycopy(blatitude, 0, ret, spos, blatitude.length);
		spos += blatitude.length;
		/* 脈拍(4byte) */
		byte[] bheartbeat = i2bs(heartbeat);
		System.arraycopy(bheartbeat, 0, ret, spos, bheartbeat.length);
		/* 値 設定 */
		charac.setValue(ret);
	}
	private void setValuetoCharacteristic(BluetoothGattCharacteristic charac, Date datetime, double longitude/*経度*/, double latitude/*緯度*/, int heartbeat) {
		byte[] ret = new byte[28];
		int spos = 0;
		/* 日付(8byte) */
		byte[] bdatetime = l2bs(datetime.getTime());
		System.arraycopy(bdatetime, 0, ret, spos, bdatetime.length);
		spos += bdatetime.length;
		/* 経度(8byte) */
		byte[] blongitude = d2bs(longitude);
		System.arraycopy(blongitude, 0, ret, spos, blongitude.length);
		/* 緯度(8byte) */
		byte[] blatitude = d2bs(latitude);
		System.arraycopy(blatitude, 0, ret, spos, blatitude.length);
		spos += blatitude.length;
		/* 脈拍(4byte) */
		byte[] bheartbeat = i2bs(heartbeat);
		System.arraycopy(bheartbeat, 0, ret, spos, bheartbeat.length);
		/* 値 設定 */
		charac.setValue(ret);
	}
	private byte[] s2bs(short value) {
		return ByteBuffer.allocate(2).putShort(value).array();
	}
	private byte[] i2bs(int value) {
		return ByteBuffer.allocate(4).putInt(value).array();
	}
	private byte[] l2bs(long value) {
		return ByteBuffer.allocate(8).putLong(value).array();
	}
	private byte[] d2bs(double value) {
		return ByteBuffer.allocate(8).putDouble(value).array();
	}
}
