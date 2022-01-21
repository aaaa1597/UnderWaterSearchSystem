package com.tks.uwsclient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender.SendIntentException;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Pair;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.api.ResolvableApiException;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.LocationSettingsRequest;
import com.google.android.gms.location.LocationSettingsStatusCodes;
import com.google.android.gms.location.SettingsClient;
import com.tks.uwsclient.ui.FragMainViewModel;
import java.util.Arrays;

import static com.tks.uwsclient.Constants.ACTION.FINALIZEFROMS;
import static com.tks.uwsclient.Constants.Sender;
import static com.tks.uwsclient.Constants.SERVICE_STATUS_AD_LOC_BEAT;
import static com.tks.uwsclient.Constants.SERVICE_STATUS_CON_LOC_BEAT;

public class MainActivity extends AppCompatActivity {
	private final static int	REQUEST_LOCATION_SETTINGS	= 1111;
	private final static int	REQUEST_PERMISSIONS			= 2222;
	private FragMainViewModel	mViewModel;

	private Intent mStartServiceintent = null;
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TLog.d("MainActivity.class={0}", MainActivity.class);

		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(this).get(FragMainViewModel.class);
		/* Lock/Lock解除 設定 */
		mViewModel.UnLock().observe(this, new Observer<Pair<Sender, Boolean>>() {
			@Override
			public void onChanged(Pair<Sender, Boolean> pair) {
				if(pair.first == Sender.Service) return;
				boolean isUnLock = pair.second;

				TLog.d("Service要求の処理 UnLock isUnLock={0}", isUnLock);
				/* UIの処理はFragMainで実行している。 */
				if( !isUnLock) {
					/* 実行前チェック */
					boolean ret = checkExecution(getApplicationContext());
					if( !ret) {
						TLog.d("実行前チェックError!! 条件が揃ってない。 ret={0}", ret);
						return;
					}
					TLog.d("mViewModel.getSeekerId()={0}", mViewModel.getSeekerId());
					mViewModel.startUws(mViewModel.getSeekerId());
				}
				else {
					mViewModel.stopUws();
				}
			}
		});

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if( !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			ErrDialog.create(MainActivity.this, "Bluetoothが、未サポートの端末です。").show();

		/* 権限(Bluetooth/位置情報)が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
			else
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
		}

		/* 設定の位置情報ON/OFFチェック */
		LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder().build();
		SettingsClient settingsClient = LocationServices.getSettingsClient(this);
		settingsClient.checkLocationSettings(locationSettingsRequest)
				.addOnSuccessListener(this, locationSettingsResponse -> {
					mViewModel.mIsSetedLocationON = true;
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
								mViewModel.mIsSetedLocationON = true;
								break;
							}
							ErrDialog.create(MainActivity.this, "位置情報の機能が存在しない端末です。\n動作しないので、終了します。").show();
							break;
					}
				});

		/* BluetoothManager取得 */
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
					});
			startForResult.launch(enableBtIntent);
		}

		mFilter.addAction(FINALIZEFROMS);
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
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);
		if (requestCode != REQUEST_LOCATION_SETTINGS) return;	/* 対象外 */
		switch (resultCode) {
			case Activity.RESULT_OK:
				mViewModel.mIsSetedLocationON = true;
				break;
			case Activity.RESULT_CANCELED:
				ErrDialog.create(MainActivity.this, "このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").show();
				break;
		}
	}

	@Override
	protected void onStart() {
		super.onStart();
		TLog.d("xxxxx");
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, mFilter);
		startForeServ();
		bindService(new Intent(getApplicationContext(), UwsClientService.class), mCon, Context.BIND_AUTO_CREATE);
	}

	@Override
	protected void onStop() {
		super.onStop();
		unbindService(mCon);
//		stopForeServ();			通知からの終了だけをサポートする。
		LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
		TLog.d("xxxxx");
	}

	private final ServiceConnection mCon = new ServiceConnection() {
		@Override public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			IClientService ServiceIf = IClientService.Stub.asInterface(iBinder);
			mViewModel.setClientServiceIf(ServiceIf);

			/* サービス状態を取得 */
			StatusInfo si;
			try { si = ServiceIf.getServiceStatus(); }
			catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException(e.getMessage()); }

			TLog.d("si=(seekerid={0} Status={1})", si.getSeekerId(), si.getStatus());

			/* サービス状態が、アドバタイズ中/接続中 */
			if(si.getStatus() == SERVICE_STATUS_AD_LOC_BEAT || si.getStatus() == SERVICE_STATUS_CON_LOC_BEAT) {
				/* SeekerIdを設定 */
				mViewModel.setSeekerIdSmoothScrollToPosition(si.getSeekerId());
				/* 画面をアドバタイズ中/接続中に更新 */
				mViewModel.UnLock().postValue(Pair.create(Sender.Service, false));
			}
		}
		@Override public void onServiceDisconnected(ComponentName componentName) {
			mViewModel.setClientServiceIf(null);
		}
	};

	/* フォアグランドサービス起動 */
	private void startForeServ() {
		if(mStartServiceintent != null) {
			TLog.d("サービス起動済。処理不要.");
			return;
		}
		/* サービス起動 */
		mStartServiceintent = new Intent(MainActivity.this, UwsClientService.class);
		mStartServiceintent.setAction(Constants.ACTION.INITIALIZE);
		startForegroundService(mStartServiceintent);
	}

	/* フォアグランドサービス終了 */
	private void stopForeServ() {
		/* サービス起動済チェック */
		if(mStartServiceintent == null) {
			TLog.d("サービス起動してないので終了処理不要。");
			return;
		}
		/* サービス終了 */
		mStartServiceintent = null;
		Intent intent = new Intent(MainActivity.this, UwsClientService.class);
		intent.setAction(Constants.ACTION.FINALIZE);
		startService(intent);
	}

	/* 実行前の権限/条件チェック */
	private boolean checkExecution(Context context) {
		/* Bluetooth未サポート */
		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			ErrDialog.create(MainActivity.this, "Bluetooth未サポートの端末です。\n終了します。").show();
			return false;
		}

		/* 権限が許可されていない */
		if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			ErrDialog.create(MainActivity.this, "Bluetoothや位置情報に必要な権限がありません。\n終了します。").show();
			return false;
		}

		/* Bluetooth未サポート */
		final BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null) {
			ErrDialog.create(MainActivity.this, "Bluetooth未サポートの端末です。\n終了します。").show();
			return false;
		}

		/* Bluetooth未サポート */
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			ErrDialog.create(MainActivity.this, "Bluetooth未サポートの端末です。\n終了します。").show();
			return false;
		}

		/* Bluetooth ON/OFF判定 */
		if( !bluetoothAdapter.isEnabled()) {
			ErrDialog.create(MainActivity.this, "設定のBluetoothがOFFになっています。\nONにして下さい。\n終了します。").show();
			return false;
		}

		/* 設定の位置情報ON/OFF判定 */
		if( !mViewModel.mIsSetedLocationON) {
			ErrDialog.create(MainActivity.this, "設定の位置情報がOFFになっています。\nONにして下さい。\n終了します。").show();
			return false;
		}

		return true;
	}

	/* Serviceからの終了要求 受信設定 */
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			unbindService(mCon);
			LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
			ErrDialog.create(MainActivity.this, "裏で動作している位置情報/BLEが終了しました。\nアプリも終了します。").show();
		}
	};
	IntentFilter mFilter = new IntentFilter();
}
