package com.test.blesample.central;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeScanner;
import android.bluetooth.le.ScanCallback;
import android.bluetooth.le.ScanResult;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.Button;

import com.google.android.material.snackbar.Snackbar;

import java.util.List;

public class MainActivity extends AppCompatActivity {
	private BluetoothAdapter						mBluetoothAdapter = null;
	private final ActivityResultLauncher<Intent>	mStartForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
			result -> {
				if (result.getResultCode() == Activity.RESULT_OK) {
					/* Bluetooth機能ONになった */
					TLog.d("Bluetooth OFF -> ON");
					startScan();
				}
				else {
					ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothを有効にする必要があります。").Show(MainActivity.this);
				}
			});
	private RecyclerView							mDeviceListRvw;
	private DeviceListAdapter						mDeviceListAdapter;
	private Handler									mHandler;
	private ScanCallback							mScanCallback = null;
	private final static int  REQUEST_PERMISSIONS	= 0x2222;
	private final static long SCAN_PERIOD			= 30000;	/* m秒 */

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* BLEデバイスリストの初期化 */
		mDeviceListRvw = findViewById(R.id.rvw_devices);
		mDeviceListRvw.setHasFixedSize(true);
		mDeviceListRvw.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
		mDeviceListAdapter = new DeviceListAdapter((deviceName, deviceAddress) -> {
//			Intent intent = new Intent(this, DeviceConnectActivity.class);
//			intent.putExtra(DeviceConnectActivity.EXTRAS_DEVICE_NAME, deviceName);
//			intent.putExtra(DeviceConnectActivity.EXTRAS_DEVICE_ADDRESS, deviceAddress);
//			startActivity(intent);
		});
		mDeviceListRvw.setAdapter(mDeviceListAdapter);

		findViewById(R.id.btnScan).setOnClickListener(view -> {
			startScan();
		});

		/* UIスレッド非同期管理 */
		mHandler = new Handler(Looper.getMainLooper());

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
		}

		/* 権限が許可されていない場合はリクエスト. */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION}, REQUEST_PERMISSIONS);
		}

		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		mBluetoothAdapter = bluetoothManager.getAdapter();
		/* Bluetooth未サポート判定 未サポートならエラーpopupで終了 */
		if (mBluetoothAdapter == null) {
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
		}
		/* Bluetooth ON/OFF判定 -> OFFならONにするようにリクエスト */
		else if( !mBluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			mStartForResult.launch(enableBtIntent);
		}
		else {
			/* Bluetooth機能ONだった */
			startScan();
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		TLog.d("onRequestPermissionsResult s");
		/* 権限リクエストの結果を取得する. */
		if (requestCode == REQUEST_PERMISSIONS) {
			if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
				/* Bluetooth使用の権限を得た */
				startScan();
			} else {
				ErrPopUp.create(MainActivity.this).setErrMsg("失敗しました。\n\"許可\"を押下して、このアプリにBluetoothの権限を与えて下さい。\n終了します。").Show(MainActivity.this);
			}
		}
		/* 知らん応答なのでスルー。 */
		else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
		TLog.d("onRequestPermissionsResult e");
	}

	/* セントラルとして起動 */
	private void startScan() {
		TLog.d("s");

		if(mScanCallback != null) {
			TLog.d("e すでにscan中。");
			return;
		}

		/* Bluetooth機能がONになってないのでreturn */
		if( !mBluetoothAdapter.isEnabled()) {
			TLog.d("e Bluetooth機能がOFFってる。");
			Snackbar.make(findViewById(R.id.rootview), "Bluetooth機能をONにして下さい。", Snackbar.LENGTH_LONG).show();
			return;
		}
		TLog.d("Bluetooth ON.");

		/* Bluetooth使用の権限がないのでreturn */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			TLog.d( "startPrepare() e Bluetooth使用の権限が拒否られた。");
			ErrPopUp.create(MainActivity.this).setErrMsg("このアプリに権限を与えて下さい。").Show(MainActivity.this);
			return;
		}
		TLog.d( "Bluetooth使用権限OK.");

		TLog.d("scan開始");
		mDeviceListAdapter.clearDevice();
		runOnUiThread(() -> {
			Button btn = findViewById(R.id.btnScan);
			btn.setText("scan中");
			btn.setEnabled(false);
		});

		mScanCallback = new ScanCallback() {
			@Override
			public void onBatchScanResults(List<ScanResult> results) {
				super.onBatchScanResults(results);
				mDeviceListAdapter.addDevice(results);
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
				mDeviceListAdapter.addDevice(result);
				if(result !=null && result.getDevice() != null) {
					TLog.d("----------------------------------");
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
			public void onScanFailed(int errorCode) {
				super.onScanFailed(errorCode);
				TLog.d("aaaaa Bluetoothのscan失敗!! errorCode=" + errorCode);
			}
		};

		/* Bluetoothサポート有,Bluetooth使用権限有,Bluetooth ONなので、セントラルとして起動 */
		BluetoothLeScanner bLeScanner = mBluetoothAdapter.getBluetoothLeScanner();
		bLeScanner.startScan(mScanCallback);
		mHandler.postDelayed(() -> {
			bLeScanner.stopScan(mScanCallback);
			Button btn = findViewById(R.id.btnScan);
			btn.setText("scan開始");
			btn.setEnabled(true);
			TLog.d("scan終了");
			mScanCallback = null;
		}, SCAN_PERIOD);
	}
}
