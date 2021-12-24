package com.tks.uwsserverunit00.ui;

import android.os.Handler;
import android.os.RemoteException;
import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.IBleServerService;
import com.tks.uwsserverunit00.IBleServerServiceCallback;
import com.tks.uwsserverunit00.TLog;
import java.text.MessageFormat;
import java.util.Date;
import java.util.List;

import static com.tks.uwsserverunit00.Constants.UWS_NG_SUCCESS;
import static com.tks.uwsserverunit00.Constants.UWS_NG_GATT_SUCCESS;
import static com.tks.uwsserverunit00.Constants.UWS_NG_DEVICE_NOTFOUND;
import static com.tks.uwsserverunit00.Constants.UWS_NG_AIDL_CALLBACK_FAILED;
import static com.tks.uwsserverunit00.Constants.UWS_NG_AIDL_INIT_BLE_FAILED;
import static com.tks.uwsserverunit00.Constants.UWS_NG_AIDL_STARTSCAN_FAILED;

public class FragBleViewModel extends ViewModel {
	final int PERIODIC_INTERVAL_TIME = 3000;

	private IBleServerService				mBleServiceIf;
	/* ---------------- */
	private final MutableLiveData<Boolean>	mNotifyDataSetChanged	= new MutableLiveData<>(false);
	public MutableLiveData<Boolean>			NotifyDataSetChanged()	{ return mNotifyDataSetChanged; }
	/* ---------------- */
	private final MutableLiveData<Integer>	mNotifyItemChanged		= new MutableLiveData<>(-1);
	public MutableLiveData<Integer>			NotifyItemChanged()		{ return mNotifyItemChanged; }
	/* ---------------- */
	private final MutableLiveData<String>	mShowSnacbar			= new MutableLiveData<>("");
	public LiveData<String>					ShowSnacbar()			{ return mShowSnacbar; }
	public void								showSnacbar(String showmMsg) { mShowSnacbar.postValue(showmMsg);}
	/* ---------------- */
	private DeviceListAdapter				mDeviceListAdapter;
	public void								setDeviceListAdapter(DeviceListAdapter adapter)	{ mDeviceListAdapter = adapter; }
	public DeviceListAdapter				getDeviceListAdapter()	{ return mDeviceListAdapter; }
	/* ---------------- */

	/** *****************
	 * サービス接続Callback
	 * *****************/
	public int onServiceConnected(IBleServerService service) {
		mBleServiceIf = service;

		/* コールバック設定 */
		try { mBleServiceIf.setCallback(mCb); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_CALLBACK_FAILED;}

		/* BLE初期化 */
		int ret;
		try { ret = mBleServiceIf.initBle(); }
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_INIT_BLE_FAILED;}

		if(ret != UWS_NG_SUCCESS)
			return ret;

		/* scan開始 */
		int retscan = startScan();
		TLog.d("scan開始 ret={0}", retscan);

		return retscan;
	}

	/** *****************
	 * サービス接続Callback
	 * *****************/
	public void onServiceDisconnected() {
		mBleServiceIf = null;
	}

	/** **********
	 * Scan開始
	 * **********/
	public int startScan() {
		TLog.d("Scan開始.");
		int ret;
		try { ret = mBleServiceIf.startScan();}
		catch (RemoteException e) { e.printStackTrace(); return UWS_NG_AIDL_STARTSCAN_FAILED;}
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
		int ret;
		try { ret = mBleServiceIf.stopScan();}
		catch (RemoteException e) { e.printStackTrace(); return;}
		TLog.d("scan停止 ret={0}", ret);
	}

	/** *********
	 * 定期読込開始
	 * *********/
	private final Handler mHandler = new Handler();
	private Runnable mPeriodicReader = null;
	public void startPeriodicRead(@NonNull String sUuid, @NonNull String address) {
		mPeriodicReader = () -> {
			String laddress = mDeviceListAdapter.getAddress(sUuid, address);
			if(laddress == null) {
				/* 今回はデバイスが見つからない、計測実行。 */
				TLog.d("今回はデバイスが見つからない -> 継続実行。sUuid={0} adress={1}", sUuid, address);
				mHandler.postDelayed(mPeriodicReader, PERIODIC_INTERVAL_TIME);
			}

			int ret = 0;
			try { ret = mBleServiceIf.readData(address);}
			catch (RemoteException e) { e.printStackTrace();}
			if( ret < 0) {
				TLog.d("BLE読込み失敗!! DEVICE NOT FOUND.");
				if(ret == UWS_NG_DEVICE_NOTFOUND) {
					showSnacbar("デバイスなし!!\n別のデバイスを選択して下さい。");
					int idx = mDeviceListAdapter.setChecked(sUuid, address,false);
					mNotifyItemChanged.postValue(idx);
				}
			}
			/* 2秒定期で実行 */
			mHandler.postDelayed(mPeriodicReader, PERIODIC_INTERVAL_TIME);
		};
		mHandler.post(mPeriodicReader);
		showSnacbar("定期読込み 開始しました。");
	}

	/** ************
	 * 定期読込開始終了
	 * ************/
	public void stopPeriodicRead(String sUuid, String address) {
		TLog.d("aaaaaaaaaaa  期待通り、停止が動く. suuid={2} address={1}", sUuid, address);

		mHandler.removeCallbacks(mPeriodicReader);
		mPeriodicReader = null;
		showSnacbar("定期読込み 終了しました。");
	}

	/** *****************
	 * AIDLコールバック
	 * *****************/
	private final IBleServerServiceCallback mCb = new IBleServerServiceCallback.Stub() {
		@Override
		public void notifyDeviceInfolist(List<DeviceInfo> devices) {
			mDeviceListAdapter.addDevice(devices);
			mNotifyDataSetChanged.postValue(true);
		}

		@Override
		public void notifyDeviceInfo(DeviceInfo device) {
			mDeviceListAdapter.addDevice(device);
			mNotifyDataSetChanged.postValue(true);
			TLog.d("発見!! No:{0}, {1}({2}):Rssi({3})", device.getSeekerId(), device.getDeviceAddress(), device.getDeviceName(), device.getDeviceRssi());
		}

//		@Override
//		public void notifyScanEnd() throws RemoteException {
//			TLog.d("scan終了");
//		}

		@Override
		public void notifyGattConnected(String shortUuid, String address) {
			/* Gatt接続完了 */
			TLog.d("Gatt接続OK!! -> Services探索中. sUuid={0} Address={1}", shortUuid, address);
			int pos = mDeviceListAdapter.setStatus(shortUuid, address, DeviceListAdapter.ConnectStatus.EXPLORING);
			mNotifyItemChanged.postValue(pos);
		}

		@Override
		public void notifyGattDisConnected(String shortUuid, String address) {
			String logstr = MessageFormat.format("Gatt接続断!! sUuid={0} address={1}", shortUuid, address);
			TLog.d(logstr);
			int idx = mDeviceListAdapter.setStatus(shortUuid, address, DeviceListAdapter.ConnectStatus.DISCONNECTED);
			mNotifyItemChanged.postValue(idx);
		}

		@Override
		public void notifyServicesDiscovered(String shortUuid, String address, int status) {
			if(status == UWS_NG_GATT_SUCCESS) {
				TLog.d("Services発見. -> 対象Serviceかチェック sUuid={0} address={1} ret={2}", shortUuid, address, status);
				int pos = mDeviceListAdapter.setStatus(shortUuid, address, DeviceListAdapter.ConnectStatus.CHECKAPPLI);
				mNotifyItemChanged.postValue(pos);
			}
			else {
				String logstr = MessageFormat.format("Services探索失敗!! 処理終了 sUuid={0} address={1} ret={2}", shortUuid, address, status);
				TLog.d(logstr);
				int pos = mDeviceListAdapter.setStatus(shortUuid, address, DeviceListAdapter.ConnectStatus.FAILURE);
				mNotifyItemChanged.postValue(pos);
			}
		}

		@Override
		public void notifyApplicable(String shortUuid, String address, boolean status) {
			if(status) {
				TLog.d("対象Chk-OK. -> 通信中 sUuid={0} address={1}", shortUuid, address);
				int pos = mDeviceListAdapter.setStatus(shortUuid, address, DeviceListAdapter.ConnectStatus.TOBEPREPARED);
				mNotifyItemChanged.postValue(pos);
			}
			else {
				String logstr = MessageFormat.format("対象外デバイス.　処理終了. sUuid={0} address={1}", shortUuid, address);
				TLog.d(logstr);
				int pos = mDeviceListAdapter.setStatus(shortUuid, address, DeviceListAdapter.ConnectStatus.OUTOFSERVICE);
						  mDeviceListAdapter.setChecked(shortUuid, address,false);
				mNotifyItemChanged.postValue(pos);
			}
		}

		@Override
		public void notifyWaitforRead(String shortUuid, String address, boolean status) {
			if(status) {
				String logstr = MessageFormat.format("BLEデバイス通信 読込み中. sUuid={0} address={1}", shortUuid, address);
				TLog.d(logstr);
				int pos = mDeviceListAdapter.setStatus(shortUuid, address, DeviceListAdapter.ConnectStatus.WAITFORREAD);
				mNotifyItemChanged.postValue(pos);
			}
			else {
				String logstr = MessageFormat.format("BLEデバイス通信 読込み失敗!! sUuid={0} address={1}", shortUuid, address);
				TLog.d(logstr);
				int pos = mDeviceListAdapter.setStatus(shortUuid, address, DeviceListAdapter.ConnectStatus.FAILURE);
				mNotifyItemChanged.postValue(pos);
			}
		}

		@Override
		public void notifyResRead(String shortUuid, String address, long ldatetime, double longitude, double latitude, int heartbeat, int status) {
			if(status == UWS_NG_SUCCESS) {
				TLog.d("読込成功. {0}({1})=({2} 経度:{3} 緯度:{4} 脈拍:{5}) status={6}", shortUuid, address, new Date(ldatetime), longitude, latitude, heartbeat, status);
				int pos = mDeviceListAdapter.setStatusAndReadData(shortUuid, address, DeviceListAdapter.ConnectStatus.READSUCCEED, longitude, latitude, heartbeat);
				mNotifyItemChanged.postValue(pos);

//				/* 読込み完了 -> Gatt切断(ここではやらない) */
//				try { mBleServiceIf.disconnectDevice(address);}
//				catch (RemoteException e) { e.printStackTrace();}
			}
			else {
				TLog.d("読込失敗!! {0}({1})=({2} 経度:{3} 緯度:{4} 脈拍:{5}) status={6}", shortUuid, address, new Date(ldatetime), longitude, latitude, heartbeat, status);
				int pos = mDeviceListAdapter.setStatus(shortUuid, address, DeviceListAdapter.ConnectStatus.FAILURE);
				mNotifyItemChanged.postValue(pos);

//				/* 読込み完了 -> Gatt切断(ここではやらない) */
//				try { mBleServiceIf.disconnectDevice(address);}
//				catch (RemoteException e) { e.printStackTrace();}
			}
		}

//		@Override
//		public void notifyFromPeripheral(String Address, long ldatetime, double longitude, double latitude, int heartbeat) throws RemoteException {
//			String logstr = MessageFormat.format("デバイス通知 {0}=({1} 経度:{2} 緯度:{3} 脈拍:{4})", Address, new Date(ldatetime), longitude, latitude, heartbeat);
//			TLog.d(logstr);
//		}
//
//		@Override
//		public void notifyError(int errcode, String errmsg) throws RemoteException {
//			String logstr = MessageFormat.format("ERROR!! errcode={0} : {1}", errcode, errmsg);
//			TLog.d(logstr);
	};
}
