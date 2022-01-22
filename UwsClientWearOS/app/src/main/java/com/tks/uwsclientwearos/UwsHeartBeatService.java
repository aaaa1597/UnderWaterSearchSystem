package com.tks.uwsclientwearos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.RemoteException;
import android.widget.RemoteViews;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.tks.uwsclientwearos.Constants.ACTION.FINALIZEFROMS;
import static com.tks.uwsclientwearos.Constants.NOTIFICATION_CHANNEL_STARTSTOP2;

public class UwsHeartBeatService extends Service {
	private SensorManager	mSensorManager;
	private final SensorEventCallback mSensorEventCallback = new SensorEventCallback() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			super.onSensorChanged(event);
			if(event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
				int hb = (int)event.values[0];
				TLog.d("bbbbbbbbbb 脈拍取得 heartbeat = {0}", hb);
				try { mIClientService.notifyHeartBeat(hb); }
				catch(RemoteException e) { e.printStackTrace(); }
			}
		}
	};

	IClientService mIClientService;
	private final ServiceConnection mCon = new ServiceConnection() {
		@Override public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			mIClientService = IClientService.Stub.asInterface(iBinder);
			TLog.d("UwsClientServiceと接続完了.");

			TLog.d("脈拍 開始");
			mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
			Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
			mSensorManager.registerListener(mSensorEventCallback, sensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
		@Override public void onServiceDisconnected(ComponentName componentName) {
			TLog.d("脈拍 停止");
			mSensorManager.unregisterListener(mSensorEventCallback);
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		switch (intent.getAction()) {
			case Constants.ACTION.INITIALIZE:
				TLog.d("yyyyy startForeground.");
				startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE, prepareNotification());
				bindService(new Intent(getApplicationContext(), UwsClientService.class), mCon, Context.BIND_AUTO_CREATE);
				break;
			case Constants.ACTION.FINALIZE:
				TLog.d("yyyyy stopForeground.");
				unbindService(mCon);
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
		NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_STARTSTOP2, "beartbeat", NotificationManager.IMPORTANCE_DEFAULT);
		channel.enableVibration(false);
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.createNotificationChannel(channel);

		/* 停止ボタン押下の処理実装 */
		Intent stopIntent = new Intent(this, UwsHeartBeatService.class)
										.setAction(Constants.ACTION.FINALIZE);
		PendingIntent pendingStopIntent = PendingIntent.getService(this, 2222, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		RemoteViews remoteViews = new RemoteViews(getPackageName(), R.layout.notification2);
		remoteViews.setOnClickPendingIntent(R.id.btnStop, pendingStopIntent);

		/* Notification生成 */
		return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_STARTSTOP2)
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

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
