package com.tks.uwsclientwearos.ui;

import static android.content.Context.WIFI_P2P_SERVICE;
import androidx.core.app.ActivityCompat;
import androidx.lifecycle.ViewModelProvider;
import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.net.NetworkInfo;
import android.net.wifi.p2p.WifiP2pDevice;
import android.net.wifi.p2p.WifiP2pInfo;
import android.net.wifi.p2p.WifiP2pManager;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import com.tks.uwsclientwearos.ErrDialog;
import com.tks.uwsclientwearos.R;
import com.tks.uwsclientwearos.TLog;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class FragMain extends Fragment {
	private FragMainViewModel		mViewModel;
	private WifiP2pManager			mWifiP2pManager;
	private WifiP2pManager.Channel	mChannel;

	@Nullable
	@Override
	public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
		return inflater.inflate(R.layout.frag_main, container, false);
	}

	@Override
	public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		/* ViewModelインスタンス取得 */
		mViewModel = new ViewModelProvider(requireActivity()).get(FragMainViewModel.class);

		/* SeekerIDのlistView定義 */
		RecyclerView recyclerView = getActivity().findViewById(R.id.rvw_seekerid);
		recyclerView.setLayoutManager(new LinearLayoutManager(getActivity().getApplicationContext(), LinearLayoutManager.VERTICAL, false));
		recyclerView.setAdapter(new SeekerIdAdapter());
		LinearSnapHelper linearSnapHelper = new LinearSnapHelper();
		linearSnapHelper.attachToRecyclerView(recyclerView);
		/* SeekerIDのlistView(子の中心で収束する設定) */
		recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
			@Override
			public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
				super.onScrollStateChanged(recyclerView, newState);
				if(newState == RecyclerView.SCROLL_STATE_IDLE) {
					View lview = linearSnapHelper.findSnapView(recyclerView.getLayoutManager());
					int pos =  recyclerView.getChildAdapterPosition(lview);
					TLog.d("aaaaa pos={0}", pos);
//					mViewModel.setSeekerID(pos);
				}
			}
		});

		/* インテントフィルタ設定 */
		IntentFilter ifr = new IntentFilter();
		ifr.addAction(WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION);
		ifr.addAction(WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION);
		ifr.addAction(WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION);
		ifr.addAction(WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION);
		getActivity().registerReceiver(mBroadcastReceiver, ifr);

		/* TODO : スマートウォッチ(TicWatch E2)だとここは無効になって動かんかった。 */
		mWifiP2pManager = (WifiP2pManager)getActivity().getSystemService(WIFI_P2P_SERVICE);
		if(mWifiP2pManager==null)
			ErrDialog.create(getActivity(), "Wifi Direct未サポートの端末です。\n終了します。").show();
		else
			mChannel = mWifiP2pManager.initialize(getActivity().getApplicationContext(), getActivity().getMainLooper(), () -> { TLog.d("onChannelDisconnected() Channelからの切断を検知."); });
	}

	BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action != null) {
				switch (action) {
					/* Wifi P2P ON/OFF */
					case WifiP2pManager.WIFI_P2P_STATE_CHANGED_ACTION: {
						int state = intent.getIntExtra(WifiP2pManager.EXTRA_WIFI_STATE, -100);
						if (state == WifiP2pManager.WIFI_P2P_STATE_ENABLED) {
//							mDirectActionListener.wifiP2pEnabled(true);
							TLog.d("Wifi ON.");
						}
						else {
							TLog.d("Wifi OFF.");
//							mDirectActionListener.wifiP2pEnabled(false);
//							List<WifiP2pDevice> wifiP2pDeviceList = new ArrayList<>();
//							mDirectActionListener.onPeersAvailable(wifiP2pDeviceList);
						}
						break;
					}
					/* ピアリスト変更 */
					case WifiP2pManager.WIFI_P2P_PEERS_CHANGED_ACTION: {
						if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
							return;
						}
//						mWifiP2pManager.requestPeers(mChannel, peers -> mDirectActionListener.onPeersAvailable(peers.getDeviceList()));
						break;
					}
					// Wifi P2P接続ステータス変更
					case WifiP2pManager.WIFI_P2P_CONNECTION_CHANGED_ACTION: {
						NetworkInfo networkInfo = intent.getParcelableExtra(WifiP2pManager.EXTRA_NETWORK_INFO);
						if (networkInfo != null && networkInfo.isConnected()) {
//							mWifiP2pManager.requestConnectionInfo(mChannel, new WifiP2pManager.ConnectionInfoListener() {
//								@Override
//								public void onConnectionInfoAvailable(WifiP2pInfo info) {
//									mDirectActionListener.onConnectionInfoAvailable(info);
//								}
//							});
							TLog.e("接続OK.");
						} else {
//							mDirectActionListener.onDisconnection();
							TLog.e("切断.");
						}
						break;
					}
					//このデバイスのデバイス情報 変更
					case WifiP2pManager.WIFI_P2P_THIS_DEVICE_CHANGED_ACTION: {
						WifiP2pDevice wifiP2pDevice = intent.getParcelableExtra(WifiP2pManager.EXTRA_WIFI_P2P_DEVICE);
						TLog.e("このデバイスのデバイス情報 変更.wifiP2pDevice={0}", wifiP2pDevice);
//						mDirectActionListener.onSelfDeviceAvailable(wifiP2pDevice);
						break;
					}
				}
			}
		}
	};
}
