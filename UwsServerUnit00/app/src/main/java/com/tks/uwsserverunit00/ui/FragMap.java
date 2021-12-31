package com.tks.uwsserverunit00.ui;

import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.ViewModelProvider;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;
import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.R;
import com.tks.uwsserverunit00.TLog;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class FragMap extends SupportMapFragment {
	private FragBleViewModel				mBleViewModel;
	private FragMapViewModel				mMapViewModel;
	private GoogleMap						mGoogleMap;
	private Location						mLocation;
	private final Map<String, SerchInfo>	mSerchInfos = new HashMap<>();
	int										mNowSerchColor = 0xff78e06b;    /* 緑っぽい色 */

	/* 検索情報 */
	static class SerchInfo {
		public Marker	maker;	/* GoogleMapの Marker */
		public Polygon	polygon;/* GoogleMapの Polygon */
		public Circle	circle;	/* GoogleMapの Circle 中心点は隊員の現在値 */
	};

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		mBleViewModel = new ViewModelProvider(requireActivity()).get(FragBleViewModel.class);
		mBleViewModel.NewDeviceInfo().observe(getViewLifecycleOwner(), deviceInfo -> {
			if(deviceInfo==null) return;
			updSerchInfo(mGoogleMap, mSerchInfos, deviceInfo);
		});

		mMapViewModel = new ViewModelProvider(requireActivity()).get(FragMapViewModel.class);
		mMapViewModel.Permission().observe(getViewLifecycleOwner(), aBoolean -> {
			getNowPosAndDraw();
		});

		/* SupportMapFragmentを取得し、マップを使用する準備ができたら通知を受取る */
		SupportMapFragment mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.frgMap);
		Objects.requireNonNull(mapFragment).getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(@NonNull GoogleMap googleMap) {
				TLog.d("mLocation={0} googleMap={1}", mLocation, googleMap);
				if (mLocation == null) {
					/* 位置が取れない時は、小城消防署で */
					mLocation = new Location("");
					mLocation.setLongitude(130.20307019743947);
					mLocation.setLatitude(33.25923509336276);
				}
				mGoogleMap = googleMap;
				initDraw(mLocation, mGoogleMap);
			}
		});

		/* 現在値取得 → 地図更新 */
		getNowPosAndDraw();
	}

	/* 現在値取得 → 地図更新 */
	private void getNowPosAndDraw() {
		/* 権限なしなら何もしない。 */
		if(ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return;
		}

		/* 位置情報管理オブジェクト */
		FusedLocationProviderClient flpc = LocationServices.getFusedLocationProviderClient(getActivity().getApplicationContext());
		flpc.getLastLocation().addOnSuccessListener(getActivity(), location -> {
			if (location == null) {
				TLog.d("mLocation={0} googleMap={1}", location, mGoogleMap);
				LocationRequest locreq = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(500).setFastestInterval(300);
				flpc.requestLocationUpdates(locreq, new LocationCallback() {
					@Override
					public void onLocationResult(@NonNull LocationResult locationResult) {
						super.onLocationResult(locationResult);
						TLog.d("locationResult={0}({1},{2})", locationResult, locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
						mLocation = locationResult.getLastLocation();
						flpc.removeLocationUpdates(this);
						initDraw(mLocation, mGoogleMap);
					}
				}, Looper.getMainLooper());
			}
			else {
				TLog.d("mLocation=(経度:{0} 緯度:{1}) mMap={1}", location.getLatitude(), location.getLongitude(), mGoogleMap);
				mLocation = location;
				initDraw(mLocation, mGoogleMap);
			}
		});
	}

	/* 初期地図描画(起動直後は現在地を表示する) */
	private void initDraw(Location location, GoogleMap googleMap) {
		if (location == null) return;
		if (googleMap == null) return;

		LatLng nowposgps = new LatLng(location.getLatitude(), location.getLongitude());
		TLog.d("経度:{0} 緯度:{1}", location.getLatitude(), location.getLongitude());
		TLog.d("拡縮 min:{0} max:{1}", googleMap.getMinZoomLevel(), googleMap.getMaxZoomLevel());

		/* 現在地マーカ追加 */
		Marker basemarker = googleMap.addMarker(new MarkerOptions().position(nowposgps).title("BasePos"));
		mSerchInfos.put("base", new SerchInfo(){{maker=basemarker; polygon =null;}});

		/* 現在地マーカを中心に */
		googleMap.moveCamera(CameraUpdateFactory.newLatLng(nowposgps));
		TLog.d("CameraPosition:{0}", googleMap.getCameraPosition().toString());

		/* 地図拡大率設定 */
		TLog.d("拡縮 zoom:{0}", 19);
		googleMap.moveCamera(CameraUpdateFactory.zoomTo(19));

		/* 地図俯角 70° */
		CameraPosition tilt = new CameraPosition.Builder(googleMap.getCameraPosition()).tilt(70).build();
		googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(tilt));
	}

	/* 探索線表示 */
	private void updSerchInfo(GoogleMap googleMap, Map<String, SerchInfo> mSerchInfos, DeviceInfo deviceInfo) {
		String key = String.valueOf(deviceInfo.getSeekerId());
		if(key.equals("-1")) {
			TLog.d("対象外エントリ.何もしない.info({0}, {1}, {2}, {3}, {4}, {5}, {6}, {7})", deviceInfo.getDate(), deviceInfo.getSeekerId(), deviceInfo.getSeqNo(), deviceInfo.getDeviceName(), deviceInfo.getDeviceAddress(), deviceInfo.getLongitude(), deviceInfo.getLatitude(), deviceInfo.getHeartbeat());
			return;
		}

		/* TODO 削除予定 */
		for(String keyaaa: mSerchInfos.keySet())
			TLog.d("SerchInfos::key={0} maker={1} circle={2} polygon={3}", keyaaa, mSerchInfos.get(keyaaa).maker, mSerchInfos.get(keyaaa).circle, mSerchInfos.get(keyaaa).polygon);

		SerchInfo drawinfo = mSerchInfos.get(key);
		if(drawinfo == null) {
			/* 新規追加 */
			LatLng nowposgps = new LatLng(deviceInfo.getLatitude(), deviceInfo.getLongitude());
			Marker marker = googleMap.addMarker(new MarkerOptions()
												.position(nowposgps)
												.title(key)
												.icon(createIcon(deviceInfo.getSeekerId())));

			Circle nowPoint = googleMap.addCircle(new CircleOptions().center(nowposgps)
																	  .radius(1.0)
																	  .fillColor(Color.MAGENTA)
																	  .strokeColor(Color.MAGENTA));
			TLog.d("Circle = {0}", nowPoint);
			SerchInfo si = new SerchInfo();
			si.maker = marker;
			si.circle= nowPoint;
			mSerchInfos.put(key, si);

			/* TODO 削除予定 */
			for(String keyaaa: mSerchInfos.keySet())
				TLog.d("SerchInfos::key={0} maker={1} circle={2} polygon={3}", keyaaa, mSerchInfos.get(keyaaa).maker, mSerchInfos.get(keyaaa).circle, mSerchInfos.get(keyaaa).polygon);
		}
		else {
			/* TODO 削除予定 */
			for(String keyaaa: mSerchInfos.keySet())
				TLog.d("SerchInfos::key={0} maker={1} circle={2} polygon={3}", keyaaa, mSerchInfos.get(keyaaa).maker, mSerchInfos.get(keyaaa).circle, mSerchInfos.get(keyaaa).polygon);

			TLog.d("SerchInfos::maker={0} circle={1} polygon={2}", drawinfo.maker, drawinfo.circle, drawinfo.polygon);

			drawinfo.maker.remove();
			drawinfo.maker = null;
			drawinfo.circle.remove();
			drawinfo.circle = null;
			LatLng nowposgps = new LatLng(deviceInfo.getLatitude(), deviceInfo.getLongitude());
			Marker marker = googleMap.addMarker(new MarkerOptions()
					.position(nowposgps)
					.title(key)
					.icon(createIcon(deviceInfo.getSeekerId())));

			Circle nowPoint = googleMap.addCircle(new CircleOptions().center(nowposgps)
					.radius(1.0)
					.fillColor(Color.MAGENTA)
					.strokeColor(Color.MAGENTA));

			drawinfo.maker = marker;
			drawinfo.circle= nowPoint;

//// Add polygons to indicate areas on the map.
//			Polygon polygon1 = googleMap.addPolygon(new PolygonOptions()
//					.clickable(true)
//					.add(
//							new LatLng(-27.457, 153.040),
//							new LatLng(-33.852, 151.211),
//							new LatLng(-37.813, 144.962),
//							new LatLng(-34.928, 138.599)));
//// Store a data object with the polygon, used here to indicate an arbitrary type.
//			polygon1.setTag("alpha");
		}

	}

	private BitmapDescriptor createIcon(short seekerid) {
		switch(seekerid) {
			case 0: return BitmapDescriptorFactory.fromResource(R.drawable.marker0);
			case 1: return BitmapDescriptorFactory.fromResource(R.drawable.marker1);
			case 2: return BitmapDescriptorFactory.fromResource(R.drawable.marker2);
			case 3: return BitmapDescriptorFactory.fromResource(R.drawable.marker3);
			case 4: return BitmapDescriptorFactory.fromResource(R.drawable.marker4);
			case 5: return BitmapDescriptorFactory.fromResource(R.drawable.marker5);
			case 6: return BitmapDescriptorFactory.fromResource(R.drawable.marker6);
			case 7: return BitmapDescriptorFactory.fromResource(R.drawable.marker7);
			case 8: return BitmapDescriptorFactory.fromResource(R.drawable.marker8);
			case 9: return BitmapDescriptorFactory.fromResource(R.drawable.marker9);
			default:
				return BitmapDescriptorFactory.fromResource(R.drawable.marker9);
		}
	}
}
