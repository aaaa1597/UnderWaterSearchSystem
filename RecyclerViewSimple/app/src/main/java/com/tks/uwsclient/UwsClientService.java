package com.tks.uwsclient;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.os.Binder;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.RemoteException;
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
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import static com.tks.uwsclient.Constants.ACTION.INITIALIZE;
import static com.tks.uwsclient.Constants.ACTION.FINALIZE;
import static com.tks.uwsclient.Constants.BT_CLASSIC_UUID;
import static com.tks.uwsclient.Constants.NOTIFICATION_CHANNEL_ID;
import static com.tks.uwsclient.Constants.ERR_ALREADY_STARTED;
import static com.tks.uwsclient.Constants.ERR_OK;
import static com.tks.uwsclient.Constants.d2Str;

public class UwsClientService extends Service {
	private int							mStatus = R.string.status_initializing;
	private short						mSeekerId = -1;
	private IOnUwsInfoChangeListner		mCallback;
	private IStatusNotifier				mListner2;
	private IStartCheckClearedCallback	mCb;
	private final Handler				mHandler = new Handler();
	private final BlockingQueue<byte[]>	mSndQue = new LinkedBlockingQueue<>();

	@Override
	public void onCreate() {
		super.onCreate();
		TLog.d("xxxxx");
		uwsInit();
//		startLoc();		このタイミングだとまだ権限許可が得られてない可能性がある。
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

	private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			if(!intent.getAction().equals(FINALIZE)) return;

			uwsStopBt();
			LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(mReceiver);
			stopForeground(true);
			stopSelf();
		}
	};

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		switch(intent.getAction()) {
			case INITIALIZE:
				TLog.d("startForeground.");
				startForeground(Constants.NOTIFICATION_ID_FOREGROUND_SERVICE_BLE, prepareNotification());
				break;
//			/* この処理は不要。FINALIZEは、mReceiver::onReceive()で処理する */
//			case FINALIZE:
//				TLog.d("stopForeground.");
//				LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent(FINALIZE));
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
		NotificationChannel channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, "BLE/位置情報", NotificationManager.IMPORTANCE_DEFAULT);
		channel.enableVibration(false);
		NotificationManager notificationManager = (NotificationManager)getSystemService(NOTIFICATION_SERVICE);
		notificationManager.createNotificationChannel(channel);

		/* 停止ボタン押下の処理実装 */
		Intent stopIntent = new Intent(this, UwsClientService.class);    /* まず自分に送信。その後アプリと脈拍サービスに送信する */
		stopIntent.setAction(FINALIZE);
		PendingIntent pendingStopIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);
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
		public void setListners(IOnUwsInfoChangeListner onUwsInfoChangeListner, IStatusNotifier onStatusChangeListner) {
			mCallback = onUwsInfoChangeListner;
			mListner2 = onStatusChangeListner;
		}

		@Override
		public void notifyStartCheckCleared() {
			/* 位置情報取得開始 */
			startLoc();

			/* 脈拍サービスと接続待ち */
			mHandler.postDelayed(new Runnable() {
				@Override
				public void run() {
					if(mCb == null) {
						mHandler.postDelayed(this, 1000);
						return;
					}

					TLog.d("脈拍サービスと接続待ち..");
					try {
						mCb.notifyStartCheckCleared();
					}
					catch(RemoteException ignore) {
					}
				}
			}, 1000);
		}

		@Override
		public int startBt(int seekerid, BluetoothDevice btServer) {
			TLog.d("bt接続開始 seekerid={0}", seekerid);
			mStatus = R.string.status_btconnecting;
			mSeekerId = (short)seekerid;

			setFstMsg();

			return uwsStartBt(mSeekerId, btServer);
		}

		@Override
		public void stopBt() {
			TLog.d("bt停止");
			uwsStopBt();
			mStatus = R.string.status_loc_and_beat;
			mSeekerId = -1;
		}

		/* UwsHeartBeatServiceから、脈拍通知で呼ばれる */
		@Override
		public void setNotifyStartCheckCleared(IStartCheckClearedCallback cb) {
			mCb = cb;
		}

		@Override
		public void notifyHeartBeat(int heartbeat) {
			TLog.d("脈拍通知 from HeartBeat-Service!! = {0}", heartbeat);
			mSndQue.removeIf(item->item[11]=='h');
			try { mSndQue.put(createByteArray(heartbeat)); }
			catch(InterruptedException ignore) {}
			try {
				mCallback.onHeartbeatResultChange((short)heartbeat);
			}
			catch(RemoteException e) {
				e.printStackTrace();
			}
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
		mStatus = R.string.status_loc_and_beat;
		/* 位置情報初期化 */
		mFlc = LocationServices.getFusedLocationProviderClient(this);
	}

	private void uwsFin() {
		TLog.d("xxxxx");
		mFlc = null;
	}

	/* *************/
	/* 位置情報 機能 */
	/* *************/
	private final static int			LOC_UPD_INTERVAL = 2000;
	private FusedLocationProviderClient mFlc;
	private final LocationRequest		mLocationRequest = LocationRequest.create().setInterval(LOC_UPD_INTERVAL).setFastestInterval(LOC_UPD_INTERVAL).setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
	private final LocationCallback		mLocationCallback = new LocationCallback() {
		@Override
		public void onLocationResult(@NonNull LocationResult locationResult) {
			super.onLocationResult(locationResult);
			Location location = locationResult.getLastLocation();
			TLog.d("1秒定期 (緯度:{0} 経度:{1})", d2Str(location.getLatitude()), d2Str(location.getLongitude()));
			mSndQue.removeIf(item->item[11]=='l');
			try { mSndQue.put(createByteArray(location.getLongitude(), location.getLatitude())); }
			catch(InterruptedException ignore) {}

			if(mCallback != null) {
				try {
					mCallback.onLocationResultChange(location);
				}
				catch(RemoteException e) {
					e.printStackTrace();
				}
			}

			/* 毎回OFF->ONにすることで、更新間隔が1秒になるようにしている。 */
			stoptLoc();
			try {
				Thread.sleep(LOC_UPD_INTERVAL);
			}
			catch(InterruptedException ignored) {
			}
			startLoc();
		}
	};

	/* 位置情報取得開始 */
	private void startLoc() {
		TLog.d("xxxxx");
		if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)
			throw new RuntimeException("ありえない権限エラー。すでにチェック済。");
		mFlc.requestLocationUpdates(mLocationRequest, mLocationCallback, Looper.myLooper());
	}

	/* 位置情報取得停止 */
	private void stoptLoc() {
		TLog.d("xxxxx");
		mFlc.removeLocationUpdates(mLocationCallback);
	}

	/* **************/
	/* Bluetooth機能 */
	/* **************/
	private BtClientThread mBtClientThread;

	/* 接続開始 */
	private int uwsStartBt(short seekerid, BluetoothDevice btServer) {
		TLog.d("seekerid={0} device={1}", seekerid, btServer);

		if(mBtClientThread != null) return ERR_ALREADY_STARTED;
		mBtClientThread = new BtClientThread(btServer);
		mBtClientThread.start();

		return ERR_OK;
	}

	private void uwsStopBt() {
		/* Bluetooth接続終了 */
		try { Thread.sleep(100);}
		catch(InterruptedException ignore) {}
		if(mBtClientThread == null) return;    /* 停止済なら、停止不要。 */
		mBtClientThread.sndClosing();
		mBtClientThread.interrupt();
		mBtClientThread = null;
	}

	/* 接続再トライ */
	private int uwsRestartBt(short seekerid, BluetoothDevice btServer) {
		TLog.d("seekerid={0} device={1}", seekerid, btServer);

		/* 初回メッセージ生成 */
		setFstMsg();

		if(mBtClientThread != null) {
			mBtClientThread.interrupt();
			try { Thread.sleep(100); }	/* 少し待つ */
			catch(InterruptedException ignore) {}
		}
		mBtClientThread = new BtClientThread(btServer);
		mBtClientThread.start();

		return ERR_OK;
	}

	/* Bluetooth通信実行スレッド */
	public class BtClientThread extends Thread {
		private final BluetoothDevice bluetoothDevice;
		private boolean mFlgClosing = false;

		public BtClientThread(BluetoothDevice device) {
			if(device == null) throw new RuntimeException("デバイスが選択されてない!!");
			bluetoothDevice = device;
		}

		@Override
		public void run() {
			if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if(ActivityCompat.checkSelfPermission(UwsClientService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
					throw new RuntimeException("ここではありえない。");
			}

			BluetoothSocket bluetoothSocket;
			try {
				bluetoothSocket = bluetoothDevice.createRfcommSocketToServiceRecord(BT_CLASSIC_UUID);
			}
			catch(IOException e) {
				throw new RuntimeException("bluetootが無効化してると発生。基本的に起きない。");
			}

			/* サーバとの接続待ち... */
			while(true) {
				try {
					TLog.d("接続中...");
					try { mListner2.onStatusChange(R.string.status_btconnecting); }
					catch(RemoteException ignore) {}
					bluetoothSocket.connect();
					break;
				}
				catch(IOException e) {
					TLog.d("ServerアプリがOpenしてない。リトライします");
					try {Thread.sleep(1000);} catch(InterruptedException ignore) {}
//					continue;
				}
			}

			/* 成功ログ出力 */
			String name = bluetoothDevice.getName();
			String addr = bluetoothDevice.getAddress();
			TLog.d("Connected. {0}:{1}", name, addr);
			try { mListner2.onStatusChange(R.string.status_btconnected_and_loc_beat); }
			catch(RemoteException ignore) {}

			/* Stream取得 */
			InputStream inputStream = null;
			OutputStream outputStrem= null;
			try {
				inputStream = bluetoothSocket.getInputStream();
				outputStrem = bluetoothSocket.getOutputStream();
			}
			catch(IOException e) {
				e.printStackTrace();
				TLog.d("接続失敗!! {0}:{1}", name, addr);
				try { mListner2.onStatusChange(R.string.status_btdisconnected); }
				catch(RemoteException ignore) {}
				try {
					if(inputStream!=null) inputStream.close();
					if(outputStrem!=null) outputStrem.close();
					bluetoothSocket.close();
				}
				catch(IOException ioException) { ioException.printStackTrace(); /* ここで発生してもどうしようもない */}
				return;	/* Stream取得に失敗したらThread終了。 */
			}

			byte[] incomingBuff = new byte[64];
			mStatus = R.string.status_btconnected_and_loc_beat;
			/* 接続確立 -> 送受信中 */
			while(true) {
				if(mFlgClosing) {
					mFlgClosing = false;
					byte[] closemsg = createCloseMsg();
					try { outputStrem.write(closemsg);}
					catch(IOException ignore) {}
					continue;
				}
				if(Thread.interrupted()){
					try {
						inputStream.close();
						outputStrem.close();
						bluetoothSocket.close();
					}
					catch(IOException ioException) { ioException.printStackTrace(); /* ここで発生してもどうしようもない */}
					return;	/* Stream取得に失敗したらThread終了。 */
				}

				/* 送信データがなければ待つ。 */
				if(mSndQue.size() == 0) {
					try { Thread.sleep(10); }
					catch(InterruptedException ignore) {}
					continue;
				}

				TLog.d("送信開始");

				/* 送信データtake */
				byte[] sndData = null;
				try { sndData = mSndQue.take();}
				catch(InterruptedException ignore) {}
				if(sndData== null) continue;

				TLog.d("	送信開始2 snd({0},{1})", sndData.length, Arrays.toString(sndData));

				/* 送信 */
				try { outputStrem.write(sndData);}
				catch(IOException e) {
					e.printStackTrace();
					TLog.d("切断検知!! -> 再リトライします。 {0}:{1}", name, addr);
					try { mListner2.onStatusChange(R.string.status_btdisconnected_and_retry); }
					catch(RemoteException ignore) {}
					/* リトライ */
					new Handler(Looper.getMainLooper()).postDelayed((Runnable) () -> uwsRestartBt( mSeekerId, bluetoothDevice), 1000);
					/* このスレッドは停止する */
					this.interrupt();
					continue;
				}

				TLog.d("	送信終了");
			}
		}

		public void sndClosing() {
			mFlgClosing = true;
		}
	}

	/* 初回メッセージ生成 */
	private void setFstMsg() {
		/* 送信キュー詰め替え 一旦全取り出し */
		List<byte[]> tmplist = new ArrayList<>();
		for(int lpct = 0; lpct < mSndQue.size(); lpct++) {
			try { tmplist.add(mSndQue.poll(0, TimeUnit.MILLISECONDS)); }
			catch(InterruptedException ignore) {}
		}
		/* クリア */
		mSndQue.clear();
		try { mSndQue.put(createFstMsg()); }
		catch(InterruptedException ignore) {}
		/* 全戻し */
		for(int lpct = 0; lpct < tmplist.size(); lpct++) {
			try { mSndQue.put(tmplist.get(lpct)); }
			catch(InterruptedException ignore) {}
		}
	}

	private byte[] createFstMsg() {
		byte[] sndBin = new byte[12];
		int spos = 0;
		/* (ヘッダを含まない)メッセージ長(1byte) 制限:最大256byteまで */
		byte[] blen = new byte[]{(byte)(sndBin.length-1)};
		System.arraycopy(blen, 0, sndBin, spos, blen.length);
		spos += blen.length;
		/* seekerid(2byte) */
		byte[] bseekerid = s2bs(mSeekerId);
		System.arraycopy(bseekerid, 0, sndBin, spos, bseekerid.length);
		spos += bseekerid.length;
		/* 日付(8byte) */
		byte[] bdate = l2bs(new Date().getTime());
		System.arraycopy(bdate, 0, sndBin, spos, bdate.length);
		spos += bdate.length;
		/* データ種別(1byte) */
		byte[] btype = new byte[]{'1'};
		System.arraycopy(btype, 0, sndBin, spos, btype.length);
		return sndBin;
	}
	private byte[] createByteArray(int heartbeat) {
		byte[] sndBin = new byte[16];
		int spos = 0;
		/* (ヘッダを含まない)メッセージ長(1byte) 制限:最大256byteまで */
		byte[] blen = new byte[]{(byte)(sndBin.length-1)};
		System.arraycopy(blen, 0, sndBin, spos, blen.length);
		spos += blen.length;
		/* seekerid(2byte) */
		byte[] bseekerid = s2bs(mSeekerId);
		System.arraycopy(bseekerid, 0, sndBin, spos, bseekerid.length);
		spos += bseekerid.length;
		/* 日付(8byte) */
		byte[] bdate = l2bs(new Date().getTime());
		System.arraycopy(bdate, 0, sndBin, spos, bdate.length);
		spos += bdate.length;
		/* データ種別(1byte) */
		byte[] btype = new byte[]{'h'};
		System.arraycopy(btype, 0, sndBin, spos, btype.length);
		spos += btype.length;
		/* 脈拍(4byte) */
		byte[] bheartbeat = i2bs(heartbeat);
		System.arraycopy(bheartbeat, 0, sndBin, spos, bheartbeat.length);
		return sndBin;
	}
	private byte[] createByteArray(double longitude, double latitude) {
		byte[] sndBin = new byte[28];
		int spos = 0;
		/* (ヘッダを含まない)メッセージ長(1byte) 制限:最大256byteまで */
		byte[] blen = new byte[]{(byte)(sndBin.length-1)};
		System.arraycopy(blen, 0, sndBin, spos, blen.length);
		spos += blen.length;
		/* seekerid(2byte) */
		byte[] bseekerid = s2bs(mSeekerId);
		System.arraycopy(bseekerid, 0, sndBin, spos, bseekerid.length);
		spos += bseekerid.length;
		/* 日付(8byte) */
		byte[] bdate = l2bs(new Date().getTime());
		System.arraycopy(bdate, 0, sndBin, spos, bdate.length);
		spos += bdate.length;
		/* データ種別(1byte) */
		byte[] btype = new byte[]{'l'};
		System.arraycopy(btype, 0, sndBin, spos, btype.length);
		spos += btype.length;
		/* 経度(8byte) */
		byte[] blongitude = d2bs(longitude);
		System.arraycopy(blongitude, 0, sndBin, spos, blongitude.length);
		spos += blongitude.length;
		/* 緯度(8byte) */
		byte[] blatitude = d2bs(latitude);
		System.arraycopy(blatitude, 0, sndBin, spos, blatitude.length);
		return sndBin;
	}
	private byte[] createCloseMsg() {
		byte[] sndBin = new byte[12];
		int spos = 0;
		/* (ヘッダを含まない)メッセージ長(1byte) 制限:最大256byteまで */
		byte[] blen = new byte[]{(byte)(sndBin.length-1)};
		System.arraycopy(blen, 0, sndBin, spos, blen.length);
		spos += blen.length;
		/* seekerid(2byte) */
		byte[] bseekerid = s2bs(mSeekerId);
		System.arraycopy(bseekerid, 0, sndBin, spos, bseekerid.length);
		spos += bseekerid.length;
		/* 日付(8byte) */
		byte[] bdate = l2bs(new Date().getTime());
		System.arraycopy(bdate, 0, sndBin, spos, bdate.length);
		spos += bdate.length;
		/* データ種別(1byte) */
		byte[] btype = new byte[]{'c'};
		System.arraycopy(btype, 0, sndBin, spos, btype.length);
		return sndBin;
	}
	private byte[] s2bs(short value) {
		return ByteBuffer.allocate(2).putShort(value).array();
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
}

