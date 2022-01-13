package com.tks.uwsclient;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Locale;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.bluetooth.le.AdvertiseCallback;
import android.bluetooth.le.AdvertiseData;
import android.bluetooth.le.AdvertiseSettings;
import android.bluetooth.le.BluetoothLeAdvertiser;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.IBinder;
import android.os.Looper;
import android.os.ParcelUuid;
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
import static com.tks.uwsclient.Constants.UWS_OWNDATA_KEY;

public class UwsClientService extends Service {
	private int mStatus = SERVICE_STATUS_INITIALIZING;
	private IOnStatusChangeListner mOnStatusChangeListner;

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
			uwsStart((short)seekerid, listner);
			return 0;
		}

		@Override
		public void stopUws() {
			mStatus = SERVICE_STATUS_IDLE;
			uwsStop();
		}
	};

	@Override
	public boolean onUnbind(Intent intent) {
		TLog.d("xxxxx");
		return super.onUnbind(intent);
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

	/* ***************/
	/* 開始/停止処理 */
	/* ***************/
	private void uwsStart(short seekerid, IOnStatusChangeListner listner) {
		mSeekerId = seekerid;
		mOnStatusChangeListner = listner;
		/* 位置情報 取得開始 */
		startLoc();

		/* BLEアドバタイズ 開始 */
		startAdvertise(seekerid);
	}

	private void uwsStop() {
		/* 位置情報 停止 */
		stoptLoc();

		/* BLE停止 */
		stopAdvertise();
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
	private double mLongitude;
	private double mLatitude;
	private final LocationCallback		mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			mLongitude = location.getLongitude();
			mLatitude  = location.getLatitude();
			TLog.d("1秒定期 (緯度:{0} 経度:{1})", String.format(Locale.JAPAN, "%1$.12f", mLatitude), String.format(Locale.JAPAN, "%1$.12f", mLongitude));

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

	/* アドバタイズ開始 */
	private void startAdvertise(short seekerid) {
		boolean ret = BluetoothAdapter.getDefaultAdapter().setName(MessageFormat.format("消防士{0}", seekerid));
		TLog.d("デバイス名変更 ret={0}", ret);
		AdvertiseSettings settings	= buildAdvertiseSettings();
		AdvertiseData data			= buildAdvertiseData(seekerid, (float)mLongitude, (float)mLatitude, mHeartbeat);
		mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);
	}

	/* アドバタイズ終了 */
	private void stopAdvertise() {
		TLog.d("アドバタイズ停止 {0}", mBluetoothLeAdvertiser);
		if (mBluetoothLeAdvertiser != null) {
			mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
		}
	}

	/* アドバタイズ設定生成 */
	private AdvertiseSettings buildAdvertiseSettings() {
		AdvertiseSettings.Builder settingsBuilder = new AdvertiseSettings.Builder();
		settingsBuilder.setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY);
		settingsBuilder.setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_HIGH);
		settingsBuilder.setTimeout(0);  /* タイムアウトは自前で管理する。 */
		return settingsBuilder.build();
	}

	/* アドバタイズのデータ生成 */
	private byte mSeqNo = 0;
	private AdvertiseData buildAdvertiseData(short seekerid, float longitude, float latitude, short heartbeat) {
		AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
		dataBuilder.addServiceUuid(ParcelUuid.fromString(Constants.createServiceUuid(seekerid)));
		dataBuilder.setIncludeDeviceName(true);

		/* 拡張データ生成 */
		byte[] sndBin = new byte[12];	/* 全部で12byteまでは送信可 */
		int spos = 0;
		/* SeqNo(1byte) */
		sndBin[0] = mSeqNo++;
		spos += 1;
		/* 経度(4byte) */
		byte[] bdifflong = f2bs(longitude);
		System.arraycopy(bdifflong, 0, sndBin, spos, bdifflong.length);
		spos += bdifflong.length;
		/* 緯度(4byte) */
		byte[] bdifflat = f2bs(latitude);
		System.arraycopy(bdifflat, 0, sndBin, spos, bdifflat.length);
		spos += bdifflat.length;
		/* 脈拍(2byte) */
		byte[] bheartbeat = s2bs(heartbeat);
		System.arraycopy(bheartbeat, 0, sndBin, spos, bheartbeat.length);
		/* 拡張データ設定 */
		dataBuilder.addManufacturerData(UWS_OWNDATA_KEY, sndBin);

		return dataBuilder.build();
	}
	private byte[] s2bs(short value) {
		return ByteBuffer.allocate(2).putShort(value).array();
	}
	private byte[] f2bs(float value) {
		return ByteBuffer.allocate(4).putFloat(value).array();
	}

	private final AdvertiseCallback mAdvertiseCallback = new AdvertiseCallback() {
		@Override
		public void onStartSuccess(AdvertiseSettings settingsInEffect) {
			TLog.d("アドバタイズ開始OK.");
		}

		@Override
		public void onStartFailure(int errorCode) {
			TLog.d("アドバタイズ開始失敗 error={0}", errorCode);
		}
	};

	/* *********/
	/* 脈拍機能 */
	/* *********/
	private short mHeartbeat;
}
