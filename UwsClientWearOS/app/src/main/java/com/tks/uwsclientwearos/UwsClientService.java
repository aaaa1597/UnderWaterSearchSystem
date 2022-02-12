package com.tks.uwsclientwearos;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
import android.widget.Toast;
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

import static com.tks.uwsclientwearos.Constants.ACTION.INITIALIZE;
import static com.tks.uwsclientwearos.Constants.ACTION.FINALIZE;
import static com.tks.uwsclientwearos.Constants.NOTIFICATION_CHANNEL_ID;
import static com.tks.uwsclientwearos.Constants.SERVICE_STATUS_CON_LOC_BEAT;
import static com.tks.uwsclientwearos.Constants.SERVICE_STATUS_INITIALIZING;
import static com.tks.uwsclientwearos.Constants.SERVICE_STATUS_IDLE;
import static com.tks.uwsclientwearos.Constants.d2Str;

public class UwsClientService extends Service {
	private int		mStatus		= SERVICE_STATUS_INITIALIZING;
	private short	mSeekerId	= -1;
	private IOnUwsInfoListner	mCallback;

	@Override
	public void onCreate() {
		super.onCreate();
		TLog.d("xxxxx");
		uwsInit();
		startLoc();
		IntentFilter filter = new IntentFilter();
		filter.addAction(FINALIZE);
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		TLog.d("xxxxx");
		LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
		stoptLoc();
		uwsFin();
	}

	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if( !intent.getAction().equals(FINALIZE)) return;

			Toast.makeText(getApplicationContext(), "終了します。", Toast.LENGTH_SHORT).show();
			LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
			stopForeground(true);
			stopSelf();
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		switch (intent.getAction()) {
			case INITIALIZE:
				TLog.d("startForeground.");
				startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE_BLE, prepareNotification());
				break;
			case FINALIZE:
				TLog.d("stopForeground.");
				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(FINALIZE));
				Toast.makeText(getApplicationContext(), "終了します。", Toast.LENGTH_SHORT).show();
				try { Thread.sleep(1000); } catch(InterruptedException ignore) { }
				stopForeground(true);
				stopSelf();
				break;
		}
		return START_NOT_STICKY;
	}

	/* ****************************/
	/* フォアグランドサービス機能 */
	/* ****************************/
	private Notification prepareNotification() {
		/* 通知のチャンネル生成 */
		NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "BLE/位置情報", NotificationManager.IMPORTANCE_DEFAULT);
		channel.enableVibration(false);
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.createNotificationChannel(channel);

		/* 停止ボタン押下の処理実装 */
		Intent stopIntent = new Intent(this, UwsClientService.class);	/* まず自分に送信。その後アプリと脈拍サービスに送信する */
		stopIntent.setAction(FINALIZE);
		PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(R.drawable.okicon, "終了", pendingStopIntent).build();

		/* Notification生成 */
		return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle("BLE/位置情報")
				.setContentText("頑張ってBLE/位置情報 取得中...")
				.setStyle(new NotificationCompat.BigTextStyle().bigText("頑張ってBLE/位置情報 取得中..."))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.addAction(stopAction)
				.build();
	}

	/* *****************/
	/* onBind/onUnbind */
	/* *****************/
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
		public void setOnUwsInfoChangeListner(IOnUwsInfoListner onUwsInfoListner) {
			mCallback = onUwsInfoListner;
		}

		@Override
		public int startUws(int seekerid) {
			return 0;
		}

		@Override
		public void stopUws() {
		}

		/* UwsHeartBeatServiceから、脈拍通知で呼ばれる */
		@Override
		public void notifyHeartBeat(int heartbeat) {
			TLog.d("脈拍通知 from HeartBeat-Service!! = {0}", heartbeat);
			try { mCallback.onHeartbeatResult((short)heartbeat); }
			catch(RemoteException e) { e.printStackTrace();}
		}
	};

	@Override
	public boolean onUnbind(Intent intent) {
		TLog.d("xxxxx");
		return super.onUnbind(intent);
	}

	/* ***************/
	/* 初期化/終了処理 */
	/* ***************/
	private void uwsInit() {
		TLog.d("xxxxx");
		mStatus = SERVICE_STATUS_IDLE;
		/* 位置情報初期化 */
		mFlc = LocationServices.getFusedLocationProviderClient(this);
		/* 位置情報取得開始 */
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			throw new RuntimeException("ありえない権限エラー。すでにチェック済。");
		mFlc.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
//		/* TODO Bluetooth初期化 */
	}
	private void uwsFin() {
		TLog.d("xxxxx");
		mFlc = null;
//		/* TODO Bluetooth終了 */
	}

	/* *************/
	/* 開始/停止処理 */
	/* *************/

	/* *************/
	/* 位置情報 機能 */
	/* *************/
	private final static int			LOC_UPD_INTERVAL = 2000;
	private FusedLocationProviderClient	mFlc;
	private final LocationRequest		mLocationRequest = LocationRequest.create().setInterval(LOC_UPD_INTERVAL).setFastestInterval(LOC_UPD_INTERVAL).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	private final LocationCallback		mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			TLog.d("1秒定期 (緯度:{0} 経度:{1})", d2Str(location.getLatitude()), d2Str(location.getLongitude()));

			if(mCallback != null) {
				try { mCallback.onLocationResult(location); }
				catch(RemoteException e) { e.printStackTrace();}
			}

			/* 毎回OFF->ONにすることで、更新間隔が1秒になるようにしている。 */
			stoptLoc();
			try { Thread.sleep(LOC_UPD_INTERVAL); } catch (InterruptedException ignored) { }
			startLoc();
		}
	};

	/* 位置情報取得開始 */
	private void startLoc() {
		TLog.d("xxxxx");
		if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			throw new RuntimeException("ありえない権限エラー。すでにチェック済。");
		mFlc.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
	}

	/* 位置情報取得停止 */
	private void stoptLoc() {
		TLog.d("xxxxx");
		mFlc.removeLocationUpdates(mLocationCallback);
	}
}

