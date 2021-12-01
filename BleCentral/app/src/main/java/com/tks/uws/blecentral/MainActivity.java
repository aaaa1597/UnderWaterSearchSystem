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
		intentFilter.addAction(BleService.UWS_SERVICE_WAKEUP_OK);
		intentFilter.addAction(BleService.UWS_SERVICE_WAKEUP_NG);
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

		findViewById(R.id.btnScan).setOnClickListener(view -> {
			int ret = 0;
			try { ret = mBleServiceIf.startScan();}
			catch (RemoteException e) { e.printStackTrace();}
			TLog.d("{0}", ret);

//			int errReason = intent.getIntExtra(BleService.UWS_KEY_WAKEUP_NG_REASON, BleService.UWS_NG_ADAPTER_NOTFOUND);
//			if(errReason == BleService.UWS_NG_SERVICE_NOTFOUND)
//				ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!終了します。").Show(MainActivity.this);
//			else if(errReason == BleService.UWS_NG_ADAPTER_NOTFOUND)
//				ErrPopUp.create(MainActivity.this).setErrMsg("Service起動中のBT初期化に失敗!!終了します。").Show(MainActivity.this);
//			else if(errReason == BleService.UWS_NG_REASON_DEVICENOTFOUND)
//				Snackbar.make(findViewById(R.id.root_view_device), "デバイスアドレスなし!!\n前画面で、別のデバイスを選択して下さい。", Snackbar.LENGTH_LONG).show();
//			else if(errReason == BleService.UWS_NG_REASON_CONNECTBLE)
//				Snackbar.make(findViewById(R.id.root_view_device), "デバイス接続失敗!!\n前画面で、別のデバイスを選択して下さい。", Snackbar.LENGTH_LONG).show();

			mDeviceListAdapter.clearDevice();
			Button btn = (Button)view;
			btn.setText("scan中");
			btn.setEnabled(false);
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
//				try { mBleServiceIf.startScan();}
//				catch (RemoteException e) { e.printStackTrace();}
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

	private IBleService mBleServiceIf;
	private final ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			TLog.d("onServiceConnected(): name={0}", name);
			mBleServiceIf = IBleService.Stub.asInterface(service);
			try {
				/* コールバック設定 */
				mBleServiceIf.setCallback(mCb);
				TLog.d("コールバック設定完了");
				/* BT初期化 */
				int retini = mBleServiceIf.initBle();
				TLog.d("Bletooth初期化 ret={0}", retini);
				if(retini == BleService.UWS_NG_SERVICE_NOTFOUND)
					ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!終了します。").Show(MainActivity.this);
				else if(retini == BleService.UWS_NG_ADAPTER_NOTFOUND)
					ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!終了します。").Show(MainActivity.this);
				else if(retini == BleService.UWS_NG_BT_OFF) {
					Snackbar.make(findViewById(R.id.root_view_device), "BluetoothがOFFです。\nONにして操作してください。", Snackbar.LENGTH_LONG).show();
					return;
				}
				else if(retini != BleService.UWS_NG_SUCCESS)
					ErrPopUp.create(MainActivity.this).setErrMsg("原因不明のエラーが発生しました!!終了します。").Show(MainActivity.this);
				TLog.d("BT初期化完了");

				/* BT初期化正常終了 -> scan開始 */
				int retscan = mBleServiceIf.startScan();
				TLog.d("startScan() ret={0}", retscan);
				if(retscan == BleService.UWS_NG_ALREADY_SCANNED) {
					Snackbar.make(findViewById(R.id.root_view_device), "すでにscan中です。継続します。", Snackbar.LENGTH_LONG).show();
					return;
				}
				else if(retscan == BleService.UWS_NG_BT_OFF) {
					Snackbar.make(findViewById(R.id.root_view_device), "BluetoothがOFFです。\nONにして操作してください。", Snackbar.LENGTH_LONG).show();
					return;
				}
				TLog.d("scan開始");

				mDeviceListAdapter.clearDevice();
				runOnUiThread(() -> {
					Button btn = findViewById(R.id.btnScan);
					btn.setText("scan中");
					btn.setEnabled(false);
				});
			}
			catch (RemoteException e) {
				TLog.d("Error!! setCallback()");
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			TLog.d("onServiceDisconnected() name={0}", name);
			mBleServiceIf = null;
		}
	};

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
		public void notifyError(int errcode, String errmsg) throws RemoteException {
			TLog.d("ERROR!! errcode={0} : {1}", errcode, errmsg);
		}
	};













	private final BroadcastReceiver mIntentListner = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action == null)
				return;

			switch (action) {
				/* Ble管理サービス正常起動 */
				case BleService.UWS_SERVICE_WAKEUP_OK:
					TLog.d("Service wake up OK.");
					break;

				/* Ble管理サービス起動失敗 */
				case BleService.UWS_SERVICE_WAKEUP_NG:
					int errReason = intent.getIntExtra(BleService.UWS_KEY_WAKEUP_NG_REASON, BleService.UWS_NG_ADAPTER_NOTFOUND);
					if(errReason == BleService.UWS_NG_SERVICE_NOTFOUND)
						ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!終了します。").Show(MainActivity.this);
					else if(errReason == BleService.UWS_NG_ADAPTER_NOTFOUND)
						ErrPopUp.create(MainActivity.this).setErrMsg("Service起動中のBT初期化に失敗!!終了します。").Show(MainActivity.this);
					else if(errReason == BleService.UWS_NG_REASON_DEVICENOTFOUND)
						Snackbar.make(findViewById(R.id.root_view_device), "デバイスアドレスなし!!\n前画面で、別のデバイスを選択して下さい。", Snackbar.LENGTH_LONG).show();
					else if(errReason == BleService.UWS_NG_REASON_CONNECTBLE)
						Snackbar.make(findViewById(R.id.root_view_device), "デバイス接続失敗!!\n前画面で、別のデバイスを選択して下さい。", Snackbar.LENGTH_LONG).show();
					break;

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
