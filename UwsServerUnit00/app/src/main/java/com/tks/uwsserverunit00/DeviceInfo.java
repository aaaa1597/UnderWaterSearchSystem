package com.tks.uwsserverunit00;

import android.os.Parcel;
import android.os.Parcelable;

public class DeviceInfo implements Parcelable {
	private final String	mShortUuid;
	private final int		mSeekerId;
	private final String	mDeviceName;
	private final String	mDeviceAddress;
	private final int		mDeviceRssi;
	private final boolean	mIsApplicable;
	private final boolean	mIsReading;
	private final double	mLongitude;
	private final double	mLatitude;

	public DeviceInfo(String shortuuid, int seekerid, String devicename, String deviceaddress, int devicerssi, boolean isApplicable, boolean isReading, double longitude, double latitude) {
		mShortUuid		= shortuuid;
		mSeekerId		= seekerid;
		mDeviceName		= devicename;
		mDeviceAddress	= deviceaddress;
		mDeviceRssi		= devicerssi;
		mIsApplicable	= isApplicable;
		mIsReading		= isReading;
		mLongitude		= longitude;
		mLatitude		= latitude;
	}

	protected DeviceInfo(Parcel in) {
		mShortUuid		= in.readString();
		mSeekerId		= in.readInt();
		mDeviceName		= in.readString();
		mDeviceAddress	= in.readString();
		mDeviceRssi		= in.readInt();
		mIsApplicable	= in.readByte() != 0x00;
		mIsReading		= in.readByte() != 0x00;
		mLongitude		= in.readDouble();
		mLatitude		= in.readDouble();
	}

	public static final Creator<DeviceInfo> CREATOR = new Creator<DeviceInfo>() {
		@Override
		public DeviceInfo createFromParcel(Parcel in) {
			return new DeviceInfo(in);
		}

		@Override
		public DeviceInfo[] newArray(int size) {
			return new DeviceInfo[size];
		}
	};

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(mShortUuid);
		dest.writeInt(mSeekerId);
		dest.writeString(mDeviceName);
		dest.writeString(mDeviceAddress);
		dest.writeInt(mDeviceRssi);
		dest.writeByte((byte)(mIsApplicable?0x01:0x00));
		dest.writeByte((byte)(mIsReading?0x01:0x00));
		dest.writeDouble(mLongitude);
		dest.writeDouble(mLatitude);
	}

	public String	getShortUuid()		{ return mShortUuid;}
	public int		getSeekerId()		{ return mSeekerId;}
	public String	getDeviceName()		{ return mDeviceName;}
	public String	getDeviceAddress()	{ return mDeviceAddress;}
	public int		getDeviceRssi()		{ return mDeviceRssi;}
	public boolean	isApplicable()		{ return mIsApplicable;}
	public boolean	isReading()			{ return mIsReading;}
	public double	getLongitude()		{ return mLongitude;}
	public double	getLatitude()		{ return mLatitude;}
}
