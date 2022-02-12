package com.tks.uwsclientwearos.ui;

import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.os.RemoteException;
import android.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tks.uwsclientwearos.Constants.Sender;
import com.tks.uwsclientwearos.IClientService;
import com.tks.uwsclientwearos.IOnServiceStatusChangeListner;
import com.tks.uwsclientwearos.IOnUwsInfoChangeListner;
import com.tks.uwsclientwearos.StatusInfo;
import com.tks.uwsclientwearos.TLog;

import static com.tks.uwsclientwearos.Constants.SERVICE_STATUS_CONNECTING;
import static com.tks.uwsclientwearos.Constants.SERVICE_STATUS_CON_LOC_BEAT;
import static com.tks.uwsclientwearos.Constants.SERVICE_STATUS_INITIALIZING;
import static com.tks.uwsclientwearos.Constants.SERVICE_STATUS_LOC_BEAT;

public class FragMainViewModel extends ViewModel {
	private final MutableLiveData<Double>					mLatitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Double>					mLongitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Short>					mHearBeat		= new MutableLiveData<>((short)0);
	private final MutableLiveData<String>					mStatusStr		= new MutableLiveData<>("");
	private final MutableLiveData<Pair<Sender, Boolean>>	mUnLock			= new MutableLiveData<>(Pair.create(Sender.App, true));
	public MutableLiveData<Double>					Latitude()		{ return mLatitude; }
	public MutableLiveData<Double>					Longitude()		{ return mLongitude; }
	public MutableLiveData<Short>					HearBeat()		{ return mHearBeat; }
	public MutableLiveData<String>					StatusStr()		{ return mStatusStr; }
	public MutableLiveData<Pair<Sender, Boolean>>	UnLock()		{ return mUnLock; }

	private short	mSeekerId = 0;
	public void		setSeekerId(short id)	{ mSeekerId = id; }
	public short	getSeekerId()			{ return mSeekerId; }
	public MutableLiveData<Short>			UpdDisplaySeerkerId = new MutableLiveData<>();
	public void setSeekerIdSmoothScrollToPosition(short seekerId) {
		mSeekerId = seekerId;
		UpdDisplaySeerkerId.postValue(seekerId);
	}

	IClientService mClientServiceIf;
	public void setClientServiceIf(IClientService serviceIf) {
		mClientServiceIf = serviceIf;

		try {
			serviceIf.setListners(new IOnUwsInfoChangeListner.Stub() {
				@Override
				public void onLocationResultChange(Location location) {
					mLongitude.postValue(location.getLongitude());
					mLatitude .postValue(location.getLatitude());
				}

				@Override
				public void onHeartbeatResultChange(int heartbeat) {
					mHearBeat.postValue((short)heartbeat);
				}
			}, new IOnServiceStatusChangeListner.Stub() {
				@Override
				public void onServiceStatusChange(StatusInfo statusInfo) {
					String statusstr = "";
					switch(statusInfo.getStatus()) {
						case SERVICE_STATUS_INITIALIZING:	statusstr = "初期化中";			break;
						case SERVICE_STATUS_LOC_BEAT:		statusstr = "脈拍/GPS取得中";		break;
						case SERVICE_STATUS_CONNECTING:		statusstr = "サーバ接続待ち...";	break;
						case SERVICE_STATUS_CON_LOC_BEAT:	statusstr = "Bluetooth通信中";	break;
					}
					mStatusStr.postValue("   " + statusstr);
				}
			});
		}
		catch(RemoteException e) {
			e.printStackTrace();
		}
	}

	/** ********
	 *  Location
	 *  ********/
	public boolean mIsSetedLocationON = false;

	/** ********
	 *  Bluetooth
	 *  ********/
	/* Bluetooth開始 */
	public void startBt(short seekerId, BluetoothDevice device) {
		TLog.d("seekerid={0}", seekerId);
		if(mClientServiceIf == null) return;
		try { mClientServiceIf.startBt(seekerId, device); }
		catch (RemoteException e) { e.printStackTrace(); }
	}

	/* Bluetooth終了 */
	public void stopBt() {
		TLog.d("");
		if(mClientServiceIf == null) return;
		try { mClientServiceIf.stopBt(); }
		catch (RemoteException e) { e.printStackTrace(); }
	}

	/* 開始条件クリア */
	public void notifyStartCheckCleared() {
		TLog.d("");
		if(mClientServiceIf == null) return;
		try { mClientServiceIf.notifyStartCheckCleared(); }
		catch (RemoteException e) { e.printStackTrace(); }
	}
}
