package com.tks.uwsclientwearos;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventCallback;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.RemoteException;
import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import static com.tks.uwsclientwearos.Constants.ACTION.INITIALIZE;
import static com.tks.uwsclientwearos.Constants.ACTION.FINALIZE;
import static com.tks.uwsclientwearos.Constants.NOTIFICATION_CHANNEL_ID;

public class UwsHeartBeatService extends Service {

	@Override
	public void onCreate() {
		super.onCreate();
		TLog.d("xxxxx");
		IntentFilter filter = new IntentFilter();
		filter.addAction(FINALIZE);
		LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(mReceiver, filter);
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		TLog.d("xxxxx");
		LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
	}

	/* 終了要求インテント 受信設定 */
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if( !intent.getAction().equals(FINALIZE)) return;

			mSensorManager.unregisterListener(mSensorEventCallback);
			unbindService(mCon);
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
				startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE_HB, prepareNotification());
				bindService(new Intent(getApplicationContext(), UwsClientService.class), mCon, Context.BIND_AUTO_CREATE);
				break;
//			/* この処理は不要。FINALIZEは、mReceiver::onReceive()で処理する */
//			case FINALIZE:
//				TLog.d("stopForeground.");
//				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcastSync(new Intent(FINALIZE));
//				unbindService(mCon);
//				try { Thread.sleep(1000); } catch(InterruptedException ignore) { }
//				stopForeground(true);
//				stopSelf();
//				break;
		}
		return START_NOT_STICKY;
	}

	/* ****************************/
	/* フォアグランドサービス機能 */
	/* ****************************/
	private Notification prepareNotification() {
		/* 通知のチャンネル生成 */
		NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "脈拍", NotificationManager.IMPORTANCE_DEFAULT);
		channel.enableVibration(false);
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.createNotificationChannel(channel);

		/* 停止ボタン押下の処理実装 */
		Intent stopIntent = new Intent(UwsHeartBeatService.this, UwsHeartBeatService.class);	/* まず自分に送信。その後アプリとBEL/位置情報サービスに送信する */
		stopIntent.setAction(FINALIZE);
		PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_UPDATE_CURRENT);
		NotificationCompat.Action stopAction = new NotificationCompat.Action.Builder(R.drawable.okicon, "終了", pendingStopIntent).build();

		/* Notification生成 */
		return new NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
				.setSmallIcon(R.mipmap.ic_launcher)
				.setContentTitle("脈拍")
				.setContentText("頑張って脈拍取得中...")
				.setStyle(new NotificationCompat.BigTextStyle().bigText("頑張って脈拍取得中..."))
				.setPriority(NotificationCompat.PRIORITY_DEFAULT)
				.addAction(stopAction)
				.build();
	}

	private SensorManager	mSensorManager;
	private	IClientService	mIClientService;
	private final ServiceConnection mCon = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName componentName, IBinder iBinder) {
			TLog.d("");
			mIClientService = IClientService.Stub.asInterface(iBinder);
			try {
				mIClientService.setNotifyStartCheckCleared(new IStartCheckClearedCallback.Stub() {
					@Override
					public void notifyStartCheckCleared() {
						TLog.d("脈拍 開始");
						Sensor sensor = mSensorManager.getDefaultSensor(Sensor.TYPE_HEART_RATE);
						mSensorManager.registerListener(mSensorEventCallback, sensor, SensorManager.SENSOR_DELAY_NORMAL);
					}
				});
			}
			catch(RemoteException ignore) {}

			mSensorManager = (SensorManager)getSystemService(SENSOR_SERVICE);
		}

		@Override
		public void onServiceDisconnected(ComponentName componentName) {
			TLog.d("脈拍 停止");
			mSensorManager.unregisterListener(mSensorEventCallback);
		}
	};

	private final SensorEventCallback mSensorEventCallback = new SensorEventCallback() {
		@Override
		public void onSensorChanged(SensorEvent event) {
			super.onSensorChanged(event);
			if(event.sensor.getType() == Sensor.TYPE_HEART_RATE) {
				int hb = (int)event.values[0];
				try { mIClientService.notifyHeartBeat(hb); }
				catch(RemoteException e) { e.printStackTrace(); }
			}
		}
	};

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		return null;
	}
}
