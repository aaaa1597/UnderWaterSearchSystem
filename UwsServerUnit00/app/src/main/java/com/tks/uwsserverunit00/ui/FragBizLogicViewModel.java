package com.tks.uwsserverunit00.ui;

import androidx.lifecycle.ViewModel;

public class FragBizLogicViewModel extends ViewModel {
	private boolean mIsSerching = false;
	public boolean getSerchStatus() { return mIsSerching; }
	public void setSerchStatus(boolean isSerching) { mIsSerching = isSerching; }
}