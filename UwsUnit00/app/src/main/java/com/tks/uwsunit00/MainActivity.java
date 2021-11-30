package com.tks.uwsunit00;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Bundle;

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

import java.util.Objects;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
	private final static int REQUEST_PERMISSIONS = 111;
	private GoogleMap	mMap;
	private Location	mLocation;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		TLog.d("");

		/* 権限が許可されていない場合はリクエスト. */
		if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_PERMISSIONS);
		}

		/* SupportMapFragmentを取得し、マップを使用する準備ができたら通知を受取る */
		SupportMapFragment mapFragment = (SupportMapFragment)getSupportFragmentManager().findFragmentById(R.id.frfMap);
		Objects.requireNonNull(mapFragment).getMapAsync(this);

		/* 位置情報管理オブジェクト */
		FusedLocationProviderClient flpc = LocationServices.getFusedLocationProviderClient(this);
		flpc.getLastLocation().addOnSuccessListener(this, location -> {
			if(location == null) {
				TLog.d("mLocation={0} googleMap={1}",location, mMap);
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
		TLog.d("mLocation={0} googleMap={1}",mLocation, googleMap);
		if(mLocation == null) {
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
		if(location == null) return;
		if(googleMap== null) return;

		LatLng nowposgps = new LatLng(location.getLatitude(), location.getLongitude());
		TLog.d("経度:{0} 緯度:{1}", location.getLatitude(), location.getLongitude());
		TLog.d("拡縮 min:{0} max:{1}", googleMap.getMinZoomLevel(), googleMap.getMaxZoomLevel());

		/* 現在地マーカ追加 */
		googleMap.addMarker(new MarkerOptions().position(nowposgps).title("BasePos"));

		/* 現在地マーカを中心に */
		googleMap.moveCamera(CameraUpdateFactory.newLatLng(nowposgps));
		TLog.d("CameraPosition:{0}", googleMap.getCameraPosition().toString());

		/* 地図拡大率設定 */
		TLog.d("拡縮 zoom:{0}",19);
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
				MsgPopUp.create(MainActivity.this).setErrMsg("失敗しました。\nこのアプリでは、どうしようもないので終了します。").Show(MainActivity.this);
			}
		}
		else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
}