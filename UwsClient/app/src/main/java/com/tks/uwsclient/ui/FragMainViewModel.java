package com.tks.uwsclient.ui;

import android.os.IBinder;
import android.os.RemoteException;
import android.util.Pair;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tks.uwsclient.Constants.Sender;
import com.tks.uwsclient.IClientService;
import com.tks.uwsclient.IOnStatusChangeListner;
import com.tks.uwsclient.TLog;

public class FragMainViewModel extends ViewModel {
	private final MutableLiveData<Double>					mLatitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Double>					mLongitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Short>					mHearBeat		= new MutableLiveData<>((short)0);
	private final MutableLiveData<Pair<Sender, Boolean>>	mUnLock			= new MutableLiveData<>(Pair.create(Sender.App, true));
	private final MutableLiveData<ConnectStatus>			mStatus			= new MutableLiveData<>(ConnectStatus.NONE);
	public MutableLiveData<Double>					Latitude()		{ return mLatitude; }
	public MutableLiveData<Double>					Longitude()		{ return mLongitude; }
	public MutableLiveData<Short>					HearBeat()		{ return mHearBeat; }
	public MutableLiveData<Pair<Sender, Boolean>>	UnLock()		{ return mUnLock; }
	public MutableLiveData<ConnectStatus>			ConnectStatus()	{ return mStatus; }

	public enum ConnectStatus {
		NONE,
		SETTING_ID,		/* ID設定中 */
		START_ADVERTISE,/* アドバタイズ開始 */
		ADVERTISING,	/* アドバタイズ中... 接続開始 */
		CONNECTED,		/* 接続確立 */
		DISCONNECTED,	/* 接続断 */
		ERROR,			/* エラー発生!! */
	}

	private final MutableLiveData<Boolean>	mAdvertisingFlg	= new MutableLiveData<>(false);
	public MutableLiveData<Boolean>			AdvertisingFlg()	{ return mAdvertisingFlg; }
	private short	mSeekerId = 0;
	public void		setSeekerId(short id)	{ mSeekerId = id; }
	public short	getSeekerId()			{ return mSeekerId; }
	public MutableLiveData<Object>			UpdDisplaySeerkerId = new MutableLiveData<>();

	private final MutableLiveData<String>	mShowSnacbar			= new MutableLiveData<>();
	public LiveData<String>					ShowSnacbar()			{ return mShowSnacbar; }
	public void								showSnacbar(String showmMsg) { mShowSnacbar.postValue(showmMsg);}
	private final MutableLiveData<String>	mShowErrMsg				= new MutableLiveData<>();
	public LiveData<String>					ShowErrMsg()			{ return mShowErrMsg; }
	public void								showErrMsg(String showmMsg) { mShowErrMsg.postValue(showmMsg);}

	/** ********
	 *  Location
	 *  ********/
	public boolean mIsSetedLocationON = false;

	IClientService mClientServiceIf;
	public void setClientServiceIf(IClientService serviceIf) {
		mClientServiceIf = serviceIf;
	}

	public void setSeekerIdSmoothScrollToPosition(short seekerId) {
		mSeekerId = seekerId;
		UpdDisplaySeerkerId.postValue(seekerId);
	}
}
