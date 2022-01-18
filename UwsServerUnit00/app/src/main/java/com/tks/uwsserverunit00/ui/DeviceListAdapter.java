package com.tks.uwsserverunit00.ui;

import android.content.Context;
import android.graphics.Color;
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
import com.tks.uwsserverunit00.UwsInfo;

import static com.tks.uwsserverunit00.Constants.UWS_NG_DEVICE_NOTFOUND;
import static com.tks.uwsserverunit00.Constants.d2Str;

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
	interface OnCheckedChangeListener	{ void onCheckedChanged(short seekerid, boolean isChecked); }
	interface OnSetBuoyListener			{ void onSetBuoyListener(short seekerid, boolean isChecked); }

	/* コンストラクタ */
	private final Context					mContext;
	private final OnCheckedChangeListener	mOnCheckedChangeListener;
	private final OnSetBuoyListener			mOnSetBuoyListener;
	public DeviceListAdapter(Context context, OnCheckedChangeListener lisner, OnSetBuoyListener lisner2) {
		mContext				= context;
		mOnCheckedChangeListener= lisner;
		mOnSetBuoyListener		= lisner2;
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
		public boolean			mIsBuoy;
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
								model.mSelected			? R.drawable.status5_ready : R.drawable.status0_none;
		holder.mtxtDatetime.setText(d2Str(model.mDatetime));
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
		if(seekerid==-1) {
			holder.mBtnBuoy.setEnabled(false);
			holder.mBtnBuoy.setBackgroundColor(Color.DKGRAY);
		}
		else {
			holder.mBtnBuoy.setEnabled(true);
			holder.mBtnBuoy.setOnClickListener(v -> {
				/* 浮標ボタン押下 */
				mOnSetBuoyListener.onSetBuoyListener(seekerid, !model.mIsBuoy);
			});
			if(model.mIsBuoy)
				holder.mBtnBuoy.setBackgroundColor(Color.WHITE);
			else
				holder.mBtnBuoy.setBackgroundColor(Color.GRAY);
		}
		holder.mTxtLongitude.setText(String.valueOf(model.mLongitude));
		holder.mTxtLatitude .setText(String.valueOf(model.mLatitude));
		if(seekerid == -1) {
			holder.mSwhSelected.setOnCheckedChangeListener(null);
			holder.mSwhSelected.setChecked(model.mSelected);
			holder.mSwhSelected.setEnabled(false);
		}
		else {
			holder.mSwhSelected.setEnabled(true);
			holder.mSwhSelected.setOnCheckedChangeListener(null);
			holder.mSwhSelected.setChecked(model.mSelected);
			holder.mSwhSelected.setOnCheckedChangeListener((buttonView, isChecked) -> {
				/* 選択中スイッチ */
				mOnCheckedChangeListener.onCheckedChanged(seekerid, isChecked);
			});
		}
	}

	@Override
	public int getItemCount() {
		return mDeviceList.size();
	}

	/** * @return 新データかどうかのフラグ */
	public int updDeviceInfo(DeviceInfo deviceInfo, Boolean isOnlySeeker) {
		if (deviceInfo == null)
			return -1;

		/* 隊員のみ表示中なら、対象外デバイスは追加しない */
		if(isOnlySeeker && deviceInfo.getSeekerId()==-1)
			return -1;

		/* リスト追加済確認 */
		AtomicInteger idx = new AtomicInteger(-1);
		DevicveInfoModel device = mDeviceList.stream().peek(x->idx.incrementAndGet()).filter((item) -> {
			if(item.mSeekerId!=-1)	/* SeekerIdが-1でない場合は、対象デバイス。SeekerIdで識別する */
				return item.mSeekerId==deviceInfo.getSeekerId();
			else					/* SeekerIdが-1の場合は、対象外デバイス。Adressで識別する */
				return item.mDeviceAddress.equals(deviceInfo.getDeviceAddress());
		}).findAny().orElse(null);

		if(device == null) {
			/* 新規追加 */
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
						mIsBuoy			= false;
					}});

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

			/* indexを検索 */
			AtomicInteger index = new AtomicInteger(-1);
			DevicveInfoModel findit = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item->item.mSeekerId==deviceInfo.getSeekerId()).findAny().orElse(null);
			if(findit == null) return UWS_NG_DEVICE_NOTFOUND;	/* 対象外デバイスが存在しない。ありえないはず。 */

			/* 呼び元で実行する */
//		if (notify) notifyDataSetChanged();

			/* indexを返却 */
			return index.get();
		}
		else {
			/* 新データ判定 */
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
//			device.mIsBuoy			= deviceInfo.getIsBuoy();		更新しない

			/* 呼び元で実行する */
//		if (notify) notifyDataSetChanged();

			return idx.get();
		}
	}

	public int updDeviceInfo(UwsInfo uwsInfo) {
		AtomicInteger index = new AtomicInteger(-1);
		DevicveInfoModel device = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item->item.mSeekerId==uwsInfo.getSeekerId()).findAny().orElse(null);
		if(device == null) return UWS_NG_DEVICE_NOTFOUND;	/* 対象外デバイスが存在しない。ありえないはず。 */

		device.mDatetime = uwsInfo.getDate();
		device.mSeekerId = uwsInfo.getSeekerId();
		device.mLongitude= uwsInfo.getLongitude();
		device.mLatitude = uwsInfo.getLatitude();
		device.mHertBeat = uwsInfo.getHeartbeat();
		return index.get();
	}

	public void clearDeviceInfo() {
		mDeviceList.clear();
//		notifyDataSetChanged(); UIスレッドで実行する必要がある。
	}

	public void clearDeviceWithoutAppliciated() {
		mDeviceList.removeIf(item -> item.mSeekerId == -1);
	}

	public int setSelected(short seekerid, boolean isChecked) {
		AtomicInteger index = new AtomicInteger(-1);
		DevicveInfoModel device = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item->item.mSeekerId==seekerid).findAny().orElse(null);
		if(device == null) return UWS_NG_DEVICE_NOTFOUND;	/* 対象外デバイスが存在しない。ありえないはず。 */

		device.mSelected = isChecked;
		return index.get();
	}

	public boolean isSelected(short seekerid) {
		DevicveInfoModel device = mDeviceList.stream().filter(item->item.mSeekerId==seekerid).findAny().orElse(null);
		return (device!=null) && device.mSelected;
	}

	public boolean getChecked(short seekerid) {
		DevicveInfoModel device = mDeviceList.stream().filter(item->item.mSeekerId==seekerid).findAny().orElse(null);
		return (device==null) ? false : device.mSelected;
	}

	public void setBuoy(short seekerid, boolean isChecked) {
		/* 以前のBuoyは無効化 */
		DevicveInfoModel oldbuoy = mDeviceList.stream().filter(item->item.mIsBuoy).findAny().orElse(null);
		if(oldbuoy!=null)
			oldbuoy.mIsBuoy = false;

		/* 今回のBuoyを有効化 */
		DevicveInfoModel nowbuoy = mDeviceList.stream().filter(item->item.mSeekerId==seekerid).findAny().orElse(null);
		if(nowbuoy!=null)
			nowbuoy.mIsBuoy = isChecked;
	}

	public String getAddress(short seekerid) {
		DevicveInfoModel di = mDeviceList.stream().filter(item->item.mSeekerId==seekerid).findAny().orElse(null);
		return di==null ? "" : di.mDeviceAddress;
	}
}
