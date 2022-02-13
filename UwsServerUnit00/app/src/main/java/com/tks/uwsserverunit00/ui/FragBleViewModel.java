package com.tks.uwsserverunit00.ui;

import androidx.lifecycle.ViewModel;
import com.tks.uwsserverunit00.IUwsServer;
import com.tks.uwsserverunit00.ui.DeviceListAdapter.DeviceInfoModel;

import static com.tks.uwsserverunit00.Constants.d2Str;

import java.util.List;

public class FragBleViewModel extends ViewModel {
	private IUwsServer			mUwsServiceIf;
	private DeviceListAdapter	mDeviceListAdapter;

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
}
