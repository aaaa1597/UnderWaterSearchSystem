package com.tks.maptest.ui;

import static com.tks.maptest.Constants.UWS_LOC_BASE_DISTANCE_X;
import static com.tks.maptest.Constants.UWS_LOC_BASE_DISTANCE_Y;
import static com.tks.maptest.Constants.UWS_LOC_BASE_LATITUDE;
import static com.tks.maptest.Constants.UWS_LOC_BASE_LONGITUDE;
import static com.tks.maptest.Constants.d2Str;

import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Matrix;
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
import com.tks.maptest.Matrix33d;
import com.tks.maptest.R;
import com.tks.maptest.TLog;

import java.util.Arrays;
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
//				initDraw(mLocation, mGoogleMap);
				/* TODO ここまで */
				Location loc = new Location("");
				loc.setLongitude(UWS_LOC_BASE_LONGITUDE);
				loc.setLatitude(UWS_LOC_BASE_LATITUDE);
				initDraw(loc, mGoogleMap);
				/* ここまで */
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
//						initDraw(mLocation, mGoogleMap);
						/* TODO ここまで */
						Location loc = new Location("");
						loc.setLongitude(UWS_LOC_BASE_LONGITUDE);
						loc.setLatitude(UWS_LOC_BASE_LATITUDE);
						initDraw(loc, mGoogleMap);
						/* ここまで */
						TLog.d("");
					}
				}, Looper.getMainLooper());
			}
			else {
				TLog.d("mLocation=(経度:{0} 緯度:{1}) mMap={1}", location.getLatitude(), location.getLongitude(), mGoogleMap);
				mLocation = location;
//				initDraw(mLocation, mGoogleMap);
				/* TODO ここまで */
				Location loc = new Location("");
				loc.setLongitude(UWS_LOC_BASE_LONGITUDE);
				loc.setLatitude(UWS_LOC_BASE_LATITUDE);
				initDraw(loc, mGoogleMap);
				/* ここまで */
			}
		});
		TLog.d("");
	}

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
		TLog.d("現在地付近の1°当たりの距離[m]:		{0}	[m]	{1}	[m]", UWS_LOC_BASE_DISTANCE_X, UWS_LOC_BASE_DISTANCE_Y);
		TLog.d("現在地 	緯度:	{0}	経度:	{1}", d2Str(nowpos.latitude), d2Str(nowpos.longitude));

		/* p1 */
		SerchInfo p1info = mSerchInfos.get("p1");
		if(p1info != null) {
			if(p1info.maker != null) p1info.maker.remove();
			if(p1info.circle != null) p1info.circle.remove();
			if(p1info.polygon != null) p1info.polygon.remove();
		}
//		LatLng p1pos = new LatLng(nowpos.latitude+(1/UWS_LOC_BASE_DISTANCE_Y)*5, nowpos.longitude+(1/UWS_LOC_BASE_DISTANCE_X)*5);
//		LatLng p1pos = new LatLng(nowpos.latitude+(1/UWS_LOC_BASE_DISTANCE_Y)*5, nowpos.longitude-(1/UWS_LOC_BASE_DISTANCE_X)*5);
//		LatLng p1pos = new LatLng(nowpos.latitude-(1/UWS_LOC_BASE_DISTANCE_Y)*5, nowpos.longitude+(1/UWS_LOC_BASE_DISTANCE_X)*5);
		LatLng p1pos = new LatLng(nowpos.latitude-(1/UWS_LOC_BASE_DISTANCE_Y)*5, nowpos.longitude-(1/UWS_LOC_BASE_DISTANCE_X)*5);
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
//		LatLng p2pos = new LatLng(nowpos.latitude+(1/UWS_LOC_BASE_DISTANCE_Y)*10, nowpos.longitude+(1/UWS_LOC_BASE_DISTANCE_X)*10);
//		LatLng p2pos = new LatLng(nowpos.latitude+(1/UWS_LOC_BASE_DISTANCE_Y)*10, nowpos.longitude-(1/UWS_LOC_BASE_DISTANCE_X)*10);
//		LatLng p2pos = new LatLng(nowpos.latitude-(1/UWS_LOC_BASE_DISTANCE_Y)*10, nowpos.longitude+(1/UWS_LOC_BASE_DISTANCE_X)*10);
		LatLng p2pos = new LatLng(nowpos.latitude-(1/UWS_LOC_BASE_DISTANCE_Y)*10, nowpos.longitude-(1/UWS_LOC_BASE_DISTANCE_X)*10);
		TLog.d("p2	緯度:	{0}	経度:	{1}", d2Str(p2pos.latitude), d2Str(p2pos.longitude));
		Marker p2marker = googleMap.addMarker(new MarkerOptions()
												.position(p2pos)
												.title("p2")
												.icon(createIcon((short)2)));		BitmapDescriptorFactory.fromResource(R.drawable.marker2);

		/* 検索線 描画 */
		LatLng[] square = createSquare(p1pos, p2pos, UWS_LOC_BASE_DISTANCE_X, UWS_LOC_BASE_DISTANCE_Y);
		Polygon lpolygon = googleMap.addPolygon(new PolygonOptions()
									.fillColor(Color.CYAN)
//									.strokeColor(Color.CYAN)
									.add(square[0], square[1], square[3], square[2])
									);


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
		/* 0-1.前準備 検索線分の角度(degrees)を求める */
		double dx = epos.longitude* (BASE_DISTANCE_X/100000) - spos.longitude* (BASE_DISTANCE_X/100000);
		double dy = epos.latitude * (BASE_DISTANCE_Y/100000) - spos.latitude * (BASE_DISTANCE_Y/100000);
		double degrees = Math.atan2(dy, dx) * 180 / Math.PI;
		/*TODO*/TLog.d("0-1 歪み補正 dx={0} dy={1}", d2Str(dx), d2Str(dy));
		/*TODO*/TLog.d("0-1 検索線分の角度(°:degrees)を求める spos({1},{0}) epos({3},{2}) degrees={4}", d2Str(spos.longitude), d2Str(spos.latitude), d2Str(epos.longitude), d2Str(epos.latitude), d2Str(degrees));

		/* 0-2.前準備 行列生成 */
		Matrix mat = new Matrix();
		mat.reset();

		/* 1.単位変換適用(cm座標系 → 度分秒座標系) */
		mat.postScale((float)(1/(BASE_DISTANCE_X*100)), (float)(1/(BASE_DISTANCE_Y*100)));

		/* 2.回転適用(検索線分の角度) */
		mat.postRotate((float)degrees);

		float[] src4vertex = {/*右上*/50,50,/*右下*/50,-50,/*左上*/-50,50,/*左下*/-50,-50};
		float[] dst4vertex = new float[src4vertex.length];
		mat.mapPoints(dst4vertex, src4vertex);

		TLog.d("pre完成  右上(	{0}	{1}	)	\n右下(	{2}	{3}	)	\n左上(	{4}	{5}	) \n左下(	{6}	{7}	)",
				d2Str(dst4vertex[0]), d2Str(dst4vertex[1]),
				d2Str(dst4vertex[2]), d2Str(dst4vertex[3]),
				d2Str(dst4vertex[4]), d2Str(dst4vertex[5]),
				d2Str(dst4vertex[6]), d2Str(dst4vertex[7]));

		TLog.d("完成  spos:\n{0},{1}\nepos:\n{2},{3}", d2Str(spos.latitude), d2Str(spos.longitude), d2Str(epos.latitude), d2Str(epos.longitude));
		TLog.d("完成2 右上:\n{0},{1}\n右下:\n{2},{3}\n左上:\n{4},{5}\n左下:\n{6},{7}\n",
				d2Str(epos.latitude+dst4vertex[1]), d2Str(epos.longitude+dst4vertex[0]),
				d2Str(epos.latitude+dst4vertex[3]), d2Str(epos.longitude+dst4vertex[2]),
				d2Str(spos.latitude+dst4vertex[5]), d2Str(spos.longitude+dst4vertex[4]),
				d2Str(spos.latitude+dst4vertex[7]), d2Str(spos.longitude+dst4vertex[6]));

		/* 0.前準備  */
		Matrix mataaa = new Matrix();
		mataaa.reset();

		/* 1.単位変換(cm座標系 → 度分秒座標系)適用 */
		mataaa.postScale((float)(1/(BASE_DISTANCE_X*100)), (float)(1/(BASE_DISTANCE_Y*100)));

		TLog.d("確認値1-1	1°距離({0},{1})", d2Str(BASE_DISTANCE_X), d2Str(BASE_DISTANCE_Y));
		TLog.d("確認値1-2	-----({0},{1})", d2Str(1/(BASE_DISTANCE_X*100)), d2Str(1/(BASE_DISTANCE_Y*100)));

		/* 2.四隅の座標を求める。 */
		float[] src4Corners = {/*右上*/50,50,/*右下*/50,-50,/*左上*/-50,50,/*左下*/-50,-50};
		TLog.d("確認値pre ph0 lt(	{0}	{1}	)	\nrt(	{2}	{3}	)	\nlb(	{4}	{5}	) \nrb(	{6}	{7}	)",
				d2Str(src4Corners[0]), d2Str(src4Corners[1]),
				d2Str(src4Corners[2]), d2Str(src4Corners[3]),
				d2Str(src4Corners[4]), d2Str(src4Corners[5]),
				d2Str(src4Corners[6]), d2Str(src4Corners[7]));

		float[] dst4Corners = new float[src4Corners.length];
		mataaa.mapPoints(dst4Corners, src4Corners);
		TLog.d("確認値pre ph1 diff lt(	{0}	{1}	)	\nrt(	{2}	{3}	)	\nlb(	{4}	{5}	) \nrb(	{6}	{7}	)",
				d2Str(dst4Corners[0]), d2Str(dst4Corners[1]),
				d2Str(dst4Corners[2]), d2Str(dst4Corners[3]),
				d2Str(dst4Corners[4]), d2Str(dst4Corners[5]),
				d2Str(dst4Corners[6]), d2Str(dst4Corners[7]));

		float[] dst2 = new float[src4Corners.length];
		mataaa.postRotate(135);
		mataaa.mapPoints(dst2, src4Corners);
		TLog.d("確認値pre ph2 lt(	{0}	{1}	)	\nrt(	{2}	{3}	)	\nlb(	{4}	{5}	) \nrb(	{6}	{7}	)",
								d2Str(dst2[0]), d2Str(dst2[1]),
								d2Str(dst2[2]), d2Str(dst2[3]),
								d2Str(dst2[4]), d2Str(dst2[5]),
								d2Str(dst2[6]), d2Str(dst2[7]));

		TLog.d("--------------------------------------------------------------------");
		TLog.d("確認値2 	spos({1},{0})	epos({3},{2})",	d2Str(spos.longitude), d2Str(spos.latitude), d2Str(epos.longitude), d2Str(epos.latitude));
		TLog.d("確認値2 	diff lt({1},{0})	rt({3},{2})	lb({5},{4}) rb({7},{6})",	d2Str(dst4Corners[0]), d2Str(dst4Corners[1]),
																						d2Str(dst4Corners[2]), d2Str(dst4Corners[3]),
																						d2Str(dst4Corners[4]), d2Str(dst4Corners[5]),
																						d2Str(dst4Corners[6]), d2Str(dst4Corners[7]));
		TLog.d("確認値2 	lt({1},{0})	rt({3},{2})	lb({5},{4}) rb({7},{6})",	d2Str(epos.longitude+dst4Corners[0]), d2Str(epos.latitude+dst4Corners[1]),
																				d2Str(spos.longitude+dst4Corners[2]), d2Str(spos.latitude+dst4Corners[3]),
																				d2Str(epos.longitude+dst4Corners[4]), d2Str(epos.latitude+dst4Corners[5]),
																				d2Str(spos.longitude+dst4Corners[6]), d2Str(spos.latitude+dst4Corners[7]));

		/* 3.検索線分の角度(degrees)を求める */
		double dxaaa = epos.longitude*(-BASE_DISTANCE_X/100000) - spos.longitude*(-BASE_DISTANCE_X/100000);
		double dyaaa = epos.latitude * (BASE_DISTANCE_Y/100000) - spos.latitude * (BASE_DISTANCE_Y/100000);
		TLog.d("2.1-歪み補正 dx={0} dy={1}", d2Str(dxaaa), d2Str(dyaaa));
		double degreesaaa = Math.atan2(dyaaa, dxaaa) * 180 / Math.PI;
		TLog.d("確認値3角度 直行線分の傾きの角度(°)を求める spos({1},{0}) epos({3},{2}) degrees={4}", d2Str(spos.longitude), d2Str(spos.latitude), d2Str(epos.longitude), d2Str(epos.latitude), d2Str(degreesaaa));

//		double radians = degrees * Math.PI / 180;
//		public final static double	UWS_LOC_BASE_LONGITUDE	= 130.19153989612116;
//		public final static double	UWS_LOC_BASE_LATITUDE	= 33.29307995564407;
		double radians = 0.1/*degress*/ * Math.PI / 180;
		double dstX = UWS_LOC_BASE_LONGITUDE * Math.cos(radians) - UWS_LOC_BASE_LATITUDE * Math.sin(radians);
		double dstY = UWS_LOC_BASE_LONGITUDE * Math.sin(radians) + UWS_LOC_BASE_LATITUDE * Math.cos(radians);
		TLog.d("確認値ret 小城署0.1°ずれ pos({1},{0}) degrees={2}", d2Str(dstX), d2Str(dstY), 1);

		/* 左上/左下頂点の位置を生成 */
		double[] srcLPos = {/*左上*/epos.longitude+dst4Corners[0], epos.latitude+dst4Corners[1],
							/*左下*/epos.longitude+dst4Corners[4], epos.latitude+dst4Corners[5]};
		double[] dstLPos = new double[srcLPos.length];
		{
			Matrix33d lmat = new Matrix33d();
			lmat.reset();
			lmat.postTranslate(-epos.longitude, -epos.latitude);
			lmat.postRotate(degreesaaa);
			lmat.postTranslate(epos.longitude, epos.latitude);
			lmat.mapPoints(dstLPos, srcLPos);
		}

		/* 右上/右下頂点の位置を生成 */
		double[] srcRPos = {/*右上*/spos.longitude+dst4Corners[2], spos.latitude+dst4Corners[3],
							/*右下*/spos.longitude+dst4Corners[6], spos.latitude+dst4Corners[7]};
		double[] dstRPos = new double[srcRPos.length];
		{
			Matrix33d rmat = new Matrix33d();
			rmat.reset();
			rmat.postTranslate(-spos.longitude, -spos.latitude);
			rmat.postRotate(degreesaaa);
			rmat.postTranslate(spos.longitude, spos.latitude);
			rmat.mapPoints(dstRPos, srcRPos);
		}

		TLog.d("出来上がり 	lt({1},{0})	rt({3},{2})	lb({5},{4}) rb({7},{6})",	d2Str(dstLPos[0]), d2Str(dstLPos[1]),
																					d2Str(dstRPos[0]), d2Str(dstRPos[1]),
																					d2Str(dstLPos[2]), d2Str(dstLPos[3]),
																					d2Str(dstRPos[2]), d2Str(dstRPos[3]));

//		{
//			double[] srcTrans = {/*左上*/130+10,33+10,/*右上*/130-10,33+10,/*左下*/130+10,33-10,/*右下*/130-10,33-10};
//			double[] inval = new double[9];
//
//			/* 移動行列 検算 */
//			Matrix33d transmat = new Matrix33d();
//			transmat.reset();
//			transmat.postTranslate(-130,-33);
//			double[] dstTrans = new double[srcTrans.length];
//			transmat.mapPoints(dstTrans, srcTrans);
//			transmat.getValues(inval);
//			TLog.d("移動行列 検算 中身={0}", Arrays.toString(inval));
//			TLog.d("移動行列 検算 arg={0}", Arrays.toString(srcTrans));
//			TLog.d("移動行列 検算 dst={0}", Arrays.toString(dstTrans));
//
//			{
//				Matrix33d aaaa = new Matrix33d();
//				aaaa.setValues(new double[]{0,1,2,3,4,5,6,7,8});
////				aaaa.setValues(new double[]{1,1,1,1,1,1,1,1,1});
//				Matrix33d bbbb = new Matrix33d();
//				bbbb.setValues(new double[]{1,2,3,4,5,6,7,8,9});
////				bbbb.setValues(new double[]{2,1,1,1,1,1,1,1,1});
//				Matrix33d newaaaa = aaaa.multiMatrix(aaaa, bbbb);
//				newaaaa.getValues(inval);
//				TLog.d("試し 中身aaa={0}", Arrays.toString(inval));
//			}
//
//			transmat.postRotate(30);
//			double[] srcRot = srcTrans;
//			double[] dstRot = new double[srcRot.length];
//			transmat.mapPoints(dstRot, srcRot);
//			transmat.getValues(inval);
//			TLog.d("回転行列 検算 中身={0}", Arrays.toString(inval));
//			TLog.d("回転行列 検算 arg={0}", Arrays.toString(srcRot));
//			TLog.d("回転行列 検算 dst={0}", Arrays.toString(dstRot));
//
//			transmat.postTranslate(130,33);
//			double[] srcRTrans = srcTrans;
//			double[] dstRTrans = new double[dstRot.length];
//			transmat.mapPoints(dstRTrans, srcRTrans);
//			transmat.getValues(inval);
//			TLog.d("移動行列戻し 検算 中身={0}", Arrays.toString(inval));
//			TLog.d("移動行列戻し 検算 arg={0}", Arrays.toString(srcRTrans));
//			TLog.d("移動行列戻し 検算 dst={0}", Arrays.toString(dstRTrans));
//		}
//
//		TLog.d("::: Android::Matrix :::::::::::::::::::::::::::::::::::");
//
//		{
//			float[] srcTrans = {/*左上*/130+10,33+10,/*右上*/130-10,33+10,/*左下*/130+10,33-10,/*右下*/130-10,33-10};
//			float[] inval = new float[9];
//
//			/* 移動行列 検算 */
//			Matrix transmat = new Matrix();
//			transmat.reset();
//			transmat.postTranslate(-130,-33);
//			float[] dstTrans = new float[srcTrans.length];
//			transmat.mapPoints(dstTrans, srcTrans);
//			transmat.getValues(inval);
//			TLog.d("移動行列(a) 検算 中身={0}", Arrays.toString(inval));
//			TLog.d("移動行列(a) 検算 arg={0}", Arrays.toString(srcTrans));
//			TLog.d("移動行列(a) 検算 dst={0}", Arrays.toString(dstTrans));
//
//			{
//				Matrix aaaa = new Matrix();
//				aaaa.setValues(new float[]{0,1,2,3,4,5,6,7,8});
////				aaaa.setValues(new float[]{1,1,1,1,1,1,1,1,1});
//				Matrix bbbb = new Matrix();
//				bbbb.setValues(new float[]{1,2,3,4,5,6,7,8,9});
////				bbbb.setValues(new float[]{2,1,1,1,1,1,1,1,1});
//				aaaa.postConcat(bbbb);
//				aaaa.getValues(inval);
//				TLog.d("試し 中身aaa={0}", Arrays.toString(inval));
//			}
//
//			transmat.postRotate(30);
//			float[] srcRot = srcTrans;
//			float[] dstRot = new float[srcRot.length];
//			transmat.mapPoints(dstRot, srcRot);
//			transmat.getValues(inval);
//			TLog.d("回転行列(a) 検算 中身={0}", Arrays.toString(inval));
//			TLog.d("回転行列(a) 検算 arg={0}", Arrays.toString(srcRot));
//			TLog.d("回転行列(a) 検算 dst={0}", Arrays.toString(dstRot));
//
//			transmat.postTranslate(130,33);
//			float[] srcRTrans = srcTrans;
//			float[] dstRTrans = new float[dstRot.length];
//			transmat.mapPoints(dstRTrans, srcRTrans);
//			transmat.getValues(inval);
//			TLog.d("移動行列戻し(a) 検算 中身={0}", Arrays.toString(inval));
//			TLog.d("移動行列戻し(a) 検算 arg={0}", Arrays.toString(srcRTrans));
//			TLog.d("移動行列戻し(a) 検算 dst={0}", Arrays.toString(dstRTrans));
//		}




//		/* 3-1.求めた角度で回転行列適用(左上,左下用) */
//		Matrix matLRot = new Matrix();
//		matLRot.reset();
////		matLRot.postRotate((float)degrees, -dst4Corners[0], -dst4Corners[1]);
////		matLRot.postRotate((float)degrees);
//		matLRot.postRotate((float)0);
//
//		float[] srclRot = {	/*左上*/(float)(epos.longitude+dst4Corners[0]), (float)(epos.latitude+dst4Corners[1]),
//							/*左下*/(float)(epos.longitude+dst4Corners[4]), (float)(epos.latitude+dst4Corners[5]) };
//		TLog.d("前 	lt({1},{0})	lb({3},{2})", d2Str(epos.longitude+dst4Corners[0]), d2Str(epos.latitude+dst4Corners[1]), d2Str(epos.longitude+dst4Corners[4]), d2Str(epos.latitude+dst4Corners[5]));
//		TLog.d("前 	lt({1},{0})	lb({3},{2})", d2Str((float)(epos.longitude+dst4Corners[0])), d2Str((float)(epos.latitude+dst4Corners[1])), d2Str((float)(epos.longitude+dst4Corners[4])), d2Str((float)(epos.latitude+dst4Corners[5])));
//		TLog.d("前 	lt({1},{0})	lb({3},{2})", d2Str(srclRot[0]), d2Str(srclRot[1]), d2Str(srclRot[2]), d2Str(srclRot[3]));
//		float[] dstlRot = new float[srclRot.length];
//		matLRot.mapPoints(dstlRot, srclRot);
//		TLog.d("後 	lt({1},{0})	lb({3},{2})", d2Str(dstlRot[0]), d2Str(dstlRot[1]), d2Str(dstlRot[2]), d2Str(dstlRot[3]));
//		TLog.d("後 	mat=({0})", matLRot);
//
//		/* 3-1.求めた角度で回転行列適用(左上,左下用) */
//		Matrix matrRot = new Matrix();
//		matrRot.reset();
////		matrRot.postRotate((float)degrees, -dst4Corners[2], -dst4Corners[3]);
////		matrRot.postRotate((float)degrees);
//		matrRot.postRotate((float)0);
//
//		float[] srcrRot = {	/*右上*/(float)(spos.longitude+dst4Corners[2]), (float)(spos.latitude+dst4Corners[3]),
//							/*右下*/(float)(spos.longitude+dst4Corners[6]), (float)(spos.latitude+dst4Corners[7]) };
//		float[] dstrRot = new float[srcrRot.length];
//		matrRot.mapPoints(dstrRot, srcrRot);
//
//
//		LatLng retlt = new LatLng(dstlRot[1], dstlRot[0]);
//		LatLng retrt = new LatLng(dstrRot[1], dstrRot[0]);
//		LatLng retlb = new LatLng(dstlRot[3], dstlRot[2]);
//		LatLng retrb = new LatLng(dstrRot[3], dstrRot[2]);
//
//		TLog.d("--------------------------------------------------------------------");
//		TLog.d("ret 	lt({1},{0})	rt({3},{2})	lb({5},{4}) rb({7},{6})",	d2Str(retlt.longitude), d2Str(retlt.latitude),
//																				d2Str(retrt.longitude), d2Str(retrt.latitude),
//																				d2Str(retlb.longitude), d2Str(retlb.latitude),
//																				d2Str(retrb.longitude), d2Str(retrb.latitude));

//		/* 1.傾きを求める */
//		double slope = dy / dx;
//		TLog.d("1.傾きを求める slope={0}", d2Str(slope));
//
//		/* 2.直行線分の傾きを求める */
//		double rslope = -1 / slope;
//		TLog.d("2.直行線分の傾きを求める rslope={0}", d2Str(rslope));

//		/* 3.直行線分の傾きの角度(rad)を求める */
//		double rdegree = Math.atan2(dx, -dy);	/* 直交座標なので反転(xy入替え)する */
//		TLog.d("3.直行線分の傾きの角度(°)を求める rdegree={0}", d2Str(rdegree));
//
//		/* 4. 50cmをx,y成分に分ける1 x成分を求める */
//		double newdx = 50/*cm*/ * Math.cos(rdegree);	/* 引数はすでにrad. */
//		TLog.d("4. 50cmをx,y成分に分ける1 x成分を求める newdx={0}", d2Str(newdx));
//
//		/* 5. 50cmをx,y成分に分ける2 y成分を求める */
//		double newdy = 50/*cm*/ * Math.sin(rdegree);	/* 引数はすでにrad. */
//		TLog.d("5. 50cmをx,y成分に分ける2 y成分を求める newdy={0}", d2Str(newdy));
//
//		/* 6. x成分を度分秒に変換 */
//		double difflng = newdx * (1/(BASE_DISTANCE_X*100));
//		TLog.d("6. x成分を度分秒に変換 difflng={0}", d2Str(difflng));
//
//		/* 7. y成分を度分秒に変換 */
//		double difflat = newdx * (1/(BASE_DISTANCE_Y*100));
//		TLog.d("7. y成分を度分秒に変換 difflat={0}", d2Str(difflat));
//
//		/* 8. (左上/右上/左下/右下) 経度/緯度を算出 */
//		LatLng ltpos, rtpos, lbpos, rbpos;
//		if( (epos.longitude-spos.longitude > 0 && epos.latitude-spos.latitude > 0) ||	/* 第1象限 or */
//			(epos.longitude-spos.longitude < 0 && epos.latitude-spos.latitude < 0)) {	/* 第3象限 */
//			ltpos = new LatLng(spos.latitude-difflat, spos.longitude+difflng);
//			rtpos = new LatLng(epos.latitude-difflat, epos.longitude+difflng);
//			lbpos = new LatLng(spos.latitude+difflat, spos.longitude-difflng);
//			rbpos = new LatLng(epos.latitude+difflat, epos.longitude-difflng);
//		}
//		else {
//			/* 第2象限 or 第4象限 */
//			ltpos = new LatLng(spos.latitude-difflat, spos.longitude-difflng);
//			rtpos = new LatLng(epos.latitude-difflat, epos.longitude-difflng);
//			lbpos = new LatLng(spos.latitude+difflat, spos.longitude+difflng);
//			rbpos = new LatLng(epos.latitude+difflat, epos.longitude+difflng);
//		}
//
//		TLog.d("8. 完成 左上=({0})", String.format(Locale.JAPAN, "%.10f,%.10f", ltpos.latitude, ltpos.longitude));
//		TLog.d("8. 完成 右上=({0})", String.format(Locale.JAPAN, "%.10f,%.10f", rtpos.latitude, rtpos.longitude));
//		TLog.d("8. 完成 左下=({0})", String.format(Locale.JAPAN, "%.10f,%.10f", lbpos.latitude, lbpos.longitude));
//		TLog.d("8. 完成 右下=({0})", String.format(Locale.JAPAN, "%.10f,%.10f", rbpos.latitude, rbpos.longitude));

		return new LatLng[]{
				new LatLng(spos.latitude, spos.longitude),
				new LatLng(spos.latitude, spos.longitude),
				new LatLng(epos.latitude, epos.longitude),
				new LatLng(epos.latitude, epos.longitude),
			};
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