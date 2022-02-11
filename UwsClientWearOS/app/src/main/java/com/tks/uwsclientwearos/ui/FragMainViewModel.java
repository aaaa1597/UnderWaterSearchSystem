package com.tks.uwsclientwearos.ui;

import android.location.Location;
import android.os.RemoteException;
import android.util.Pair;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tks.uwsclientwearos.IClientService;
import com.tks.uwsclientwearos.IOnUwsInfoListner;
import com.tks.uwsclientwearos.Constants.Sender;
import com.tks.uwsclientwearos.TLog;
import com.tks.uwsclientwearos.UwsInfo;

public class FragMainViewModel extends ViewModel {
	private final MutableLiveData<Double>					mLatitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Double>					mLongitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Short>					mHearBeat		= new MutableLiveData<>((short)0);
	private final MutableLiveData<Pair<Sender, Boolean>>	mUnLock			= new MutableLiveData<>(Pair.create(Sender.App, true));
	public MutableLiveData<Double>					Latitude()		{ return mLatitude; }
	public MutableLiveData<Double>					Longitude()		{ return mLongitude; }
	public MutableLiveData<Short>					HearBeat()		{ return mHearBeat; }
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
	}

	/** ********
	 *  Location
	 *  ********/
	public boolean mIsSetedLocationON = false;

	/* *********************************/
	/* 業務プロセス(BLE,位置情報,脈拍) */
	/* *********************************/
	/* 開始 */
	public void startUws(short seekerId) {
		TLog.d("seekerid={0}", seekerId);
		if(mClientServiceIf == null) return;
		try { mClientServiceIf.startUws(seekerId, new IOnUwsInfoListner.Stub() {
													@Override
													public void onLocationResult(Location location) {
														mLongitude.postValue(location.getLongitude());
														mLatitude .postValue(location.getLatitude());
													}

													@Override
													public void onHeartbeatResult(int heartbeat) {
														mHearBeat.postValue((short)heartbeat);
													}
												});
		}
		catch (RemoteException e) { e.printStackTrace(); }
	}
	/* 終了 */
	public void stopUws() {
		if(mClientServiceIf == null) return;
		try { mClientServiceIf.stopUws(); }
		catch (RemoteException e) { e.printStackTrace(); }
	}
}
