package com.tks.uwsclientwearos;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.material.snackbar.Snackbar;
import java.util.Arrays;
import java.util.Locale;

import com.tks.uwsclientwearos.ui.FragMainViewModel;
import static com.tks.uwsclientwearos.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsclientwearos.Constants.UWS_NG_AIDL_REMOTE_ERROR;
import static com.tks.uwsclientwearos.Constants.UWS_NG_ADAPTER_NOTFOUND;
import static com.tks.uwsclientwearos.Constants.UWS_NG_PERMISSION_DENIED;
import static com.tks.uwsclientwearos.Constants.UWS_NG_SERVICE_NOTFOUND;
import static com.tks.uwsclientwearos.Constants.UWS_NG_GATTSERVER_NOTFOUND;

public class MainActivity extends AppCompatActivity {
	private	FragMainViewModel	mViewModel;
	private final static int	REQUEST_LOCATION_SETTINGS	= 1111;
	private final static int	REQUEST_PERMISSIONS	= 2222;
	private final static int	LOC_UPD_INTERVAL	= 1000;
	private FusedLocationProviderClient	mFusedLocationClient;
	private final LocationRequest		mLocationRequest = LocationRequest.create().setInterval(LOC_UPD_INTERVAL)
																					.setFastestInterval(LOC_UPD_INTERVAL/2)
																					.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	private final LocationCallback		mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			TLog.d("1秒定期 (緯度:{0} 経度:{1})", String.format(Locale.JAPAN, "%1$.12f", location.getLatitude()), String.format(Locale.JAPAN, "%1$.12f", location.getLongitude()));
			mViewModel.Longitude().setValue(location.getLongitude());
			mViewModel.Latitude().setValue(location.getLatitude());
			mViewModel.HearBeat().setValue((short)(mDmyHeartBeat++));
		}
	};
	private int mDmyHeartBeat = 30;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(this).get(FragMainViewModel.class);

		LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		final boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		final boolean wifiEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		TLog.d("gpsEnabled={0} wifiEnabled={1}", gpsEnabled, wifiEnabled);

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if( !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			ErrDialog.create(MainActivity.this, R.string.error_notsupported).show();
		}

		/* Bluetooth権限が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
			else
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
		}

		/* 設定の位置情報ON/OFFチェック */
		LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder().addLocationRequest(mLocationRequest).build();
		SettingsClient settingsClient = LocationServices.getSettingsClient(this);
		settingsClient.checkLocationSettings(locationSettingsRequest)
				.addOnSuccessListener(this, locationSettingsResponse -> {
					mViewModel.mIsSettedLocationON = true;
					/* Bleサーバへの接続処理開始 */
					mViewModel.bindBleService(getApplicationContext(), mCon);
				})
				.addOnFailureListener(this, exception -> {
					int statusCode = ((ApiException)exception).getStatusCode();
					switch (statusCode) {
						case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
							try {
								ResolvableApiException rae = (ResolvableApiException)exception;
								rae.startResolutionForResult(MainActivity.this, REQUEST_LOCATION_SETTINGS);
							}
							catch (SendIntentException sie) {
								ErrDialog.create(MainActivity.this, "システムエラー!\n再起動で直ることがあります。\n終了します。").show();
							}
							break;
						case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
							ErrDialog.create(MainActivity.this, "このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").show();
							break;
						case LocationSettingsStatusCodes.DEVELOPER_ERROR:
							if(((ApiException)exception).getMessage().contains("Not implemented")) {
								/* checkLocationSettings()の実装がない=常にONと想定する。 */
								/* Bleサーバへの接続処理開始 */
								mViewModel.mIsSettedLocationON = true;
								mViewModel.bindBleService(getApplicationContext(), mCon);
								break;
							}
							ErrDialog.create(MainActivity.this, "位置情報の機能が存在しない端末です。\n動作しないので、終了します。").show();
							break;
					}
				});

		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		/* Bluetooth未サポート判定 未サポートならエラーpopupで終了 */
		if (bluetoothAdapter == null) {
			ErrDialog.create(MainActivity.this, "Bluetooth未サポートの端末です。\n終了します。").show();
		}
		/* Bluetooth ON/OFF判定 -> OFFならONにするようにリクエスト */
		else if( !bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
					result -> {
						if(result.getResultCode() != Activity.RESULT_OK) {
							ErrDialog.create(MainActivity.this, "BluetoothがOFFです。ONにして操作してください。\n終了します。").show();
						}
						else {
							/* Bleサーバへの接続処理開始 */
							mViewModel.bindBleService(getApplicationContext(), mCon);
						}
					});
			startForResult.launch(enableBtIntent);
		}

		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
		mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());

		/* Bleサーバへの接続処理開始 */
		mViewModel.bindBleService(getApplicationContext(), mCon);
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
			return;
		}
		else {
			/* Bleサーバへの接続処理開始 */
			mViewModel.bindBleService(getApplicationContext(), mCon);
		}
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != REQUEST_LOCATION_SETTINGS) return;	/* 対象外 */
		switch (resultCode) {
			case Activity.RESULT_OK:
				mViewModel.mIsSettedLocationON = true;
				/* Bleサーバへの接続処理開始 */
				mViewModel.bindBleService(getApplicationContext(), mCon);
				break;
			case Activity.RESULT_CANCELED:
				ErrDialog.create(MainActivity.this, "このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").show();
				break;
		}
	}

	private final ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			int ret = mViewModel.onServiceConnected(IBleClientService.Stub.asInterface(service));
			if(ret == UWS_NG_AIDL_REMOTE_ERROR)
				ErrDialog.create(MainActivity.this, "システム異常!! サービスとの接続に異常が発生しました。!\nシステム異常なので、終了します。").show();
			else if(ret == UWS_NG_PERMISSION_DENIED)
				ErrDialog.create(MainActivity.this, "このアプリに権限がありません!!\n終了します。").show();
			else if(ret == UWS_NG_SERVICE_NOTFOUND)
				ErrDialog.create(MainActivity.this, "この端末はBluetoothに対応していません!!\n終了します。").show();
			else if(ret == UWS_NG_ADAPTER_NOTFOUND)
				ErrDialog.create(MainActivity.this, "この端末はBluetoothに対応していません!!\n終了します。").show();
			else if(ret == UWS_NG_GATTSERVER_NOTFOUND)
				ErrDialog.create(MainActivity.this, "Ble初期化に失敗!!\n終了します。再起動で直る可能性があります。").show();
			else if(ret != UWS_NG_SUCCESS)
				ErrDialog.create(MainActivity.this, "原因不明のエラーが発生しました!!\n終了します。").show();

			/* 監視イベント一括登録 */
			setObserve();

			return;
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			mViewModel.onServiceDisconnected();
		}
	};

	@Override
	protected void onDestroy() {
		super.onDestroy();
		mFusedLocationClient.removeLocationUpdates(mLocationCallback);
		unbindService(mCon);
	}

	/** **************************************************************************************
	 * LiveDataのObserve一括設定
	 * onCreate()で実行すると、初期化が完了してない状態で、動き始めてエラーになるので、初期化完了後に実行する。
	 ** **************************************************************************************/
	private void setObserve() {
		/* Lock ON/OFF */
		mViewModel.UnLock().observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean isUnLock) {
				if(isUnLock) {
					mViewModel.AdvertisingFlg().postValue(false);
				}
				else {
					/* アドバタイズ開始 */
					mViewModel.AdvertisingFlg().setValue(true);
				}
			}
		});
		/* アドバタイズON/OFF */
		mViewModel.AdvertisingFlg().observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean advertisingFlg) {
				if(advertisingFlg) {
					int ret = mViewModel.startAdvertising();
					if(ret != UWS_NG_SUCCESS)
						ErrDialog.create(MainActivity.this, "システム異常!! アドバタイズ開始に失敗!!\nシステム異常なので、終了します。").show();
				}
				else {
					int ret = mViewModel.stopAdvertising();
					if(ret != UWS_NG_SUCCESS)
						ErrDialog.create(MainActivity.this, "システム異常!! アドバタイズ開始に失敗!!\nシステム異常なので、終了します。").show();
				}
			}
		});
		/* Snackbar表示要求 */
		mViewModel.ShowSnacbar().observe(this, showMsg -> {
			Snackbar.make(findViewById(R.id.root_view), showMsg, Snackbar.LENGTH_LONG).show();
		});
		/* エラーメッセージ表示要求 */
		mViewModel.ShowErrMsg().observe(this, showMsg -> {
			new Throwable().printStackTrace();
			ErrDialog.create(MainActivity.this, showMsg).show();
		});
	}
}
