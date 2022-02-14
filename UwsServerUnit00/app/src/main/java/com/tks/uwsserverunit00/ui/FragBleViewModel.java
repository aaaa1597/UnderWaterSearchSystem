package com.tks.uwsserverunit00.ui;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tks.uwsserverunit00.IUwsServer;
import com.tks.uwsserverunit00.TLog;
import com.tks.uwsserverunit00.ui.DeviceListAdapter.DeviceInfoModel;

import static com.tks.uwsserverunit00.Constants.d2Str;

import android.app.Application;
import android.location.Location;

import java.util.List;

public class FragBleViewModel extends AndroidViewModel {
	private IUwsServer			mUwsServiceIf;
	private DeviceListAdapter	mDeviceListAdapter;
	private Application			mContext;

	public FragBleViewModel(@NonNull Application application) {
		super(application);
		mContext = application;
	}

	/** *************
	 * サービスbind完了
	 * **************/
	public void onServiceConnected(IUwsServer service) {
		mUwsServiceIf = service;
	}

	/** ***********
	 * サービスbind断
	 * ************/
	public void onServiceDisconnected() {
		mUwsServiceIf = null;
	}

	public void setDeviceListAdapter(DeviceListAdapter deviceListAdapter) {
		mDeviceListAdapter = deviceListAdapter;
	}

	public DeviceListAdapter getDeviceListAdapter() {
		return mDeviceListAdapter;
	}

	public void setSelected(short seekerid, boolean isChecked) {
//		/* TODO */TLog.d("seekerid={0} isChecked={1}", seekerid, isChecked);
//		boolean nowStatus = mDeviceListAdapter.getChecked(seekerid);
//		if(nowStatus == isChecked) return;
//		/* TODO */TLog.d("seekerid={0} nowStatus:({1})->isChecked:({2})", seekerid, nowStatus, isChecked);
//
//		/* メンバ選択Switchに設定 */
//		int pos = mDeviceListAdapter.setSelected(seekerid, isChecked);
//		mNotifyItemChanged.postValue(pos);
//
//		/* サービスに通知(開始/終了) */
//		int ret = ERR_OK;
//		try {
//			if(isChecked)
//				ret = mUwsServiceIf.startPeriodicNotify(seekerid, new IUwsInfoCallback.Stub() {
//						@Override
//						public void notifyUwsData(UwsInfo uwsInfo) {
//							int pos = mDeviceListAdapter.updDeviceInfo(uwsInfo);
//							mNotifyItemChanged.postValue(pos);
//							mUpdUwsInfo.postValue(uwsInfo);
//							TLog.d("UwsInfo受信({0} {1} {2} {3})", d2Str(uwsInfo.getDate()), d2Str(uwsInfo.getLongitude()), d2Str(uwsInfo.getLatitude()), uwsInfo.getHeartbeat());
//						}
//
//					@Override
//					public void notifyStatus(int status) {
//						/* TODO 要実装 */
//					}
//				});
//			else
//				mUwsServiceIf.stopPeriodicNotify(seekerid);
//		}
//		catch (RemoteException e) { e.printStackTrace(); ret = ERR_AIDL_REMOTE_ERROR; }
//		TLog.d("ret={0}", ret);
	}

	public void setBuoy(short seekerid, boolean isChecked) {
//		mDeviceListAdapter.setBuoy(seekerid, isChecked);
//		mNotifyDataSetChanged.postValue(true);
	}

	/* 脈拍設定 */
	public void setHeartBeat(String name, String addr, long datetime, short hearbeat) {
		mDeviceListAdapter.setHeartBeat(name, addr, datetime, hearbeat);
	}

	/* 経度/緯度設定 */
	public void setLocation(String name, String addr, long datetime, Location loc) {
		mDeviceListAdapter.setLocation(name, addr, datetime, loc);
	}

	/* 状態変化通知 */
	public void OnChangeStatus(String name, String addr, int resourceid) {
		/* TODO */
		TLog.d(" 状態変化通知 {0}", mContext.getString(resourceid));
	}
}
