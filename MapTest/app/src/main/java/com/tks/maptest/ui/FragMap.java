package com.tks.maptest.ui;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Picture;
import android.location.Location;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

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
import com.google.android.gms.maps.model.PolygonOptions;
import com.tks.maptest.R;
import com.tks.maptest.TLog;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public class FragMap extends SupportMapFragment {
	private FragMapViewModel			mViewModel;
	private GoogleMap					mGoogleMap;
	private Location					mLocation;
	private final Map<String, SerchInfo>mSerchInfos = new HashMap<>();
//	private final double				mDistanceMPerDegree = 40000*1000/*4万*1000m*/ * Math.cos(UWS_LOC_BASE_LATITUDE*Math.PI/180) / 360;
//	private final static double			UWS_LOC_BASE_LONGITUDE	= 130.19189394973347;
//	private final static double			UWS_LOC_BASE_LATITUDE	= 33.29333107719108;

	/* 検索情報 */
	static class SerchInfo {
		public Marker	maker;	/* GoogleMapの Marker */
		public Polygon polygon;/* GoogleMapの Polygon */
		public Circle circle;	/* GoogleMapの Circle 中心点は隊員の現在値 */
	};

	@NonNull
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		TLog.d("");
		super.onViewCreated(view, savedInstanceState);

		mViewModel = new ViewModelProvider(requireActivity()).get(FragMapViewModel.class);
		mViewModel.Permission().observe(getViewLifecycleOwner(), aBoolean -> {
			TLog.d("");
			getNowPosAndDraw();
			TLog.d("");
		});

		/* SupportMapFragmentを取得し、マップを使用する準備ができたら通知を受取る */
		SupportMapFragment mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager().findFragmentById(R.id.frgMap);
		Objects.requireNonNull(mapFragment).getMapAsync(new OnMapReadyCallback() {
			@Override
			public void onMapReady(@NonNull GoogleMap googleMap) {
				TLog.d("");
				TLog.d("mLocation={0} googleMap={1}", mLocation, googleMap);
				if (mLocation == null) {
					/* 位置が取れない時は、小城消防署で */
					mLocation = new Location("");
					mLocation.setLongitude(130.20307019743947);
					mLocation.setLatitude(33.25923509336276);
				}
				mGoogleMap = googleMap;
				initDraw(mLocation, mGoogleMap);
				TLog.d("");
			}
		});

		/* 現在値取得 → 地図更新 */
		getNowPosAndDraw();
		TLog.d("");
	}

	/* 現在値取得 → 地図更新 */
	private void getNowPosAndDraw() {
		TLog.d("");
		/* 権限なしなら何もしない。 */
		if(ActivityCompat.checkSelfPermission(getActivity().getApplicationContext(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			return;
		}

		TLog.d("");
		/* 位置情報管理オブジェクト */
		FusedLocationProviderClient flpc = LocationServices.getFusedLocationProviderClient(getActivity().getApplicationContext());
		flpc.getLastLocation().addOnSuccessListener(getActivity(), location -> {
			if (location == null) {
				TLog.d("mLocation={0} googleMap={1}", location, mGoogleMap);
				LocationRequest locreq = LocationRequest.create().setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY).setInterval(500).setFastestInterval(300);
				flpc.requestLocationUpdates(locreq, new LocationCallback() {
					@Override
					public void onLocationResult(@NonNull LocationResult locationResult) {
						TLog.d("");
						super.onLocationResult(locationResult);
						TLog.d("locationResult={0}({1},{2})", locationResult, locationResult.getLastLocation().getLatitude(), locationResult.getLastLocation().getLongitude());
						mLocation = locationResult.getLastLocation();
						flpc.removeLocationUpdates(this);
						initDraw(mLocation, mGoogleMap);
						TLog.d("");
					}
				}, Looper.getMainLooper());
			}
			else {
				TLog.d("mLocation=(経度:{0} 緯度:{1}) mMap={1}", location.getLatitude(), location.getLongitude(), mGoogleMap);
				mLocation = location;
				initDraw(mLocation, mGoogleMap);
			}
		});
		TLog.d("");
	}

	Marker ltMarker, rtMarker, lbMarker, rbMarker;
	/* 初期地図描画(起動直後は現在地を表示する) */
	private void initDraw(Location location, GoogleMap googleMap) {
		if (location == null) return;
		if (googleMap == null) return;

		LatLng nowpos = new LatLng(location.getLatitude(), location.getLongitude());
		TLog.d("経度:{0} 緯度:{1}", nowpos.longitude, nowpos.latitude);
		TLog.d("拡縮 min:{0} max:{1}", googleMap.getMinZoomLevel(), googleMap.getMaxZoomLevel());

		/* 現在地マーカ追加 */
		Marker basemarker = googleMap.addMarker(new MarkerOptions().position(nowpos).title("BasePos"));

		/* nowposから5m離れ~10m離れ間で線を引く */
		mSerchInfos.put("base", new SerchInfo(){{maker=basemarker; polygon=null;}});

		TLog.d("");
		/* 現在地付近の1°当たりの距離[m] = 92,874.58433678[m]   :::   現在地付近の1[m]当たりの角度(°) = 1/92,874.58433678(°) */
		final double BASE_DISTANCE_X = 40000*1000/*4万*1000m*/ * Math.cos(nowpos.latitude*180/Math.PI) / 360;
		final double BASE_DISTANCE_Y = 40000*1000/*4万*1000m*/ / 360.0;
		TLog.d("現在地付近の1°当たりの距離[m]:		{0}	[m]	{1}	[m]", BASE_DISTANCE_X, BASE_DISTANCE_Y);
		TLog.d("現在地 	緯度:	{0}	経度:	{1}", d2Str(nowpos.latitude), d2Str(nowpos.longitude));

		/* p1 */
		SerchInfo p1info = mSerchInfos.get("p1");
		if(p1info != null) {
			if(p1info.maker != null) p1info.maker.remove();
			if(p1info.circle != null) p1info.circle.remove();
			if(p1info.polygon != null) p1info.polygon.remove();
		}
		LatLng p1pos = new LatLng(nowpos.latitude-(1/BASE_DISTANCE_Y)*5, nowpos.longitude+(1/BASE_DISTANCE_X)*5);
		TLog.d("p1	緯度:	{0}	経度:	{1}", d2Str(p1pos.latitude), d2Str(p1pos.longitude));
		Marker p1marker = googleMap.addMarker(new MarkerOptions()
												.position(p1pos)
												.title("p1")
												.icon(createIcon((short)1)));		BitmapDescriptorFactory.fromResource(R.drawable.marker1);
//		Circle nowP1 = googleMap.addCircle(new CircleOptions()
//						.center(p1pos)
//						.radius(1.0)
//						.fillColor(Color.MAGENTA)
//						.strokeColor(Color.MAGENTA));
//		mSerchInfos.put("p1", new SerchInfo(){{maker=p1marker; circle=nowP1; polygon=null;}});
		mSerchInfos.put("p1", new SerchInfo(){{maker=p1marker; circle=null; polygon=null;}});

		/* p2 */
		SerchInfo p2info = mSerchInfos.get("p2");
		if(p2info != null) {
			if(p2info.maker != null) p2info.maker.remove();
			if(p2info.circle != null) p2info.circle.remove();
			if(p2info.polygon != null) p2info.polygon.remove();
		}
		LatLng p2pos = new LatLng(nowpos.latitude-(1/BASE_DISTANCE_Y)*10, nowpos.longitude+(1/BASE_DISTANCE_X)*10);
		TLog.d("p2	緯度:	{0}	経度:	{1}", d2Str(p2pos.latitude), d2Str(p2pos.longitude));
		Marker p2marker = googleMap.addMarker(new MarkerOptions()
												.position(p2pos)
												.title("p2")
												.icon(createIcon((short)2)));		BitmapDescriptorFactory.fromResource(R.drawable.marker2);

		/* 検索線 描画 */
		LatLng[] square = createSquare(p1pos, p2pos, BASE_DISTANCE_X, BASE_DISTANCE_Y);
		Polygon lpolygon = googleMap.addPolygon(new PolygonOptions()
									.fillColor(Color.CYAN)
//									.strokeColor(Color.CYAN)
									.add(square[0], square[1], square[3], square[2])
									);


		/* TODO aaaaaaaaaaaaa */
		if(ltMarker!=null) ltMarker.remove();
		if(rtMarker!=null) rtMarker.remove();
		if(lbMarker!=null) lbMarker.remove();
		if(rbMarker!=null) rbMarker.remove();
		ltMarker=googleMap.addMarker(new MarkerOptions() .position(square[0]).title("左上").icon(createIcon((short)5)));		BitmapDescriptorFactory.fromResource(R.drawable.marker5);
		rtMarker=googleMap.addMarker(new MarkerOptions() .position(square[1]).title("右上").icon(createIcon((short)6)));		BitmapDescriptorFactory.fromResource(R.drawable.marker6);
		lbMarker=googleMap.addMarker(new MarkerOptions() .position(square[2]).title("左下").icon(createIcon((short)7)));		BitmapDescriptorFactory.fromResource(R.drawable.marker7);
		rbMarker=googleMap.addMarker(new MarkerOptions() .position(square[3]).title("右下").icon(createIcon((short)8)));		BitmapDescriptorFactory.fromResource(R.drawable.marker8);




		Circle lcircle = googleMap.addCircle(new CircleOptions()
								.center(p2pos)
								.radius(0.5)
								.fillColor(Color.MAGENTA)
								.strokeColor(Color.MAGENTA));

		mSerchInfos.put("p2", new SerchInfo(){{maker=p2marker; circle=lcircle; polygon=lpolygon;}});



		/* 現在地マーカを中心に */
		googleMap.moveCamera(CameraUpdateFactory.newLatLng(nowpos));
		TLog.d("CameraPosition:{0}", googleMap.getCameraPosition().toString());

		/* 地図拡大率設定 */
		TLog.d("拡縮 zoom:{0}", 19);
		googleMap.moveCamera(CameraUpdateFactory.zoomTo(19));

		/* 地図俯角 50° */
		CameraPosition tilt = new CameraPosition.Builder(googleMap.getCameraPosition()).tilt(70).build();
		googleMap.moveCamera(CameraUpdateFactory.newCameraPosition(tilt));
	}

	private LatLng[] createSquare(final LatLng spos, final LatLng epos, final double BASE_DISTANCE_X, final double BASE_DISTANCE_Y) {
		/* 0.準備 */
		double dx = epos.longitude- spos.longitude;
		double dy = epos.latitude - spos.latitude;
		TLog.d("0.準備 dx={0} dy={1}", d2Str(dx), d2Str(dy));

//		/* 1.傾きを求める */
//		double slope = dy / dx;
//		TLog.d("1.傾きを求める slope={0}", d2Str(slope));
//
//		/* 2.直行線分の傾きを求める */
//		double rslope = -1 / slope;
//		TLog.d("2.直行線分の傾きを求める rslope={0}", d2Str(rslope));

		/* 3.直行線分の傾きの角度(rad)を求める */
		double rdegree = Math.atan2(dx, -dy);	/* 直交座標なので反転(xy入替え)する */
		TLog.d("3.直行線分の傾きの角度(°)を求める rdegree={0}", d2Str(rdegree));

		/* 4. 50cmをx,y成分に分ける1 x成分を求める */
		double newdx = 50/*cm*/ * Math.cos(rdegree);	/* 引数はすでにrad. */
		TLog.d("4. 50cmをx,y成分に分ける1 x成分を求める newdx={0}", d2Str(newdx));

		/* 5. 50cmをx,y成分に分ける2 y成分を求める */
		double newdy = 50/*cm*/ * Math.sin(rdegree);	/* 引数はすでにrad. */
		TLog.d("5. 50cmをx,y成分に分ける2 y成分を求める newdy={0}", d2Str(newdy));

		/* 6. x成分を度分秒に変換 */
		double difflng = newdx * (1/(BASE_DISTANCE_X*100));
		TLog.d("6. x成分を度分秒に変換 difflng={0}", d2Str(difflng));

		/* 7. y成分を度分秒に変換 */
		double difflat = newdx * (1/(BASE_DISTANCE_Y*100));
		TLog.d("7. y成分を度分秒に変換 difflat={0}", d2Str(difflat));

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