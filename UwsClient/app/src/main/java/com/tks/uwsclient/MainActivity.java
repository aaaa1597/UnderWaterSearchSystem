package com.tks.uwsclient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
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
import com.google.android.material.snackbar.Snackbar;
import com.tks.uwsclient.ui.FragMainViewModel;
import com.google.android.gms.location.LocationCallback;

import static com.tks.uwsclient.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsclient.Constants.UWS_NG_AIDL_REMOTE_ERROR;
import static com.tks.uwsclient.Constants.UWS_NG_ADAPTER_NOTFOUND;
import static com.tks.uwsclient.Constants.UWS_NG_PERMISSION_DENIED;
import static com.tks.uwsclient.Constants.UWS_NG_SERVICE_NOTFOUND;
import static com.tks.uwsclient.Constants.UWS_NG_GATTSERVER_NOTFOUND;

import java.util.Arrays;
import java.util.Date;
import java.util.Random;

public class MainActivity extends AppCompatActivity {
	private	FragMainViewModel	mViewModel;
	private final static int	REQUEST_LOCATION_SETTINGS	= 1111;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(this).get(FragMainViewModel.class);

		LocationManager lm = (LocationManager)getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
		final boolean gpsEnabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
		final boolean wifiEnabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
		TLog.d("TODO1 gpsEnabled={0} wifiEnabled={1}", gpsEnabled, wifiEnabled);

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
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
					mViewModel.mIsSettingLocationON = true;
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
								TLog.d("PendingIntent unable to execute request.");
							}
							break;
						case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
							ErrPopUp.create(MainActivity.this).setErrMsg("このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").Show(MainActivity.this);
							break;
					}
				});

		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		/* Bluetooth未サポート判定 未サポートならエラーpopupで終了 */
		if (bluetoothAdapter == null) {
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
		}
		/* Bluetooth ON/OFF判定 -> OFFならONにするようにリクエスト */
		else if( !bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
					result -> {
						if(result.getResultCode() != Activity.RESULT_OK) {
							ErrPopUp.create(MainActivity.this).setErrMsg("BluetoothがOFFです。ONにして操作してください。\n終了します。").Show(MainActivity.this);
						}
						else {
							/* Bleサーバへの接続処理開始 */
							mViewModel.bindBleService(getApplicationContext(), mCon);
						}
					});
			startForResult.launch(enableBtIntent);
		}

		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

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
			ErrPopUp.create(MainActivity.this).setErrMsg("このアプリには必要な権限です。\n再起動後に許可してください。\n終了します。").Show(MainActivity.this);
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
				mViewModel.mIsSettingLocationON = true;
				/* Bleサーバへの接続処理開始 */
				mViewModel.bindBleService(getApplicationContext(), mCon);
				break;
			case Activity.RESULT_CANCELED:
				ErrPopUp.create(MainActivity.this).setErrMsg("このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").Show(MainActivity.this);
				break;
		}
	}

	private final ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			int ret = mViewModel.onServiceConnected(IBleClientService.Stub.asInterface(service));
			if(ret == UWS_NG_AIDL_REMOTE_ERROR)
				ErrPopUp.create(MainActivity.this).setErrMsg("システム異常!! サービスとの接続に異常が発生しました。!\nシステム異常なので、終了します。").Show(MainActivity.this);
			else if(ret == UWS_NG_PERMISSION_DENIED)
				ErrPopUp.create(MainActivity.this).setErrMsg("このアプリに権限がありません!!\n終了します。").Show(MainActivity.this);
			else if(ret == UWS_NG_SERVICE_NOTFOUND)
				ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!\n終了します。").Show(MainActivity.this);
			else if(ret == UWS_NG_ADAPTER_NOTFOUND)
				ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!\n終了します。").Show(MainActivity.this);
			else if(ret == UWS_NG_GATTSERVER_NOTFOUND)
				ErrPopUp.create(MainActivity.this).setErrMsg("Ble初期化に失敗!!\n終了します。再起動で直る可能性があります。").Show(MainActivity.this);
			else if(ret != UWS_NG_SUCCESS)
				ErrPopUp.create(MainActivity.this).setErrMsg("原因不明のエラーが発生しました!!\n終了します。").Show(MainActivity.this);

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
		unbindService(mCon);
	}

	/** **************************************************************************************
	 * LiveDataのObserve一括設定
	 * onCreate()で実行すると、初期化が完了してない状態で、動き始めるてエラーになるので、初期化完了語に実行する。
	 ** **************************************************************************************/
	private void setObserve() {
		/* Lock ON/OFF */
		mViewModel.UnLock().observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean isUnLock) {
				TLog.d("UnLock isLock={0}", isUnLock);
				if(isUnLock) {
					mViewModel.AdvertisingFlg().postValue(false);
					mViewModel.Priodic1sNotifyFlg().postValue(false);
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
						ErrPopUp.create(MainActivity.this).setErrMsg("システム異常!! アドバタイズ開始に失敗!!\nシステム異常なので、終了します。").Show(MainActivity.this);
				}
				else {
					int ret = mViewModel.stopAdvertising();
					if(ret != UWS_NG_SUCCESS)
						ErrPopUp.create(MainActivity.this).setErrMsg("システム異常!! アドバタイズ開始に失敗!!\nシステム異常なので、終了します。").Show(MainActivity.this);
				}
			}
		});
		/* 1s定期周期通知 切替え */
		mViewModel.Priodic1sNotifyFlg().observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean priodic1sNotifyFlg) {
				if(priodic1sNotifyFlg) {
					/* ↓Androidでエラーになるから追加するコード。実際はここでエラーにはならない */
					if (ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
					 && ActivityCompat.checkSelfPermission(getApplicationContext(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
						return;
					/* ↑Androidでエラーになるから追加するコード。実際はここでエラーにはならない */
					mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
				}
				else {
					TLog.d("1秒定期 終了");
					mFusedLocationClient.removeLocationUpdates(mLocationCallback);
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
			ErrPopUp.create(MainActivity.this).setErrMsg(showMsg).Show(MainActivity.this);
		});
	}

	private Random						mRandom = new Random(new Date().getTime());
	private final static int			REQUEST_PERMISSIONS			= 2222;
	private FusedLocationProviderClient	mFusedLocationClient;
	private final LocationRequest		mLocationRequest = LocationRequest.create().setInterval(1000)
																					.setFastestInterval(1000/2)
																					.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	private final LocationCallback		mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			TLog.d("aaaaaaaa 1秒定期 (経度:{0} 緯度:{1})", locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
			mViewModel.Longitude().setValue(location.getLongitude());
			mViewModel.Latitude().setValue(location.getLatitude());
			mViewModel.HearBeat().setValue(mRandom.nextInt(40)+30);
			mViewModel.notifyOneShot();
		}
	};
}
