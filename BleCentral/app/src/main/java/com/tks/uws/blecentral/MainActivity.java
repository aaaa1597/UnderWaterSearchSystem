package com.tks.uws.blecentral;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.ScanResult;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.widget.Button;
import android.widget.TextView;

import java.util.List;

public class MainActivity extends AppCompatActivity {
	private DeviceListAdapter	mDeviceListAdapter;
	private final static int	REQUEST_ENABLE_BT	= 0x1111;
	private final static int	REQUEST_PERMISSIONS	= 0x2222;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* 受信するブロードキャストintentを登録 */
		final IntentFilter intentFilter = new IntentFilter();
		intentFilter.addAction(BleService.UWS_GATT_CONNECTED);
		intentFilter.addAction(BleService.UWS_GATT_DISCONNECTED);
		intentFilter.addAction(BleService.UWS_GATT_SERVICES_DISCOVERED);
		intentFilter.addAction(BleService.UWS_DATA_AVAILABLE);
		registerReceiver(mIntentListner, intentFilter);

		/* BLEデバイスリストの初期化 */
		RecyclerView deviceListRvw = findViewById(R.id.rvw_devices);
		deviceListRvw.setHasFixedSize(true);
		deviceListRvw.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
		mDeviceListAdapter = new DeviceListAdapter(new DeviceListAdapter.DeviceListAdapterListener() {
			@Override
			public void onDeviceItemClick(String deviceName, String deviceAddress) {
				try { mBleServiceIf.stopScan(); }
				catch (RemoteException e) { e.printStackTrace(); }
				/* 接続画面に遷移 */
				Intent intent = new Intent(MainActivity.this, DeviceConnectActivity.class);
				intent.putExtra(DeviceConnectActivity.EXTRAS_DEVICE_NAME	, deviceName);
				intent.putExtra(DeviceConnectActivity.EXTRAS_DEVICE_ADDRESS	, deviceAddress);
				startActivity(intent);
			}
		});
		deviceListRvw.setAdapter(mDeviceListAdapter);

		/* scanボタン押下 */
		findViewById(R.id.btnScan).setOnClickListener(view -> {
			startScan();
		});

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
		}

		/* 権限が許可されていない場合はリクエスト. */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSIONS);
		}

		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		/* Bluetooth未サポート判定 未サポートならエラーpopupで終了 */
		if (bluetoothAdapter == null) {
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
		}
		/* Bluetooth ON/OFF判定 -> OFFならONにするようにリクエスト */
		else if( !bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}

		/* Bluetoothサービス起動 */
		Intent intent = new Intent(MainActivity.this, BleService.class);
		bindService(intent, mCon, Context.BIND_AUTO_CREATE);
		TLog.d("Bluetoothサービス起動");
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if(requestCode == REQUEST_PERMISSIONS && grantResults.length > 0) {
			if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				ErrPopUp.create(MainActivity.this).setErrMsg("このアプリには必要な権限です。\n再起動後に許可してください。\n終了します。").Show(MainActivity.this);
			}
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ENABLE_BT) {
			/* Bluetooth機能ONになった。 */
			if(resultCode == Activity.RESULT_OK){
				startScan();
			}
			else {
				ErrPopUp.create(MainActivity.this).setErrMsg("このアプリでは、Bluetooth機能をONにする必要があります。\n終了します。").Show(MainActivity.this);
			}
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		TLog.d("onDestroy()");
		unbindService(mCon);
		unregisterReceiver(mIntentListner);
	}

	/* Serviceコールバック */
	private IBleService mBleServiceIf;
	private final ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBleServiceIf = IBleService.Stub.asInterface(service);

			/* コールバック設定 */
			try { mBleServiceIf.setCallback(mCb); }
			catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException("AIDL-callback設定で失敗!!"); /* ここで例外が起きたら終了する */}

			/* BT初期化 */
			boolean retinit = initBt();
			if(!retinit) return;
			TLog.d("BT初期化完了");

			/* scan開始 */
			boolean retscan = startScan();
			if(!retscan) return;
			TLog.d("scan開始");
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			TLog.d("onServiceDisconnected() name={0}", name);
			mBleServiceIf = null;
		}
	};

	/* AIDLコールバック */
	private IBleServiceCallback mCb = new IBleServiceCallback.Stub() {
		@Override
		public void notifyScanResultlist() throws RemoteException {
			List<ScanResult> result = mBleServiceIf.getScanResultlist();
			runOnUiThread(() -> mDeviceListAdapter.addDevice(result));
		}

		@Override
		public void notifyScanResult() throws RemoteException {
			ScanResult result = mBleServiceIf.getScanResult();
			runOnUiThread(() -> mDeviceListAdapter.addDevice(result));
			if(result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null)
				TLog.d("発見!! {0}({1}):Rssi({2}) Uuids({3})", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi(), result.getScanRecord().getServiceUuids().toString());
			else
				TLog.d("発見!! {0}({1}):Rssi({2})", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi());
		}

		@Override
		public void notifyMsg(int msgid, String msg) throws RemoteException {
			TLog.d("Msg受信!! msgid={0} : {1}", msgid, msg);
			runOnUiThread(new Runnable() {
				@Override
				public void run() {
					Button btn = (Button)findViewById(R.id.btnScan);
					btn.setText("scan開始");
					btn.setEnabled(true);
				}
			});
		}

		@Override
		public void notifyError(int errcode, String errmsg) throws RemoteException {
			TLog.d("ERROR!! errcode={0} : {1}", errcode, errmsg);
		}
	};

	/* Bluetooth初期化 */
	private boolean initBt() {
		TLog.d("コールバック設定完了");
		/* BT初期化 */
		int retini = 0;
		try { retini = mBleServiceIf.initBle(); }
		catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException("Bt初期化で失敗!!"); /* ここで例外が起きたら終了する */}
		TLog.d("Bletooth初期化 ret={0}", retini);
		if(retini == BleService.UWS_NG_SERVICE_NOTFOUND)
			ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!終了します。").Show(MainActivity.this);
		else if(retini == BleService.UWS_NG_ADAPTER_NOTFOUND)
			ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!終了します。").Show(MainActivity.this);
		else if(retini == BleService.UWS_NG_BT_OFF) {
			Snackbar.make(findViewById(R.id.root_view_device), "BluetoothがOFFです。\nONにして操作してください。", Snackbar.LENGTH_LONG).show();
			return false;
		}
		else if(retini != BleService.UWS_NG_SUCCESS)
			ErrPopUp.create(MainActivity.this).setErrMsg("原因不明のエラーが発生しました!!終了します。").Show(MainActivity.this);

		return true;
	}

	/* scan開始 */
	private boolean startScan() {
		int ret = 0;
		try { ret = mBleServiceIf.startScan();}
		catch (RemoteException e) { e.printStackTrace();}
		TLog.d("ret={0}", ret);
		if(ret == BleService.UWS_NG_ALREADY_SCANNED) {
			Snackbar.make(findViewById(R.id.root_view_device), "すでにscan中です。継続します。", Snackbar.LENGTH_LONG).show();
			return false;
		}
		else if(ret == BleService.UWS_NG_BT_OFF) {
			Snackbar.make(findViewById(R.id.root_view_device), "BluetoothがOFFです。\nONにして操作してください。", Snackbar.LENGTH_LONG).show();
			return false;
		}
		mDeviceListAdapter.clearDevice();
		Button btn = (Button)findViewById(R.id.btnScan);
		btn.setText("scan中");
		btn.setEnabled(false);

		return true;
	}












	private final BroadcastReceiver mIntentListner = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null)
				return;

			switch (action) {
//				int errReason = intent.getIntExtra(BleService.UWS_KEY_WAKEUP_NG_REASON, BleService.UWS_NG_ADAPTER_NOTFOUND);
//				if(errReason == BleService.UWS_NG_SERVICE_NOTFOUND)
//					ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!終了します。").Show(MainActivity.this);
//				else if(errReason == BleService.UWS_NG_ADAPTER_NOTFOUND)
//					ErrPopUp.create(MainActivity.this).setErrMsg("Service起動中のBT初期化に失敗!!終了します。").Show(MainActivity.this);
//				else if(errReason == BleService.UWS_NG_REASON_DEVICENOTFOUND)
//					Snackbar.make(findViewById(R.id.root_view_device), "デバイスアドレスなし!!\n前画面で、別のデバイスを選択して下さい。", Snackbar.LENGTH_LONG).show();
//				else if(errReason == BleService.UWS_NG_REASON_CONNECTBLE)
//					Snackbar.make(findViewById(R.id.root_view_device), "デバイス接続失敗!!\n前画面で、別のデバイスを選択して下さい。", Snackbar.LENGTH_LONG).show();

				/* Gattサーバ接続完了 */
				case BleService.UWS_GATT_CONNECTED:
					runOnUiThread(() -> {
						/* 表示 : Connected */
						((TextView)findViewById(R.id.txtConnectionStatus)).setText(R.string.connected);
					});
					findViewById(R.id.btnReqReadCharacteristic).setEnabled(true);
					break;

				/* Gattサーバ断 */
				case BleService.UWS_GATT_DISCONNECTED:
					runOnUiThread(() -> {
						/* 表示 : Disconnected */
						((TextView)findViewById(R.id.txtConnectionStatus)).setText(R.string.disconnected);
					});
					findViewById(R.id.btnReqReadCharacteristic).setEnabled(false);
					break;

				case BleService.UWS_GATT_SERVICES_DISCOVERED:
//					mCharacteristic = findTerget(mBLeMngServ.getSupportedGattServices(), UWS_SERVICE_UUID, UWS_CHARACTERISTIC_SAMLE_UUID);
//					if (mCharacteristic != null) {
//						mBLeMngServ.readCharacteristic(mCharacteristic);
//						mBLeMngServ.setCharacteristicNotification(mCharacteristic, true);
//					}
					break;

				case BleService.UWS_DATA_AVAILABLE:
					int msg = intent.getIntExtra(BleService.UWS_DATA, -1);
					TLog.d("RcvData =" + msg);
					break;
			}
		}
	};
}
