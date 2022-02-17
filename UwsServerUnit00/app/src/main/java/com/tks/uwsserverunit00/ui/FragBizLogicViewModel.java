package com.tks.uwsserverunit00.ui;

import android.app.Application;
import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FragBizLogicViewModel extends ViewModel {
	private boolean				mIsSerching = false;
	private final MutableLiveData<Pair<Short, Short>>	mOnHearBeatChange = new MutableLiveData<>();
	public MutableLiveData<Pair<Short, Short>>			onHearBeatChange() { return mOnHearBeatChange; }
	private final MutableLiveData<Pair<Short, Boolean>>	mOnSelectedChange = new MutableLiveData<>();
	public MutableLiveData<Pair<Short, Boolean>>		onSelectedChange() { return mOnSelectedChange; }

	public boolean getSerchStatus() { return mIsSerching; }
	public void setSerchStatus(boolean isSerching) { mIsSerching = isSerching; }

	public void setHeartBeat(short seekerid, String name, String addr, long datetime, short hearbeat) {
		if(seekerid == -1) return;
		mOnHearBeatChange.postValue(new Pair<>(seekerid, hearbeat));
	}

	public void setSelected(short seekerid, boolean isChecked) {
		mOnSelectedChange.postValue(new Pair<>(seekerid, isChecked));
	}
}