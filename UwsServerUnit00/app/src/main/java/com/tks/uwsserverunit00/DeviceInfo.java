package com.tks.uwsserverunit00;

import android.os.Parcel;
import android.os.Parcelable;
import android.provider.ContactsContract;

import java.util.Date;

public class DeviceInfo implements Parcelable {
	private final Date		mDatetime;
	private final short		mSeekerId;
	private final String	mDeviceName;
	private final String	mDeviceAddress;
	private final int		mDeviceRssi;
	private final double	mLongitude;
	private final double	mLatitude;
	private final short		mHeartbeat;

	public DeviceInfo(Date datetime, short seekerid, String devicename, String deviceaddress, int devicerssi, double longitude, double latitude, short heartbeat) {
		mDatetime		= datetime;
		mSeekerId		= seekerid;
		mDeviceName		= devicename;
		mDeviceAddress	= deviceaddress;
		mDeviceRssi		= devicerssi;
		mLongitude		= longitude;
		mLatitude		= latitude;
		mHeartbeat		= heartbeat;
	}

	protected DeviceInfo(Parcel in) {
		String tmp1, tmp2;
		mDatetime		= new Date(in.readLong());
		mSeekerId		= (short)in.readInt();
		tmp1			= in.readString();
		if(tmp1.equals(""))	tmp1 = null;
		mDeviceName		= tmp1;
		tmp2			= in.readString();
		if(tmp2.equals(""))	tmp2 = null;
		mDeviceAddress	= tmp2;
		mDeviceRssi		= in.readInt();
		mLongitude		= in.readDouble();
		mLatitude		= in.readDouble();
		mHeartbeat		= (short)in.readInt();
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
		dest.writeLong(mDatetime.getTime());
		dest.writeInt(mSeekerId);
		dest.writeString(mDeviceName==null?"":mDeviceName);
		dest.writeString(mDeviceAddress==null?"":mDeviceAddress);
		dest.writeInt(mDeviceRssi);
		dest.writeDouble(mLongitude);
		dest.writeDouble(mLatitude);
		dest.writeInt(mHeartbeat);
	}

	public Date		getDate()			{ return mDatetime;}
	public short	getSeekerId()		{ return mSeekerId;}
	public String	getDeviceName()		{ return mDeviceName;}
	public String	getDeviceAddress()	{ return mDeviceAddress;}
	public int		getDeviceRssi()		{ return mDeviceRssi;}
	public double	getLongitude()		{ return mLongitude;}
	public double	getLatitude()		{ return mLatitude;}
	public short	getHeartbeat()		{ return mHeartbeat;}
}
