package com.tsk.uws.blecentral;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanFilter;
import android.bluetooth.le.ScanResult;
import android.bluetooth.le.ScanSettings;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {
	private DeviceListAdapter						mDeviceListAdapter;
	private final static int  REQUEST_ENABLE_BT		= 0x1111;
	private final static int  REQUEST_PERMISSIONS	= 0x2222;

	private int cnt = 0;
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
					int errReason = intent.getIntExtra(BleService.UWS_KEY_WAKEUP_NG_REASON, BleService.UWS_NG_REASON_BTADAPTER_NOTFOUND);
					if(errReason == BleService.UWS_NG_REASON_SERVICE_NOTFOUND)
						MsgPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!終了します。").Show(MainActivity.this);
					else if(errReason == BleService.UWS_NG_REASON_BTADAPTER_NOTFOUND)
						MsgPopUp.create(MainActivity.this).setErrMsg("Service起動中のBT初期化に失敗!!終了します。").Show(MainActivity.this);
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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* ↓↓↓動作確認用ボタン(削除予定) */
		findViewById(R.id.btnSetStr).setOnClickListener(new View.OnClickListener() {
			@Override
			public void onClick(View view) {
				try {
					mBleServiceIf.setAddStr("00" + cnt);
					mBleServiceIf.setRemoceStr("000" + cnt++);
				}
				catch (RemoteException e) {
					e.printStackTrace();
				}
			}
		});
		/* ↑↑↑動作確認用ボタン(削除予定) */

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
			try { mBleServiceIf.startScan();}
			catch (RemoteException e) { e.printStackTrace();}
		});

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			MsgPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
		}

		/* 権限が許可されていない場合はリクエスト. */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSIONS);
		}

		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		/* Bluetooth未サポート判定 未サポートならエラーpopupで終了 */
		if (bluetoothAdapter == null) {
			MsgPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
		}
		/* Bluetooth ON/OFF判定 -> OFFならONにするようにリクエスト */
		else if( !bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
		}
//		else {
//			/* Bluetooth機能ONだった */
//			try { mBleServiceIf.startScan();}
//			catch (RemoteException e) { e.printStackTrace();}
//		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if(requestCode == REQUEST_ENABLE_BT) {
//			/* Bluetooth機能ONになった。 */
//			try { mBleServiceIf.startScan();}
//			catch (RemoteException e) { e.printStackTrace();}
		}
		else {
			MsgPopUp.create(MainActivity.this).setErrMsg("Bluetooth機能をONにする必要があります。").Show(MainActivity.this);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		TLog.d("onStart()");
		Intent intent = new Intent(MainActivity.this, BleService.class);
		bindService(intent, mCon, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		TLog.d("onStop()");
		try {
			mBleServiceIf.removeCallback(mCb);
		}
		catch (RemoteException e) {
			e.printStackTrace();
		}
		unbindService(mCon);
	}

	private IBleService mBleServiceIf;
	private ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder iBinder) {
			TLog.d("onServiceConnected(): name=" + name);
			mBleServiceIf = IBleService.Stub.asInterface(iBinder);
			try {
				mBleServiceIf.addCallback(mCb);
				String ret = mBleServiceIf.startScan();
				TLog.d("startScan() ret=", ret);
			}
			catch (RemoteException e) {
				TLog.d("Error!! addCallback()");
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			TLog.d("onServiceDisconnected(): name=" + name);
			mBleServiceIf = null;
		}
	};

	private IBleServiceCallback mCb = new IBleServiceCallback.Stub() {
		@Override
		public void onItemAdded(String name) {
			TLog.d("onItemAdded() arg=" + name);
		}

		@Override
		public void onItemRemoved(String name) {
			TLog.d("onItemRemoved() arg=" + name);
		}

		@Override
		public void notifyScanResultlist() throws RemoteException {

		}

		@Override
		public void notifyScanResult() throws RemoteException {

		}

		@Override
		public void notifyError(int errcode, String errmsg) throws RemoteException {

		}
	};
}
