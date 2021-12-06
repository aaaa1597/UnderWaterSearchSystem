package com.tks.uwsunit00;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.view.View;
import android.widget.Button;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.material.snackbar.Snackbar;
import com.tks.uws.blecentral.DeviceInfo;
import com.tks.uws.blecentral.IBleService;
import com.tks.uws.blecentral.IBleServiceCallback;
import com.tks.uwsunit00.ui.DeviceListAdapter;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
//	private FragBizLogicViewModel	mViewModel;
	private DeviceListAdapter		mDeviceListAdapter;
	private final static int		REQUEST_PERMISSIONS = 111;
	private GoogleMap				mMap;
	private Location				mLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TLog.d("");
//		mViewModel = new ViewModelProvider(this).get(FragBizLogicViewModel.class);

		/* リストに区切り線を表示する。 */
		RecyclerView rvw = findViewById(R.id.rvw_devices);
		DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(rvw.getContext(),
				new LinearLayoutManager(getApplicationContext()).getOrientation());
		rvw.addItemDecoration(dividerItemDecoration);

		/* Scanボタン押下処理 */
		findViewById(R.id.btnScan).setOnClickListener(v -> {
			if( !((Button)v).getText().equals("scan開始") ) {
				Snackbar.make(findViewById(R.id.root_view), "すでに実行中です。\n完了してから開始して下さい。", Snackbar.LENGTH_LONG).show();
				return;
			}

			startScan();
		});

		/* BLE初期化アプリを起動 */
		Intent bleAppWakeupintent = new Intent();
		bleAppWakeupintent.setClassName("com.tks.uws.blecentral", "com.tks.uws.blecentral.MainActivity");
		startActivity(bleAppWakeupintent);
//		ActivityResultLauncher<Intent> startForResult = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
//				result -> {
//					if(result.getResultCode() != Activity.RESULT_OK)
//						return;	/* 対象外なので無視 */
//					if( !result.getData().getAction().equals("com.tks.uws.blecentral.BT_INIT"))
//						return;	/* 対象外なので無視 */
//
//					int ret = result.getData().getIntExtra("RET_BT_INIT", -999);
//					switch (ret) {
//						case Constants.UWS_NG_SUCCESS:
//							/* 問題なし ->Bleアプリ起動へ. */
//							break;
//						case Constants.UWS_NG_PERMISSION_DENIED:
//							ErrPopUp.create(MainActivity.this).setErrMsg("このアプリに権限がありません!!\n終了します。").Show(MainActivity.this);
//							break;
//						case Constants.UWS_NG_SERVICE_NOTFOUND:
//							ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!\n終了します。").Show(MainActivity.this);
//							break;
//						case Constants.UWS_NG_ADAPTER_NOTFOUND:
//							ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!\n終了します。").Show(MainActivity.this);
//							break;
//						case Constants.UWS_NG_BT_OFF:
//							ErrPopUp.create(MainActivity.this).setErrMsg("BluetoothがOFFです。ONにして操作してください。\n終了します。").Show(MainActivity.this);
//							break;
//						default:
//							ErrPopUp.create(MainActivity.this).setErrMsg("原因不明のエラーが発生しました!!\n終了します。").Show(MainActivity.this);
//							break;
//					}
//
//					TLog.d("Bleアプリ初期正常終了 -> Bleサービス接続開始. ret={0}", ret);
//					Intent intent = new Intent("com.tsk.uws.blecentral.BINDSERVICE");
//					intent.setPackage("com.tks.uws.blecentral");
//					bindService(intent, mCon, Context.BIND_AUTO_CREATE);
//				});
//		startForResult.launch(bleAppWakeupintent);

//		TLog.d("Bleアプリ初期正常終了 -> Bleサービス接続開始. ret={0}", ret);
		Intent intent = new Intent("com.tsk.uws.blecentral.BINDSERVICE");
		intent.setPackage("com.tks.uws.blecentral");
		bindService(intent, mCon, Context.BIND_AUTO_CREATE);

		/* BLEデバイスリストの初期化 */
		RecyclerView deviceListRvw = findViewById(R.id.rvw_devices);
		deviceListRvw.setHasFixedSize(true);
		deviceListRvw.setLayoutManager(new LinearLayoutManager(getApplicationContext()));
		mDeviceListAdapter = new DeviceListAdapter(new DeviceListAdapter.DeviceListAdapterListener() {
			@Override
			public void onDeviceItemClick(View view, String deviceName, String deviceAddress) {
				stopScan();

				/* デバイス接続開始 */
				int ret = 0;
				try { ret = mBleServiceIf.connectDevice(deviceAddress); }
				catch (RemoteException e) { e.printStackTrace(); }
				switch(ret) {
					case Constants.UWS_NG_SUCCESS:
						TLog.d("デバイス接続中... {0}:{1}", deviceName, deviceAddress);
						mDeviceListAdapter.setStatus(deviceAddress, DeviceListAdapter.ConnectStatus.CONNECTING);
						break;
					case Constants.UWS_NG_RECONNECT_OK:
						TLog.d("デバイス再接続OK. {0}:{1}", deviceName, deviceAddress);
						mDeviceListAdapter.setStatus(deviceAddress, DeviceListAdapter.ConnectStatus.READY);
						break;
					case Constants.UWS_NG_DEVICE_NOTFOUND: {
						String logstr = MessageFormat.format("デバイスが見つかりません。デバイス:({0} : {1})", deviceName, deviceAddress);
						TLog.d(logstr);
						Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
					}
					break;
					default: {
						String logstr = MessageFormat.format("デバイス接続::不明なエラー. デバイス:({0} : {1})", deviceName, deviceAddress);
						TLog.d(logstr);
						Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
					}
					break;
				}
			}
		});
		deviceListRvw.setAdapter(mDeviceListAdapter);

		/* Bluetoothのサポート状況チェック 未サポート端末なら起動しない */
		if (!getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			ErrPopUp.create(MainActivity.this).setErrMsg("Bluetoothが、未サポートの端末です。\n終了します。").Show(MainActivity.this);
		}

		/* 地図用権限が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
		}

		/* SupportMapFragmentを取得し、マップを使用する準備ができたら通知を受取る */
		SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.frfMap);
		Objects.requireNonNull(mapFragment).getMapAsync(this);

		/* 位置情報管理オブジェクト */
		FusedLocationProviderClient flpc = LocationServices.getFusedLocationProviderClient(this);
		flpc.getLastLocation().addOnSuccessListener(this, location -> {
			if (location == null) {
				TLog.d("mLocation={0} googleMap={1}", location, mMap);
				LocationRequest locreq = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(500).setFastestInterval(300);
				flpc.requestLocationUpdates(locreq, new LocationCallback() {
					@Override
					public void onLocationResult(@NonNull LocationResult locationResult) {
						super.onLocationResult(locationResult);
						TLog.d("locationResult={0}({1},{2})", locationResult, locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
						mLocation = locationResult.getLastLocation();
						flpc.removeLocationUpdates(this);
						initDraw(mLocation, mMap);
					}
				}, Looper.getMainLooper());
			}
			else {
				TLog.d("mLocation=(経度:{0} 緯度:{1}) mMap={1}", location.getLatitude(), location.getLongitude(), mMap);
				mLocation = location;
				initDraw(mLocation, mMap);
			}
		});

	}

	@Override
	public void onMapReady(@NonNull GoogleMap googleMap) {
		TLog.d("mLocation={0} googleMap={1}", mLocation, googleMap);
		if (mLocation == null) {
			/* 位置が取れない時は、小城消防署で */
			mLocation = new Location("");
			mLocation.setLongitude(130.20307019743947);
			mLocation.setLatitude(33.25923509336276);
		}
		mMap = googleMap;
		initDraw(mLocation, mMap);
	}

	/* 初期地図描画(起動直後は現在地を表示する) */
	private void initDraw(Location location, GoogleMap googleMap) {
		if (location == null) return;
		if (googleMap == null) return;

		LatLng nowposgps = new LatLng(location.getLatitude(), location.getLongitude());
		TLog.d("経度:{0} 緯度:{1}", location.getLatitude(), location.getLongitude());
		TLog.d("拡縮 min:{0} max:{1}", googleMap.getMinZoomLevel(), googleMap.getMaxZoomLevel());

		/* 現在地マーカ追加 */
		googleMap.addMarker(new MarkerOptions().position(nowposgps).title("BasePos"));

		/* 現在地マーカを中心に */
		googleMap.moveCamera(CameraUpdateFactory.newLatLng(nowposgps));
		TLog.d("CameraPosition:{0}", googleMap.getCameraPosition().toString());

		/* 地図拡大率設定 */
		TLog.d("拡縮 zoom:{0}", 19);
		googleMap.moveCamera(CameraUpdateFactory.zoomTo(19));

		/* 地図俯角 50° */
		CameraPosition tilt = new CameraPosition.Builder(googleMap.getCameraPosition()).tilt(70).build();
		googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(tilt));
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		/* 対象外なので、無視 */
		if (requestCode != REQUEST_PERMISSIONS) return;

		/* 権限リクエストの結果を取得する. */
		if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
			ErrPopUp.create(MainActivity.this).setErrMsg("失敗しました。\nこのアプリに、権限を与えて下さい。").Show(MainActivity.this);
		}
	}

	/* Serviceコールバック */
	private IBleService			mBleServiceIf;
	private final Handler		mHandler = new Handler(Looper.getMainLooper());
	private final static long	SCAN_PERIOD = 30000;	/* m秒 */
	private final ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBleServiceIf = IBleService.Stub.asInterface(service);

			/* コールバック設定 */
			try { mBleServiceIf.setCallback(mCb); }
			catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException("AIDL-callback設定で失敗!!"); /* ここで例外が起きたら終了する */}

			/* BT初期化 */
			int retini = 0;
			try { retini = mBleServiceIf.initBle(); }
			catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException("Bt初期化で失敗!!"); /* ここで例外が起きたら終了する */}
			TLog.d("Bletooth初期化 ret={0}", retini);
			if(retini == Constants.UWS_NG_PERMISSION_DENIED)
				ErrPopUp.create(MainActivity.this).setErrMsg("このアプリに権限がありません!!\n終了します。").Show(MainActivity.this);
			else if(retini == Constants.UWS_NG_SERVICE_NOTFOUND)
				ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!\n終了します。").Show(MainActivity.this);
			else if(retini == Constants.UWS_NG_ADAPTER_NOTFOUND)
				ErrPopUp.create(MainActivity.this).setErrMsg("この端末はBluetoothに対応していません!!\n終了します。").Show(MainActivity.this);
			else if(retini == Constants.UWS_NG_BT_OFF) {
				Snackbar.make(findViewById(R.id.root_view), "BluetoothがOFFです。\nONにして操作してください。", Snackbar.LENGTH_LONG).show();
//				return Constants.UWS_NG_BT_OFF;
			}
			else if(retini != Constants.UWS_NG_SUCCESS)
				ErrPopUp.create(MainActivity.this).setErrMsg("原因不明のエラーが発生しました!!\n終了します。").Show(MainActivity.this);

			/* scan開始 */
			boolean retscan = startScan();
			TLog.d("scan開始 ret={0}", retscan);
			if(!retscan) return;

			/* 30秒後にscan停止 */
			mHandler.postDelayed(() -> {
				stopScan();
			}, SCAN_PERIOD);
		}

		@Override
		public void onServiceDisconnected(ComponentName name) {
			TLog.d("onServiceDisconnected() name={0}", name);
			mBleServiceIf = null;
		}
	};

	/* AIDLコールバック */
	private IBleServiceCallback mCb = new IBleServiceCallback.Stub() {
		@Override
		public void notifyDeviceInfolist() throws RemoteException {
			List<DeviceInfo> result = mBleServiceIf.getDeviceInfolist();
			result.forEach(scanResult -> {

			});
			runOnUiThread(() -> mDeviceListAdapter.addDevice(result));
		}

		@Override
		public void notifyDeviceInfo() throws RemoteException {
			DeviceInfo result = mBleServiceIf.getDeviceInfo();
			runOnUiThread(() -> mDeviceListAdapter.addDevice(result));
			TLog.d("発見!! {0}({1}):Rssi({2})", result.getDeviceAddress(), result.getDeviceName(), result.getDeviceRssi());
		}

		@Override
		public void notifyScanEnd() throws RemoteException {
			TLog.d("scan終了");
			runOnUiThread(() -> {
				Button btn = findViewById(R.id.btnScan);
				btn.setText("scan開始");
				btn.setEnabled(true);
			});
		}

		@Override
		public void notifyGattConnected(String Address) throws RemoteException {
			/* Gatt接続完了 */
			TLog.d("Gatt接続OK!! -> Services探検中. Address={0}", Address);
			runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.EXPLORING); });
		}

		@Override
		public void notifyGattDisConnected(String Address) throws RemoteException {
			String logstr = MessageFormat.format("Gatt接続断!! Address={0}", Address);
			TLog.d(logstr);
			Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
			runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.NONE); });
		}

		@Override
		public void notifyServicesDiscovered(String Address, int status) throws RemoteException {
			if(status == Constants.UWS_NG_GATT_SUCCESS) {
				TLog.d("Services発見. -> 対象Serviceかチェック ret={0}", status);
				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.CHECKAPPLI); });
			}
			else {
				String logstr = MessageFormat.format("Services探索失敗!! 処理終了 ret={0}", status);
				TLog.d(logstr);
				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.NONE); });
				Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
			}
		}

		@Override
		public void notifyApplicable(String Address, boolean status) throws RemoteException {
			if(status) {
				TLog.d("対象Chk-OK. -> 通信準備中 Address={0}", Address);
				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.TOBEPREPARED); });
			}
			else {
				String logstr = MessageFormat.format("対象外デバイス.　処理終了. Address={0}", Address);
				TLog.d(logstr);
				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.NONE); });
				Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
			}
		}

		@Override
		public void notifyReady2DeviceCommunication(String Address, boolean status) throws RemoteException {
			if(status) {
				String logstr = MessageFormat.format("BLEデバイス通信 準備完了. Address={0}", Address);
				TLog.d(logstr);
				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.READY); });
				Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
			}
			else {
				String logstr = MessageFormat.format("BLEデバイス通信 準備失敗!! Address={0}", Address);
				TLog.d(logstr);
				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.NONE); });
				Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
			}
		}

		@Override
		public void notifyResRead(String Address, int rcvval, int status) throws RemoteException {
			String logstr = MessageFormat.format("デバイス読込成功. Address={0} val={1} status={2}", Address, rcvval, status);
			TLog.d(logstr);
			runOnUiThread(() -> { mDeviceListAdapter.setHertBeat(Address, rcvval); });
			Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
		}

		@Override
		public void notifyFromPeripheral(String Address, int rcvval) throws RemoteException {
			String logstr = MessageFormat.format("デバイス通知({0}). Address={1}", rcvval, Address);
			TLog.d(logstr);
			runOnUiThread(() -> { mDeviceListAdapter.setHertBeat(Address, rcvval); });
			Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
		}

		@Override
		public void notifyError(int errcode, String errmsg) throws RemoteException {
			String logstr = MessageFormat.format("ERROR!! errcode={0} : {1}", errcode, errmsg);
			TLog.d(logstr);
			Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
		}
	};

	/* scan開始 */
	private boolean startScan() {
		int ret = 0;
		try { ret = mBleServiceIf.startScan();}
		catch (RemoteException e) { e.printStackTrace();}
		TLog.d("ret={0}", ret);
		if(ret == Constants.UWS_NG_ALREADY_SCANNED) {
			Snackbar.make(findViewById(R.id.root_view), "すでにscan中です。継続します。", Snackbar.LENGTH_LONG).show();
			return false;
		}
		else if(ret == Constants.UWS_NG_BT_OFF) {
			Snackbar.make(findViewById(R.id.root_view), "BluetoothがOFFです。\nONにして操作してください。", Snackbar.LENGTH_LONG).show();
			return false;
		}
		try { mBleServiceIf.clearDevice();}
		catch (RemoteException e) { e.printStackTrace(); return false;}

		runOnUiThread(() -> {
			mDeviceListAdapter.clearDevice();
			Button btn = findViewById(R.id.btnScan);
			btn.setText("scan中");
			btn.setEnabled(false);
		});

		return true;
	}

	/* scan終了 */
	private void stopScan() {
		int ret;
		try { ret = mBleServiceIf.stopScan();}
		catch (RemoteException e) { e.printStackTrace(); return;}
		TLog.d("scan停止 ret={0}", ret);
		runOnUiThread(() -> {
			Button btn = findViewById(R.id.btnScan);
			btn.setText("scan開始");
			btn.setEnabled(true);
		});
	}
}
