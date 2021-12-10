package com.tks.uws.uwsmember;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.tks.uws.uwsmember.ui.main.FragMainViewModel;
import java.nio.ByteBuffer;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static com.tks.uws.uwsmember.Constants.UWS_CHARACTERISTIC_SAMLE_UUID;
import static com.tks.uws.uwsmember.Constants.createServiceUuid;
import static com.tks.uws.uwsmember.PeripheralAdvertiseService.KEY_NO;
import com.tks.uws.uwsmember.ui.main.FragMainViewModel.ConnectStatus;

public class MainActivity extends AppCompatActivity {
	private final static int				REQUEST_PERMISSIONS = 1111;
	private FragMainViewModel				mViewModel;
	private final HashSet<BluetoothDevice>	mBluetoothCentrals = new HashSet<>();

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.main_activity);
		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(this).get(FragMainViewModel.class);
		mViewModel.PressSetBtn().observe(this, new Observer<Boolean>() {
			@Override
			public void onChanged(Boolean aBoolean) {
				mUwsCharacteristic.setValue(getBytesFromModelView());
			}
		});
		mViewModel.ConnectStatus().observe(this, new Observer<FragMainViewModel.ConnectStatus>() {
			@Override
			public void onChanged(FragMainViewModel.ConnectStatus status) {
				switch (status) {
					/* 初期化 */
					case NONE:
						createOwnCharacteristic();
						break;
					case SETTING_ID:/* 何もしない */ break;
					/* アドバタイズ開始 */
					case START_ADVERTISE:
						Intent intent = new Intent(getApplicationContext(), PeripheralAdvertiseService.class);
						TLog.d("ID ={0}", String.valueOf(mViewModel.getID()));
						intent.putExtra(KEY_NO, mViewModel.getID());
						/* Service起動 */
						bindService(intent, new ServiceConnection() {
							@Override
							public void onServiceConnected(ComponentName name, IBinder service) {
								mViewModel.ConnectStatus().setValue(FragMainViewModel.ConnectStatus.ADVERTISING);
							}
							@Override public void onServiceDisconnected(ComponentName name) {}
						}, Context.BIND_AUTO_CREATE);
						break;
					case ADVERTISING:/* 何もしない */ break;
					/* アドバタイズ停止(サービス終了) */
					case CONNECTED:
						stopService(new Intent(getApplicationContext(), PeripheralAdvertiseService.class));
						break;
				}
			}
		});

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。\n終了します。").Show(MainActivity.this);
		}

		/* 現在値権限とBluetooth権限が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH_ADVERTISE, Manifest.permission.BLUETOOTH_CONNECT}, REQUEST_PERMISSIONS);
			else
				requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
		}

		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		/* Bluetooth未サポート判定 未サポートならエラーpopupで終了 */
		if (bluetoothAdapter == null)
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。").Show(MainActivity.this);
		/* Bluetooth ON/OFF判定 -> OFFならONにするようにリクエスト */
		else if( !bluetoothAdapter.isEnabled()) {
			Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
					result -> {
						if(result.getResultCode() != Activity.RESULT_OK) {
							ErrPopUp.create(MainActivity.this).setErrMsg("BluetoothがOFFです。ONにして操作してください。\n終了します。").Show(MainActivity.this);
						}
						else {
							/* 初期化 */
							createOwnCharacteristic();
						}
					});
			startForResult.launch(enableBtIntent);
		}

		/* 位置情報管理オブジェクト */
		FusedLocationProviderClient flpc = LocationServices.getFusedLocationProviderClient(this);
		flpc.getLastLocation().addOnSuccessListener(this, location -> {
			if (location == null) {
				TLog.d("mLocation={0}", location);
				LocationRequest locreq = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(500).setFastestInterval(300);
				flpc.requestLocationUpdates(locreq, new LocationCallback() {
					@Override
					public void onLocationResult(@NonNull LocationResult locationResult) {
						super.onLocationResult(locationResult);
						TLog.d("locationResult={0}({1},{2})", locationResult, locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
						runOnUiThread(() -> {
							mViewModel.Longitude().setValue(locationResult.getLastLocation().getLongitude());
							mViewModel.Latitude().setValue(locationResult.getLastLocation().getLatitude());
						});
						flpc.removeLocationUpdates(this);
					}
				}, Looper.getMainLooper());
			}
			else {
				TLog.d("mLocation=(経度:{0} 緯度:{1})", location.getLatitude(), location.getLongitude());
				runOnUiThread(() -> {
					mViewModel.Longitude().setValue(location.getLongitude());
					mViewModel.Latitude().setValue(location.getLatitude());
				});
			}
		});

		/* 初期化 */
		createOwnCharacteristic();
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
			/* 初期化 */
			createOwnCharacteristic();
		}
	}

	private BluetoothGattCharacteristic	mUwsCharacteristic;
	private BluetoothGattServer			mGattServer;
	private void createOwnCharacteristic() {
		/* Bluetooth未サポート */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			TLog.d("Bluetooth未サポートの端末.何もしない.");
			return;
		}

		/* 権限が許可されていない */
		if(checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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

		if(mGattServer != null) {
			for(BluetoothDevice device : mBluetoothCentrals)
				mGattServer.cancelConnection(device);
			mGattServer.clearServices();
		}
		mGattServer = bluetoothManager.openGattServer(this, mGattServerCallback);

		/* 全条件クリア 初期化開始 */
		/* 自身が提供するサービスを定義 */
		AtomicInteger lid = new AtomicInteger();
		runOnUiThread(() -> {
			lid.set(mViewModel.getID());
		});
		UUID serviceUuid = UUID.fromString(createServiceUuid(lid.get()));
		BluetoothGattService ownService = new BluetoothGattService(serviceUuid, BluetoothGattService.SERVICE_TYPE_PRIMARY);

		/* 自身が提供するCharacteristic(特性)を定義 : 通知と読込みに対し、読込み許可 */
		mUwsCharacteristic = new BluetoothGattCharacteristic(UWS_CHARACTERISTIC_SAMLE_UUID, BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ, BluetoothGattCharacteristic.PERMISSION_READ);
		mUwsCharacteristic.setValue(getBytesFromModelView());

		/* 定義したサービスにCharacteristic(特性)を付与 */
		ownService.addCharacteristic(mUwsCharacteristic);

		/* 定義したサービスを有効化 */
		mGattServer.addService(ownService);
	}

	private byte[] getBytesFromModelView() {
		Date now = new Date();
		AtomicReference<Double> dlongitude = new AtomicReference<>((double)0);
		AtomicReference<Double> dlatitude = new AtomicReference<>((double)0);
		AtomicReference<Integer> ihearBeat = new AtomicReference<>(0);
		runOnUiThread(() -> {
			dlongitude.set(mViewModel.Longitude().getValue());
			dlatitude.set(mViewModel.Latitude().getValue());
			ihearBeat.set(mViewModel.HearBeat().getValue());
		});

		SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd E HH:mm:ss.SSS Z", Locale.JAPANESE);
		TLog.d("{0} 脈拍:{1} 緯度:{2} 経度:{3}", sdf.format(now), ihearBeat.get(), dlatitude.get(), dlongitude.get());

		byte[] ret = new byte[8 + 8 + 8 + 4];
		byte[] datetime = l2bs(now.getTime());
		System.arraycopy(datetime, 0, ret, 0, datetime.length);
		byte[] longitude = d2bs(dlongitude.get());
		System.arraycopy(longitude, 0, ret, datetime.length, longitude.length);
		byte[] latitude = d2bs(dlatitude.get());
		System.arraycopy(latitude, 0, ret, datetime.length+longitude.length, latitude.length);
		byte[] heartbeat = i2bs(ihearBeat.get());
		System.arraycopy(heartbeat, 0, ret, datetime.length+longitude.length+latitude.length, heartbeat.length);
		return ret;
	}
	private byte[] i2bs(int value) {
		return ByteBuffer.allocate(4).putInt(value).array();
	}
	private byte[] l2bs(long value) {
		return ByteBuffer.allocate(8).putLong(value).array();
	}
	private byte[] d2bs(double value) {
		return ByteBuffer.allocate(8).putDouble(value).array();
	}

	private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
		/**
		 * 接続状態変化通知
		 * @param device
		 * @param status	int: Status of the connect or disconnect operation. BluetoothGatt.GATT_SUCCESS if the operation succeeds.
		 * @param newState	BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile#STATE_CONNECTED
		 */
		@Override
		public void onConnectionStateChange(BluetoothDevice device, int status, int newState) {
			super.onConnectionStateChange(device, status, newState);
			TLog.d("status={0} newState={1}", status, newState);
			TLog.d("status-BluetoothGatt.GATT_SUCCESS({0}) newState-BluetoothGatt.STATE_xxxx(STATE_CONNECTED({1}),STATE_DISCONNECTED({2}))", BluetoothGatt.GATT_SUCCESS, BluetoothGatt.STATE_CONNECTED, BluetoothGatt.STATE_DISCONNECTED);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (newState == BluetoothGatt.STATE_CONNECTED) {
					/* 接続確立 */
					runOnUiThread(() -> { mViewModel.ConnectStatus().setValue(ConnectStatus.CONNECTED);});
					mBluetoothCentrals.add(device);
					TLog.d("Connected to device: {0}", device.getAddress());
				}
				else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
					/* 接続断 */
					runOnUiThread(() -> { mViewModel.ConnectStatus().setValue(ConnectStatus.NONE);});
					mBluetoothCentrals.remove(device);
					TLog.d("Disconnected from device");
				}
			}
			else {
				/* 接続断 */
				runOnUiThread(() -> { mViewModel.ConnectStatus().setValue(ConnectStatus.NONE);});
				mBluetoothCentrals.remove(device);
				TLog.e("{0} : {1}", getString(R.string.status_error_when_connecting), status);
				ErrPopUp.create(MainActivity.this).setErrMsg(getString(R.string.status_error_when_connecting) + ":" + status).Show(MainActivity.this);
			}
		}

		/* 通知/指示受信 */
		@Override
		public void onNotificationSent(BluetoothDevice device, int status) {
			super.onNotificationSent(device, status);
			TLog.d("Notification sent. Status: " + status);
		}

		/* Read要求受信 */
		@Override
		public void onCharacteristicReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicReadRequest(device, requestId, offset, characteristic);

			TLog.d("CentralからのRead要求({0}) 返却値:(UUID:{1},offset{2},val:{3}))", requestId, characteristic.getUuid(), offset, Arrays.toString(characteristic.getValue()));
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, characteristic.getValue());
		}


		/* Write要求受信 */
		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
			super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

			TLog.d("CentralからのWrite要求 受信値:(UUID:{0},vat:{1}))", mUwsCharacteristic.getUuid(), Arrays.toString(value));
			mUwsCharacteristic.setValue(value);
			if (responseNeeded)
				mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
		}

		/* Read要求受信 */
		@Override
		public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
			super.onDescriptorReadRequest(device, requestId, offset, descriptor);

			TLog.d("CentralからのDescriptor_Read要求 返却値:(UUID:{0},vat:{1}))", descriptor.getUuid(), Arrays.toString(descriptor.getValue()));
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
		}

		/* Write要求受信 */
		@Override
		public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
											 BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
											 int offset,
											 byte[] value) {
			super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

			TLog.d("CentralからのDescriptor_Write要求 受信値:(UUID:{0},vat:{1}))", descriptor.getUuid(), Arrays.toString(value));

//            int status = BluetoothGatt.GATT_SUCCESS;
//            if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
//                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
//                boolean supportsNotifications = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
//                boolean supportsIndications   = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
//
//                if (!(supportsNotifications || supportsIndications)) {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                }
//                else if (value.length != 2) {
//                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
//                }
//                else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsDisabled(characteristic);
//                    descriptor.setValue(value);
//                }
//                else if (supportsNotifications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */);
//                    descriptor.setValue(value);
//                }
//                else if (supportsIndications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */);
//                    descriptor.setValue(value);
//                }
//                else {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                }
//            }
//            else {
//                status = BluetoothGatt.GATT_SUCCESS;
//                descriptor.setValue(value);
//            }
			if (responseNeeded)
				mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,0,null);

		}
	};
}