package com.tks.uws.blecentral;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;

import static com.tks.uws.blecentral.Constants.UWS_CHARACTERISTIC_SAMLE_UUID;


public class BleService extends Service {
	/* TOOD ↓↓↓デバッグ用 */
	private String mAddStr = "new item";
	/* TOOD ↑↑↑デバッグ用 */

	/* Binder */
	private IBleServiceCallback	mListener;	/* 常に後発のみ */
	private Binder binder = new IBleService.Stub() {
		@Override
		public void setCallback(IBleServiceCallback callback) throws RemoteException {
			TLog.d("callback={0}", callback);
			mListener = callback;
		}

		@Override
		public void setAddStr(String str) throws RemoteException {
			mAddStr = str;
		}

		@Override
		public int initBle() throws RemoteException {
			return UWS_NG_SUCCESS;
		}

		@Override
		public int startScan() throws RemoteException {
			return BsvStartScan();
		}

		@Override
		public int stopScan() throws RemoteException {
			return stopBLEScan();
		}

		@Override
		public List<ScanResult> getScanResultlist() throws RemoteException {
			return mScanResultList;
		}

		@Override
		public ScanResult getScanResult() throws RemoteException {
			return mScanResult;
		}
	};

	public final static int UWS_MSGID_DEBUG			= 0x1999;	/* デバッグメッセージ */
	@Override
	public IBinder onBind(Intent intent) {
		TLog.d("onBind()");
		dddhandler.sendEmptyMessageDelayed(UWS_MSGID_DEBUG, 1000);

		mHandler = new Handler(Looper.getMainLooper());

		/* Bluetooth初期化(サービス取得) */
		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null) {
			Intent resintent = new Intent(UWS_SERVICE_WAKEUP_NG);
			resintent.putExtra(UWS_KEY_WAKEUP_NG_REASON, UWS_NG_SERVICE_NOTFOUND);
			sendBroadcast(resintent);
			stopSelf();	/* サービス未起動失敗にする */
			return null;
		}

		mBluetoothAdapter = bluetoothManager.getAdapter();
		/* Bluetooth未サポート判定 未サポートならエラーpopupで終了 */
		if (mBluetoothAdapter == null) {
			Intent resintent = new Intent(UWS_SERVICE_WAKEUP_NG);
			resintent.putExtra(UWS_KEY_WAKEUP_NG_REASON, UWS_NG_ADAPTER_NOTFOUND);
			sendBroadcast(resintent);
			stopSelf();	/* サービス未起動失敗にする */
			return null;
		}
		/* Bluetooth ON/OFF判定 -> OFFならONにするようにリクエスト */
		else if( !mBluetoothAdapter.isEnabled()) {
			Intent resintent = new Intent(UWS_SERVICE_WAKEUP_NG);
			resintent.putExtra(UWS_KEY_WAKEUP_NG_REASON, UWS_NG_BT_OFF);
			sendBroadcast(resintent);
			stopSelf();	/* サービス未起動失敗にする */
			return null;
		}

		return binder;
	}

	private Handler dddhandler = new Handler(Looper.getMainLooper()) {
		@Override
		public void handleMessage(@NonNull Message msg) {
			TLog.d("handleMessage() msg={0} mListeners={1}", msg, mListener);
			try { mListener.onItemAdded(mAddStr); }
			catch (RemoteException e) { e.printStackTrace(); return; }
			sendEmptyMessageDelayed(UWS_MSGID_DEBUG, 3000);
		}
	};

	/* BLuetooth定義 */
	private BluetoothAdapter	mBluetoothAdapter;
	private BluetoothLeScanner	mBLeScanner;
	private Handler				mHandler;
	private final static long	SCAN_PERIOD = 30000;	/* m秒 */
	/* メッセージID */
	public final static String UWS_SERVICE_WAKEUP_OK		= "com.tks.uws.blecentral.SERVICE_WAKEUP_OK";
	public final static String UWS_SERVICE_WAKEUP_NG		= "com.tks.uws.blecentral.SERVICE_WAKEUP_NG";
	public final static String UWS_KEY_WAKEUP_NG_REASON		= "com.tks.uws.blecentral.SERVICE_WAKEUP_NG_REASON";
	public final static int UWS_NG_SUCCESS			= 0;	/* OK */
	public final static int UWS_NG_SERVICE_NOTFOUND	= -1;	/* サービスが見つからない(=Bluetooth未サポ－ト) */
	public final static int UWS_NG_ADAPTER_NOTFOUND	= -2;	/* BluetoothAdapterがnull(=Bluetooth未サポ－ト) */
	public final static int UWS_NG_BT_OFF			= -3;	/* Bluetooth機能がOFF */
	public final static int UWS_NG_ALREADY_SCANNED	= -4;	/* 既にscan中 */
	public final static int UWS_NG_PERMISSION_DENIED= -5;	/* 権限なし */
	public final static int    UWS_NG_REASON_CONNECTBLE			= -5;
	public final static int    UWS_NG_REASON_DEVICENOTFOUND		= -6;
	public final static String UWS_GATT_CONNECTED			= "com.tks.uws.blecentral.GATT_CONNECTED";
	public final static String UWS_GATT_DISCONNECTED		= "com.tks.uws.blecentral.GATT_DISCONNECTED";
	public final static String UWS_GATT_SERVICES_DISCOVERED	= "com.tks.uws.blecentral.GATT_SERVICES_DISCOVERED";
	public final static String UWS_DATA_AVAILABLE			= "com.tks.uws.blecentral.DATA_AVAILABLE";
	public final static String UWS_DATA						= "com.tks.uws.blecentral.DATA";

	/* Ble scan開始 */
	private ScanCallback		mScanCallback = null;
	private List<ScanResult>	mScanResultList = new ArrayList<>();
	private ScanResult			mScanResult;
	private int BsvStartScan() {
		if(mScanCallback != null) {
			TLog.d("すでにscan中。");
			try { mListener.notifyError(UWS_NG_ALREADY_SCANNED, "すでにscan中。");}
			catch (RemoteException e) {e.printStackTrace();}
			return UWS_NG_ALREADY_SCANNED;
		}

		/* Bluetooth機能がONになってないのでreturn */
		if( !mBluetoothAdapter.isEnabled()) {
			TLog.d("Bluetooth機能がOFFってる。");
			try { mListener.notifyError(UWS_NG_BT_OFF, "Bluetooth機能がOFFってる。");}
			catch (RemoteException e) {e.printStackTrace();}
			return UWS_NG_BT_OFF;
		}

		TLog.d("Bluetooth ON.");

		/* Bluetooth使用の権限がないのでreturn */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			TLog.d( "Bluetooth使用の権限なし");
			try { mListener.notifyError(UWS_NG_PERMISSION_DENIED, "Bluetooth使用の権限なし");}
			catch (RemoteException e) {e.printStackTrace();}
			return UWS_NG_PERMISSION_DENIED;
		}
		TLog.d( "Bluetooth使用権限OK.");

		TLog.d("scan開始");
		mScanCallback = new ScanCallback() {
			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				super.onBatchScanResults(results);
				mScanResultList = results;
				try { mListener.notifyScanResultlist();}
				catch (RemoteException e) {e.printStackTrace();}
//					mDeviceListAdapter.addDevice(results);
				for(ScanResult result : results) {
					TLog.d("---------------------------------- size=" + results.size());
					TLog.d("aaaaa AdvertisingSid             =" + result.getAdvertisingSid());
					TLog.d("aaaaa device                     =" + result.getDevice());
					TLog.d("            Name                 =" + result.getDevice().getName());
					TLog.d("            Address              =" + result.getDevice().getAddress());
					TLog.d("            Class                =" + result.getDevice().getClass());
					TLog.d("            BluetoothClass       =" + result.getDevice().getBluetoothClass());
					TLog.d("            BondState            =" + result.getDevice().getBondState());
					TLog.d("            Type                 =" + result.getDevice().getType());
					TLog.d("            Uuids                =" + result.getDevice().getUuids());
					TLog.d("aaaaa DataStatus                 =" + result.getDataStatus());
					TLog.d("aaaaa PeriodicAdvertisingInterval=" + result.getPeriodicAdvertisingInterval());
					TLog.d("aaaaa PrimaryPhy                 =" + result.getPrimaryPhy());
					TLog.d("aaaaa Rssi                       =" + result.getRssi());
					TLog.d("aaaaa ScanRecord                 =" + result.getScanRecord());
					TLog.d("aaaaa TimestampNanos             =" + result.getTimestampNanos());
					TLog.d("aaaaa TxPower                    =" + result.getTxPower());
					TLog.d("aaaaa Class                      =" + result.getClass());
					TLog.d("----------------------------------");
				}
			}

			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				super.onScanResult(callbackType, result);
				mScanResult = result;
				if(result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null)
					TLog.d("発見!! {0}({1}):Rssi({2}) Uuids({3})", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi(), result.getScanRecord().getServiceUuids().toString());
				else
					TLog.d("発見!! {0}({1}):Rssi({2})", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi());
				TLog.d("mListene={0}", mListener);
				try { mListener.notifyScanResult();}
				catch (RemoteException e) {e.printStackTrace();}
				if(result !=null && result.getDevice() != null) {
					TLog.d("----------------------------------");
					TLog.d("aaaaa AdvertisingSid             =" + result.getAdvertisingSid());				//aaaaa AdvertisingSid             =255
					TLog.d("aaaaa device                     =" + result.getDevice());						//aaaaa device                     =65:57:FE:6F:E6:D9
					TLog.d("            Name                 =" + result.getDevice().getName());				//            Name                 =null
					TLog.d("            Address              =" + result.getDevice().getAddress());			//            Address              =65:57:FE:6F:E6:D9
					TLog.d("            Class                =" + result.getDevice().getClass());				//            Class                =class android.bluetooth.BluetoothDevice
					TLog.d("            BluetoothClass       =" + result.getDevice().getBluetoothClass());	//            BluetoothClass       =0
					TLog.d("            BondState            =" + result.getDevice().getBondState());			//            BondState            =10
					TLog.d("            Type                 =" + result.getDevice().getType());				//            Type                 =0
					TLog.d("            Uuids                =" + result.getDevice().getUuids());				//            Uuids                =null
					TLog.d("aaaaa DataStatus                 =" + result.getDataStatus());					//aaaaa DataStatus                 =0
					TLog.d("aaaaa PeriodicAdvertisingInterval=" + result.getPeriodicAdvertisingInterval());	//aaaaa PeriodicAdvertisingInterval=0
					TLog.d("aaaaa PrimaryPhy                 =" + result.getPrimaryPhy());					//aaaaa PrimaryPhy                 =1
					TLog.d("aaaaa Rssi                       =" + result.getRssi());							//aaaaa Rssi                       =-79
					TLog.d("aaaaa ScanRecord                 =" + result.getScanRecord());					//aaaaa ScanRecord                 =ScanRecord [mAdvertiseFlags=26, mServiceUuids=null, mServiceSolicitationUuids=[], mManufacturerSpecificData={76=[16, 5, 25, 28, 64, 39, -24]}, mServiceData={}, mTxPowerLevel=12, mDeviceName=null]
					TLog.d("aaaaa TimestampNanos             =" + result.getTimestampNanos());				//aaaaa TimestampNanos             =13798346768432
					TLog.d("aaaaa TxPower                    =" + result.getTxPower());						//aaaaa TxPower                    =127
					TLog.d("aaaaa Class                      =" + result.getClass());							//aaaaa Class                      =class android.bluetooth.le.ScanResult
					TLog.d("----------------------------------");
				}
			}

			@Override
			public void onScanFailed(int errorCode) {
				super.onScanFailed(errorCode);
				TLog.d("scan失敗!! errorCode=" + errorCode);
			}
		};

		mBLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
		mHandler.postDelayed(() -> {
			mBLeScanner.stopScan(mScanCallback);
			mScanCallback = null;
			TLog.d("scan終了");
			TLog.d("mListene={0}", mListener);
			try { mListener.notifyError(UWS_NG_SUCCESS, "scan終了");}
			catch (RemoteException e) { e.printStackTrace(); }
		}, SCAN_PERIOD);

		/* scanフィルタ */
		List<ScanFilter> scanFilters = new ArrayList<>();
		scanFilters.add(new ScanFilter.Builder().build());

		/* scan設定 */
		ScanSettings.Builder scansetting = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);

		/* scan開始 */
		mBLeScanner.startScan(scanFilters, scansetting.build(), mScanCallback);

		return UWS_NG_SUCCESS;
	}

	private int stopBLEScan() {
		return UWS_NG_SUCCESS;
	}















	/* Serviceのお約束 */
	private BluetoothGatt				mBleGatt;
	private String						mBleDeviceAddr;
	private final BluetoothGattCallback	mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			TLog.d("BluetoothGattCallback::onConnectionStateChange() {0} -> {1}", status, newState);
			TLog.d("BluetoothProfile.STATE_CONNECTING({0}) STATE_CONNECTED({1}) STATE_DISCONNECTING({2}) STATE_DISCONNECTED({3})", BluetoothProfile.STATE_CONNECTING, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED);
			/* Gattサーバ接続完了 */
			if(newState == BluetoothProfile.STATE_CONNECTED) {
//				mConnectionState = STATE_CONNECTED;
				/* Intent送信(Service->Activity) */
				sendBroadcast(new Intent(UWS_GATT_CONNECTED));
				TLog.d("GATTサーバ接続OK.");
				mBleGatt.discoverServices();
				TLog.d("Discovery開始");
			}
			/* Gattサーバ断 */
			else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
//				mConnectionState = STATE_DISCONNECTED;
				TLog.d("GATTサーバ断.");
				/* Intent送信(Service->Activity) */
				sendBroadcast(new Intent(UWS_GATT_DISCONNECTED));
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			TLog.d("Discovery終了 result : {0}", status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				/* Intent送信(Service->Activity) */
				sendBroadcast(new Intent(UWS_GATT_SERVICES_DISCOVERED));
			}
		}

		/* 読込み要求の応答 */
		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			TLog.d("読込み要求の応答 status=", status);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				parseRcvData(UWS_DATA_AVAILABLE, characteristic);
			}
			else {
				TLog.d("onCharacteristicRead GATT_FAILURE");
			}
			TLog.d("BluetoothGattCallback::onCharacteristicRead() e");
		}

		@Override
		public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
			TLog.d("ペリフェラルからの受信");
			parseRcvData(UWS_DATA_AVAILABLE, characteristic);
		}
	};

	/* データ受信(peripheral -> Service -> Activity) */
	private void parseRcvData(final String action, final BluetoothGattCharacteristic characteristic) {
		Intent intent = new Intent(action);
		if (UWS_CHARACTERISTIC_SAMLE_UUID.equals(characteristic.getUuid())) {
			/* 受信データ取出し */
			int flag = characteristic.getProperties();
			int format = ((flag & 0x01) != 0) ? BluetoothGattCharacteristic.FORMAT_UINT16 : BluetoothGattCharacteristic.FORMAT_UINT8;
			int msg = characteristic.getIntValue(format, 0);
			TLog.d("message: {0}", msg);
			/* 受信データ取出し */
			intent.putExtra(UWS_DATA, msg);
		}
		/* ↓↓↓この処理はサンプルコード。実際には動かない。 */
		else {
			final byte[] data = characteristic.getValue();
			if (data != null && data.length > 0) {
				final StringBuilder stringBuilder = new StringBuilder(data.length);
				for (byte byteChar : data)
					stringBuilder.append(String.format("%02X ", byteChar));
				intent.putExtra(UWS_DATA, -1);
			}
		}
		/* ↑↑↑この処理はサンプルコード。実際には動かない。 */
		/* 受信データ中継 */
		sendBroadcast(intent);
	}

	@Override
	public boolean onUnbind(Intent intent) {
		disconnectBleDevice();
		return super.onUnbind(intent);
	}

	/* Bluetooth接続 */
	private int connectBleDevice(final String address) {
		if (address == null) {
			TLog.d("デバイスアドレスなし");
			return UWS_NG_REASON_DEVICENOTFOUND;
		}

		BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if(btManager == null) return UWS_NG_SERVICE_NOTFOUND;

		BluetoothAdapter btAdapter = btManager.getAdapter();
		if(btAdapter == null) return UWS_NG_ADAPTER_NOTFOUND;

		/* 再接続処理 */
		if (address.equals(mBleDeviceAddr) && mBleGatt != null) {
			TLog.d("");
			if (mBleGatt.connect()) {
//				mConnectionState = STATE_CONNECTING;
				TLog.d("接続済のデバイスに再接続。成功しました。");
				return UWS_NG_SUCCESS;
			}
			else {
				TLog.d("接続済のデバイスに再接続。失敗しました。");
				return UWS_NG_REASON_DEVICENOTFOUND;
			}
		}

		/* 初回接続 */
		BluetoothDevice device = btAdapter.getRemoteDevice(address);
		if (device == null) {
			TLog.d("デバイス({0})が見つかりません。接続できませんでした。", address);
			return UWS_NG_REASON_DEVICENOTFOUND;
		}

		/* デバイスに直接接続したい時に、autoConnectをfalseにする。 */
		/* デバイスが使用可能になったら自動的にすぐに接続する様にする時に、autoConnectをtrueにする。 */
		mBleGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
		mBleDeviceAddr = address;
//		mConnectionState = STATE_CONNECTING;
		TLog.d("GATTサーバ接続開始.address={0}", address);

		return UWS_NG_SUCCESS;
	}

	public void disconnectBleDevice() {
		if (mBleGatt != null) {
			mBleGatt.disconnect();
			mBleGatt.close();
			mBleGatt = null;
		}
	}

	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
		if (mBleGatt == null) {
			TLog.d("Bluetooth not initialized");
			throw new IllegalStateException("Error!! Bluetooth not initialized!!");
		}

		mBleGatt.readCharacteristic(characteristic);
	}

	/* 指定CharacteristicのCallback登録 */
	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
		TLog.d("setCharacteristicNotification() s");
		if (mBleGatt == null) {
			TLog.d("Bluetooth not initialized");
			throw new IllegalStateException("Error!! Bluetooth not initialized!!");
		}

		mBleGatt.setCharacteristicNotification(characteristic, enabled);
		TLog.d("setCharacteristicNotification() e");
	}

	/* 対象デバイスの保有するサービスを取得 */
	public List<BluetoothGattService> getSupportedGattServices() {
		if (mBleGatt == null)
			return null;
		return mBleGatt.getServices();
	}
}
