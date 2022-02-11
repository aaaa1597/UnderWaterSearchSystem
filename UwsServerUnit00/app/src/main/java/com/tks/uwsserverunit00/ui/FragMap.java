package com.tks.uwsserverunit00.ui;

import static com.tks.uwsserverunit00.Constants.UWS_LOC_BASE_DISTANCE_X;
import static com.tks.uwsserverunit00.Constants.UWS_LOC_BASE_DISTANCE_Y;
import static com.tks.uwsserverunit00.Constants.d2Str;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import android.os.Looper;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polygon;

import com.google.android.gms.maps.model.PolygonOptions;
import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.R;
import com.tks.uwsserverunit00.TLog;
import com.tks.uwsserverunit00.UwsInfo;

public class FragMap extends SupportMapFragment {
	private final short						BASE_COMMANDER_LOCATION_IDX = 9999;
	private FragBleViewModel				mBleViewModel;
	private FragMapViewModel				mMapViewModel;
	private FragBizLogicViewModel			mBizLogicViewModel;
	private GoogleMap						mGoogleMap;
	private Location						mLocation;
	private final Map<Short, MapDrawInfo>	mMapDrawInfos = new HashMap<>();
	/* 検索情報 */
	static class MapDrawInfo {
		public LatLng	pos;
		public Marker	maker;	/* GoogleMapの Marker */
		public Polygon	polygon;/* GoogleMapの Polygon */
		public Circle	circle;	/* GoogleMapの Circle-中心点は隊員の現在値 */
	};

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		mBizLogicViewModel = new ViewModelProvider(requireActivity()).get(FragBizLogicViewModel.class);

		mBleViewModel = new ViewModelProvider(requireActivity()).get(FragBleViewModel.class);
		mBleViewModel.UpdUwsInfo().observe(getViewLifecycleOwner(), uwsInfo -> {
			if(uwsInfo==null) return;
			updMapDrawInfo(mGoogleMap, mMapDrawInfos, uwsInfo);
		});

		mMapViewModel = new ViewModelProvider(requireActivity()).get(FragMapViewModel.class);
		mMapViewModel.Permission().observe(getViewLifecycleOwner(), aBoolean -> {
			getNowPosAndDraw();
		});

		/* SupportMapFragmentを取得し、マップを使用する準備ができたら通知を受取る */
		SupportMapFragment mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.frgMap);
		mapFragment.getMapAsync(new OnMapReadyCallback() {
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

		mMapViewModel.SelectedSeeker().observe(getViewLifecycleOwner(), new Observer<Pair<Short, Boolean>>() {
			@Override
			public void onChanged(Pair<Short, Boolean> selected) {
				if(selected.first==-32768) return;/* 初期設定なので、何もしない。 */
				short	seekerid = selected.first;
				boolean	isSelected = selected.second;

				MapDrawInfo si = mMapDrawInfos.get(seekerid);
				if(si==null) return;

				if(isSelected) {
					Marker marker = mGoogleMap.addMarker(new MarkerOptions()
							.position(si.pos)
							.title(String.valueOf(selected.first))
							.icon(createIcon(selected.first)));
					Circle nowPoint = mGoogleMap.addCircle(new CircleOptions().center(si.pos)
							.radius(0.5)
							.fillColor(Color.MAGENTA)
							.strokeColor(Color.MAGENTA));
					si.maker = marker;
					si.circle= nowPoint;
				}
				else {
					si.maker.remove();
					si.maker = null;
					si.circle.remove();
					si.circle = null;
				}
			}
		});
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
		TLog.d("経度:{0} 緯度:{1}", d2Str(location.getLatitude()), d2Str(location.getLongitude()));
		TLog.d("拡縮 min:{0} max:{1}", googleMap.getMinZoomLevel(), googleMap.getMaxZoomLevel());

		/* 現在地マーカ追加 */
		Marker basemarker = googleMap.addMarker(new MarkerOptions().position(nowposgps).title("BasePos"));
		Circle nowPoint = googleMap.addCircle(new CircleOptions()
												.center(nowposgps)
												.radius(0.5)
												.fillColor(Color.CYAN)
												.strokeColor(Color.CYAN));

		mMapDrawInfos.put(BASE_COMMANDER_LOCATION_IDX, new MapDrawInfo(){{pos=nowposgps;maker=basemarker; circle=nowPoint; polygon=null;}});

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
				return BitmapDescriptorFactory.fromResource(R.drawable.marker3);
		}
	}

	/* Map用描画情報 表示 */
	private void updMapDrawInfo(GoogleMap googleMap, Map<Short, MapDrawInfo> mdList, UwsInfo uwsInfo) {
		short seekerid = uwsInfo.getSeekerId();
		if(seekerid < 0) {
			TLog.d("対象外エントリ.何もしない.uwsinfo({0}, {1}, {2}, {3}, {4})", uwsInfo.getDate(), uwsInfo.getSeekerId(), uwsInfo.getLongitude(), uwsInfo.getLatitude(), uwsInfo.getHeartbeat());
			return;
		}

		MapDrawInfo drawinfo = mdList.get(seekerid);
		if(drawinfo == null) {
			/* 新規追加 */
			LatLng nowposgps = new LatLng(uwsInfo.getLatitude(), uwsInfo.getLongitude());
			if(mBleViewModel.getDeviceListAdapter().isSelected(seekerid)/*選択中*/) {
				Marker marker = googleMap.addMarker(new MarkerOptions()
						.position(nowposgps)
						.title(String.valueOf(seekerid))
						.icon(createIcon(uwsInfo.getSeekerId())));

				Circle nowPoint = googleMap.addCircle(new CircleOptions()
						.center(nowposgps)
						.radius(0.5)
						.fillColor(Color.MAGENTA)
						.strokeColor(Color.MAGENTA));
				TLog.d("Circle = {0}", nowPoint);
				mdList.put(seekerid, new MapDrawInfo(){{pos=nowposgps;maker=marker;circle=nowPoint;}});
			}
			else {
				mdList.put(seekerid, new MapDrawInfo(){{pos=nowposgps;maker=null;circle=null;}});
			}
		}
		else {
			/* 位置更新 */
			LatLng spos = drawinfo.pos;
			LatLng epos = new LatLng(uwsInfo.getLatitude(), uwsInfo.getLongitude());
			drawinfo.pos = epos;
			double dx = epos.longitude- spos.longitude;
			double dy = epos.latitude - spos.latitude;
			if(Math.abs(dx) < 2*Float.MIN_VALUE && Math.abs(dy) < 2*Float.MIN_VALUE) {
				TLog.d("matrix 移動量が小さいので処理しない dx={0} dy={1} Double.MIN_VALUE={2}", dx, dy, Double.MIN_VALUE);
				return;
			}

			if(drawinfo.maker!=null) {
				drawinfo.maker.remove();
				drawinfo.maker = null;
			}
			if(drawinfo.circle!=null) {
				drawinfo.circle.remove();
				drawinfo.circle = null;
			}
//			if(drawinfo.polygon!=null) {	/* 検索矩形は保持したまま */
//				drawinfo.polygon.remove();
//				drawinfo.polygon = null;
//			}
			if(mBleViewModel.getDeviceListAdapter().isSelected(uwsInfo.getSeekerId())/*選抜中*/) {
				Marker marker = googleMap.addMarker(new MarkerOptions()
						.position(epos)
						.title(String.valueOf(seekerid))
						.icon(createIcon(uwsInfo.getSeekerId())));
				Circle nowPoint = googleMap.addCircle(new CircleOptions()
						.center(epos)
						.radius(0.5)
						.fillColor(Color.MAGENTA)
						.strokeColor(Color.MAGENTA));
				drawinfo.maker = marker;
				drawinfo.circle= nowPoint;

				/* 検索線描画の頂点情報取得 */
				LatLng[] square = createSquare(spos, epos, UWS_LOC_BASE_DISTANCE_X, UWS_LOC_BASE_DISTANCE_Y);

				if(mBizLogicViewModel.getSerchStatus()/*検索中*/) {
					/* 矩形追加 */
					Polygon polygon = googleMap.addPolygon(new PolygonOptions()
							.fillColor(mMapViewModel.getFillColor())
							.strokeColor(Color.BLUE)
							.strokeWidth(1)
							.add(square[0], square[1], square[3], square[2]));
					drawinfo.polygon = polygon;
				}
			}
		}
	}

	private LatLng[] createSquare(final LatLng spos, final LatLng epos, final double BASE_DISTANCE_X, final double BASE_DISTANCE_Y) {
		/* 0-1.前準備 検索線分の角度(degrees)を求める */
		double dx = epos.longitude* (BASE_DISTANCE_X/100000) - spos.longitude* (BASE_DISTANCE_X/100000);
		double dy = epos.latitude * (BASE_DISTANCE_Y/100000) - spos.latitude * (BASE_DISTANCE_Y/100000);
		double degrees = Math.atan2(dy, dx) * 180 / Math.PI;

		/* 0-2.前準備 行列生成 */
		Matrix mat = new Matrix();
		mat.reset();

		/* 1.単位変換適用(cm座標系 → 度分秒座標系) */
		mat.postScale((float)(1/(BASE_DISTANCE_X*100)), (float)(1/(BASE_DISTANCE_Y*100)));

		/* 2.回転適用(検索線分の角度) */
		mat.postRotate((float)degrees);

		/* 3.ひとまず四隅の頂点offsetを求める */
		float[] src4vertex = {/*右上*/50,50,/*右下*/50,-50,/*左上*/-50,50,/*左下*/-50,-50};
		float[] dst4vertex = new float[src4vertex.length];
		mat.mapPoints(dst4vertex, src4vertex);

		/* 4.開始点/終了点にoffsetを加算して完成 */
		LatLng ltpos = new LatLng(epos.latitude+dst4vertex[1], epos.longitude+dst4vertex[0]);
		LatLng rtpos = new LatLng(epos.latitude+dst4vertex[3], epos.longitude+dst4vertex[2]);
		LatLng lbpos = new LatLng(spos.latitude+dst4vertex[5], spos.longitude+dst4vertex[4]);
		LatLng rbpos = new LatLng(spos.latitude+dst4vertex[7], spos.longitude+dst4vertex[6]);
		return new LatLng[]{ltpos, rtpos, lbpos, rbpos};
	}
}