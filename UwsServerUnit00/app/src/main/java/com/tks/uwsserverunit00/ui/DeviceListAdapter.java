package com.tks.uwsserverunit00.ui;

import android.location.Location;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.tks.uwsserverunit00.R;

import static com.tks.uwsserverunit00.Constants.BT_NORTIFY_SEEKERID;
import static com.tks.uwsserverunit00.Constants.ERR_DEVICE_NOTFOUND;
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
		LinearLayout mllRow;
		TextView	mtxtDatetime;
		TextView	mTxtSeekerId;
		TextView	mTxtDeviceName;
		TextView	mTxtDeviceNameAddress;
		ImageView	mImvConnectStatus;
		TextView	mTxtStatus;
		TextView	mTxtHertBeat;
		ImageView	mImvBuoy;
		TextView	mTxtLongitude;
		TextView	mTxtLatitude;
		ViewHolder(View view) {
			super(view);
			mllRow					= view.findViewById(R.id.ll_row);
			mtxtDatetime			= view.findViewById(R.id.txtDatetime);
			mTxtSeekerId			= view.findViewById(R.id.txtSeekerId);
			mTxtDeviceName			= view.findViewById(R.id.txtDeeviceName);
			mTxtDeviceNameAddress	= view.findViewById(R.id.txtDeviceAddress);
			mImvConnectStatus		= view.findViewById(R.id.imvConnectStatus);
			mTxtStatus				= view.findViewById(R.id.txtStatus);
			mTxtHertBeat			= view.findViewById(R.id.txtHertBeat);
			mImvBuoy				= view.findViewById(R.id.imvBuoy);
			mTxtLongitude			= view.findViewById(R.id.txtLongitude);
			mTxtLatitude			= view.findViewById(R.id.txtLatitude);
		}
	}

	/* インターフェース */
	public interface OnSelectedChangeListener {
		void onSelectedChanged(short seekerid, boolean isChecked);
	}

	public interface OnSetBuoyListener {
		void onSetBuoyListener(short seekerid, boolean isChecked);
	}

	/* メンバ変数 */
	private final List<DeviceInfoModel>		mDeviceList;
	private final OnSelectedChangeListener	mOnSelectedChangeListener;
	private final OnSetBuoyListener			mOnSetBuoyListener;

	/* コンストラクタ */
	public DeviceListAdapter(List<DeviceInfoModel> list, OnSelectedChangeListener lisner, OnSetBuoyListener lisner2) {
		mDeviceList				= list;
		mOnSelectedChangeListener= lisner;
		mOnSetBuoyListener		= lisner2;
	}

	public static class DeviceInfoModel {
		public Date		mDatetime;
		public short	mSeekerId;
		public String	mDeviceName;
		public String	mDeviceAddress;
		public int		mStatusResId;
		public double	mLongitude;
		public double	mLatitude;
		public short	mHertBeat;
		public boolean	mConnected;
		public boolean	mSelected;
		public boolean	mIsBuoy;
	}

	@NonNull
	@Override
	public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
		View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.listitem_devices, parent, false);
		return new ViewHolder(view);
	}

	@Override
	public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
		DeviceInfoModel model = mDeviceList.get(position);
		final short seekerid		= model.mSeekerId;
		final String deviceName		= model.mDeviceName;
		final String deviceAddress	= model.mDeviceAddress;
		final int constsresid		= (!model.mConnected)? R.drawable.statusx_waitforconnect :
									    model.mSelected  ? R.drawable.status5_ready : R.drawable.status0_none;
		if( !model.mConnected) {
			holder.mllRow.setOnClickListener(null);
			holder.mImvBuoy.setOnClickListener(null);
			holder.mImvBuoy.setImageResource(R.drawable.buoy_disable);
		}
		else {
			/* メンバ決定 */
			holder.mllRow.setOnClickListener(view -> {
				model.mSelected = !model.mSelected;
				mOnSelectedChangeListener.onSelectedChanged(seekerid, model.mSelected);
				if(model.mSelected)
					holder.mImvConnectStatus.setImageResource(R.drawable.status5_ready);
				else
					holder.mImvConnectStatus.setImageResource(R.drawable.status0_none);
			});
			/* 浮標設定 */
			if(model.mIsBuoy)	holder.mImvBuoy.setImageResource(R.drawable.buoy_enable);
			else				holder.mImvBuoy.setImageResource(R.drawable.buoy_disable);
			holder.mImvBuoy.setOnClickListener(view -> {
				model.mIsBuoy = !model.mIsBuoy;
				mOnSetBuoyListener.onSetBuoyListener(seekerid, model.mIsBuoy);
				if(model.mIsBuoy)	holder.mImvBuoy.setImageResource(R.drawable.buoy_enable);
				else				holder.mImvBuoy.setImageResource(R.drawable.buoy_disable);
			});
		}
		holder.mtxtDatetime.setText(d2Str(model.mDatetime));
		holder.mTxtSeekerId.setText((seekerid == -1) ? " - " : String.valueOf(seekerid));
		holder.mTxtDeviceName.setText(TextUtils.isEmpty(deviceName) ? "" : deviceName);
		holder.mTxtDeviceNameAddress.setText(TextUtils.isEmpty(deviceAddress) ? "" : deviceAddress);
		holder.mImvConnectStatus.setImageResource(constsresid);
		holder.mTxtStatus.setText(model.mStatusResId);
		holder.mTxtHertBeat.setText((model.mHertBeat<0) ? "-" : "" + model.mHertBeat);
		holder.mTxtLongitude.setText(d2Str(model.mLongitude));
		holder.mTxtLatitude.setText(d2Str(model.mLatitude));
	}

	@Override
	public int getItemCount() {
		return mDeviceList.size();
	}

	public int setSelected(short seekerid, boolean isChecked) {
		AtomicInteger index = new AtomicInteger(-1);
		DeviceInfoModel device = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item->item.mSeekerId==seekerid).findAny().orElse(null);
		if(device == null) return ERR_DEVICE_NOTFOUND;	/* 対象外デバイスが存在しない。ありえないはず。 */

		device.mSelected = isChecked;
		return index.get();
	}

	public boolean isSelected(short seekerid) {
		DeviceInfoModel device = mDeviceList.stream().filter(item->item.mSeekerId==seekerid).findAny().orElse(null);
		return (device!=null) && device.mSelected;
	}

	public void setBuoy(short seekerid, boolean isChecked) {
		/* 以前のBuoyは無効化 */
		DeviceInfoModel oldbuoy = mDeviceList.stream().filter(item->item.mIsBuoy).findAny().orElse(null);
		if(oldbuoy!=null)
			oldbuoy.mIsBuoy = false;

		/* 今回のBuoyを有効化 */
		DeviceInfoModel nowbuoy = mDeviceList.stream().filter(item->item.mSeekerId==seekerid).findAny().orElse(null);
		if(nowbuoy!=null)
			nowbuoy.mIsBuoy = isChecked;
	}

	/* 脈拍更新 */
	public void setHeartBeat(String name, String addr, long datetime, short hearbeat) {
		AtomicInteger index = new AtomicInteger(-1);
		DeviceInfoModel device = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item->item.mDeviceAddress.equals(addr)).findAny().orElse(null);
		if(device != null) {
			device.mHertBeat = hearbeat;
			notifyItemChanged(index.get());
		}
	}

	/* 経度/緯度更新 */
	public void setLocation(String name, String addr, long datetime, Location loc) {
		AtomicInteger index = new AtomicInteger(-1);
		DeviceInfoModel device = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item->item.mDeviceAddress.equals(addr)).findAny().orElse(null);
		if(device != null) {
			device.mLongitude= loc.getLongitude();
			device.mLatitude = loc.getLatitude();
			notifyItemChanged(index.get());
		}
	}

	/* 状態変化通知 */
	public void OnChangeStatus(String name, String addr, int resourceid) {
		AtomicInteger index = new AtomicInteger(-1);
		DeviceInfoModel device = mDeviceList.stream().peek(x->index.incrementAndGet()).filter(item->item.mDeviceAddress.equals(addr)).findAny().orElse(null);

		if(device != null) {
			if(name.equals(BT_NORTIFY_SEEKERID)) {
				device.mSeekerId = (short)resourceid;
			}
			else {
				device.mStatusResId = resourceid;
				device.mConnected= true;
				notifyItemChanged(index.get());
			}
		}
	}
}
