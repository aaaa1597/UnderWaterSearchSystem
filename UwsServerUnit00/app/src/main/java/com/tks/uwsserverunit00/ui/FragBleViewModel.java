package com.tks.uwsserverunit00.ui;

import android.os.IBinder;
import android.os.RemoteException;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.IUwsInfoCallback;
import com.tks.uwsserverunit00.IUwsScanCallback;
import com.tks.uwsserverunit00.IUwsServer;
import com.tks.uwsserverunit00.TLog;
import com.tks.uwsserverunit00.UwsInfo;

import static com.tks.uwsserverunit00.Constants.UWS_NG_AIDL_REMOTE_ERROR;
import static com.tks.uwsserverunit00.Constants.UWS_NG_SUCCESS;

import java.util.List;

public class FragBleViewModel extends ViewModel {
	private IUwsServer							mUwsServiceIf;
	/* ---------------- */
	private final MutableLiveData<Boolean>		mNotifyDataSetChanged	= new MutableLiveData<>(false);
	public MutableLiveData<Boolean>				NotifyDataSetChanged()	{ return mNotifyDataSetChanged; }
	/* ---------------- */
	private final MutableLiveData<Integer>		mNotifyItemChanged		= new MutableLiveData<>(-1);
	public MutableLiveData<Integer>				NotifyItemChanged()		{ return mNotifyItemChanged; }
	/* ---------------- */
	private final MutableLiveData<String>		mShowSnacbar			= new MutableLiveData<>("");
	public LiveData<String>						ShowSnacbar()			{ return mShowSnacbar; }
	public void									showSnacbar(String showmMsg) { mShowSnacbar.postValue(showmMsg);}
	/* ---------------- */
	private DeviceListAdapter					mDeviceListAdapter;
	public void									setDeviceListAdapter(DeviceListAdapter adapter)	{ mDeviceListAdapter = adapter; }
	public DeviceListAdapter					getDeviceListAdapter()	{ return mDeviceListAdapter; }
	/* ---------------- */
	private final MutableLiveData<DeviceInfo>	mNewDeviceInfo			= new MutableLiveData<>(null);
	public MutableLiveData<DeviceInfo>			NewDeviceInfo()			{ return mNewDeviceInfo; }
	/* ---------------- */
	private final MutableLiveData<Boolean>		mOnlySeeker				= new MutableLiveData<>(true);
	public MutableLiveData<Boolean>				OnlySeeker()			{ return mOnlySeeker; }
	/* ---------------- */

	/** *********************************************
	 * サービス接続完了 -> BLE初期化,scan開始
	 * **********************************************/
	public int onServiceConnected(IUwsServer service) {
		mUwsServiceIf = service;

		/* BLE初期化 */
		int ret;
		try { ret = mUwsServiceIf.initBle(); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}

		if(ret != UWS_NG_SUCCESS)
			return ret;

		/* scan開始 */
		int retscan = startScan(new IUwsScanCallback.Stub() {
			@Override
			public void notifyDeviceInfo(DeviceInfo device) {
				boolean newDataFlg = mDeviceListAdapter.addDevice(device, mOnlySeeker.getValue());
				mNotifyDataSetChanged.postValue(true);
				if(newDataFlg && device.getSeekerId()!=-1)
					mNewDeviceInfo.postValue(device);
			}
		});
		TLog.d("scan開始 ret={0}", retscan);

		return retscan;
	}

	/** *********
	 * サービス切断
	 * **********/
	public void onServiceDisconnected() {
		mUwsServiceIf = null;
	}

	/** **********
	 * Scan開始
	 * **********/
	public int startScan(IUwsScanCallback cb) {
		TLog.d("Scan開始.");
		int ret;
		try { ret = mUwsServiceIf.startScan(cb);}
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_REMOTE_ERROR;}
		TLog.d("ret={0}", ret);

		if(ret != UWS_NG_SUCCESS)
			return ret;

		mDeviceListAdapter.clearDevice();
		mNotifyDataSetChanged.postValue(true);

		return UWS_NG_SUCCESS;
	}

	/** **********
	 * Scan終了
	 * **********/
	public void stopScan() {
		try { mUwsServiceIf.stopScan();}
		catch (RemoteException e) { e.printStackTrace(); return;}
		TLog.d("scan停止");
	}

	public void clearAll() {
		mDeviceListAdapter.clearDevice();
		mNotifyDataSetChanged.postValue(true);
	}

	public void clearDeviceWithoutAppliciated() {
		mDeviceListAdapter.clearDeviceWithoutAppliciated();
		mNotifyDataSetChanged.postValue(true);
	}

	public void setChecked(short seekerid, boolean isChecked) {
		new Thread(() -> {
			/* メンバ選択Switchに設定 */
			int idx = mDeviceListAdapter.setChecked(seekerid, isChecked);
			/* メンバ選択Switch表示更新 */
			mNotifyItemChanged.postValue(idx);

			/* サービスに通知(開始/終了) */
			int ret = UWS_NG_SUCCESS;
			try {
				if(isChecked)
					ret = mUwsServiceIf.startPeriodicNotify(seekerid, new IUwsInfoCallback.Stub() {
							@Override
							public void notifyUwsData(UwsInfo uwsInfo) {
								int pos = mDeviceListAdapter.setUwsInfo(uwsInfo);
								mNotifyItemChanged.postValue(pos);
							}

						@Override
						public void notifyStatus(int status) {
							/* TODO 要実装 */
						}
					});
				else
					mUwsServiceIf.stopPeriodicNotify(seekerid);
			}
			catch (RemoteException e) { e.printStackTrace(); ret = UWS_NG_AIDL_REMOTE_ERROR; }
			TLog.d("ret={0}", ret);
		}).start();
	}

	public void setBuoy(short seekerid, boolean isChecked) {
		mDeviceListAdapter.setBuoy(seekerid, isChecked);
		mNotifyDataSetChanged.postValue(true);
	}
}
