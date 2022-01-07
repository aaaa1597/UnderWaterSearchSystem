package com.tks.uwsclient.ui;

import java.text.MessageFormat;
import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.RemoteException;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tks.uwsclient.BleClientService;
import com.tks.uwsclient.IBleClientService;
import com.tks.uwsclient.IBleClientServiceCallback;
import com.tks.uwsclient.TLog;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS;
import static com.tks.uwsclient.Constants.UWS_LOC_BASE_LATITUDE;
import static com.tks.uwsclient.Constants.UWS_LOC_BASE_LONGITUDE;
import static com.tks.uwsclient.Constants.UWS_NG_ALREADY_ADVERTISED;
import static com.tks.uwsclient.Constants.UWS_NG_ALREADY_SCANNED;
import static com.tks.uwsclient.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsclient.Constants.UWS_NG_AIDL_REMOTE_ERROR;

public class FragMainViewModel extends ViewModel {
	private final MutableLiveData<Double>			mLatitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Double>			mLongitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Short>			mHearBeat		= new MutableLiveData<>((short)0);
	private final MutableLiveData<Boolean>			mUnLock			= new MutableLiveData<>(true);
	private final MutableLiveData<ConnectStatus>	mStatus			= new MutableLiveData<>(ConnectStatus.NONE);
	public MutableLiveData<Double>			Latitude()		{ return mLatitude; }
	public MutableLiveData<Double>			Longitude()		{ return mLongitude; }
	public MutableLiveData<Short>			HearBeat()		{ return mHearBeat; }
	public MutableLiveData<Boolean>			UnLock()		{ return mUnLock; }
	public MutableLiveData<ConnectStatus>	ConnectStatus()	{ return mStatus; }

	public enum ConnectStatus {
		NONE,
		SETTING_ID,		/* ID設定中 */
		START_ADVERTISE,/* アドバタイズ開始 */
		ADVERTISING,	/* アドバタイズ中... */
		ERROR,			/* エラー発生!! */
	}

	private final MutableLiveData<Boolean>	mAdvertisingFlg	= new MutableLiveData<>(false);
	public MutableLiveData<Boolean>			AdvertisingFlg()	{ return mAdvertisingFlg; }
	private int		mSeekerID		= 0;
	public void		setSeekerID(int id)	{ mSeekerID = id; }
	public int		getSeekerID()		{ return mSeekerID; }

	private final MutableLiveData<String>	mShowSnacbar			= new MutableLiveData<>();
	public LiveData<String>					ShowSnacbar()			{ return mShowSnacbar; }
	public void								showSnacbar(String showmMsg) { mShowSnacbar.postValue(showmMsg);}
	private final MutableLiveData<String>	mShowErrMsg				= new MutableLiveData<>();
	public LiveData<String>					ShowErrMsg()			{ return mShowErrMsg; }
	public void								showErrMsg(String showmMsg) { mShowErrMsg.postValue(showmMsg);}

	/** ********
	 *  Location
	 *  ********/
	public boolean mIsSettedLocationON = false;

	/** **********
	 * Service接続
	 ** *********/
	private IBleClientService	mBleServiceIf;
	public int onServiceConnected(IBleClientService service) {
		mBleServiceIf = service;

		/* コールバック設定 */
		try { mBleServiceIf.setCallback(mCb); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}

		/* BLE初期化 */
		int ret = 0;
		try { ret = mBleServiceIf.initBle(); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}

		return ret;
	}

	/** **********
	 * Service切断
	 ** *********/
	public void onServiceDisconnected() {
		mBleServiceIf = null;
	}

	/** ************
	 * アドバタイズ開始
	 ** ***********/
	Handler mHandler = new Handler();
	Runnable mAdvertiseRunner = null;
	public int startAdvertising() {
		if(mAdvertiseRunner != null) {
			String log = "すでにアドバタイズ中...続行します。";
			TLog.d(log);
			showSnacbar(log);
			return UWS_NG_ALREADY_ADVERTISED;
		}
		mAdvertiseRunner = new Runnable() {
			@Override
			public void run() {
				/* 一旦、アドバタイズ停止 */
				try { mBleServiceIf.stopAdvertising(); }
				catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException("起きないはず.");}

				/* 新データでアドバタイズ開始 */
				float difflong = (float)(Longitude().getValue() - UWS_LOC_BASE_LONGITUDE);
				float difflat  = (float)(Latitude().getValue() - UWS_LOC_BASE_LATITUDE);
				short heartbeat= HearBeat().getValue();
				try { mBleServiceIf.startAdvertising(mSeekerID, difflong, difflat, heartbeat); }
				catch (RemoteException e) { e.printStackTrace(); throw new RuntimeException("起きないはず.");}

				/* 3秒後再開 */
				mHandler.postDelayed(this, 3000);
			}
		};

		mHandler.post(mAdvertiseRunner);

		return UWS_NG_SUCCESS;
	}

	/** ************
	 * アドバタイズ終了
	 ** ***********/
	public int stopAdvertising() {
		mHandler.removeCallbacks(mAdvertiseRunner);
		mAdvertiseRunner = null;
		return UWS_NG_SUCCESS;
	}

	/** *******************
	 * BleClientServer起動
	 * *******************/
	public void bindBleService(Context context, ServiceConnection con) {
		/* Bluetooth未サポート */
		if (!context.getPackageManager().hasSystemFeature(PackageManager.FEATURE_BLUETOOTH_LE)) {
			TLog.d("Bluetooth未サポートの端末.何もしない.");
			return;
		}

		/* 権限が許可されていない */
		if(context.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			TLog.d("Bluetooth権限なし.何もしない.");
			return;
		}

		/* Bluetooth未サポート */
		final BluetoothManager bluetoothManager = (BluetoothManager)context.getSystemService(Context.BLUETOOTH_SERVICE);
		if(bluetoothManager == null) {
			TLog.d("Bluetooth未サポートの端末.何もしない.");
			return;
		}

		/* Bluetooth未サポート */
		BluetoothAdapter bluetoothAdapter = bluetoothManager.getAdapter();
		if (bluetoothAdapter == null) {
			TLog.d("Bluetooth未サポートの端末.何もしない.");
			return;
		}

		/* Bluetooth ON/OFF判定 */
		if( !bluetoothAdapter.isEnabled()) {
			TLog.d("Bluetooth OFF.何もしない.");
			return;
		}

		/* 設定の位置情報ON/OFF判定 */
		if( !mIsSettedLocationON) {
			TLog.d("設定の位置情報がOFFのまま.何もしない.");
			return;
		}

		/* Bluetoothサービス起動 */
		Intent intent = new Intent(context.getApplicationContext(), BleClientService.class);
		context.bindService(intent, con, Context.BIND_AUTO_CREATE);
		TLog.d("Bluetooth使用クリア -> Bluetoothサービス起動");
	}

	/** *****************
	 * AIDLコールバック
	 * *****************/
	IBleClientServiceCallback mCb = new IBleClientServiceCallback.Stub() {
		@Override
		public void notifyAdvertising(int ret) {
			TLog.d("アドバタイズ開始結果 ret={0}", ret);
			if(ret == UWS_NG_SUCCESS) {
				ConnectStatus().postValue(ConnectStatus.ADVERTISING);
				return;
			}

			if(ret == ADVERTISE_FAILED_ALREADY_STARTED) {
				ConnectStatus().postValue(ConnectStatus.ADVERTISING);
				String errstr = "すでにアドバタイズ開始済。続行します。";
				TLog.d(errstr);
				showSnacbar(errstr);
			}
			else if(ret == ADVERTISE_FAILED_DATA_TOO_LARGE) {
				ConnectStatus().postValue(ConnectStatus.ADVERTISING);
				String errstr = "アドバタイズのデータサイズがデカすぎ!!\n送れない!!";
				TLog.d(errstr);
				showSnacbar(errstr);
			}
			else if(ret == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
				ConnectStatus().postValue(ConnectStatus.ERROR);
				String errstr = "Bluetooth未サポート!!\n動作しない端末です。終了します。";
				TLog.d(errstr);
				showErrMsg(errstr);
			}
			else if(ret == ADVERTISE_FAILED_INTERNAL_ERROR) {
				ConnectStatus().postValue(ConnectStatus.ERROR);
				String errstr = "Android内部エラー!!\nどうしようもないので、終了します。";
				TLog.d(errstr);
				showErrMsg(errstr);
			}
			else if(ret == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
				ConnectStatus().postValue(ConnectStatus.ERROR);
				String errstr = "他のアプリでもアドバタイズを実行しているため、実行できません。再起動で直ることがあります。\n終了します。";
				TLog.d(errstr);
				showErrMsg(errstr);
			}
			return;
		}

		@Override
		public double getLongitude() {
			double loongitude = Longitude().getValue();
			TLog.d("loongitude={0}", loongitude);
			return loongitude;
		}

		@Override
		public double getLatitude() {
			double latitude = Latitude().getValue();
			TLog.d("latitude={0}", latitude);
			return latitude;
		}

		@Override
		public int getHeartbeat() {
			int hearBeat = HearBeat().getValue();
			TLog.d("hearBeat={0}", hearBeat);
			return hearBeat;
		}
	};
}
