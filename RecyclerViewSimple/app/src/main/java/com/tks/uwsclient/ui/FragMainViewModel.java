package com.tks.uwsclient.ui;

import android.util.Pair;

import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.tks.uwsclient.Constants.Sender;

public class FragMainViewModel extends ViewModel {
    private final MutableLiveData<Pair<Sender, Boolean>> mUnLock			= new MutableLiveData<>(Pair.create(Sender.App, true));
    public MutableLiveData<Pair<Sender, Boolean>>	UnLock()		{ return mUnLock; }
}
