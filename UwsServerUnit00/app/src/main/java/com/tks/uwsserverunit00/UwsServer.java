package com.tks.uwsserverunit00;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanCallback;
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
import java.util.Date;

import static com.tks.uwsserverunit00.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsserverunit00.Constants.UWS_OWNDATA_KEY;

/**
 * -30 dBm	素晴らしい	達成可能な最大信号強度。クライアントは、これを実現するには、APから僅か数フィートである必要があります。現実的には一般的ではなく、望ましいものでもありません	N/A
 * -60 dBm	良好	非常に信頼性の高く、データパケットのタイムリーな伝送を必要とするアプリケーションのための最小信号強度	VoIP/VoWiFi, ストリーミングビデオ
 * -70 dBm	Ok	信頼できるパケット伝送に必要な最小信号強度	Email, web
 * -80 dBm	よくない	基本的なコネクティビティに必要な最小信号強度。パケット伝送は信頼できない可能性があります	N/A
 * -90 dBm	使用不可	ノイズレベルに近いかそれ以下の信号強度。殆ど機能しない	N/A
 **/

public class UwsServer extends Service {
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
			return BsvInit();
		}

		@Override
		public int startScan(IUwsScanCallback callback) {
			return BsvStartScan(callback);
		}

		@Override
		public void stopScan() {
			BsvStopScan();
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
	private int BsvStartScan(IUwsScanCallback cb) {
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
//			TLog.d("発見!! {0}({1}):Rssi({2}) ScanRecord={3}", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi(), result.getScanRecord());
				try { cb.notifyDeviceInfo(deviceInfo);}
				catch (RemoteException e) {e.printStackTrace();}
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
			BsvStartScan(cb);
		},3000/* 30秒後scan停止する */);
	}

	/** *******
	 * Scan終了
	 * ********/
	private void BsvStopScan() {
		mScanStopFlg = true;
	}
}
