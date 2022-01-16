package com.tks.uwsclient;

import java.nio.ByteBuffer;
import java.text.MessageFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothGattService;
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
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
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
import static com.tks.uwsclient.Constants.UWS_UUID_CHARACTERISTIC_HRATBEAT;
import static com.tks.uwsclient.Constants.d2Str;

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
		stoptLoc();
		stopAdvertise();
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
			TLog.d("seekerid={0}", seekerid);
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
		TLog.d("");
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

		mGattServer = bluetoothManager.openGattServer(this, mGattServerCallback);
		if(mGattServer == null)
			throw new RuntimeException("Bluetooth未サポート.使用不可4!!");

		TLog.d( "アドバタイズの最大サイズ={0}", bluetoothAdapter.getLeMaximumAdvertisingDataLength());
	}

	private void uwsFin() {
		TLog.d("");
		mFlc = null;
		mGattServer.close();
		mGattServer = null;
		mBluetoothLeAdvertiser = null;
	}

	/* *************/
	/* 開始/停止処理 */
	/* *************/
	private void uwsStart(short seekerid, IOnStatusChangeListner listner) {
		TLog.d("seekerid={0}", seekerid);
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
	private final static int			LOC_UPD_INTERVAL = 2000;
	private FusedLocationProviderClient mFlc;
	private final LocationRequest		mLocationRequest = LocationRequest.create()
															.setInterval(LOC_UPD_INTERVAL)
															.setFastestInterval(LOC_UPD_INTERVAL)
															.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	private double						mLongitude;
	private double						mLatitude;
	private final LocationCallback		mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			mLongitude = location.getLongitude();
			mLatitude  = location.getLatitude();
			TLog.d("1秒定期 (緯度:{0} 経度:{1})", d2Str(mLatitude), d2Str(mLongitude));

			/* 毎回OFF->ONにすることで、更新間隔が1秒になるようにしている。 */
			stoptLoc();
			try { Thread.sleep(LOC_UPD_INTERVAL); } catch (InterruptedException ignored) { }
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
	private short						mSeekerId = -1;
	private BluetoothLeAdvertiser		mBluetoothLeAdvertiser;
	private BluetoothGattCharacteristic mUwsCharacteristic;
	private BluetoothGattServer			mGattServer;
	private BluetoothDevice				mServerDevice;

	/* アドバタイズ開始 */
	Handler mHandler = new Handler();
	Runnable mAdvertiseRunner = null;
	private void startAdvertise(short seekerid) {
		if(mSeekerId == seekerid && mAdvertiseRunner != null) {
			TLog.d("すでにアドバタイズ中...続行します。");
			return;
		}
		else if(mSeekerId != seekerid) {
			mSeekerId = seekerid;
			boolean ret = BluetoothAdapter.getDefaultAdapter().setName(MessageFormat.format("消防士{0}", seekerid));
			TLog.d("デバイス名変更 {0} ret={1}", MessageFormat.format("消防士{0}", seekerid), ret);
		}

		/* Advertiseのタイミングで、自分自身のペリフェラル特性定義も実施しておく(gatt接続に備えておく) */
		mUwsCharacteristic = createOwnCharacteristic(seekerid, mGattServer);

		mAdvertiseRunner = new Runnable() {
			@Override
			public void run() {
				/* 一旦、アドバタイズ停止 */
				mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
				try { Thread.sleep(100); }
				catch (InterruptedException e) { e.printStackTrace(); }

				/* 新データでアドバタイズ開始 */
				AdvertiseSettings settings	= buildAdvertiseSettings();
				AdvertiseData data			= buildAdvertiseData((float)mLongitude, (float)mLatitude, mHeartbeat);
				mBluetoothLeAdvertiser.startAdvertising(settings, data, mAdvertiseCallback);

				/* 3秒後再開 */
				mHandler.postDelayed(this, 5000);
			}
		};
		mHandler.post(mAdvertiseRunner);
	}

	/* アドバタイズ終了 */
	private void stopAdvertise() {
		mHandler.removeCallbacks(mAdvertiseRunner);
		mAdvertiseRunner = null;
		mBluetoothLeAdvertiser.stopAdvertising(mAdvertiseCallback);
	}

	/** **********************
	 * 自身のペリフェラル特性を定義
	 * ***********************/
	private BluetoothGattCharacteristic createOwnCharacteristic(int seekerid, BluetoothGattServer gattManager) {
		BluetoothGattService old = gattManager.getService(UUID.fromString(Constants.createServiceUuid(seekerid)));
		if(old != null) {
			/* TODO */TLog.d("古いUUID-Serviceを削除={0}", old);
			gattManager.removeService(old);
		}

		/* 自身が提供するサービスを定義 */
		BluetoothGattService ownService = new BluetoothGattService(UUID.fromString(Constants.createServiceUuid(seekerid)), BluetoothGattService.SERVICE_TYPE_PRIMARY);

		/* 自身が提供するCharacteristic(特性)を定義 : 通知と読込みに対し、読込み許可 */
		BluetoothGattCharacteristic charac = new BluetoothGattCharacteristic(/*UUID*/UWS_UUID_CHARACTERISTIC_HRATBEAT,
				BluetoothGattCharacteristic.PROPERTY_NOTIFY | BluetoothGattCharacteristic.PROPERTY_READ,
				/*permissions*/BluetoothGattCharacteristic.PERMISSION_READ);

		charac.setValue(new byte[]{'0','1','2','3','4','5','6','7','8','9','a','b','c','d','e','f',16,17,18,19,20,21,22,23,24,25,26,27});

		/* 定義したサービスにCharacteristic(特性)を付与 */
		ownService.addCharacteristic(charac);

		/* Gattサーバに定義したサービスを付与 */
		gattManager.addService(ownService);

		return charac;
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
	private AdvertiseData buildAdvertiseData(float longitude, float latitude, short heartbeat) {
		AdvertiseData.Builder dataBuilder = new AdvertiseData.Builder();
//		↓↓↓ TODO 削除予定
//		dataBuilder.addServiceUuid(ParcelUuid.fromString(Constants.createServiceUuid(seekerid)));
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

	/** *****************
	 * GattサーバCallBack
	 * ******************/
	private final BluetoothGattServerCallback mGattServerCallback = new BluetoothGattServerCallback() {
		/** ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++
		 * 接続状態変化通知
		 * @param serverdevice	サーバ側デバイス
		 * @param status	int: Status of the connect or disconnect operation. BluetoothGatt.GATT_SUCCESS if the operation succeeds.
		 * @param newState	BluetoothProfile.STATE_DISCONNECTED, BluetoothProfile#STATE_CONNECTED
		 * ++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++++*/
		@Override
		public void onConnectionStateChange(BluetoothDevice serverdevice, final int status, int newState) {
			super.onConnectionStateChange(serverdevice, status, newState);
			if (status == BluetoothGatt.GATT_SUCCESS) {
				if (newState == BluetoothGatt.STATE_CONNECTED) {
					stopAdvertise();
					mServerDevice = serverdevice;
					TLog.d("接続 -> serverdevice: {0}", serverdevice.getAddress());
				}
				else if (newState == BluetoothGatt.STATE_DISCONNECTED) {
					startAdvertise(mSeekerId);
					mServerDevice = null;
					TLog.d("切断 <- serverdevice");
				}
			}
			else {
				startAdvertise(mSeekerId);
				mServerDevice = null;
				TLog.d("エラー切断 <- serverdevice");
			}
		}

//		/* 通知/指示送信の結果 */
//		@Override
//		public void onNotificationSent(BluetoothDevice server, int status) {
//			super.onNotificationSent(server, status);
//			/* 積まれた通知データを送信 */
//			Runnable runner = mNotifySender.poll();
//			if(runner != null) runner.run();
//			TLog.d("Notification sent. Status:{0} 残:{1}",status, mNotifySender.size());
//		}

		/* Read要求受信 */
		@Override
		public void onCharacteristicReadRequest(BluetoothDevice server, int requestId, int offset, BluetoothGattCharacteristic characteristic) {
			super.onCharacteristicReadRequest(server, requestId, offset, characteristic);
			/* 初回送信時のみ値設定 */
			if(offset == 0) {
				double longitude = mLongitude;
				double latitude = mLatitude;
				int heartbeat = mHeartbeat;
				setValuetoCharacteristic(characteristic, new Date(),  longitude, latitude,  heartbeat);
			}
			/* 分割送信処理対応 */
			byte[] resData = new byte[characteristic.getValue().length-offset];
			System.arraycopy(characteristic.getValue(), offset, resData, 0, resData.length);

			if(offset == 0)
				TLog.d("Server->Read要求({0}) snd({1}:{2}:{3}) 返却値:(UUID:{4},resData(byte数{5}:データ{6}) org(offset{7},val:{8}))", requestId, d2Str(mLongitude), d2Str(mLatitude), mHeartbeat, characteristic.getUuid(), resData.length, Arrays.toString(resData), offset, Arrays.toString(characteristic.getValue()));
			else
				TLog.d("Server->Read要求({0}) 返却値:(UUID:{1},resData(byte数{2}:データ{3}) org(offset{4},val:{5}))", requestId, characteristic.getUuid(), resData.length, Arrays.toString(resData), offset, Arrays.toString(characteristic.getValue()));

			mGattServer.sendResponse(server, requestId, BluetoothGatt.GATT_SUCCESS, 0, resData);
		}

		/* Write要求受信 */
		@Override
		public void onCharacteristicWriteRequest(BluetoothDevice device, int requestId, BluetoothGattCharacteristic characteristic, boolean preparedWrite, boolean responseNeeded, int offset, byte[] value) {
			super.onCharacteristicWriteRequest(device, requestId, characteristic, preparedWrite, responseNeeded, offset, value);

			TLog.d("CentralからのWrite要求 受信値:(UUID:{0},vat:{1}))", mUwsCharacteristic.getUuid(), Arrays.toString(value));
			setValuetoCharacteristic(mUwsCharacteristic, new Date(), 555, 666,  123);
			if (responseNeeded)
				mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, 0, value);
		}

		/* Read要求受信 */
		@Override
		public void onDescriptorReadRequest(BluetoothDevice device, int requestId, int offset, BluetoothGattDescriptor descriptor) {
			super.onDescriptorReadRequest(device, requestId, offset, descriptor);

			TLog.d("CentralからのDescriptor_Read要求 返却値:(UUID:{0},vat:{1}))", descriptor.getUuid(), Arrays.toString(descriptor.getValue()));
			mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS, offset, descriptor.getValue());
		}

		/* Write要求受信 */
		@Override
		public void onDescriptorWriteRequest(BluetoothDevice device, int requestId,
											 BluetoothGattDescriptor descriptor, boolean preparedWrite, boolean responseNeeded,
											 int offset,
											 byte[] value) {
			super.onDescriptorWriteRequest(device, requestId, descriptor, preparedWrite, responseNeeded, offset, value);

			TLog.d("CentralからのDescriptor_Write要求 受信値:(UUID:{0},vat:{1}))", descriptor.getUuid(), Arrays.toString(value));

//            int status = BluetoothGatt.GATT_SUCCESS;
//            if (descriptor.getUuid() == CLIENT_CHARACTERISTIC_CONFIGURATION_UUID) {
//                BluetoothGattCharacteristic characteristic = descriptor.getCharacteristic();
//                boolean supportsNotifications = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_NOTIFY) != 0;
//                boolean supportsIndications   = (characteristic.getProperties() & BluetoothGattCharacteristic.PROPERTY_INDICATE) != 0;
//
//                if (!(supportsNotifications || supportsIndications)) {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                }
//                else if (value.length != 2) {
//                    status = BluetoothGatt.GATT_INVALID_ATTRIBUTE_LENGTH;
//                }
//                else if (Arrays.equals(value, BluetoothGattDescriptor.DISABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsDisabled(characteristic);
//                    descriptor.setValue(value);
//                }
//                else if (supportsNotifications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, false /* indicate */);
//                    descriptor.setValue(value);
//                }
//                else if (supportsIndications &&
//                        Arrays.equals(value, BluetoothGattDescriptor.ENABLE_INDICATION_VALUE)) {
//                    status = BluetoothGatt.GATT_SUCCESS;
//                    mCurrentServiceFragment.notificationsEnabled(characteristic, true /* indicate */);
//                    descriptor.setValue(value);
//                }
//                else {
//                    status = BluetoothGatt.GATT_REQUEST_NOT_SUPPORTED;
//                }
//            }
//            else {
//                status = BluetoothGatt.GATT_SUCCESS;
//                descriptor.setValue(value);
//            }
			if (responseNeeded)
				mGattServer.sendResponse(device, requestId, BluetoothGatt.GATT_SUCCESS,0,null);

		}
	};

	private void set1stValuetoCharacteristic(BluetoothGattCharacteristic charac, short msgid, Date datetime, double longitude/*経度*/) {
		TLog.d("1st送信データ生成");
		byte[] ret = new byte[20];
		int spos = 0;
		/* メッセージID(2byte) */
		byte[] bmsgid = s2bs(msgid);
		System.arraycopy(bmsgid, 0, ret, spos, bmsgid.length);
		spos += bmsgid.length;
		/* Seq番号(2byte) */
		byte[] bseqno = s2bs((short)0);
		System.arraycopy(bseqno, 0, ret, spos, bseqno.length);
		spos += bseqno.length;
		/* 日付(8byte) */
		byte[] bdatetime = l2bs(datetime.getTime());
		System.arraycopy(bdatetime, 0, ret, spos, bdatetime.length);
		spos += bdatetime.length;
		/* 経度(8byte) */
		byte[] blongitude = d2bs(longitude);
		System.arraycopy(blongitude, 0, ret, spos, blongitude.length);
		/* 値 設定 */
		charac.setValue(ret);
	}
	private void set2ndValuetoCharacteristic(BluetoothGattCharacteristic charac, short msgid, double latitude/*緯度*/, int heartbeat) {
		TLog.d("2nd送信データ生成");
		byte[] ret = new byte[16];
		int spos = 0;
		/* メッセージID(2byte) */
		byte[] bmsgid = s2bs(msgid);
		System.arraycopy(bmsgid, 0, ret, spos, bmsgid.length);
		spos += bmsgid.length;
		/* Seq番号(2byte) */
		byte[] bseqno = s2bs((short)1);
		System.arraycopy(bseqno, 0, ret, spos, bseqno.length);
		spos += bseqno.length;
		/* 緯度(8byte) */
		byte[] blatitude = d2bs(latitude);
		System.arraycopy(blatitude, 0, ret, spos, blatitude.length);
		spos += blatitude.length;
		/* 脈拍(4byte) */
		byte[] bheartbeat = i2bs(heartbeat);
		System.arraycopy(bheartbeat, 0, ret, spos, bheartbeat.length);
		/* 値 設定 */
		charac.setValue(ret);
	}
	private void setValuetoCharacteristic(BluetoothGattCharacteristic charac, Date datetime, double longitude/*経度*/, double latitude/*緯度*/, int heartbeat) {
		byte[] ret = new byte[28];
		int spos = 0;
		/* 日付(8byte) */
		byte[] bdatetime = l2bs(datetime.getTime());
		System.arraycopy(bdatetime, 0, ret, spos, bdatetime.length);
		spos += bdatetime.length;
		/* 経度(8byte) */
		byte[] blongitude = d2bs(longitude);
		System.arraycopy(blongitude, 0, ret, spos, blongitude.length);
		spos += blongitude.length;
		/* 緯度(8byte) */
		byte[] blatitude = d2bs(latitude);
		System.arraycopy(blatitude, 0, ret, spos, blatitude.length);
		spos += blatitude.length;
		/* 脈拍(4byte) */
		byte[] bheartbeat = i2bs(heartbeat);
		System.arraycopy(bheartbeat, 0, ret, spos, bheartbeat.length);
		/* 値 設定 */
		charac.setValue(ret);
	}
	private byte[] i2bs(int value) {
		return ByteBuffer.allocate(4).putInt(value).array();
	}
	private byte[] l2bs(long value) {
		return ByteBuffer.allocate(8).putLong(value).array();
	}
	private byte[] d2bs(double value) {
		return ByteBuffer.allocate(8).putDouble(value).array();
	}

	/* *********/
	/* 脈拍機能 */
	/* *********/
	private short mHeartbeat;
}
