package com.tks.uwsunit00;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.bluetooth.le.ScanResult;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;
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
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.tks.uws.blecentral.IBleService;
import com.tks.uws.blecentral.IBleServiceCallback;

import java.text.MessageFormat;
import java.util.List;
import java.util.Objects;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
	private final static int REQUEST_PERMISSIONS = 111;
	private GoogleMap mMap;
	private Location mLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TLog.d("");

		/* BLE初期化アプリを起動 */
		Intent intent = new Intent();
		intent.setClassName("com.tks.uws.blecentral", "com.tks.uws.blecentral.MainActivity");
		startActivity(intent);

		/* 権限が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
		}

		/* SupportMapFragmentを取得し、マップを使用する準備ができたら通知を受取る */
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.frfMap);
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
				}, null);
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
		/* 権限リクエストの結果を取得する. */
		if (requestCode == REQUEST_PERMISSIONS) {
			if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				MsgPopUp.create(MainActivity.this).setErrMsg("失敗しました。\nこのアプリに、権限を与えて下さい。").Show(MainActivity.this);
			}
		}
		else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}

	@Override
	protected void onStart() {
		super.onStart();

		Intent intent = new Intent("com.tsk.uws.blecentral.BINDSERVICE");
		intent.setPackage("com.tks.uws.blecentral");
		bindService(intent, mCon, Context.BIND_AUTO_CREATE);
	}

	/* Serviceコールバック */
	private IBleService mBleServiceIf;
	private final ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName name, IBinder service) {
			mBleServiceIf = IBleService.Stub.asInterface(service);

			/* コールバック設定 */
			try { mBleServiceIf.setCallback(mCb); }
			catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException("AIDL-callback設定で失敗!!"); /* ここで例外が起きたら終了する */}

			TLog.d("aaaaaaaaaaa ひとまず、ここまで動くことを確認したい。");

//			/* BT初期化 */
//			boolean retinit = initBt();
//			if(!retinit) return;
//			TLog.d("BT初期化完了");
//
//			/* scan開始 */
//			boolean retscan = startScan();
//			if(!retscan) return;
//			TLog.d("scan開始");
//
//			/* 30秒後にscan停止 */
//			mHandler.postDelayed(() -> {
//				stopScan();
//			}, SCAN_PERIOD);
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
		public void notifyScanResultlist() throws RemoteException {
			List<ScanResult> result = mBleServiceIf.getScanResultlist();
//			runOnUiThread(() -> mDeviceListAdapter.addDevice(result));
		}

		@Override
		public void notifyScanResult() throws RemoteException {
			ScanResult result = mBleServiceIf.getScanResult();
//			runOnUiThread(() -> mDeviceListAdapter.addDevice(result));
//			if(result.getScanRecord() != null && result.getScanRecord().getServiceUuids() != null)
//				TLog.d("発見!! {0}({1}):Rssi({2}) Uuids({3})", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi(), result.getScanRecord().getServiceUuids().toString());
//			else
//				TLog.d("発見!! {0}({1}):Rssi({2})", result.getDevice().getAddress(), result.getDevice().getName(), result.getRssi());
		}

		@Override
		public void notifyScanEnd() throws RemoteException {
			TLog.d("scan終了");
			runOnUiThread(() -> {
//				Button btn = findViewById(R.id.btnScan);
//				btn.setText("scan開始");
//				btn.setEnabled(true);
			});
		}

		@Override
		public void notifyGattConnected(String Address) throws RemoteException {
			/* Gatt接続完了 */
			TLog.d("Gatt接続OK!! -> Services探検中. Address={0}", Address);
//			runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.EXPLORING); });
		}

		@Override
		public void notifyGattDisConnected(String Address) throws RemoteException {
			String logstr = MessageFormat.format("Gatt接続断!! Address={0}", Address);
//			TLog.d(logstr);
//			Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
//			runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.NONE); });
		}

		@Override
		public void notifyServicesDiscovered(String Address, int status) throws RemoteException {
//			if(status == BleService.UWS_NG_GATT_SUCCESS) {
//				TLog.d("Services発見. -> 対象Serviceかチェック ret={0}", status);
//				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.CHECKAPPLI); });
//			}
//			else {
//				String logstr = MessageFormat.format("Services探索失敗!! 処理終了 ret={0}", status);
//				TLog.d(logstr);
//				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.NONE); });
//				Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
//			}
		}

		@Override
		public void notifyApplicable(String Address, boolean status) throws RemoteException {
//			if(status) {
//				TLog.d("対象Chk-OK. -> 通信準備中 Address={0}", Address);
//				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.TOBEPREPARED); });
//			}
//			else {
//				String logstr = MessageFormat.format("対象外デバイス.　処理終了. Address={0}", Address);
//				TLog.d(logstr);
//				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.NONE); });
//				Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
//			}
		}

		@Override
		public void notifyReady2DeviceCommunication(String Address, boolean status) throws RemoteException {
//			if(status) {
//				String logstr = MessageFormat.format("BLEデバイス通信 準備完了. Address={0}", Address);
//				TLog.d(logstr);
//				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.READY); });
//				Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
//			}
//			else {
//				String logstr = MessageFormat.format("BLEデバイス通信 準備失敗!! Address={0}", Address);
//				TLog.d(logstr);
//				runOnUiThread(() -> { mDeviceListAdapter.setStatus(Address, DeviceListAdapter.ConnectStatus.NONE); });
//				Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
//			}
		}

		@Override
		public void notifyResRead(String Address, int rcvval, int status) throws RemoteException {
//			String logstr = MessageFormat.format("デバイス読込成功. Address={0} val={1} status={2}", Address, rcvval, status);
//			TLog.d(logstr);
//			runOnUiThread(() -> { mDeviceListAdapter.setHertBeat(Address, rcvval); });
//			Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
		}

		@Override
		public void notifyFromPeripheral(String Address, int rcvval) throws RemoteException {
//			String logstr = MessageFormat.format("デバイス通知({0}). Address={1}", rcvval, Address);
//			TLog.d(logstr);
//			runOnUiThread(() -> { mDeviceListAdapter.setHertBeat(Address, rcvval); });
//			Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
		}

		@Override
		public void notifyError(int errcode, String errmsg) throws RemoteException {
//			String logstr = MessageFormat.format("ERROR!! errcode={0} : {1}", errcode, errmsg);
//			TLog.d(logstr);
//			Snackbar.make(findViewById(R.id.root_view), logstr, Snackbar.LENGTH_LONG).show();
		}
	};
}
