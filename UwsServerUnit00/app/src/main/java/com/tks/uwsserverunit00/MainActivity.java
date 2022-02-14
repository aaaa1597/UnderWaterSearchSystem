package com.tks.uwsserverunit00;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.tks.uwsserverunit00.ui.DeviceListAdapter;
import com.tks.uwsserverunit00.ui.DeviceListAdapter.DeviceInfoModel;
import com.tks.uwsserverunit00.ui.FragBleViewModel;
import com.tks.uwsserverunit00.ui.FragMapViewModel;

public class MainActivity extends AppCompatActivity {
	private FragBleViewModel	mBleViewModel;
	private FragMapViewModel	mMapViewModel;
	private boolean				mIsSettingLocationON		= false;
	private final static int	REQUEST_PERMISSIONS			= 1111;
	private final static int	REQUEST_LOCATION_SETTINGS	= 2222;
	private ServiceConnection	mCon = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		TLog.d("");
		mMapViewModel = new ViewModelProvider(this).get(FragMapViewModel.class);
		mBleViewModel = new ViewModelProvider(this).get(FragBleViewModel.class);

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if( !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			ErrDialog.create(MainActivity.this, "Bluetoothが、未サポートの端末です。\n終了します。").show();

		/* 地図権限とBluetooth権限が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
			else
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
		}

		/* 設定の位置情報ON/OFFチェック */
		LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder().build();
		SettingsClient settingsClient = LocationServices.getSettingsClient(this);
		settingsClient.checkLocationSettings(locationSettingsRequest)
			.addOnSuccessListener(this, locationSettingsResponse -> {
				mIsSettingLocationON = true;
				bindUwsService();
			})
			.addOnFailureListener(this, exception -> {
				int statusCode = ((ApiException)exception).getStatusCode();
				switch (statusCode) {
					case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
						try {
							ResolvableApiException rae = (ResolvableApiException)exception;
							rae.startResolutionForResult(MainActivity.this, REQUEST_LOCATION_SETTINGS);
						}
						catch (IntentSender.SendIntentException sie) {
							TLog.d("PendingIntent unable to execute request.");
						}
						break;
					case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
						ErrDialog.create(MainActivity.this, "このアプリでは位置情報をOnにする必要があります。\nアプリを終了します。").show();
						break;
				}
			});

		/* Bluetooth ON/OFF判定 */
		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		/* Bluetooth未サポート判定 未サポートならエラーpopupで終了 */
		if (bluetoothAdapter == null)
			ErrDialog.create(MainActivity.this, "Bluetooth未サポートの端末です。\nアプリを終了します。").show();
		/* OFFならONにするようにリクエスト */
		else if( !bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
					result -> {
						if(result.getResultCode() != Activity.RESULT_OK) {
							ErrDialog.create(MainActivity.this, "BluetoothがOFFです。ONにして操作してください。\n終了します。").show();
						}
						else {
							bindUwsService();
						}
					});
			startForResult.launch(enableBtIntent);
		}

		/* Bluetoothリストにペアリング済デバイスを追加 */
		Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
		List<DeviceInfoModel> pairedlist = devices.stream().map(i -> new DeviceInfoModel() {{
			mDatetime		= new Date();
			mSeekerId		= -1;
			mDeviceName		= i.getName();
			mDeviceAddress	= i.getAddress();
			mStatusResId = R.string.status_waitforconnect;
			mLongitude		= -1;
			mLatitude		= -1;
			mHertBeat		= -1;
			mConnected		= false;
			mSelected		= false;
			mIsBuoy			= false;
		}}).collect(Collectors.toList());
		mBleViewModel.setDeviceListAdapter(new DeviceListAdapter(pairedlist,
				(seekerid, isChecked) -> {	mBleViewModel.setSelected(seekerid, isChecked);
											mMapViewModel.setSelected(seekerid, isChecked);},
				(seekerid, isChecked) -> mBleViewModel.setBuoy(seekerid, isChecked)
		));
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		/* 対象外なので、無視 */
		if (requestCode != REQUEST_PERMISSIONS) return;

		/* 権限リクエストの結果を取得する. */
		long ngcnt = Arrays.stream(grantResults).filter(value -> value != PackageManager.PERMISSION_GRANTED).count();
		if (ngcnt > 0) {
			ErrDialog.create(MainActivity.this, "このアプリには必要な権限です。\n再起動後に許可してください。\n終了します。").show();
		}
		else {
			mMapViewModel.Permission().postValue(true);
			bindUwsService();
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != REQUEST_LOCATION_SETTINGS) return;	/* 対象外 */
		switch (resultCode) {
			case Activity.RESULT_OK:
				mIsSettingLocationON = true;
				bindUwsService();
				break;
			case Activity.RESULT_CANCELED:
				ErrDialog.create(MainActivity.this, "このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").show();
				break;
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		TLog.d("");
		unbindService(mCon);
		mCon = null;
	}

	/** **********
	 * サービスBind
	 * **********/
	private void bindUwsService() {
		if(mCon != null) {
			TLog.d("すでに、Uwsサービス起動済.処理不要.");
			return;
		}

		/* Bluetooth未サポート */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			TLog.d("Bluetooth未サポートの端末.何もしない.");
			return;
		}

		/* 権限が許可されていない */
		if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			TLog.d("Bluetooth権限なし.何もしない.");
			return;
		}

		/* Bluetooth未サポート */
		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null) {
			TLog.d("Bluetooth未サポートの端末.何もしない.");
			return;
		}

		/* Bluetooth未サポート */
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			TLog.d("Bluetooth未サポートの端末.何もしない.");
			return;
		}

		/* Bluetooth ON/OFF判定 */
		if( !bluetoothAdapter.isEnabled()) {
			TLog.d("Bluetooth OFF.何もしない.");
			return;
		}

		if( !mIsSettingLocationON) {
			TLog.d("位置情報がOFF.何もしない.");
			return;
		}

		mCon = createServiceConnection();

		/* Bluetoothサービス起動 */
		Intent intent = new Intent(MainActivity.this, UwsServerService.class);
		bindService(intent, mCon, Context.BIND_AUTO_CREATE);
		TLog.d("All Green. -> Uwsサービス起動");
	}

	/* Serviceコールバック */
	private ServiceConnection createServiceConnection() {
		return new ServiceConnection() {
			@Override
			public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
				IUwsServer serverIf = IUwsServer.Stub.asInterface(iBinder);
				mBleViewModel.onServiceConnected(serverIf);

				/* Callback設定 */
				try {
					serverIf.setListners(new IHearbertChangeListner.Stub() {
						@Override
						public void OnChange(String name, String addr, long datetime, int hearbeat) {
							runOnUiThread(() -> {
								mBleViewModel.setHeartBeat(name, addr, datetime, (short)hearbeat);
							});
						}
					}, new ILocationChangeListner.Stub() {
						@Override
						public void OnChange(String name, String addr, long datetime, Location loc) {
							runOnUiThread(() -> {
								mBleViewModel.setLocation(name, addr, datetime, loc);
							});
						}
					}, new IStatusNotifier.Stub() {
						@Override
						public void OnChangeStatus(String name, String addr, int resourceid) {
							runOnUiThread(() -> {
								mBleViewModel.OnChangeStatus(name, addr, resourceid);
							});
						}
					});
				}
				catch(RemoteException e) { e.printStackTrace(); }

				/* 起動チェックOK */
				try { serverIf.notifyStartCheckCleared();}
				catch (RemoteException e) { e.printStackTrace(); }
			}

			@Override
			public void onServiceDisconnected(ComponentName componentName) {
				mBleViewModel.onServiceDisconnected();
			}
		};
	}
}
