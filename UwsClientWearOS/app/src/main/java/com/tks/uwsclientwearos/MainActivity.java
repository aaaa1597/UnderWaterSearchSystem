package com.tks.uwsclientwearos;

import java.util.Arrays;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
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
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Pair;
import com.tks.uwsclientwearos.ui.FragMainViewModel;
import com.tks.uwsclientwearos.Constants.Sender;
import static com.tks.uwsclientwearos.Constants.ACTION.FINALIZE;
import static com.tks.uwsclientwearos.Constants.SERVICE_STATUS_CON_LOC_BEAT;

public class MainActivity extends AppCompatActivity {
//	private final static int	REQUEST_LOCATION_SETTINGS	= 1111;	/* ← TicWatch e2にはそもそも実装がないので、チェックしない。常にmIsSetedLocationON = true。 */
	private final static int	REQUEST_PERMISSIONS			= 2222;
	private	FragMainViewModel	mViewModel;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TLog.d("MainActivity.class={0}", MainActivity.class);

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if( !getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			ErrDialog.create(MainActivity.this, "Bluetoothが、未サポートの端末です。").show();

		/* 権限(Bluetooth/位置情報)が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
				requestPermissions(new String[]{Manifest.permission.BODY_SENSORS, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
			else
				requestPermissions(new String[]{Manifest.permission.BODY_SENSORS, Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
		}

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

		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(this).get(FragMainViewModel.class);
		/* ↓↓↓ TicWatch e2にはそもそも実装がないので、チェックしない。常にmIsSetedLocationON = true。 */
		mViewModel.mIsSetedLocationON = true;
//		/* 設定の位置情報ON/OFFチェック */
//		LocationSettingsRequest locationSettingsRequest = new LocationSettingsRequest.Builder().build();
//		SettingsClient settingsClient = LocationServices.getSettingsClient(this);
//		settingsClient.checkLocationSettings(locationSettingsRequest)
//				.addOnSuccessListener(this, locationSettingsResponse -> {
//					mViewModel.mIsSetedLocationON = true;
//				})
//				.addOnFailureListener(this, exception -> {
//					int statusCode = ((ApiException)exception).getStatusCode();
//					switch (statusCode) {
//						case LocationSettingsStatusCodes.RESOLUTION_REQUIRED:
//							try {
//								ResolvableApiException rae = (ResolvableApiException)exception;
//								rae.startResolutionForResult(MainActivity.this, REQUEST_LOCATION_SETTINGS);
//							}
//							catch (SendIntentException sie) {
//								ErrDialog.create(MainActivity.this, "システムエラー!\n再起動で直ることがあります。\n終了します。").show();
//							}
//							break;
//						case LocationSettingsStatusCodes.SETTINGS_CHANGE_UNAVAILABLE:
//							ErrDialog.create(MainActivity.this, "このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").show();
//							break;
//						case LocationSettingsStatusCodes.DEVELOPER_ERROR:
//							if(((ApiException)exception).getMessage().contains("Not implemented")) {
//								/* checkLocationSettings()の実装がない=常にONと想定する。 */
//								mViewModel.mIsSetedLocationON = true;
//								break;
//							}
//							ErrDialog.create(MainActivity.this, "位置情報の機能が存在しない端末です。\n動作しないので、終了します。").show();
//							break;
//					}
//				});
//		/* ↑↑↑ TicWatch e2にはそもそも実装がないので、チェックしない。常にmIsSetedLocationON = true。 */
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

//	/* ↓↓↓ TicWatch e2にはそもそも実装がないので、チェックしない。常にmIsSetedLocationON = true。 */
//	@Override
//	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//		super.onActivityResult(requestCode, resultCode, data);
//		if (requestCode != REQUEST_LOCATION_SETTINGS) return;	/* 対象外 */
//		switch (resultCode) {
//			case Activity.RESULT_OK:
//				mViewModel.mIsSetedLocationON = true;
//				break;
//			case Activity.RESULT_CANCELED:
//				ErrDialog.create(MainActivity.this, "このアプリには位置情報をOnにする必要があります。\n再起動後にOnにしてください。\n終了します。").show();
//				break;
//		}
//	}
//	/* ↑↑↑ TicWatch e2にはそもそも実装がないので、チェックしない。常にmIsSetedLocationON = true。 */


	@Override
	protected void onStart() {
		super.onStart();
		TLog.d("xxxxx");
		IntentFilter filter = new IntentFilter();
		filter.addAction(FINALIZE);
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
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

	@Override
	protected void onDestroy() {
		super.onDestroy();
		TLog.d("xxxxx");
	}

	/* Serviceからの終了要求 受信設定 */
	BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if( !intent.getAction().equals(FINALIZE))
				return;
			unbindService(mCon);
			LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
			ErrDialog.create(MainActivity.this, "裏で動作している位置情報/BLEが終了しました。\nアプリも終了します。").show();
		}
	};

	private final ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			IClientService serviceIf = IClientService.Stub.asInterface(iBinder);
			mViewModel.setClientServiceIf(serviceIf);

			/* サービス状態を取得 */
			StatusInfo si;
			try { si = serviceIf.getServiceStatus(); }
			catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException(e.getMessage()); }

			TLog.d("si=(seekerid={0} Status={1})", si.getSeekerId(), si.getStatus());

			/* サービス状態が、BT接続中 */
			if(si.getStatus() == SERVICE_STATUS_CON_LOC_BEAT) {
				/* SeekerIdを設定 */
				mViewModel.setSeekerIdSmoothScrollToPosition(si.getSeekerId());
				/* 画面をアドバタイズ中/接続中に更新 */
				runOnUiThread(() -> {
					boolean ischecked = ((SwitchCompat)findViewById(R.id.swhUnLock)).isChecked();
					if(ischecked)
						mViewModel.UnLock().setValue(Pair.create(Sender.Service, false));
				});
			}
			else {
				/* 画面をIDLE中に更新 */
				runOnUiThread(() -> {
					boolean ischecked = ((SwitchCompat)findViewById(R.id.swhUnLock)).isChecked();
					if(!ischecked)
						mViewModel.UnLock().setValue(Pair.create(Sender.Service, true));
				});
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			mViewModel.setClientServiceIf(null);
		}
	};

	/* フォアグランドサービス起動 */
	private Intent mStartServiceintent = null;
	private void startForeServ() {
		if(mStartServiceintent != null) return;
		/* サービス(位置情報+BLE)起動 */
		mStartServiceintent = new Intent(MainActivity.this, UwsClientService.class);
		mStartServiceintent.setAction(Constants.ACTION.INITIALIZE);
		startForegroundService(mStartServiceintent);
		/* サービス(脈拍)起動 */
		Intent intent = new Intent(MainActivity.this, UwsHeartBeatService.class);
		intent.setAction(Constants.ACTION.INITIALIZE);
		startForegroundService(intent);
	}

	/* フォアグランドサービス終了 */
	private void stopForeServ() {
		/* サービス起動済チェック */
		if(mStartServiceintent == null) return;
		/* サービス(位置情報+BLE)終了 */
		mStartServiceintent = null;
		Intent intent = new Intent(MainActivity.this, UwsClientService.class);
		intent.setAction(FINALIZE);
		startService(intent);
		/* サービス(脈拍)終了 */
		Intent intent2= new Intent(MainActivity.this, UwsHeartBeatService.class);
		intent2.setAction(FINALIZE);
		startService(intent2);
	}

}
