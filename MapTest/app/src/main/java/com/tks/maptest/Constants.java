package com.tks.maptest;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Constants {
	/* ベース位置(小城消防署(33.29333107719108, 130.19189394973347)) */
	public final static double	UWS_LOC_BASE_LONGITUDE	= 130.19153989612116;
	public final static double	UWS_LOC_BASE_LATITUDE	= 33.29307995564407;
//	public final static double	UWS_LOC_BASE_DISTANCE_X = 40000*1000/*4万*1000m*/ * Math.cos(UWS_LOC_BASE_LATITUDE*180/Math.PI) / 360;	/* 小城消防署付近の経度1°当たりの距離[m] */
	public final static double	UWS_LOC_BASE_DISTANCE_X = 40000*1000/*4万*1000m*/ * Math.cos(UWS_LOC_BASE_LATITUDE*Math.PI/180) / 360;	/* 小城消防署付近の経度1°当たりの距離[m] */
	public final static double	UWS_LOC_BASE_DISTANCE_Y = 40000*1000/*4万*1000m*/ / 360.0;												/* 赤道     付近の経度1°当たりの距離[m] */

	private final static SimpleDateFormat df = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss.SSSXXX", Locale.JAPAN);
	public static String d2Str(double val) {
		return String.format(Locale.JAPAN, "%1$.12f", val);
//		return String.format(Locale.JAPAN, "%.10f", val);
	}
	public static String d2Str(Date val) {
		return df.format(val);
	}
	public static String d2Str(Long val) {
		return df.format(new Date(val));
	}
}
