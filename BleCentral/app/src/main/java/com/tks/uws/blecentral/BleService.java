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
import android.os.RemoteException;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tks.uws.blecentral.Constants.UWS_CHARACTERISTIC_HRATBEAT_UUID;
import static com.tks.uws.blecentral.Constants.UWS_SERVICE_UUID;

//-30 dBm	素晴らしい	達成可能な最大信号強度。クライアントは、これを実現するには、APから僅か数フィートである必要があります。現実的には一般的ではなく、望ましいものでもありません	N/A
//-60 dBm	良好	非常に信頼性の高く、データパケットのタイムリーな伝送を必要とするアプリケーションのための最小信号強度	VoIP/VoWiFi, ストリーミングビデオ
//-70 dBm	Ok	信頼できるパケット伝送に必要な最小信号強度	Email, web
//-80 dBm	よくない	基本的なコネクティビティに必要な最小信号強度。パケット伝送は信頼できない可能性があります	N/A
//-90 dBm	使用不可	ノイズレベルに近いかそれ以下の信号強度。殆ど機能しない	N/A

public class BleService extends Service {
	/* Binder */
	private IBleServiceCallback	mListener;	/* 常に後発のみ */
	private Binder binder = new IBleService.Stub() {
		@Override
		public void setCallback(IBleServiceCallback callback) throws RemoteException {
			TLog.d("callback={0}", callback);
			mListener = callback;
		}

		@Override
		public int initBle() throws RemoteException {
			return BsvInit();
		}

		@Override
		public int startScan() throws RemoteException {
			return BsvStartScan();
		}

		@Override
		public int stopScan() throws RemoteException {
			return BsvStopScan();
		}

		@Override
		public List<ScanResult> getScanResultlist() throws RemoteException {
			return mTmpScanResultList;
		}

		@Override
		public ScanResult getScanResult() throws RemoteException {
			return mTmpScanResult;
		}

		@Override
		public int connectDevice(String deviceAddress) throws RemoteException {
			return BsvConnectDevice(deviceAddress);
		}
	};

	@Override
	public IBinder onBind(Intent intent) {
		TLog.d("onBind()");
		return binder;
	}

	/* BLuetooth定義 */
	private BluetoothAdapter	mBluetoothAdapter;
	private BluetoothLeScanner	mBLeScanner;
	/* メッセージID */
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

//	public final static String UWS_GATT_CONNECTED			= "com.tks.uws.blecentral.GATT_CONNECTED";
//	public final static String UWS_GATT_DISCONNECTED		= "com.tks.uws.blecentral.GATT_DISCONNECTED";
//	public final static String UWS_GATT_SERVICES_DISCOVERED	= "com.tks.uws.blecentral.GATT_SERVICES_DISCOVERED";
	public final static String UWS_DATA_AVAILABLE			= "com.tks.uws.blecentral.DATA_AVAILABLE";
	public final static String UWS_DATA						= "com.tks.uws.blecentral.DATA";

	int BsvInit() {
		/* Bluetooth権限なし */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			return UWS_NG_PERMISSION_DENIED;
		TLog.d( "Bluetooth権限OK.");

		/* Bluetoothサービス取得 */
		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null)
			return UWS_NG_SERVICE_NOTFOUND;

		/* Bluetoothアダプタ取得 */
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (mBluetoothAdapter == null)
			return UWS_NG_ADAPTER_NOTFOUND;

		/* Bluetooth ON */
		else if( !mBluetoothAdapter.isEnabled())
			return UWS_NG_BT_OFF;
		TLog.d( "Bluetooth ON.");

		return UWS_NG_SUCCESS;
	}

	/* Bluetoothサービス-scan開始 */
	private ScanResult			mTmpScanResult;
	private List<ScanResult>	mTmpScanResultList = new ArrayList<>();
	private ScanCallback		mScanCallback = null;
	private int BsvStartScan() {
		/* 既にscan中 */
		if(mScanCallback != null)
			return UWS_NG_ALREADY_SCANNED;

		/* Bluetooth機能がOFF */
		if( !mBluetoothAdapter.isEnabled())
			return UWS_NG_BT_OFF;

		TLog.d("Bluetooth ON.");

		TLog.d("scan開始");
		mScanCallback = new ScanCallback() {
			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				super.onBatchScanResults(results);
				mTmpScanResultList = results;
				try { mListener.notifyScanResultlist();}
				catch (RemoteException e) {e.printStackTrace();}
//					mDeviceListAdapter.addDevice(results);
//				for(ScanResult result : results) {
//					TLog.d("---------------------------------- size=" + results.size());
//					TLog.d("aaaaa AdvertisingSid             =" + result.getAdvertisingSid());
//					TLog.d("aaaaa device                     =" + result.getDevice());
//					TLog.d("            Name                 =" + result.getDevice().getName());
//					TLog.d("            Address              =" + result.getDevice().getAddress());
//					TLog.d("            Class                =" + result.getDevice().getClass());
//					TLog.d("            BluetoothClass       =" + result.getDevice().getBluetoothClass());
//					TLog.d("            BondState            =" + result.getDevice().getBondState());
//					TLog.d("            Type                 =" + result.getDevice().getType());
//					TLog.d("            Uuids                =" + result.getDevice().getUuids());
//					TLog.d("aaaaa DataStatus                 =" + result.getDataStatus());
//					TLog.d("aaaaa PeriodicAdvertisingInterval=" + result.getPeriodicAdvertisingInterval());
//					TLog.d("aaaaa PrimaryPhy                 =" + result.getPrimaryPhy());
//					TLog.d("aaaaa Rssi                       =" + result.getRssi());
//					TLog.d("aaaaa ScanRecord                 =" + result.getScanRecord());
//					TLog.d("aaaaa TimestampNanos             =" + result.getTimestampNanos());
//					TLog.d("aaaaa TxPower                    =" + result.getTxPower());
//					TLog.d("aaaaa Class                      =" + result.getClass());
//					TLog.d("----------------------------------");
//				}
			}

			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				super.onScanResult(callbackType, result);
				mTmpScanResult = result;
				if(result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null)
					TLog.d("発見!! {0}({1}):Rssi({2}) Uuids({3})", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi(), result.getScanRecord().getServiceUuids().toString());
				else
					TLog.d("発見!! {0}({1}):Rssi({2})", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi());
				try { mListener.notifyScanResult();}
				catch (RemoteException e) {e.printStackTrace();}
//				if(result !=null && result.getDevice() != null) {
//					TLog.d("----------------------------------");
//					TLog.d("aaaaa AdvertisingSid             =" + result.getAdvertisingSid());				//aaaaa AdvertisingSid             =255
//					TLog.d("aaaaa device                     =" + result.getDevice());						//aaaaa device                     =65:57:FE:6F:E6:D9
//					TLog.d("            Name                 =" + result.getDevice().getName());				//            Name                 =null
//					TLog.d("            Address              =" + result.getDevice().getAddress());			//            Address              =65:57:FE:6F:E6:D9
//					TLog.d("            Class                =" + result.getDevice().getClass());				//            Class                =class android.bluetooth.BluetoothDevice
//					TLog.d("            BluetoothClass       =" + result.getDevice().getBluetoothClass());	//            BluetoothClass       =0
//					TLog.d("            BondState            =" + result.getDevice().getBondState());			//            BondState            =10
//					TLog.d("            Type                 =" + result.getDevice().getType());				//            Type                 =0
//					TLog.d("            Uuids                =" + result.getDevice().getUuids());				//            Uuids                =null
//					TLog.d("aaaaa DataStatus                 =" + result.getDataStatus());					//aaaaa DataStatus                 =0
//					TLog.d("aaaaa PeriodicAdvertisingInterval=" + result.getPeriodicAdvertisingInterval());	//aaaaa PeriodicAdvertisingInterval=0
//					TLog.d("aaaaa PrimaryPhy                 =" + result.getPrimaryPhy());					//aaaaa PrimaryPhy                 =1
//					TLog.d("aaaaa Rssi                       =" + result.getRssi());							//aaaaa Rssi                       =-79
//					TLog.d("aaaaa ScanRecord                 =" + result.getScanRecord());					//aaaaa ScanRecord                 =ScanRecord [mAdvertiseFlags=26, mServiceUuids=null, mServiceSolicitationUuids=[], mManufacturerSpecificData={76=[16, 5, 25, 28, 64, 39, -24]}, mServiceData={}, mTxPowerLevel=12, mDeviceName=null]
//					TLog.d("aaaaa TimestampNanos             =" + result.getTimestampNanos());				//aaaaa TimestampNanos             =13798346768432
//					TLog.d("aaaaa TxPower                    =" + result.getTxPower());						//aaaaa TxPower                    =127
//					TLog.d("aaaaa Class                      =" + result.getClass());							//aaaaa Class                      =class android.bluetooth.le.ScanResult
//					TLog.d("----------------------------------");
//				}
			}

			@Override
			public void onScanFailed(int errorCode) {
				super.onScanFailed(errorCode);
				TLog.d("scan失敗!! errorCode=" + errorCode);
			}
		};

		/* scanフィルタ */
		List<ScanFilter> scanFilters = new ArrayList<>();
		scanFilters.add(new ScanFilter.Builder().build());

		/* scan設定 */
		ScanSettings.Builder scansetting = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);

		/* scan開始 */
		mBLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
		mBLeScanner.startScan(scanFilters, scansetting.build(), mScanCallback);

		return UWS_NG_SUCCESS;
	}

	/* Bluetoothサービス-scan停止 */
	private int BsvStopScan() {
		if(mScanCallback == null)
			return UWS_NG_ALREADY_SCANSTOPEDNED;

		mBLeScanner.stopScan(mScanCallback);
		mScanCallback = null;
		TLog.d("scan終了");
		try { mListener.notifyScanEnd();}
		catch (RemoteException e) { e.printStackTrace(); }

		return UWS_NG_SUCCESS;
	}

	/* Bluetoothサービス-デバイス接続 */
	private final Map<String, BluetoothGatt>	mConnectedDevices = new HashMap<>();
	private int BsvConnectDevice(String deviceAddress) {
		if(deviceAddress == null || deviceAddress.equals(""))
			return UWS_NG_ILLEGALARGUMENT;

		/* デバイスリスト検索 */
		BluetoothGatt gat = mConnectedDevices.get(deviceAddress);

		/* 再接続 */
		if(gat != null) {
			TLog.d("デバイス再接続({0})", deviceAddress);
			boolean ret = gat.connect();
			if(ret)
				return UWS_NG_RECONNECT_OK;
			/* デバイスなしなので、削除 */
			gat.disconnect();
			gat.close();
			mConnectedDevices.remove(deviceAddress);
			return UWS_NG_DEVICE_NOTFOUND;
		}

		TLog.d("デバイス初回接続({0})", deviceAddress);
		/* 初回接続 */
		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(deviceAddress);
		if (device == null) {
			TLog.d("デバイス({0})が見つかりません。接続できませんでした。", deviceAddress);
			return UWS_NG_DEVICE_NOTFOUND;
		}

		/* デバイスに直接接続したい時に、autoConnectをfalseにする。 */
		/* デバイスが使用可能になったら自動的にすぐに接続する様にする時に、autoConnectをtrueにする。 */
		BluetoothGatt blegatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
		if (blegatt == null) {
			TLog.d("Gattサーバ接続失敗!! address={0}", deviceAddress);
			return UWS_NG_DEVICE_NOTFOUND;
		}

		/* Gatt接続中 */
		TLog.d("Gattサーバ接続成功. address={0} gatt'sAddress={1}", deviceAddress, blegatt.getDevice().getAddress());

		return UWS_NG_SUCCESS;
	}

	BluetoothGattCharacteristic mCharacteristic = null;
	private final BluetoothGattCallback	mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			TLog.d("BluetoothGattCallback::onConnectionStateChange() {0} -> {1}", status, newState);
			TLog.d("BluetoothProfile.STATE_CONNECTING({0}) STATE_CONNECTED({1}) STATE_DISCONNECTING({2}) STATE_DISCONNECTED({3})", BluetoothProfile.STATE_CONNECTING, BluetoothProfile.STATE_CONNECTED, BluetoothProfile.STATE_DISCONNECTING, BluetoothProfile.STATE_DISCONNECTED);
			/* Gattサーバ接続完了 */
			if(newState == BluetoothProfile.STATE_CONNECTED) {
				try { mListener.notifyGattConnected(gatt.getDevice().getAddress()); }
				catch (RemoteException e) { e.printStackTrace(); }
				TLog.d("GATTサーバ接続OK. address={0}", gatt.getDevice().getAddress());
				gatt.discoverServices();
				TLog.d("Discovery開始 address={0}", gatt.getDevice().getAddress());
			}
			/* Gattサーバ断 */
			else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				try { mListener.notifyGattDisConnected(gatt.getDevice().getAddress()); }
				catch (RemoteException e) { e.printStackTrace(); }
				TLog.d("GATTサーバ断. address={0}", gatt.getDevice().getAddress());
				mConnectedDevices.remove(gatt.getDevice().getAddress());
				gatt.disconnect();
				gatt.close();
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			TLog.d("Services発見!! address={0} ret={1}", gatt.getDevice().getAddress(), status);
			try { mListener.notifyServicesDiscovered(gatt.getDevice().getAddress(), status); }
			catch (RemoteException e) { e.printStackTrace(); }

			if (status == BluetoothGatt.GATT_SUCCESS) {
				mCharacteristic = findTerget(gatt, UWS_SERVICE_UUID, UWS_CHARACTERISTIC_HRATBEAT_UUID);
				if (mCharacteristic != null) {
					try { mListener.notifyApplicable(gatt.getDevice().getAddress(), true); }
					catch (RemoteException e) { e.printStackTrace(); }

					TLog.d("find it. Services and Characteristic.");
					boolean ret1 = gatt.readCharacteristic(mCharacteristic);
					boolean ret2 = gatt.setCharacteristicNotification(mCharacteristic, true);
					if(ret1 && ret2) {
						TLog.d("BLEデバイス通信 準備完了. address={0}", gatt.getDevice().getAddress());
						try { mListener.notifyReady2DeviceCommunication(gatt.getDevice().getAddress(), true); }
						catch (RemoteException e) { e.printStackTrace(); }
						mConnectedDevices.put(gatt.getDevice().getAddress(), gatt);
						return;
					}
					else {
						TLog.d("BLEデバイス通信 準備失敗!! address={0}", gatt.getDevice().getAddress());
						try { mListener.notifyReady2DeviceCommunication(gatt.getDevice().getAddress(), false); }
						catch (RemoteException e) { e.printStackTrace(); }
						mConnectedDevices.remove(gatt.getDevice().getAddress());
						gatt.disconnect();
						gatt.close();
					}
				}
				else {
					TLog.d("対象外デバイス!! address={0}", gatt.getDevice().getAddress());
					try { mListener.notifyApplicable(gatt.getDevice().getAddress(), false); }
					catch (RemoteException e) { e.printStackTrace(); }
					mConnectedDevices.remove(gatt.getDevice().getAddress());
					gatt.disconnect();
					gatt.close();
				}
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

	private BluetoothGattCharacteristic findTerget(BluetoothGatt gatt, UUID ServiceUuid, UUID CharacteristicUuid) {
		BluetoothGattCharacteristic ret = null;
		for (BluetoothGattService service : gatt.getServices()) {
			/*-*-----------------------------------*/
			for(BluetoothGattCharacteristic gattChara : service.getCharacteristics())
				TLog.d("{0} : service-UUID={1} Chara-UUID={2}", gatt.getDevice().getAddress(), service.getUuid(), gattChara.getUuid());
			/*-*-----------------------------------*/
			if( !service.getUuid().equals(ServiceUuid))
				continue;

			final List<BluetoothGattCharacteristic> findc = service.getCharacteristics().stream().filter(c -> {
				return c.getUuid().equals(CharacteristicUuid);
			}).collect(Collectors.toList());
			/* 見つかった最後の分が有効なので、上書きする */
			ret = findc.get(0);
		}
		return ret;
	}












	/* データ受信(peripheral -> Service -> Activity) */
	private void parseRcvData(final String action, final BluetoothGattCharacteristic characteristic) {
		Intent intent = new Intent(action);
		if (UWS_CHARACTERISTIC_HRATBEAT_UUID.equals(characteristic.getUuid())) {
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
//		disconnectBleDevice();
		return super.onUnbind(intent);
	}

//	/* Bluetooth接続 */
//	private int connectBleDevice(final String address) {
//		if (address == null) {
//			TLog.d("デバイスアドレスなし");
//			return UWS_NG_DEVICE_NOTFOUND;
//		}
//
//		BluetoothManager btManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
//		if(btManager == null) return UWS_NG_SERVICE_NOTFOUND;
//
//		BluetoothAdapter btAdapter = btManager.getAdapter();
//		if(btAdapter == null) return UWS_NG_ADAPTER_NOTFOUND;
//
//		/* 再接続処理 */
//		if (address.equals(mBleDeviceAddr) && mBleGatt != null) {
//			TLog.d("");
//			if (mBleGatt.connect()) {
////				mConnectionState = STATE_CONNECTING;
//				TLog.d("接続済のデバイスに再接続。成功しました。");
//				return UWS_NG_SUCCESS;
//			}
//			else {
//				TLog.d("接続済のデバイスに再接続。失敗しました。");
//				return UWS_NG_DEVICE_NOTFOUND;
//			}
//		}
//
//		/* 初回接続 */
//		BluetoothDevice device = btAdapter.getRemoteDevice(address);
//		if (device == null) {
//			TLog.d("デバイス({0})が見つかりません。接続できませんでした。", address);
//			return UWS_NG_DEVICE_NOTFOUND;
//		}
//
//		/* デバイスに直接接続したい時に、autoConnectをfalseにする。 */
//		/* デバイスが使用可能になったら自動的にすぐに接続する様にする時に、autoConnectをtrueにする。 */
//		mBleGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
//		mBleDeviceAddr = address;
////		mConnectionState = STATE_CONNECTING;
//		TLog.d("GATTサーバ接続開始.address={0}", address);
//
//		return UWS_NG_SUCCESS;
//	}

//	public void disconnectBleDevice() {
//		if (mBleGatt != null) {
//			mBleGatt.disconnect();
//			mBleGatt.close();
//			mBleGatt = null;
//		}
//	}

//	public void readCharacteristic(BluetoothGattCharacteristic characteristic) {
//		if (mBleGatt == null) {
//			TLog.d("Bluetooth not initialized");
//			throw new IllegalStateException("Error!! Bluetooth not initialized!!");
//		}
//
//		mBleGatt.readCharacteristic(characteristic);
//	}

//	/* 指定CharacteristicのCallback登録 */
//	public void setCharacteristicNotification(BluetoothGattCharacteristic characteristic, boolean enabled) {
//		TLog.d("setCharacteristicNotification() s");
//		if (mBleGatt == null) {
//			TLog.d("Bluetooth not initialized");
//			throw new IllegalStateException("Error!! Bluetooth not initialized!!");
//		}
//
//		mBleGatt.setCharacteristicNotification(characteristic, enabled);
//		TLog.d("setCharacteristicNotification() e");
//	}

//	/* 対象デバイスの保有するサービスを取得 */
//	public List<BluetoothGattService> getSupportedGattServices() {
//		if (mBleGatt == null)
//			return null;
//		return mBleGatt.getServices();
//	}
}