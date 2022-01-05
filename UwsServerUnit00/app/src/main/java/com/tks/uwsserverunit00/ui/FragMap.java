package com.tks.uwsserverunit00.ui;

import androidx.core.app.ActivityCompat;
import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
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
import com.google.android.gms.maps.model.PolygonOptions;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.R;
import com.tks.uwsserverunit00.TLog;
import static com.tks.uwsserverunit00.Constants.UWS_LOC_BASE_DISTANCE_X;
import static com.tks.uwsserverunit00.Constants.UWS_LOC_BASE_DISTANCE_Y;

public class FragMap extends SupportMapFragment {
	private FragBizLogicViewModel			mBizLogicViewModel;
	private FragBleViewModel				mBleViewModel;
	private FragMapViewModel				mMapViewModel;
	private GoogleMap						mGoogleMap;
	private Location						mLocation;
	private final Map<String, SerchInfo>	mSerchInfos = new HashMap<>();
	/* 検索情報 */
	static class SerchInfo {
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
		mBleViewModel.NewDeviceInfo().observe(getViewLifecycleOwner(), deviceInfo -> {
			if(deviceInfo==null) return;
			updSerchInfo(mGoogleMap, mSerchInfos, deviceInfo);
			SerchInfo si = mSerchInfos.get(String.valueOf(deviceInfo.getSeekerId()));
			if(si != null)
				si.pos = new LatLng(deviceInfo.getLatitude(), deviceInfo.getLongitude());
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

		mMapViewModel.SelectedSeeker().observe(getViewLifecycleOwner(), new Observer<Pair<Short, Boolean>>() {
			@Override
			public void onChanged(Pair<Short, Boolean> selected) {
				if(selected.first==-32768) return;/* 初期設定なので、何もしない。 */

				String seekeridStr = String.valueOf(selected.first);
				boolean isSelected = selected.second;
				SerchInfo si = mSerchInfos.get(seekeridStr);
				if(isSelected) {
					Marker marker = mGoogleMap.addMarker(new MarkerOptions()
												.position(si.pos)
												.title(seekeridStr)
												.icon(createIcon(selected.first)));
					Circle nowPoint = mGoogleMap.addCircle(new CircleOptions().center(si.pos)
												.radius(0.5)
												.fillColor(Color.MAGENTA)
												.strokeColor(Color.MAGENTA));
//					si.pos = si.pos;
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
		TLog.d("経度:{0} 緯度:{1}", location.getLatitude(), location.getLongitude());
		TLog.d("拡縮 min:{0} max:{1}", googleMap.getMinZoomLevel(), googleMap.getMaxZoomLevel());

		/* 現在地マーカ追加 */
		Marker basemarker = googleMap.addMarker(new MarkerOptions().position(nowposgps).title("BasePos"));
		Circle nowPoint = googleMap.addCircle(new CircleOptions()
									.center(nowposgps)
									.radius(0.5)
									.fillColor(Color.CYAN)
									.strokeColor(Color.CYAN));

		mSerchInfos.put("base", new SerchInfo(){{pos=nowposgps;maker=basemarker; circle=nowPoint; polygon=null;}});

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
	private void updSerchInfo(GoogleMap googleMap, Map<String, SerchInfo> siList, DeviceInfo deviceInfo) {
		String key = String.valueOf(deviceInfo.getSeekerId());
		if(key.equals("-1")) {
			TLog.d("対象外エントリ.何もしない.info({0}, {1}, {2}, {3}, {4}, {5}, {6}, {7})", deviceInfo.getDate(), deviceInfo.getSeekerId(), deviceInfo.getSeqNo(), deviceInfo.getDeviceName(), deviceInfo.getDeviceAddress(), deviceInfo.getLongitude(), deviceInfo.getLatitude(), deviceInfo.getHeartbeat());
			return;
		}

		SerchInfo drawinfo = siList.get(key);
		if(drawinfo == null) {
			/* 新規追加 */
			LatLng nowposgps = new LatLng(deviceInfo.getLatitude(), deviceInfo.getLongitude());
			if(mMapViewModel.isSelected(deviceInfo.getSeekerId())/*選択中*/) {
				Marker marker = googleMap.addMarker(new MarkerOptions()
														.position(nowposgps)
														.title(key)
														.icon(createIcon(deviceInfo.getSeekerId())));

				Circle nowPoint = googleMap.addCircle(new CircleOptions()
																.center(nowposgps)
																.radius(0.5)
																.fillColor(Color.MAGENTA)
																.strokeColor(Color.MAGENTA));
				TLog.d("Circle = {0}", nowPoint);
				siList.put(key, new SerchInfo(){{pos=nowposgps;maker=marker;circle=nowPoint;}});
			}
			else {
				siList.put(key, new SerchInfo(){{pos=nowposgps;maker=null;circle=null;}});
			}
		}
		else {
			LatLng spos = drawinfo.pos;
			LatLng epos = new LatLng(deviceInfo.getLatitude(), deviceInfo.getLongitude());
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
			if(mMapViewModel.isSelected(deviceInfo.getSeekerId())/*選抜中*/) {
				Marker marker = googleMap.addMarker(new MarkerOptions()
														.position(epos)
														.title(key)
														.icon(createIcon(deviceInfo.getSeekerId())));
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
												.strokeColor(mMapViewModel.getFillColor())
//												.fillColor(Color.CYAN)
//												.strokeColor(Color.BLUE)
												.add(square[0], square[1], square[3], square[2]));
					drawinfo.polygon = polygon;
				}
			}
		}

	}

	private LatLng[] createSquare(final LatLng spos, final LatLng epos, final double BASE_DISTANCE_X, final double BASE_DISTANCE_Y) {
		/* 0.準備 */
		double dx = epos.longitude- spos.longitude;
		double dy = epos.latitude - spos.latitude;
		TLog.d("0.準備 dx={0} dy={1}", String.format(Locale.JAPAN, "%.10f", dx), String.format(Locale.JAPAN, "%.10f", dy));

//		/* 1.傾きを求める */
//		double slope = dy / dx;
//		TLog.d("1.傾きを求める slope={0}", String.format(Locale.JAPAN, "%.10f", slope));
//
//		/* 2.直行線分の傾きを求める */
//		double rslope = -1 / slope;
//		TLog.d("2.直行線分の傾きを求める rslope={0}", String.format(Locale.JAPAN, "%.10f", rslope));

		/* 3.直行線分の傾きの角度(rad)を求める */
		double rdegree = Math.atan2(dx, -dy);	/* 直交座標なので反転(xy入替え)する */
		TLog.d("3.直行線分の傾きの角度(°)を求める rdegree={0}", String.format(Locale.JAPAN, "%.10f", rdegree));

		/* 4. 50cmをx,y成分に分ける1 x成分を求める */
		double newdx = 50/*cm*/ * Math.cos(rdegree);	/* 引数はすでにrad. */
		TLog.d("4. 50cmをx,y成分に分ける1 x成分を求める newdx={0}", String.format(Locale.JAPAN, "%.10f", newdx));

		/* 5. 50cmをx,y成分に分ける2 y成分を求める */
		double newdy = 50/*cm*/ * Math.sin(rdegree);	/* 引数はすでにrad. */
		TLog.d("5. 50cmをx,y成分に分ける2 y成分を求める newdy={0}", String.format(Locale.JAPAN, "%.10f", newdy));

		/* 6. x成分を度分秒に変換 */
		double difflng = newdx * (1/(BASE_DISTANCE_X*100));
		TLog.d("6. x成分を度分秒に変換 difflng={0}", String.format(Locale.JAPAN, "%.10f", difflng));

		/* 7. y成分を度分秒に変換 */
		double difflat = newdx * (1/(BASE_DISTANCE_Y*100));
		TLog.d("7. y成分を度分秒に変換 difflat={0}", String.format(Locale.JAPAN, "%.10f", difflat));

		/* 8. (左上/右上/左下/右下) 経度/緯度を算出 */
		LatLng ltpos, rtpos, lbpos, rbpos;
		if( (epos.longitude-spos.longitude > 0 && epos.latitude-spos.latitude > 0) ||	/* 第1象限 or */
				(epos.longitude-spos.longitude < 0 && epos.latitude-spos.latitude < 0)) {	/* 第3象限 */
			ltpos = new LatLng(spos.latitude-difflat, spos.longitude+difflng);
			rtpos = new LatLng(epos.latitude-difflat, epos.longitude+difflng);
			lbpos = new LatLng(spos.latitude+difflat, spos.longitude-difflng);
			rbpos = new LatLng(epos.latitude+difflat, epos.longitude-difflng);
		}
		else {
			/* 第2象限 or 第4象限 */
			ltpos = new LatLng(spos.latitude-difflat, spos.longitude-difflng);
			rtpos = new LatLng(epos.latitude-difflat, epos.longitude-difflng);
			lbpos = new LatLng(spos.latitude+difflat, spos.longitude+difflng);
			rbpos = new LatLng(epos.latitude+difflat, epos.longitude+difflng);
		}

		TLog.d("8. 完成 左上=({0})", String.format(Locale.JAPAN, "%.10f,%.10f", ltpos.latitude, ltpos.longitude));
		TLog.d("8. 完成 右上=({0})", String.format(Locale.JAPAN, "%.10f,%.10f", rtpos.latitude, rtpos.longitude));
		TLog.d("8. 完成 左下=({0})", String.format(Locale.JAPAN, "%.10f,%.10f", lbpos.latitude, lbpos.longitude));
		TLog.d("8. 完成 右下=({0})", String.format(Locale.JAPAN, "%.10f,%.10f", rbpos.latitude, rbpos.longitude));

		return new LatLng[]{ltpos, rtpos, lbpos, rbpos};
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
