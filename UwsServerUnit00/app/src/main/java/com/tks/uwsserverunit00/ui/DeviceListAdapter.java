package com.tks.uwsserverunit00.ui;

import android.Manifest;
import android.bluetooth.BluetoothDevice;
import android.content.Context;
import android.content.pm.PackageManager;
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
import androidx.core.app.ActivityCompat;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.tks.uwsserverunit00.DeviceInfo;
import com.tks.uwsserverunit00.R;
import com.tks.uwsserverunit00.UwsInfo;

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
		TextView	mtxtDatetime;
		TextView	mTxtSeekerId;
		TextView	mTxtDeviceName;
		TextView	mTxtDeviceNameAddress;
		ImageView	mImvConnectStatus;
		TextView	mTxtConnectStatus;
		TextView	mTxtHertBeat;
		ImageButton	mBtnBuoy;
		TextView	mTxtLongitude;
		TextView	mTxtLatitude;
		ViewHolder(View view) {
			super(view);
			mtxtDatetime			= view.findViewById(R.id.txtDatetime);
			mTxtSeekerId			= view.findViewById(R.id.txtSeekerId);
			mTxtDeviceName			= view.findViewById(R.id.txtDeeviceName);
			mTxtDeviceNameAddress	= view.findViewById(R.id.txtDeviceAddress);
			mImvConnectStatus		= view.findViewById(R.id.imvConnectStatus);
			mTxtConnectStatus		= view.findViewById(R.id.txtConnectStatus);
			mTxtHertBeat			= view.findViewById(R.id.txtHertBeat);
			mBtnBuoy				= view.findViewById(R.id.btnBuoy);
			mTxtLongitude			= view.findViewById(R.id.txtLongitude);
			mTxtLatitude			= view.findViewById(R.id.txtLatitude);
		}
	}

	/* インターフェース */
	public interface OnCheckedChangeListener {
		void onCheckedChanged(short seekerid, boolean isChecked);
	}

	public interface OnSetBuoyListener {
		void onSetBuoyListener(short seekerid, boolean isChecked);
	}

	/* メンバ変数 */
	private final List<DeviceInfoModel>		mDeviceList;
	private final Context					mContext;
	private final OnCheckedChangeListener	mOnCheckedChangeListener;
	private final OnSetBuoyListener			mOnSetBuoyListener;

	/* コンストラクタ */
	public DeviceListAdapter(Context context, List<DeviceInfoModel> list, OnCheckedChangeListener lisner, OnSetBuoyListener lisner2) {
		mDeviceList				= list;
		mContext				= context;
		mOnCheckedChangeListener= lisner;
		mOnSetBuoyListener		= lisner2;
	}

	public static class DeviceInfoModel {
		public Date		mDatetime;
		public short	mSeekerId;
		public String	mDeviceName;
		public String	mDeviceAddress;
		public double	mLongitude;
		public double	mLatitude;
		public short	mHertBeat;
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
		final short seekerid = model.mSeekerId;
		final String deviceName = model.mDeviceName;
		final String deviceAddress = model.mDeviceAddress;
		final int constsresid = seekerid == -1 ? R.drawable.statusx_na :
				model.mSelected ? R.drawable.status5_ready : R.drawable.status0_none;
		holder.mtxtDatetime.setText(d2Str(model.mDatetime));
		holder.mTxtSeekerId.setText((seekerid == -1) ? " - " : String.valueOf(seekerid));
		holder.mTxtDeviceName.setText(TextUtils.isEmpty(deviceName) ? "" : deviceName);
		holder.mTxtDeviceNameAddress.setText(TextUtils.isEmpty(deviceAddress) ? "" : deviceAddress);
		holder.mImvConnectStatus.setImageResource(constsresid);
//		holder.mTxtConnectStatus.setText(statusinfo.first);
//		holder.mTxtConnectStatus.setTextColor(statusinfo.second);
		holder.mTxtHertBeat.setText(model.mHertBeat == 0 ? "-" : "" + model.mHertBeat);
//		holder.mBtnBuoy.setOnClickListener(null);
//		holder.mBtnBuoy.setValue(true);
		if(seekerid == -1) {
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
		holder.mTxtLatitude.setText(String.valueOf(model.mLatitude));
	}

	@Override
	public int getItemCount() {
		return mDeviceList.size();
	}

	public void setList(List<DeviceInfoModel> devices) {
		mDeviceList.addAll(devices);
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
}
