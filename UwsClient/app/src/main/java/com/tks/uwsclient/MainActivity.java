package com.tks.uwsclient;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.IntentSender;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
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
import com.tks.uwsclient.Constants.Sender;
import static com.tks.uwsclient.Constants.ACTION.FINALIZE;

public class MainActivity extends AppCompatActivity {
	private final static int	REQUEST_LOCATION_SETTINGS	= 1111;
	private final static int	REQUEST_PERMISSIONS			= 2222;
	private FragMainViewModel	mViewModel;

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
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
			else
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
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
						else {
							if (checkExecution(getApplicationContext()))
								mViewModel.notifyStartCheckCleared();
						}
					});
			startForResult.launch(enableBtIntent);
		}

		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(this).get(FragMainViewModel.class);
		/* Lock/Lock解除 設定 */
		mViewModel.UnLock().observe(this, new Observer<Pair<Sender, Boolean>>() {
			@Override
			public void onChanged(Pair<Sender, Boolean> pair) {
				if(pair.first == Sender.Service) return;
				boolean isUnLock = pair.second;

				/* UI更新はFragMainで実行 */
				if( !isUnLock) {
					TLog.d("mViewModel.getSeekerId()={0}", mViewModel.getSeekerId());

					/* ペアリング済デバイスを取得 */
					if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
						if (ActivityCompat.checkSelfPermission(MainActivity.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
							throw new RuntimeException("ここでは、権限不足はありえない。");
					}
					Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
					if(devices.size()==0)
						ErrDialog.create(MainActivity.this, "ペアリング済デバイスがありません。\n先にペアリングを終了してください。\n終了します。").show();
					else if(devices.size()==1) {
						new Handler().postDelayed(() -> {
							/* デバイス取得 */
							BluetoothDevice device = (BluetoothDevice)devices.toArray()[0];
							/* Bluetooth通信開始 */
							mViewModel.startBt(mViewModel.getSeekerId(), device);
						}, 100);
					}
					/* 選択Dialog(ペアリング済デバイスから一つを選ぶ) */
					final String[] items = devices.stream().map(i -> i.getName() + " : " + i.getAddress()).toArray(String[]::new);
					new AlertDialog.Builder(MainActivity.this)
							.setTitle("Btサーバ選択")
							.setItems(items, new DialogInterface.OnClickListener() {
								@Override
								public void onClick(DialogInterface dialog, int which) {
									/* Bluetooth通信開始 */
									new Handler().postDelayed(() -> {
										/* デバイス取得 */
										BluetoothDevice device = (BluetoothDevice)devices.toArray()[which];
										/* Bluetooth通信開始 */
										mViewModel.startBt(mViewModel.getSeekerId(), device);
									}, 100);
								}
							})
							.show();
				}
				else {
					TLog.d("サービスStop要求 UnLock isUnLock={0}", isUnLock);
					mViewModel.stopBt();
				}
			}
		});

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
							catch (IntentSender.SendIntentException sie) {
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
			if(checkExecution(getApplicationContext()))
				mViewModel.notifyStartCheckCleared();
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

			if(checkExecution(getApplicationContext()))
				mViewModel.notifyStartCheckCleared();

			/* サービス状態を取得 */
			StatusInfo si;
			try { si = serviceIf.getServiceStatus(); }
			catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException(e.getMessage()); }

			TLog.d("si=(seekerid={0} Status={1})", si.getSeekerId(), si.getStatus());

			/* サービス状態が、BT接続中 */
			List<Integer> connctstatuses = Arrays.asList(R.string.status_btconnecting, R.string.status_btconnected_and_loc_beat);
			if(connctstatuses.contains(si.getStatus())) {
				/* SeekerIdを設定 */
				mViewModel.setSeekerIdSmoothScrollToPosition(si.getSeekerId());
				/* 画面を接続中に更新 */
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
	}

	/* 実行前の権限/条件チェック */
	private boolean checkExecution(Context context) {
		/* Bluetooth未サポート */
		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE))
			return false;

		/* 権限が許可されていない */
		if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			return false;

		/* Bluetooth未サポート */
		final BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null)
			return false;

		/* Bluetooth未サポート */
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null)
			return false;

		/* Bluetooth ON/OFF判定 */
		if( !bluetoothAdapter.isEnabled())
			return false;

		/* 設定の位置情報ON/OFF判定 */
		if( !mViewModel.mIsSetedLocationON)
			return false;

		Set<BluetoothDevice> devices = BluetoothAdapter.getDefaultAdapter().getBondedDevices();
		if (devices.size() == 0)
			ErrDialog.create(MainActivity.this, "ペアリング済デバイスがありません。\n先にペアリングを終了してください。\n終了します。").show();

		return true;
	}
}
