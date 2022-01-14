package com.tks.uwsserverunit00;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.tks.uwsserverunit00.Constants.UWS_NG_DEVICE_NOTFOUND;
import static com.tks.uwsserverunit00.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsserverunit00.Constants.UWS_OWNDATA_KEY;
import static com.tks.uwsserverunit00.Constants.UWS_UUID_CHARACTERISTIC_HRATBEAT;
import static com.tks.uwsserverunit00.Constants.UWS_UUID_SERVICE;
import static com.tks.uwsserverunit00.Constants.UWS_SERVICE_STATUS_GATT_DISCONNECT;
/**
 * -30 dBm	素晴らしい	達成可能な最大信号強度。クライアントは、これを実現するには、APから僅か数フィートである必要があります。現実的には一般的ではなく、望ましいものでもありません	N/A
 * -60 dBm	良好	非常に信頼性の高く、データパケットのタイムリーな伝送を必要とするアプリケーションのための最小信号強度	VoIP/VoWiFi, ストリーミングビデオ
 * -70 dBm	Ok	信頼できるパケット伝送に必要な最小信号強度	Email, web
 * -80 dBm	よくない	基本的なコネクティビティに必要な最小信号強度。パケット伝送は信頼できない可能性があります	N/A
 * -90 dBm	使用不可	ノイズレベルに近いかそれ以下の信号強度。殆ど機能しない	N/A
 **/

public class UwsServerService extends Service {
	private BluetoothAdapter			mBluetoothAdapter;
	private final Handler				mHandler = new Handler();

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		TLog.d("");
		return mBinder;
	}

	/** *****
	 * Binder
	 * ******/
	private Binder mBinder = new IUwsServer.Stub() {
		@Override
		public int initBle() {
			return uwsInit();
		}

		@Override
		public int startScan(IUwsScanCallback callback) {
			return uwsStartScan(callback);
		}

		@Override
		public void stopScan() {
			uwsStopScan();
		}

		@Override
		public int startPeriodicNotify(int seekerid, IUwsInfoCallback callback) {
			return uwsStartPeriodicNotify((short)seekerid, callback);
		}

		@Override
		public void stopPeriodicNotify(int seekerid) {
			uwsStopPeriodicNotify((short)seekerid);
		}
	};

	/* ********
	 * BLE処理
	 * ********/
	Map<Short/*seekerid*/, Tuple3<String/*address*/, BluetoothGatt, Runnable/*readChara()*/>> mSeekerDevices = new HashMap<>();

	/** *******
	 * BLE初期化
	 * ********/
	int uwsInit() {
		/* Bluetooth権限なし */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			return Constants.UWS_NG_PERMISSION_DENIED;
		TLog.d( "Bluetooth権限OK.");

		/* Bluetoothサービス取得 */
		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null)
			return Constants.UWS_NG_SERVICE_NOTFOUND;
		TLog.d( "Bluetoothサービス取得OK.");

		/* Bluetoothアダプタ取得 */
		mBluetoothAdapter = bluetoothManager.getAdapter();
		if (mBluetoothAdapter == null)
			return Constants.UWS_NG_ADAPTER_NOTFOUND;

		/* Bluetooth ON */
		if( !mBluetoothAdapter.isEnabled())
			return Constants.UWS_NG_BT_OFF;
		TLog.d( "Bluetooth ON.");

		return UWS_NG_SUCCESS;
	}

	/* ********************************************************************************
	 * Scan処理
	 * ********************************************************************************/
	private ScanCallback mScanCallbackFromDtoS;

	/** ************
	 * Scanデータ展開
	 ** ************/
	private DeviceInfo parseScanResult(ScanResult result) {
		if(result.getScanRecord()==null || result.getScanRecord().getDeviceName()==null)
			TLog.d("ServiceUuids is null. 正体不明デバイス. addr={0}", result.getDevice().getAddress());

		Date	nowtime = new Date();
		short	seekerid = -1;
		String	name	= result.getScanRecord().getDeviceName();
		String	address = result.getDevice().getAddress();
		int		rssi	= result.getRssi();
		byte	seqno = 0;
		double	longitude = 0;
		double	latitude = 0;
		short	heartbeat = 0;
		String	deviceName = result.getScanRecord().getDeviceName();
		if(deviceName != null && deviceName.startsWith("消防士")) {
			/* 対象デバイスを発見 */
			seekerid= Short.parseShort(deviceName.substring("消防士".length()));
			/* 経度/緯度 脈拍を取得 */
			byte[] rcvdata = result.getScanRecord().getManufacturerSpecificData(UWS_OWNDATA_KEY);
			seqno		= rcvdata[0];
			longitude	= ByteBuffer.wrap(rcvdata).getFloat(1);
			latitude	= ByteBuffer.wrap(rcvdata).getFloat(5);
			heartbeat	= ByteBuffer.wrap(rcvdata).getShort(9);
		}

		TLog.d("読込データ=({0} {1}({2}):{3} 経度:{4} 緯度:{5} 脈拍:{6})", nowtime, address, name, seekerid, longitude, latitude, heartbeat);
		return new DeviceInfo(nowtime, seekerid, name, address, rssi, seqno, longitude, latitude, heartbeat);
	}

	/** *******
	 * Scan開始
	 * ********/
	private int uwsStartScan(IUwsScanCallback cb) {
		/* Bluetooth機能がOFF */
		if( !mBluetoothAdapter.isEnabled())
			return Constants.UWS_NG_BT_OFF;

		TLog.d("Bluetooth ON.");

		if(mScanCallbackFromDtoS != null)
			return Constants.UWS_NG_ALREADY_SCANNED;

		TLog.d("scan開始");

//		/* scanフィルタ */
//		List<ScanFilter> scanFilters = new ArrayList<>();
//		scanFilters.add(new ScanFilter.Builder().build());
//
//		/* scan設定 */
//		ScanSettings.Builder scansetting = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);

		/* scan Callback設定 */
		mScanCallbackFromDtoS = new ScanCallback() {
			@Override
			public void onScanResult(int callbackType, ScanResult result) {
				super.onScanResult(callbackType, result);
				DeviceInfo deviceInfo = parseScanResult(result);
				if(deviceInfo.getSeekerId()!=-1)
					mSeekerDevices.put(deviceInfo.getSeekerId(), Tuple3.create(deviceInfo.getDeviceAddress(), null, null));
//			TLog.d("発見!! {0}({1}):Rssi({2}) ScanRecord={3}", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi(), result.getScanRecord());
				try { cb.notifyDeviceInfo(deviceInfo);}
				catch (RemoteException e) {e.printStackTrace();}

				/* TODO ******************/
				TLog.d("ccccc scan後の追加 mSeekerDevices.size()={0}", mSeekerDevices.size());
				for(short lseekerid : mSeekerDevices.keySet())
					TLog.d("ccccc     消防士{0} address={1} btgatt={2} readCharc()={3}", lseekerid, mSeekerDevices.get(lseekerid).get1(), mSeekerDevices.get(lseekerid).get2(), mSeekerDevices.get(lseekerid).get3());
				/* ******************/
			}

			@Override
			public void onScanFailed(int errorCode) {
				super.onScanFailed(errorCode);
				/* TODO */throw new RuntimeException("scan失敗!! errorCode=" + errorCode);
			}
		};

		/* scan開始 */
		mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallbackFromDtoS);

		/* 30秒後 Scan停止 */
		mHandler.postDelayed(() -> {
			/* scan停止 */
			TLog.d("30秒経過 3秒後 Scan再開する.");
			BsvRestartScan(cb);
		},30000/* 30秒後scan停止する */);

		return UWS_NG_SUCCESS;
	}

	/** *******
	 * 再Scan
	 * ********/
	private boolean mScanStopFlg = false;
	private void BsvRestartScan(IUwsScanCallback cb) {
		mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallbackFromDtoS);
		mScanCallbackFromDtoS = null;
		TLog.d("scan停止.3描画再開.");

		/* Scan停止なら再開しない。 */
		if(mScanStopFlg) {
			mScanStopFlg = false;
			return;
		}

		/* 3秒後 Scan開始 */
		mHandler.postDelayed(() -> {
			/* scan停止 */
			TLog.d("3秒経過 Scan再開.");
			uwsStartScan(cb);
		},3000/* 30秒後scan停止する */);
	}

	/** *******
	 * Scan終了
	 * ********/
	private void uwsStopScan() {
		mScanStopFlg = true;
	}

	/* ********************************************************************************
	 * BLE接続 処理
	 * ********************************************************************************/
	private	IUwsInfoCallback mCallback;

	/* 定期通知 開始 */
	private int uwsStartPeriodicNotify(short seekerid, IUwsInfoCallback callback) {
		/* TODO ******************/
		TLog.d("ccccc 定期通知-開始 arg(seekerid:{0}) mSeekerDevices.size()={0}", seekerid, mSeekerDevices.size());
		for(short lseekerid : mSeekerDevices.keySet())
			TLog.d("ccccc     消防士{0} address={1} btgatt={2} readCharc()={3}", lseekerid, mSeekerDevices.get(lseekerid).get1(), mSeekerDevices.get(lseekerid).get2(), mSeekerDevices.get(lseekerid).get3());
		/* ******************/
		Tuple3<String, BluetoothGatt, Runnable> tuple = mSeekerDevices.get(seekerid);
		if(tuple == null) {
			TLog.d("scanできてないデバイス(消防士{0})です。接続できません。", seekerid);
			return UWS_NG_DEVICE_NOTFOUND;
		}

		mCallback = callback;
		String address		= tuple.get1();
		BluetoothGatt btgatt= tuple.get2();

		if(btgatt != null) {
			boolean ret = btgatt.connect();
			if(ret) return UWS_NG_SUCCESS;
		}

		BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
		if (device == null) {
			TLog.d("デバイス({0})が見つかりません。接続できません。", address);
			return UWS_NG_DEVICE_NOTFOUND;
		}

		/* デバイスに直接接続したい時に、autoConnectをfalseにする。 */
		/* デバイスが使用可能になったら自動的にすぐに接続する様にする時に、autoConnectをtrueにする。 */
		BluetoothGatt btGatt = device.connectGatt(getApplicationContext(), false, mGattCallback);
		if (btGatt == null) {
			TLog.d("接続要求失敗!! seekerid={0} address={1}", seekerid, address);
			return UWS_NG_DEVICE_NOTFOUND;
		}
		else {
			mSeekerDevices.put(seekerid, Tuple3.create(address, btGatt, null));
			TLog.d("接続要求OK->接続確立待ち. seekerid={0} address={0}", seekerid, btGatt.getDevice().getAddress());
			/* TODO ******************/
			TLog.d("ccccc 接続OK->確立待ち. arg(seekerid:{0}) mSeekerDevices.size()={0}", seekerid, mSeekerDevices.size());
			for(short lseekerid : mSeekerDevices.keySet())
				TLog.d("ccccc     消防士{0} address={1} btgatt={2} readCharc()={3}", lseekerid, mSeekerDevices.get(lseekerid).get1(), mSeekerDevices.get(lseekerid).get2(), mSeekerDevices.get(lseekerid).get3());
			/* ******************/
			return UWS_NG_SUCCESS;
		}
	}

	/* 定期通知 終了 */
	private void uwsStopPeriodicNotify(short seekerid) {
		Tuple3<String, BluetoothGatt, Runnable> tuple = mSeekerDevices.get(seekerid);
		if(tuple == null) return;
		BluetoothGatt btgatt = tuple.get2();
		if(btgatt == null) return;
		btgatt.disconnect();
//		mSeekerDevices.remove(seekerid);	/* 再接続を想定して、ここで削除はしない */
		/* TODO ******************/
		TLog.d("ccccc 定期通知-終了(再接続を想定して、ここで削除はしない). arg(seekerid:{0}) mSeekerDevices.size()={0}", seekerid, mSeekerDevices.size());
		for(short lseekerid : mSeekerDevices.keySet())
			TLog.d("ccccc     消防士{0} address={1} btgatt={2} readCharc()={3}", lseekerid, mSeekerDevices.get(lseekerid).get1(), mSeekerDevices.get(lseekerid).get2(), mSeekerDevices.get(lseekerid).get3());
		/* ******************/
	}

	/* GattCallback */
	private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
		@Override
		public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) {
			if(newState == BluetoothProfile.STATE_CONNECTED) {
				TLog.d("接続要求OK->接続確立OK->Discover待ち. address={0}", gatt.getDevice().getAddress());
				gatt.discoverServices();
			}
			else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
				TLog.d("Gatt切断!!");
				try { mCallback.notifyStatus(UWS_SERVICE_STATUS_GATT_DISCONNECT); }
				catch (RemoteException e) { e.printStackTrace();}
				Map.Entry<Short, Tuple3<String, BluetoothGatt, Runnable>> findit = mSeekerDevices.entrySet().stream().filter(
																						itm->itm.getValue().get1().equals(gatt.getDevice().getAddress())
																				).findAny().orElse(null);
				if(findit != null) {
					mSeekerDevices.remove(findit.getKey());	/* アドバタイズの再開を待つ */
				}
				/* TODO ******************/
				TLog.d("ccccc gatt切断(新addressになるはずなのでここで削除). arg(seekerid:{0}) mSeekerDevices.size()={0}", findit==null?"null":findit.getKey().toString(), mSeekerDevices.size());
				for(short lseekerid : mSeekerDevices.keySet())
					TLog.d("ccccc     消防士{0} address={1} btgatt={2} readCharc()={3}", lseekerid, mSeekerDevices.get(lseekerid).get1(), mSeekerDevices.get(lseekerid).get2(), mSeekerDevices.get(lseekerid).get3());
				/* ******************/
			}
		}

		@Override
		public void onServicesDiscovered(BluetoothGatt gatt, int status) {
			if (status != BluetoothGatt.GATT_SUCCESS) return;	/* どうしようもないのでreturn. */

			String suuid= getShortUuid(gatt, UWS_UUID_SERVICE);
			String addr = gatt.getDevice().getAddress();

			Map.Entry<Short, Tuple3<String, BluetoothGatt, Runnable>> findit = mSeekerDevices.entrySet().stream().filter(
																						itm->itm.getValue().get1().equals(addr)
																				).findAny().orElse(null);
			if(findit == null) return;					/* どうしようもないのでreturn. */
			if(findit.getValue().get3() != null) return;/* すでに実行済return. */

			TLog.d("接続要求OK->接続確立OK->DiscoverOK->charac解析中. address={0}", gatt.getDevice().getAddress());

			BluetoothGattCharacteristic charac = findTerget(gatt, UWS_UUID_SERVICE, UWS_UUID_CHARACTERISTIC_HRATBEAT);
			if (charac != null) {
				Runnable readCharaRunner = new Runnable() {
					@Override
					public void run() {
						boolean ret = gatt.readCharacteristic(charac);
						if( !ret) {
							/* TODO ******************/
							TLog.d("ccccc デバイス読込み失敗! arg(address:{0}) mSeekerDevices.size()={0}", addr, mSeekerDevices.size());
							for(short lseekerid : mSeekerDevices.keySet())
								TLog.d("ccccc     消防士{0} address={1} btgatt={2} readCharc()={3}", lseekerid, mSeekerDevices.get(lseekerid).get1(), mSeekerDevices.get(lseekerid).get2(), mSeekerDevices.get(lseekerid).get3());
							/* ******************/
						}

						/* 1秒後 再度読込み */
						mHandler.postDelayed(this, 1000);
					}
				};
				/* 生成したRunnableをセット */
				mSeekerDevices.put(findit.getKey(), Tuple3.create(findit.getValue().get1(), findit.getValue().get2(), readCharaRunner));
				/* デバイス読込み開始(1秒定期) */
				mHandler.post(readCharaRunner);
			}
			else {
				/* TODO ******************/
				TLog.d("ccccc 対象外デバイス!! arg(address:{0}) mSeekerDevices.size()={0}", addr, mSeekerDevices.size());
				for(short lseekerid : mSeekerDevices.keySet())
					TLog.d("ccccc     消防士{0} address={1} btgatt={2} readCharc()={3}", lseekerid, mSeekerDevices.get(lseekerid).get1(), mSeekerDevices.get(lseekerid).get2(), mSeekerDevices.get(lseekerid).get3());
				/* ******************/
				Map.Entry<Short, Tuple3<String, BluetoothGatt, Runnable>> findid = mSeekerDevices.entrySet().stream().filter(
																				itm->itm.getValue().get1().equals(addr)
																		).findAny().orElse(null);
				TLog.d("対象外デバイス!! seekerid={0} address={1} suuid={2}", findid==null?"null":findid.getKey().toString(), addr, suuid);
			}
		}

		@Override
		public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
			String suuid= getShortUuid(gatt, UWS_UUID_SERVICE);
			String addr = gatt.getDevice().getAddress();
			Map.Entry<Short, Tuple3<String, BluetoothGatt, Runnable>> findit = mSeekerDevices.entrySet().stream().filter(
																				itm->itm.getValue().get1().equals(addr)
																		).findAny().orElse(null);
			if(findit ==null) throw new RuntimeException("mSeekerDevicesにエントリがない!! あり得ない。!!");

			short seekerid = findit.getKey();
			if (status == BluetoothGatt.GATT_SUCCESS) {
				UwsInfo rcvdata = parseRcvData(seekerid, characteristic);
				try { mCallback.notifyUwsData(rcvdata); }
				catch (RemoteException e) { e.printStackTrace(); }
			}
			else {
				/* TODO ******************/
				TLog.d("ccccc readCharc失敗!! arg(address:{0}) mSeekerDevices.size()={0}", addr, mSeekerDevices.size());
				for(short lseekerid : mSeekerDevices.keySet())
					TLog.d("ccccc     消防士{0} address={1} btgatt={2} readCharc()={3}", lseekerid, mSeekerDevices.get(lseekerid).get1(), mSeekerDevices.get(lseekerid).get2(), mSeekerDevices.get(lseekerid).get3());
				/* ******************/
				Map.Entry<Short, Tuple3<String, BluetoothGatt, Runnable>> findid = mSeekerDevices.entrySet().stream().filter(
																						itm->itm.getValue().get1().equals(addr)
																				).findAny().orElse(null);
				TLog.d("readCharc失敗!! seekerid={0} address={1} suuid={2}", findid==null?"null":findid.getKey().toString(), addr, suuid);
			}
		}
	};

	/** ******************************************
	 * GattからShortUuidを取得(対象外デバイスの時はnull)
	 ** ******************************************/
	private String getShortUuid(BluetoothGatt gatt, UUID TergetUuid) {
		/* サービスUUIDを持たない場合もある */
		if(gatt.getServices().size() == 0) return null;

		/* TODO */
		for(BluetoothGattService service : gatt.getServices()) {
			TLog.d("serviceUUID={0} address={1}", service.getUuid(), gatt.getDevice().getAddress());
		}

		/* 最後に定義されているサービスUUIDが有効 */
		/* TODO */
		TLog.d("return serviceUUID={0} address={1}", gatt.getServices().get(gatt.getServices().size()-1).getUuid(), gatt.getDevice().getAddress());
		return gatt.getServices().get(gatt.getServices().size()-1).getUuid().toString().substring(4,8);
	}

	/** *************************
	 * 対象のCharacteristicを検索
	 ** *************************/
	private BluetoothGattCharacteristic findTerget(BluetoothGatt gatt, UUID ServiceUuid, UUID CharacteristicUuid) {
		BluetoothGattCharacteristic ret = null;
		for (BluetoothGattService service : gatt.getServices()) {
			/*-*-----------------------------------*/
			for(BluetoothGattCharacteristic gattChara : service.getCharacteristics())
				TLog.d("{0} : service-UUID={1} Chara-UUID={2}", gatt.getDevice().getAddress(), service.getUuid(), gattChara.getUuid());
			/*-*-----------------------------------*/
			if( !service.getUuid().toString().startsWith(ServiceUuid.toString().substring(0,5)))
				continue;

			final List<BluetoothGattCharacteristic> findc = service.getCharacteristics().stream().filter(c -> {
																return c.getUuid().equals(CharacteristicUuid);
															}).collect(Collectors.toList());
			/* 見つかった最後の分が有効なので、上書きする */
			ret = findc.get(0);
			/* TODO */
			if(ret!=null)
				TLog.d("get it. serviceUUID = {0} address={1}", service.getUuid(), gatt.getDevice().getAddress());
		}
		/* TODO */
		if(ret!=null)
			TLog.d("return charc-UUID = {0} address={1}", ret.getUuid(), gatt.getDevice().getAddress());
		return ret;
	}

	/** **********
	 * データParse
	 ** **********/
	private UwsInfo parseRcvData(short seekerid, final BluetoothGattCharacteristic characteristic) {
		if (UWS_UUID_CHARACTERISTIC_HRATBEAT.equals(characteristic.getUuid())) {
			/* 受信データ取出し */
			final byte[] rcvdata = characteristic.getValue();
			TLog.d("rcvdata:(size={0}, {1})", rcvdata.length, Arrays.toString(rcvdata));
			if(rcvdata.length == 28) {
				/* 全データ受信(日付,経度,緯度,脈拍) */
				long	ldatetime	= ByteBuffer.wrap(rcvdata).getLong();
				double	longitude	= ByteBuffer.wrap(rcvdata).getDouble(8);
				double	latitude	= ByteBuffer.wrap(rcvdata).getDouble(16);
				int		heartbeat	= ByteBuffer.wrap(rcvdata).getInt(24);
				TLog.d("読込データ=({0} 経度:{1} 緯度:{2} 脈拍:{3})", new Date(ldatetime), longitude, latitude, heartbeat);
				return  new UwsInfo(new Date(ldatetime), seekerid, longitude, latitude, (short)heartbeat);
			}
			short seqno = ByteBuffer.wrap(rcvdata).getShort(2);
			if(seqno == 0) {
				/* 先行の通知データ(msgid,seqno,日付,経度)受信 */
				short	msgid		= ByteBuffer.wrap(rcvdata).getShort();
//				short	seqno		= ByteBuffer.wrap(rcvdata).getShort(2);
				long	ldatetime	= ByteBuffer.wrap(rcvdata).getLong(4);
				double	longitude	= ByteBuffer.wrap(rcvdata).getDouble(12);
				TLog.d("受信データ=({0} 経度:{1} 緯度:{2} 脈拍:{3})", new Date(ldatetime), longitude, 999, 999);
				return new UwsInfo(new Date(ldatetime), seekerid, longitude, 999.0, (short)999);
			}
			else {
				/* 後発の通知データ(msgid,seqno,緯度,脈拍)受信 */
				short	msgid		= ByteBuffer.wrap(rcvdata).getShort();
//				short	seqno		= ByteBuffer.wrap(rcvdata).getShort(2);
				double	latitude	= ByteBuffer.wrap(rcvdata).getDouble(4);
				int		heartbeat	= ByteBuffer.wrap(rcvdata).getInt(12);
				TLog.d("受信データ=({0} 経度:{1} 緯度:{2} 脈拍:{3})", new Date(0), 999, latitude, heartbeat);
				return new UwsInfo(new Date(999), seekerid, 999.0, latitude, (short)heartbeat);
			}
		}
		return new UwsInfo(new Date(), seekerid, 0.0, 0.0, (short)0);
	}
}
