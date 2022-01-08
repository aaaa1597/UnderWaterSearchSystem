package com.tks.uwsclientwearos;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.database.CursorWindowCompat;
import androidx.lifecycle.ViewModelProvider;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.widget.TextView;
import com.tks.uwsclientwearos.databinding.ActivityMainBinding;
import com.tks.uwsclientwearos.ui.FragMainViewModel;

import java.util.Arrays;

public class MainActivity extends AppCompatActivity {
	private	FragMainViewModel	mViewModel;
	private final static int	REQUEST_PERMISSIONS	= 2222;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(this).get(FragMainViewModel.class);

		/* Wifi権限が許可されていない場合はリクエスト. */
		if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
			requestPermissions(new String[]{Manifest.permission.CHANGE_NETWORK_STATE,
					Manifest.permission.ACCESS_NETWORK_STATE,
					Manifest.permission.ACCESS_WIFI_STATE,
					Manifest.permission.CHANGE_WIFI_STATE,
					Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_PERMISSIONS);
		}
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);

		/* 対象外なので、無視 */
		if (requestCode != REQUEST_PERMISSIONS) return;

		/* 権限リクエストの結果を取得する. */
		long ngcnt = Arrays.stream(grantResults).filter(value -> value != PackageManager.PERMISSION_GRANTED).count();
		if (ngcnt > 0) {
			ErrDialog.create(MainActivity.this, "このアプリには必要な権限です。\n再起動後に許可してください。\n終了します。").show();
			return;
		}
		else {
//			/* Bleサーバへの接続処理開始 */
//			TLog.d("bindService(cb={0})", mCon);
//			mViewModel.bindBleService(getApplicationContext(), mCon);
		}
	}

}