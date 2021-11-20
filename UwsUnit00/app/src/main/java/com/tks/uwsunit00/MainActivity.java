package com.tks.uwsunit00;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

public class MainActivity extends FragmentActivity implements OnMapReadyCallback {
	private final static int REQUEST_PERMISSIONS = 111;
	private GoogleMap					mMap;
	private FusedLocationProviderClient	mFlpc;
	private Marker						mDmyMarker;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* 権限が許可されていない場合はリクエスト. */
		if(checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED){
			requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION},REQUEST_PERMISSIONS);
		}

		/* SupportMapFragmentを取得し、マップを使用する準備ができたら通知を受取る */
		SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.frfMap);
		mapFragment.getMapAsync(this);

		mFlpc = LocationServices.getFusedLocationProviderClient(this);
		mFlpc.getLastLocation().addOnSuccessListener(this, location -> {
			if (location != null) {
				LatLng nowposgps = new LatLng(location.getLatitude(), location.getLongitude());
				TLog.d("経度:{0} 緯度:{1}", location.getLatitude(), location.getLongitude());
				TLog.d("拡縮 min:{0} max:{1}", mMap.getMinZoomLevel(), mMap.getMaxZoomLevel());

				/* 初期マーカ削除 */
				mDmyMarker.remove();

				/* 基点マーカ追加 */
				mMap.addMarker(new MarkerOptions().position(nowposgps).title("BasePos"));

				/* 基点マーカを中心に */
				mMap.moveCamera(CameraUpdateFactory.newLatLng(nowposgps));
				TLog.d("CameraPosition:{0}", mMap.getCameraPosition().toString());

				/* 地図拡大率設定 */
				TLog.d("拡縮 zoom:{0}",19);
				mMap.moveCamera(CameraUpdateFactory.zoomTo(19));

				/* 地図俯角 50° */
				CameraPosition tilt = new CameraPosition.Builder(mMap.getCameraPosition()).tilt(70).build();
				mMap.moveCamera(CameraUpdateFactory.newCameraPosition(tilt));
			}
		});
	}

	@Override
	public void onMapReady(@NonNull GoogleMap googleMap) {
		mMap = googleMap;

		/* 東京スカイツリーの位置でマーカを設定 */
		LatLng skytree = new LatLng(35.710063, 139.8107);
		mDmyMarker = mMap.addMarker(new MarkerOptions().position(skytree).title("Marker in SkyTree"));
		mMap.moveCamera(CameraUpdateFactory.newLatLng(skytree));
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		/* 権限リクエストの結果を取得する. */
		if (requestCode == REQUEST_PERMISSIONS) {
			if (grantResults[0] != PackageManager.PERMISSION_GRANTED) {
				ErrPopUp.create(MainActivity.this).setErrMsg("失敗しました。\nこのアプリでは、どうしようもないので終了します。").Show(MainActivity.this);
			}
		}
		else {
			super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		}
	}
}