package com.sample.testfragswitchtab2.ui.main;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentPagerAdapter;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.sample.testfragswitchtab2.R;
import com.sample.testfragswitchtab2.ui.Frag1Init;
import com.sample.testfragswitchtab2.ui.Frag2Search;

/**
 * A [FragmentPagerAdapter] that returns a fragment corresponding to
 * one of the sections/tabs/pages.
 */
public class FragmentFactoryAdapter extends FragmentStateAdapter {
	public FragmentFactoryAdapter(FragmentActivity activity) {
		super(activity);
	}

	@NonNull
	@Override
	public Fragment createFragment(int idx) {
		switch(idx) {
			case 0:
				return Frag1Init.newInstance();
			case 1:
				return Frag2Search.newInstance();
		}

		/* デフォルトは検索画面 */
		return Frag2Search.newInstance();
	}

	@Override
	public int getItemCount() {
		return 2;	/* 初期設定と検索画面の2つ */
	}
}