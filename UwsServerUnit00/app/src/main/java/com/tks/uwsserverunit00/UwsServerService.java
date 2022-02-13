package com.tks.uwsserverunit00;

import android.Manifest;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothServerSocket;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

import static com.tks.uwsserverunit00.Constants.BT_CLASSIC_UUID;
import static com.tks.uwsserverunit00.Constants.UWS_UUID_CHARACTERISTIC_HRATBEAT;
import static com.tks.uwsserverunit00.Constants.d2Str;

/**
 * -30 dBm	素晴らしい	達成可能な最大信号強度。クライアントは、これを実現するには、APから僅か数フィートである必要があります。現実的には一般的ではなく、望ましいものでもありません	N/A
 * -60 dBm	良好	非常に信頼性の高く、データパケットのタイムリーな伝送を必要とするアプリケーションのための最小信号強度	VoIP/VoWiFi, ストリーミングビデオ
 * -70 dBm	Ok	信頼できるパケット伝送に必要な最小信号強度	Email, web
 * -80 dBm	よくない	基本的なコネクティビティに必要な最小信号強度。パケット伝送は信頼できない可能性があります	N/A
 * -90 dBm	使用不可	ノイズレベルに近いかそれ以下の信号強度。殆ど機能しない	N/A
 **/

public class UwsServerService extends Service {
	private IHearbertChangeListner	mHearbertGb;
	private ILocationChangeListner	mLocationGb;
	private IStatusNotifier			mStatusCb;
	private BluetoothAdapter		mBluetoothAdapter;
	private BtStandbyThread			mBtStandbyThread;
	private List<BtSndRcvThread>	mBtSndRcvThreads = new ArrayList<>();

	@Override
	public void onCreate() {
		super.onCreate();
		/* Bluetoothアダプタ取得 */
		mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
		if(mBluetoothAdapter == null) {
			stopSelf();
			throw new RuntimeException("ありえない。BluetoothAdapter is null!!");
		}

		/* Bluetooth無効 */
		if(!mBluetoothAdapter.isEnabled()) {
			stopSelf();
			throw new RuntimeException("ありえない。Bluetooth無効!!");
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		mBtSndRcvThreads.forEach(i -> {
			i.interrupt();
		});
		mBtSndRcvThreads.clear();
		mBtSndRcvThreads = null;
		mBtStandbyThread.interrupt();
		mBtStandbyThread = null;
		mBluetoothAdapter = null;
	}

	@Nullable
	@Override
	public IBinder onBind(Intent intent) {
		TLog.d("");
		return mBinder;
	}

	/** *****
	 * Binder
	 * ******/
	private Binder mBinder = new IUwsServer.Stub() {
		@Override
		public void setListners(IHearbertChangeListner hGb, ILocationChangeListner lGb, IStatusNotifier sCb) {
			mHearbertGb	= hGb;
			mLocationGb	= lGb;
			mStatusCb	= sCb;
		}

		/* 起動条件チェッククリア */
		@Override
		public void notifyStartCheckCleared() {
			mBtStandbyThread = new BtStandbyThread();
			mBtStandbyThread.start();
		}
	};

	/* *********
	 * Bluetooth
	 * *********/
	public static final String BT_NAME = "BtServer";

	/* Acceput待ちスレッド */
	public class BtStandbyThread extends Thread {
		BluetoothServerSocket bluetoothServerSocket = null;

		@Override
		public void run() {
			/* 権限チェック、ホントはここでは不要だけど、Androidがうるさいから */
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
				if (ActivityCompat.checkSelfPermission(UwsServerService.this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED)
					throw new RuntimeException("すでに権限付与済のはず");
			}

			/* RFCOMM チャンネルの確立 */
			try {
				/* この関数はすぐ戻る */
				bluetoothServerSocket = mBluetoothAdapter.listenUsingInsecureRfcommWithServiceRecord(BT_NAME, BT_CLASSIC_UUID);
			}
			catch(IOException e) { TLog.d("ここでの失敗は想定外!!"); e.printStackTrace();}

			while(true) {
				/* スレッド停止チェック */
				if(Thread.interrupted()) {
					try { bluetoothServerSocket.close(); } catch(IOException ignore) { }
					break;
				}

				/* Client接続待ち */
				String name = null, addr = null;
				try {
					TLog.d("Client接続待ち...");
					BluetoothSocket btSocket = bluetoothServerSocket.accept();
					name = btSocket.getRemoteDevice().getName();
					addr = btSocket.getRemoteDevice().getAddress();
					TLog.d("Client接続確立 {0}:{1}", name, addr);
					try {
						BtSndRcvThread thred = new BtSndRcvThread(name, addr, btSocket);
						mBtSndRcvThreads.add(thred);
						thred.start();
					}
					catch(IOException e) {
						e.printStackTrace();
						try { mStatusCb.OnChangeStatus(name, addr, R.string.err_btconnect_failured); }
						catch(RemoteException ignore) {/* ここで例外が発生するとどうしようもない */}
					}
				}
				catch(IOException e) {
					e.printStackTrace();
				}
			}
		}
	}

	/* 送受信スレッド */
	public class BtSndRcvThread extends Thread {
		private String	name;
		private String	addr;
		private BluetoothSocket	bluetoothSocket;
		private InputStream		inputStream;
		private OutputStream	outputStream;
		public BtSndRcvThread(String aname, String aaddr, BluetoothSocket btSocket) throws IOException {
			name = aname;
			addr = aaddr;
			bluetoothSocket	= btSocket;
			inputStream		= bluetoothSocket.getInputStream();
			outputStream	= bluetoothSocket.getOutputStream();
		}

		@Override
		public void run() {
			byte[] incomingBuff = new byte[64];
			while(true) {
				/* スレッド停止チェック */
				if (Thread.interrupted()) {
					try { inputStream    .close(); }catch(IOException ignore) { }
					try { outputStream   .close(); }catch(IOException ignore) { }
					try { bluetoothSocket.close(); }catch(IOException ignore) { }
					break;
				}

				int incomingSize = 0;
				try {
					incomingSize = inputStream.read(incomingBuff);}
				catch(IOException e) {
					e.printStackTrace();
					try { mStatusCb.OnChangeStatus(name, addr, R.string.err_btdisconnected); }
					catch(RemoteException ignore) {/* ここで例外が発生するとどうしようもない */}
					super.interrupt();
					continue;
				}

				byte[] buff = new byte[incomingSize];
				System.arraycopy(incomingBuff, 0, buff, 0, incomingSize);
				parseRcvAndCallback(name, addr, buff);

				try {
					outputStream.write((new Date().toString() + " OK").getBytes(StandardCharsets.UTF_8));}
				catch(IOException e) {
					e.printStackTrace();
					try { mStatusCb.OnChangeStatus(name, addr, R.string.err_btdisconnected); }
					catch(RemoteException ignore) {/* ここで例外が発生するとどうしようもない */}
					super.interrupt();
					continue;
				}
			}
		}
	}

	/** **********
	 * データParse
	 ** **********/
	private void parseRcvAndCallback(String name, String addr, byte[] buff) {
		/* 日付 */
		long 	ldatetime	= ByteBuffer.wrap(buff).getLong();
		/* データ種別 */
		char	datatype	= ByteBuffer.wrap(buff).getChar(8);	/* 'h':脈拍, 'l':位置情報  */
		if(datatype == 'h') {
			/* 脈拍 */
			int	heartbeat	= ByteBuffer.wrap(buff).getInt(9);
			TLog.d("脈拍受信 {0} {1} {2}:{3}", d2Str(new Date(ldatetime)), heartbeat, name, addr);
			/* 脈拍コールバック */
			try { mHearbertGb.OnChange(name, addr, ldatetime, heartbeat);}
			catch(RemoteException e) { e.printStackTrace(); }
		}
		else if(datatype == 'l') {
			/* 位置情報 */
			double longitude= ByteBuffer.wrap(buff).getDouble(9);
			double latitude	= ByteBuffer.wrap(buff).getDouble(17);
			TLog.d("位置情報受信 {0} ({1},{2}) {3}:{4}", d2Str(new Date(ldatetime)), longitude,latitude , name, addr);
			Location retloc =new Location(LocationManager.GPS_PROVIDER);
			retloc.setLongitude(longitude);
			retloc.setLatitude(latitude);
			/* 位置情報コールバック */
			try { mLocationGb.OnChange(name, addr, ldatetime, retloc); }
			catch(RemoteException e) { e.printStackTrace(); }
		}
	}
}
