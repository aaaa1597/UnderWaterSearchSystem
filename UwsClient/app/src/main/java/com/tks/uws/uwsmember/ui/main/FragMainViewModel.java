package com.tks.uws.uwsmember.ui.main;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FragMainViewModel extends ViewModel {
	private final MutableLiveData<Double>			mLatitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Double>			mLongitude		= new MutableLiveData<>(0.0);
	private final MutableLiveData<Integer>			mHearBeat		= new MutableLiveData<>(0);
	private final MutableLiveData<Integer>			mID				= new MutableLiveData<>(-1);
	private final MutableLiveData<ConnectStatus>	mStatus			= new MutableLiveData<>(ConnectStatus.NONE);
	private final MutableLiveData<Boolean>			mPressSetBtn	= new MutableLiveData<>(false);

	public MutableLiveData<Double>	Latitude()	{
		return mLatitude;
	}
	public MutableLiveData<Double>	Longitude()	{
		return mLongitude;
	}
	public MutableLiveData<Integer>	HearBeat()	{
		return mHearBeat;
	}
	public void setID(int id)	{
		mID.setValue(id);
	}
	public int getID()	{
		Integer ret = mID.getValue();
		return ret == null ? -1 : ret;
	}
	public MutableLiveData<ConnectStatus>	ConnectStatus()	{
		return mStatus;
	}
	public MutableLiveData<Boolean> PressSetBtn()	{
		return mPressSetBtn;
	}

	public enum ConnectStatus {
		NONE,
		SETTING_ID,		/* ID設定中 */
		START_ADVERTISE,/* アドバタイズ開始 */
		ADVERTISING,	/* アドバタイズ中... 接続開始 */
		CONNECTED,		/* 接続確立 */
	}
}