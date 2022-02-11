package com.tks.uwsclient;

import android.app.AlertDialog;
import android.content.Context;
import android.util.Log;
import java.text.MessageFormat;

public class ErrDialog {
	/* AlertDialog生成 */
	public static AlertDialog.Builder create(Context context, String ErrStr) {
		StackTraceElement throwableStackTraceElement = new Throwable().getStackTrace()[1];
		String head = MessageFormat.format("{0}::{1}({2})", throwableStackTraceElement.getClassName(), throwableStackTraceElement.getMethodName(), throwableStackTraceElement.getLineNumber());
		TLog.d("{0} {1}", head, ErrStr);

		return new AlertDialog.Builder(context)
				.setMessage(ErrStr)
				.setNegativeButton(R.string.error_end, (dialog, id) -> {
					android.os.Process.killProcess(android.os.Process.myPid());
				});
	}

	/* AlertDialog生成 */
	public static AlertDialog.Builder create(Context context, int resid) {
		return create(context, context.getString(resid));
	}
}
