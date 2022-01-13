package com.tks.uwsclient;

import java.util.Locale;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.RemoteViews;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import static com.tks.uwsclient.Constants.ACTION.FINALIZEFROMS;
import static com.tks.uwsclient.Constants.NOTIFICATION_CHANNEL_STARTSTOP;
import static com.tks.uwsclient.Constants.SERVICE_STATUS_AD_LOC_BEAT;
import static com.tks.uwsclient.Constants.SERVICE_STATUS_INITIALIZING;
import static com.tks.uwsclient.Constants.SERVICE_STATUS_IDLE;

public class UwsClientService extends Service {
	private int mStatus = SERVICE_STATUS_INITIALIZING;
	private IOnStatusChangeListner mOnStatusChangeListner;

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		TLog.d("xxxxx");
		return mBinder;
	}
	private final Binder mBinder = new IClientService.Stub() {
		@Override
		public StatusInfo getServiceStatus() {
			return new StatusInfo(mStatus, mSeekerId);
		}

		@Override
		public int startUws(int seekerid, IOnStatusChangeListner listner) {
			mStatus = SERVICE_STATUS_AD_LOC_BEAT;
			mSeekerId = (short)seekerid;
			mOnStatusChangeListner = listner;
			startLoc();
			return 0;
		}

		@Override
		public void stopUws() {
			stoptLoc();
		}
	};

	@Override
	public boolean onUnbind(Intent intent) {
		TLog.d("xxxxx");
		return super.onUnbind(intent);
	}

	@Override
	public void onCreate() {
		super.onCreate();
		uwsInit();
		mStatus = SERVICE_STATUS_IDLE;
		TLog.d("xxxxx");
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		uwsFin();
		TLog.d("xxxxx");
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		switch (intent.getAction()) {
			case Constants.ACTION.INITIALIZE:
				TLog.d("xxxxx startForeground.");
				startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
				break;
			case Constants.ACTION.FINALIZE:
				TLog.d("xxxxx stopForeground.");
				stopForeground(true);
				stopSelf();
				reqAppFinish();
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

	/* アプリ終了要求 */
	private void reqAppFinish() {
		Intent intent = new Intent(FINALIZEFROMS);
		LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(intent);
	}

	/* ***************/
	/* 初期化/終了処理 */
	/* ***************/
	private void uwsInit() {
		/* 位置情報初期化 */
		mFlc = LocationServices.getFusedLocationProviderClient(this);
		/* Bluetooth初期化 */
		final BluetoothManager bluetoothManager = (BluetoothManager)getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null)
			throw new RuntimeException("Bluetooth未サポート.使用不可!!");

		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null)
			throw new RuntimeException("Bluetooth未サポート.使用不可2!!");

		mBluetoothLeAdvertiser = bluetoothAdapter.getBluetoothLeAdvertiser();
		if (mBluetoothLeAdvertiser == null)
			throw new RuntimeException("Bluetooth未サポート.使用不可3!!");

		TLog.d( "アドバタイズの最大サイズ={0}", bluetoothAdapter.getLeMaximumAdvertisingDataLength());
	}

	private void uwsFin() {
		mFlc = null;
		mBluetoothLeAdvertiser = null;
	}

	/* *************/
	/* 位置情報 機能 */
	/* *************/
	private final static int			LOC_UPD_INTERVAL = 2500;
	private FusedLocationProviderClient mFlc;
	private final LocationRequest		mLocationRequest = LocationRequest.create()
															.setInterval(LOC_UPD_INTERVAL)
															.setFastestInterval(LOC_UPD_INTERVAL)
															.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	private final LocationCallback		mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			TLog.d("1秒定期 (緯度:{0} 経度:{1})", String.format(Locale.JAPAN, "%1$.12f", location.getLatitude()), String.format(Locale.JAPAN, "%1$.12f", location.getLongitude()));

			/* 毎回OFF->ONにすることで、更新間隔が1秒になるようにしている。 */
			stoptLoc();
			try { Thread.sleep(1000); } catch (InterruptedException e) { }
			startLoc();
		}
	};

	/* 位置情報取得開始 */
	private void startLoc() {
		TLog.d("");
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			throw new RuntimeException("ありえない権限エラー。すでにチェック済。");
		mFlc.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
	}

	/* 位置情報取得停止 */
	private void stoptLoc() {
		mFlc.removeLocationUpdates(mLocationCallback);
	}

	/* ********/
	/* BLE機能 */
	/* ********/
	private short					mSeekerId = 0;
	private BluetoothLeAdvertiser	mBluetoothLeAdvertiser;

}
