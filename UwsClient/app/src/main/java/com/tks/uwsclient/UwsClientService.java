package com.tks.uwsclient;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.IBinder;
import android.os.Looper;
import android.widget.RemoteViews;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import static com.tks.uwsclient.Constants.NOTIFICATION_CHANNEL_STARTSTOP;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.util.Locale;

public class UwsClientService extends Service {
	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		TLog.d("");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		switch (intent.getAction()) {
			case Constants.ACTION.INITIALIZE:
				TLog.d("aaaaaaaaaaaa 初期化.");
				startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
				uwsInit();
				break;
			case Constants.ACTION.FINALIZE:
				TLog.d("aaaaaaaaaaaa 終了.");
				uwsFin();
				stopForeground(true);
				stopSelf();
				break;
		}

		return START_NOT_STICKY;
	}

	/* ***********************/
	/* フォアグランドサービス機能 */
	/* ***********************/
	private Notification prepareNotification() {
		/* 通知のチャンネル生成 */
		NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_STARTSTOP, "startstop", NotificationManager.IMPORTANCE_DEFAULT);
		channel.enableVibration(false);
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.createNotificationChannel(channel);

		/* 停止ボタン押下の処理実装 */
		Intent stopIntent = new Intent(this, UwsClientService.class)
								.setAction(Constants.ACTION.FINALIZE);
		PendingIntent pendingStopIntent = PendingIntent.getService(this, 2222, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification);
		remoteViews.setOnClickPendingIntent(R.id.btnStop, pendingStopIntent);

		/* Notification生成 */
		return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_STARTSTOP)
				.setContent(remoteViews)
				.setSmallIcon(R.mipmap.ic_launcher)
//				.setCategory(NotificationCompat.CATEGORY_SERVICE)
//				.setOnlyAlertOnce(true)
//				.setOngoing(true)
//				.setAutoCancel(true)
//				.setContentIntent(pendingIntent);
//				.setVisibility(Notification.VISIBILITY_PUBLIC)
				.build();
	}

	/* ***************/
	/* 初期化/終了処理 */
	/* ***************/
	private void uwsInit() {
		startLoc();
	}

	private void uwsFin() {
		stoptLoc();
	}

	/***************/
	/* 位置情報 機能 */
	/***************/
	private final static int			LOC_UPD_INTERVAL = 2500;
	private FusedLocationProviderClient mFusedLocationClient;
	private final LocationRequest mLocationRequest = LocationRequest.create()
																	.setInterval(LOC_UPD_INTERVAL)
																	.setFastestInterval(LOC_UPD_INTERVAL)
																	.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	private final LocationCallback mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			TLog.d("1秒定期 (緯度:{0} 経度:{1})", String.format(Locale.JAPAN, "%1$.12f", location.getLatitude()), String.format(Locale.JAPAN, "%1$.12f", location.getLongitude()));

			/* 毎回OFF->ONにすることで、更新間隔が1秒になるようにしている。 */
			mFusedLocationClient.removeLocationUpdates(mLocationCallback);
			try { Thread.sleep(1000); } catch (InterruptedException e) { }
			restartLoc();
		}
	};

	/* 位置情報取得開始 */
	private void startLoc() {
		TLog.d("");
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			throw new RuntimeException("ありえない権限エラー。すでにチェック済。");
		mFusedLocationClient = LocationServices.getFusedLocationProviderClient(this);
		mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
	}

	/* 位置情報取得停止 */
	private void stoptLoc() {
		mFusedLocationClient.removeLocationUpdates(mLocationCallback);
	}

	/* 位置情報取得開始 */
	private void restartLoc() {
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			throw new RuntimeException("ありえない権限エラー。すでにチェック済。");
		mFusedLocationClient.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
	}
}
