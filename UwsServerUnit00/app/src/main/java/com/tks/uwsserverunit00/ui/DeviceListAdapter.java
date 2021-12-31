package com.tks.uwsserverunit00.ui;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.widget.SwitchCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.R;
import com.tks.uwsserverunit00.TLog;
import static com.tks.uwsserverunit00.Constants.UWS_NG_DEVICE_NOTFOUND;

/**
 * -30 dBm	素晴らしい	達成可能な最大信号強度。クライアントは、これを実現するには、APから僅か数フィートである必要があります。現実的には一般的ではなく、望ましいものでもありません	N/A
 * -60 dBm	良好	非常に信頼性の高く、データパケットのタイムリーな伝送を必要とするアプリケーションのための最小信号強度	VoIP/VoWiFi, ストリーミングビデオ
 * -70 dBm	Ok	信頼できるパケット伝送に必要な最小信号強度	Email, web
 * -80 dBm	よくない	基本的なコネクティビティに必要な最小信号強度。パケット伝送は信頼できない可能性があります	N/A
 * -90 dBm	使用不可	ノイズレベルに近いかそれ以下の信号強度。殆ど機能しない	N/A
 * */

public class DeviceListAdapter extends RecyclerView.Adapter<DeviceListAdapter.ViewHolder> {
	static class ViewHolder extends RecyclerView.ViewHolder {
		TextView	mtxtDatetime;
		TextView	mTxtSeekerId;
		TextView	mTxtDeviceName;
		TextView	mTxtDeviceNameAddress;
		ImageView	mImvRssi;
		ImageView	mImvConnectStatus;
		TextView	mTxtConnectStatus;
		TextView	mTxtHertBeat;
		ImageButton	mBtnBuoy;
		TextView	mTxtLongitude;
		TextView	mTxtLatitude;
		SwitchCompat mSwhSelected;
		ViewHolder(View view) {
			super(view);
			mtxtDatetime = view.findViewById(R.id.txtDatetime);
			mTxtSeekerId			= view.findViewById(R.id.txtSeekerId);
			mTxtDeviceName			= view.findViewById(R.id.txtDeeviceName);
			mTxtDeviceNameAddress	= view.findViewById(R.id.txtDeviceAddress);
			mImvRssi				= view.findViewById(R.id.imvRssi);
			mImvConnectStatus		= view.findViewById(R.id.imvConnectStatus);
			mTxtConnectStatus		= view.findViewById(R.id.txtConnectStatus);
			mTxtHertBeat			= view.findViewById(R.id.txtHertBeat);
			mBtnBuoy				= view.findViewById(R.id.btnBuoy);
			mTxtLongitude			= view.findViewById(R.id.txtLongitude);
			mTxtLatitude			= view.findViewById(R.id.txtLatitude);
			mSwhSelected			= view.findViewById(R.id.swhSelected);
		}
	}

	/* インターフェース */
	interface OnCheckedChangeListener {
		void onCheckedChanged(short seekerid, boolean isChecked);
	}

	/* コンストラクタ */
	private final Context					mContext;
	private final OnCheckedChangeListener	mOnCheckedChangeListener;
	private final SimpleDateFormat mDf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSXXX", Locale.JAPAN);
	public DeviceListAdapter(Context context, OnCheckedChangeListener lisner) {
		mContext				= context;
		mOnCheckedChangeListener= lisner;
	}

	/* メンバ変数 */
	private final List<DevicveInfoModel> mDeviceList = new ArrayList<>();
	private static class DevicveInfoModel {
		public Date				mDatetime;
		public short			mSeekerId;
		public String			mDeviceName;
		public String			mDeviceAddress;
		public int				mDeviceRssi;
		public byte				mSeqNo;
		public double			mLongitude;
		public double			mLatitude;
		public short			mHertBeat;
		public boolean			mSelected;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_devices, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		DevicveInfoModel model = mDeviceList.get(position);
		final short  seekerid		= model.mSeekerId;
		final String deviceName		= model.mDeviceName;
		final String deviceAddress	= model.mDeviceAddress;
		final int rssiresid	=	model.mDeviceRssi > -60 ? R.drawable.wifi_level_3 :
								model.mDeviceRssi > -70 ? R.drawable.wifi_level_2 :
								model.mDeviceRssi > -80 ? R.drawable.wifi_level_1 : R.drawable.wifi_level_0;
		final int constsresid =	seekerid==-1			? R.drawable.statusx_na :
								model.mSelected		? R.drawable.status5_ready : R.drawable.status0_none;
		holder.mtxtDatetime.setText(mDf.format(model.mDatetime));
		holder.mTxtSeekerId.setText((seekerid==-1) ? " - " : String.valueOf(seekerid));
		holder.mTxtDeviceName.setText(TextUtils.isEmpty(deviceName) ? "" : deviceName);
		holder.mTxtDeviceNameAddress.setText(TextUtils.isEmpty(deviceAddress) ? "" : deviceAddress);
		holder.mImvRssi.setImageResource(rssiresid);
		holder.mImvConnectStatus.setImageResource(constsresid);
//		holder.mTxtConnectStatus.setText(statusinfo.first);
//		holder.mTxtConnectStatus.setTextColor(statusinfo.second);
		holder.mTxtHertBeat.setText(model.mHertBeat == 0 ? "-" : ""+model.mHertBeat);
//		holder.mBtnBuoy.setOnClickListener(null);
//		holder.mBtnBuoy.setValue(true);
		holder.mBtnBuoy.setOnClickListener(v -> {
			/* 浮標ボタン押下 */
		});
		holder.mTxtLongitude.setText(String.valueOf(model.mLongitude));
		holder.mTxtLatitude .setText(String.valueOf(model.mLatitude));
		if(seekerid == -1) {
			holder.mSwhSelected.setEnabled(false);
		}
		else {
			holder.mSwhSelected.setEnabled(true);
			holder.mSwhSelected.setOnCheckedChangeListener(null);
			holder.mSwhSelected.setChecked(model.mSelected);
			holder.mSwhSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
				mOnCheckedChangeListener.onCheckedChanged(seekerid, isChecked);
			});
		}

	}

	@Override
	public int getItemCount() {
		return mDeviceList.size();
	}

	public void addDevice(List<DeviceInfo> deviceInfos) {
		if (deviceInfos != null) {
			for (DeviceInfo deviceInfo : deviceInfos) {
				addDevice(deviceInfo);
			}
//			notifyDataSetChanged();
		}
	}

//	public void addDevice(DeviceInfo deviceInfo) {
//		addDevice(deviceInfo, true);
//	}

	/** * @return 新データかどうかのフラグ */
	public boolean addDevice(DeviceInfo deviceInfo) {
		if (deviceInfo == null)
			return false;

		/* リスト済確認 */
		DevicveInfoModel device = mDeviceList.stream().filter((item) -> {
			if(item.mSeekerId!=-1)	/* SeekerIdが-1でない場合は、対象デバイス。SeekerIdで識別する */
				return item.mSeekerId==deviceInfo.getSeekerId();
			else					/* SeekerIdが-1の場合は、対象外デバイス。Adressで識別する */
				return item.mDeviceAddress.equals(deviceInfo.getDeviceAddress());
		}).findFirst().orElse(null);

		boolean retNewDataFlg;
		if(device == null) {
			/* 新規追加 */
			retNewDataFlg = true;
			mDeviceList.add(
					new DevicveInfoModel() {{
						mDatetime		= deviceInfo.getDate();
						mSeekerId		= deviceInfo.getSeekerId();
						mDeviceName		= deviceInfo.getDeviceName();
						mDeviceAddress	= deviceInfo.getDeviceAddress();
						mDeviceRssi		= deviceInfo.getDeviceRssi();
						mSeqNo			= deviceInfo.getSeqNo();
						mLongitude		= deviceInfo.getLongitude();
						mLatitude		= deviceInfo.getLongitude();
						mHertBeat		= deviceInfo.getHeartbeat();
						mSelected		= false;
					}});
		}
		else {
			/* 新データ判定 */
			retNewDataFlg = device.mSeqNo != deviceInfo.getSeqNo();

//			device.mDatetime		= deviceInfo.mDatetime;			更新しない
			device.mSeekerId		= deviceInfo.getSeekerId();
			device.mDeviceName		= deviceInfo.getDeviceName();
			device.mDeviceAddress	= deviceInfo.getDeviceAddress();
			device.mDeviceRssi		= deviceInfo.getDeviceRssi();
			device.mSeqNo			= deviceInfo.getSeqNo();
			device.mLongitude		= deviceInfo.getLongitude();
			device.mLatitude		= deviceInfo.getLatitude();
			device.mHertBeat		= deviceInfo.getHeartbeat();
//			device.mSelected		= false;						更新しない
		}

		/* 並び替え。 */
		mDeviceList.sort((o1, o2) -> {
			/* SeekerIdの昇順 */
			if(o1.mSeekerId!=-1 && o2.mSeekerId!=-1)
				return Integer.compare(o1.mSeekerId, o2.mSeekerId);
				/* 対象のデバイス優先 */
			else if(o1.mSeekerId!=-1/* && !o2.mIsApplicable*/)
				return -1;
			else if(/*!o1.mIsApplicable &&*/ o2.mSeekerId!=-1)
				return 1;

			/* 次にアドレス名で並び替え */
			int compare = o1.mDeviceAddress.compareTo(o2.mDeviceAddress);
			if(compare == 0) return 0;
			return compare < 0 ? -1 : 1;
		});

//		if (notify) {
//			notifyDataSetChanged();
//		}

		return retNewDataFlg;
	}

	public void clearDevice() {
		mDeviceList.clear();
//		notifyDataSetChanged(); UIスレッドで実行する必要がある。
	}

	public void clearDeviceWithoutAppliciated() {
		mDeviceList.removeIf(item -> item.mSeekerId == -1);
	}

	public int setChecked(short seekerid, boolean isChecked) {
		AtomicInteger index = new AtomicInteger(-1);
		DevicveInfoModel device = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item->item.mSeekerId==seekerid).findAny().orElse(null);
		if(device == null) return UWS_NG_DEVICE_NOTFOUND;	/* 対象外デバイスが存在しない。ありえないはず。 */

		device.mSelected = isChecked;
		return index.get();
	}
}
