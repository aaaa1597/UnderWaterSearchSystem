package com.tks.uwsclient;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
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
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;

import com.google.android.material.snackbar.Snackbar;
import com.tks.uwsclient.ui.FragMainViewModel;

import static com.tks.uwsclient.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsclient.Constants.UWS_NG_AIDL_CALLBACK_FAILED;
import static com.tks.uwsclient.Constants.UWS_NG_AIDL_INIT_BLE_FAILED;
import static com.tks.uwsclient.Constants.UWS_NG_ADAPTER_NOTFOUND;
import static com.tks.uwsclient.Constants.UWS_NG_PERMISSION_DENIED;
import static com.tks.uwsclient.Constants.UWS_NG_SERVICE_NOTFOUND;
import static com.tks.uwsclient.Constants.UWS_NG_GATTSERVER_NOTFOUND;

public class MainActivity extends AppCompatActivity {
	private	FragMainViewModel	mViewModel;
	private final static int	REQUEST_PERMISSIONS	= 0x2222;

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
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
		}

		/* Bluetooth権限が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
				requestPermissions(new String[]{Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
			else
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

		/* Bleサーバへの接続処理開始 */
		mViewModel.bindBleService(getApplicationContext(), mCon);
	}

	private final ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			int ret = mViewModel.onServiceConnected(IBleClientService.Stub.asInterface(service));
			if(ret == UWS_NG_AIDL_CALLBACK_FAILED)
				ErrPopUp.create(MainActivity.this).setErrMsg("システム異常!! Callback登録失敗!!\nシステム異常なので、終了します。").Show(MainActivity.this);
			else if(ret == UWS_NG_AIDL_INIT_BLE_FAILED)
				ErrPopUp.create(MainActivity.this).setErrMsg("システム異常!! BT初期化で失敗!!\nシステム異常なので、終了します。").Show(MainActivity.this);
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
		mViewModel.UnLock().observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean isUnLock) {
				TLog.d("UnLock isLock={0}", isUnLock);
				if( !isUnLock)
					/* アドバタイズ開始 */
					mViewModel.AdvertisingFlg().setValue(true);
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
				/* TODO TODO TODO TODO */
			}
		});
		/* Snackbar表示要求 */
		mViewModel.ShowSnacbar().observe(this, showMsg -> {
			new Throwable().printStackTrace();
			Snackbar.make(findViewById(R.id.root_view), showMsg, Snackbar.LENGTH_LONG).show();
		});
		/* エラーメッセージ表示要求 */
		mViewModel.ShowErrMsg().observe(this, showMsg -> {
			new Throwable().printStackTrace();
			ErrPopUp.create(MainActivity.this).setErrMsg(showMsg).Show(MainActivity.this);
		});
	}
}