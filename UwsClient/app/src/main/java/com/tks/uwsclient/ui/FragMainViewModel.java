package com.tks.uwsclient.ui;

import android.app.Application;
import android.bluetooth.BluetoothDevice;
import android.location.Location;
import android.os.RemoteException;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import com.tks.uwsclient.Constants.Sender;
import com.tks.uwsclient.IClientService;
import com.tks.uwsclient.IStatusNotifier;
import com.tks.uwsclient.IOnUwsInfoChangeListner;
import com.tks.uwsclient.TLog;

public class FragMainViewModel extends AndroidViewModel {
	private final MutableLiveData<Double>					mLatitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Double>					mLongitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Short>					mHearBeat		= new MutableLiveData<>((short)0);
	private final MutableLiveData<Integer>					mOnStatusChange = new MutableLiveData<>();
	private final MutableLiveData<Pair<Sender, Boolean>>	mUnLock			= new MutableLiveData<>(Pair.create(Sender.App, true));
	private Application										mContext;

	public FragMainViewModel(@NonNull Application application) {
		super(application);
		mContext = application;
	}

	public MutableLiveData<Double>					Latitude()		{ return mLatitude; }
	public MutableLiveData<Double>					Longitude()		{ return mLongitude; }
	public MutableLiveData<Short>					HearBeat()		{ return mHearBeat; }
	public MutableLiveData<Integer>					OnStatusChange(){ return mOnStatusChange; }
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
			}
			, new IStatusNotifier.Stub() {
				@Override
				public void onStatusChange(int statusid) {
					mOnStatusChange.postValue(statusid);
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
