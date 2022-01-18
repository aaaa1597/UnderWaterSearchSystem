package com.tks.uwsserverunit00;

import android.os.Parcel;
import android.os.Parcelable;
import java.util.Date;

public class UwsInfo implements Parcelable {
	private final Date		mDatetime;
	private final short		mSeekerId;
	private final double	mLongitude;
	private final double	mLatitude;
	private final short		mHeartbeat;

	public UwsInfo(DeviceInfo deviceInfo) {
		mDatetime		= deviceInfo.getDate();
		mSeekerId		= deviceInfo.getSeekerId();
		mLongitude		= deviceInfo.getLongitude();
		mLatitude		= deviceInfo.getLatitude();
		mHeartbeat		= deviceInfo.getHeartbeat();
	}

	public UwsInfo(Date datetime, short seekerid, double longitude, double latitude, short heartbeat) {
		mDatetime		= datetime;
		mSeekerId		= seekerid;
		mLongitude		= longitude;
		mLatitude		= latitude;
		mHeartbeat		= heartbeat;
	}

	protected UwsInfo(Parcel in) {
		mDatetime		= new Date(in.readLong());
		mSeekerId		= (short)in.readInt();
		mLongitude		= in.readDouble();
		mLatitude		= in.readDouble();
		mHeartbeat		= (short)in.readInt();
	}

	public static final Creator<UwsInfo> CREATOR = new Creator<UwsInfo>() {
		@Override
		public UwsInfo createFromParcel(Parcel in) {
			return new UwsInfo(in);
		}

		@Override
		public UwsInfo[] newArray(int size) {
			return new UwsInfo[size];
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
		dest.writeDouble(mLongitude);
		dest.writeDouble(mLatitude);
		dest.writeInt(mHeartbeat);
	}

	public Date		getDate()			{ return mDatetime;}
	public short	getSeekerId()		{ return mSeekerId;}
	public double	getLongitude()		{ return mLongitude;}
	public double	getLatitude()		{ return mLatitude;}
	public short	getHeartbeat()		{ return mHeartbeat;}
}
