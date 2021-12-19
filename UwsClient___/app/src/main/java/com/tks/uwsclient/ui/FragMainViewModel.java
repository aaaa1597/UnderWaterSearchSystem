package com.tks.uwsclient.ui;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattServer;
import android.bluetooth.BluetoothGattServerCallback;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.os.RemoteException;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tks.uwsclient.BleClientService;
import com.tks.uwsclient.Constants;
import com.tks.uwsclient.ErrPopUp;
import com.tks.uwsclient.IBleClientService;
import com.tks.uwsclient.IBleClientServiceCallback;
import com.tks.uwsclient.MainActivity;
import com.tks.uwsclient.TLog;

import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_ALREADY_STARTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_DATA_TOO_LARGE;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_FEATURE_UNSUPPORTED;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_INTERNAL_ERROR;
import static android.bluetooth.le.AdvertiseCallback.ADVERTISE_FAILED_TOO_MANY_ADVERTISERS;
import static com.tks.uwsclient.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsclient.Constants.UWS_NG_AIDL_CALLBACK_FAILED;
import static com.tks.uwsclient.Constants.UWS_NG_AIDL_INIT_BLE_FAILED;
import static com.tks.uwsclient.Constants.UWS_NG_AIDL_START_ADVERTISING;
import static com.tks.uwsclient.Constants.UWS_NG_AIDL_STOP_ADVERTISING;

public class FragMainViewModel extends ViewModel {
	private final MutableLiveData<String>			mDeviceAddress	= new MutableLiveData<>("");
	private final MutableLiveData<Double>			mLatitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Double>			mLongitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Integer>			mHearBeat		= new MutableLiveData<>(0);
	private final MutableLiveData<Boolean>			mUnLock			= new MutableLiveData<>(true);
	private final MutableLiveData<ConnectStatus>	mStatus			= new MutableLiveData<>(ConnectStatus.NONE);
	public MutableLiveData<String>			DeviceAddress()	{ return mDeviceAddress; }
	public MutableLiveData<Double>			Latitude()		{ return mLatitude; }
	public MutableLiveData<Double>			Longitude()		{ return mLongitude; }
	public MutableLiveData<Integer>			HearBeat()		{ return mHearBeat; }
	public MutableLiveData<Boolean>			UnLock()		{ return mUnLock; }
	public MutableLiveData<ConnectStatus>	ConnectStatus()	{ return mStatus; }

	public enum ConnectStatus {
		NONE,
		SETTING_ID,		/* ID設定中 */
		START_ADVERTISE,/* アドバタイズ開始 */
		ADVERTISING,	/* アドバタイズ中... 接続開始 */
		CONNECTED,		/* 接続確立 */
	}

	private final MutableLiveData<Boolean>			mAdvertisingFlg	= new MutableLiveData<>(false);
	private final MutableLiveData<Boolean>			m1sNotifyFlg	= new MutableLiveData<>(false);
	private int										mSeekerID		= 0;
	public MutableLiveData<Boolean>		AdvertisingFlg()	{ return mAdvertisingFlg; }
	public MutableLiveData<Boolean>		Priodic1sNotifyFlg(){ return m1sNotifyFlg; }
	public void							setSeekerID(int id)	{ mSeekerID = id; }
	public int							getSeekerID()		{ return mSeekerID; }

	private final MutableLiveData<String>	mShowSnacbar			= new MutableLiveData<>();
	public LiveData<String>					ShowSnacbar()			{ return mShowSnacbar; }
	public void								showSnacbar(String showmMsg) { mShowSnacbar.postValue(showmMsg);}
	private final MutableLiveData<String>	mShowErrMsg				= new MutableLiveData<>();
	public LiveData<String>					ShowErrMsg()			{ return mShowErrMsg; }
	public void								showErrMsg(String showmMsg) { mShowErrMsg.postValue(showmMsg);}


	/** **********
	 * Service接続
	 ** *********/
	private IBleClientService	mBleServiceIf;
	public int onServiceConnected(IBleClientService service) {
		mBleServiceIf = service;

		/* コールバック設定 */
		try { mBleServiceIf.setCallback(mCb); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_CALLBACK_FAILED;}

		/* BLE初期化 */
		int ret = 0;
		try { ret = mBleServiceIf.initBle(); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_INIT_BLE_FAILED;}

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
	public int startAdvertising() {
		try { mBleServiceIf.startAdvertising(mSeekerID); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_START_ADVERTISING;}
		return UWS_NG_SUCCESS;
	}

	/** ************
	 * アドバタイズ終了
	 ** ***********/
	public int stopAdvertising() {
		try { mBleServiceIf.stopAdvertising(); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_STOP_ADVERTISING;}
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
		if(context.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
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
		public void notifyAdvertising(int ret) throws RemoteException {
			TLog.d("アドバタイズ開始結果 ret={0}", ret);
			if(ret == UWS_NG_SUCCESS)
				return;

			if(ret == ADVERTISE_FAILED_ALREADY_STARTED) {
				String errstr = "すでにアドバタイズ開始済。続行します。";
				TLog.d(errstr);
				showSnacbar(errstr);
			}
			else if(ret == ADVERTISE_FAILED_DATA_TOO_LARGE) {
				String errstr = "アドバタイズのデータサイズがデカすぎ!!\n送れない!!";
				TLog.d(errstr);
				showSnacbar(errstr);
			}
			else if(ret == ADVERTISE_FAILED_FEATURE_UNSUPPORTED) {
				String errstr = "Bluetooth未サポート!!\n動作しない端末です。終了します。";
				TLog.d(errstr);
				showSnacbar(errstr);
			}
			else if(ret == ADVERTISE_FAILED_INTERNAL_ERROR) {
				String errstr = "Android内部エラー!!\nどうしようもないので、終了します。";
				TLog.d(errstr);
				showSnacbar(errstr);
			}
			else if(ret == ADVERTISE_FAILED_TOO_MANY_ADVERTISERS) {
				String errstr = "他のアプリでもアドバタイズを実行しているため、実行できません。再起動で直ることがあります。\n終了します。";
				TLog.d(errstr);
				showSnacbar(errstr);
			}
			return;
		}
	};
}
