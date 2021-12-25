package com.tks.uwsserverunit00;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanRecord;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import static com.tks.uwsserverunit00.Constants.UWS_LOC_BASE_LATITUDE;
import static com.tks.uwsserverunit00.Constants.UWS_LOC_BASE_LONGITUDE;
import static com.tks.uwsserverunit00.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsserverunit00.Constants.UWS_OWNDATA_KEY;

/**
 * -30 dBm	素晴らしい	達成可能な最大信号強度。クライアントは、これを実現するには、APから僅か数フィートである必要があります。現実的には一般的ではなく、望ましいものでもありません	N/A
 * -60 dBm	良好	非常に信頼性の高く、データパケットのタイムリーな伝送を必要とするアプリケーションのための最小信号強度	VoIP/VoWiFi, ストリーミングビデオ
 * -70 dBm	Ok	信頼できるパケット伝送に必要な最小信号強度	Email, web
 * -80 dBm	よくない	基本的なコネクティビティに必要な最小信号強度。パケット伝送は信頼できない可能性があります	N/A
 * -90 dBm	使用不可	ノイズレベルに近いかそれ以下の信号強度。殆ど機能しない	N/A
 **/

public class BleServerService extends Service {
	private BluetoothAdapter			mBluetoothAdapter;
	private IBleServerServiceCallback	mCb;	/* 常に後発優先 */
	private final Handler				mHandler = new Handler();

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		TLog.d("");
		return mBinder;
	}

	@Override
	public boolean onUnbind(Intent intent) {
		BsvClearDevice();
		return super.onUnbind(intent);
	}

	/** *****
	 * Binder
	 * ******/
	private Binder mBinder = new IBleServerService.Stub() {
		@Override
		public void setCallback(IBleServerServiceCallback callback) {
			mCb = callback;
		}

		@Override
		public int initBle() {
			return BsvInit();
		}

		@Override
		public int startScan() {
			return BsvStartScan();
		}

		@Override
		public int stopScan() {
			return BsvStopScan();
		}
	};

	/** *******
	 * BLE初期化
	 * ********/
	int BsvInit() {
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

	/** *******
	 * BLEクリア
	 * ********/
	private void BsvClearDevice() {
	}

	/* ********************************************************************************
	 * Scan処理
	 * ********************************************************************************/
	private final ScanCallback	mScanCallback = new ScanCallback() {
		@Override
		public void onBatchScanResults(List<ScanResult> results) {
			super.onBatchScanResults(results);
			List<DeviceInfo> deviceInfoList = results.stream().map(ret -> {
				return parseScanResult(ret);
			}).collect(Collectors.toList());
			try { mCb.notifyDeviceInfolist(deviceInfoList);}
			catch (RemoteException e) {e.printStackTrace();}
		}

		@Override
		public void onScanResult(int callbackType, ScanResult result) {
			super.onScanResult(callbackType, result);

			/* TODO 削除予定  */
			if(result.getScanRecord().getDeviceName()!=null && result.getScanRecord().getDeviceName().startsWith("消防士")) {
				ScanRecord aaaa = result.getScanRecord();
				TLog.d("zzzzz 拡張データ={0}", Arrays.toString(result.getScanRecord().getManufacturerSpecificData(UWS_OWNDATA_KEY)));
				TLog.d("zzzzz 拡張データall(size:{0} data={1})", result.getScanRecord().getManufacturerSpecificData().size(), result.getScanRecord().getManufacturerSpecificData().toString());
				TLog.d("zzzzz aaaaaaaaaaaaaa");
			}
			/* TODO 削除予定  */

			DeviceInfo deviceInfo = parseScanResult(result);
//			TLog.d("発見!! {0}({1}):Rssi({2}) ScanRecord={3}", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi(), result.getScanRecord());
			try { mCb.notifyDeviceInfo(deviceInfo);}
			catch (RemoteException e) {e.printStackTrace();}
		}

		@Override
		public void onScanFailed(int errorCode) {
			super.onScanFailed(errorCode);
			TLog.d("scan失敗!! errorCode=" + errorCode);
		}
	};

	/** ************
	 * Scanデータ展開
	 ** ************/
	private DeviceInfo parseScanResult(ScanResult result) {
		if(result.getScanRecord()==null || result.getScanRecord().getDeviceName()==null)
			TLog.d("ServiceUuids is null. 正体不明デバイス. addr={0}", result.getDevice().getAddress());

		Date	nowtime = new Date();
		short	seekerid = -1;
		String	name	= result.getDevice().getName();
		String	address = result.getDevice().getAddress();
		int		rssi	= result.getRssi();
		double	longitude = 0;
		double	latitude = 0;
		short	heartbeat = 0;
		String deviceName = result.getScanRecord().getDeviceName();
		if (deviceName != null && deviceName.startsWith("消防士")) {
			/* 対象デバイスを発見 */
			seekerid= Short.parseShort(deviceName.substring("消防士".length()));
			/* 経度/緯度 脈拍を取得 */
			byte[] rcvdata = result.getScanRecord().getManufacturerSpecificData(UWS_OWNDATA_KEY);
			longitude	= ByteBuffer.wrap(rcvdata).getFloat()		+ UWS_LOC_BASE_LONGITUDE;
			latitude	= ByteBuffer.wrap(rcvdata).getFloat(4)+ UWS_LOC_BASE_LATITUDE;
			heartbeat	= ByteBuffer.wrap(rcvdata).getShort(8);
		}

		TLog.d("読込データ=({0} {1}({2}):{3} 経度:{4} 緯度:{5} 脈拍:{6})", nowtime, address, name, seekerid, longitude, latitude, heartbeat);
		return new DeviceInfo(nowtime, seekerid, name, address, rssi, longitude, latitude, heartbeat);
	}

	/** *******
	 * Scan開始
	 * ********/
	private int BsvStartScan() {
		/* Bluetooth機能がOFF */
		if( !mBluetoothAdapter.isEnabled())
			return Constants.UWS_NG_BT_OFF;

		TLog.d("Bluetooth ON.");
		TLog.d("scan開始");

//		/* scanフィルタ */
//		List<ScanFilter> scanFilters = new ArrayList<>();
//		scanFilters.add(new ScanFilter.Builder().build());
//
//		/* scan設定 */
//		ScanSettings.Builder scansetting = new ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_POWER);

		/* scan開始 */
		mBluetoothAdapter.getBluetoothLeScanner().startScan(mScanCallback);

		/* 30秒後 Scan停止 */
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				/* scan停止 */
				TLog.d("30秒経過 scan停止.");
				BsvStopScan();
			}
		},30000/* 30秒後scan停止する */);

		return UWS_NG_SUCCESS;
	}

	/** *******
	 * Scan終了
	 * ********/
	private int BsvStopScan() {
		mBluetoothAdapter.getBluetoothLeScanner().stopScan(mScanCallback);
		TLog.d("scan終了");

		/* 1秒後 Scan開始 */
		mHandler.postDelayed(new Runnable() {
			@Override
			public void run() {
				/* scan停止 */
				TLog.d("1秒経過 Scan開始.");
				BsvStartScan();
			}
		},1000/* 30秒後scan停止する */);

		return UWS_NG_SUCCESS;
	}
}
