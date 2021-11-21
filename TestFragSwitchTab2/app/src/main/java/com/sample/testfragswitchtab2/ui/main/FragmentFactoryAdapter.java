package com.sample.testfragswitchtab2.ui.main;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;

import com.sample.testfragswitchtab2.ui.Frag0Init;
import com.sample.testfragswitchtab2.ui.Frag1Search;

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
				return Frag0Init.newInstance();
			case 1:
				return Frag1Search.newInstance();
		}

		/* デフォルトは検索画面 */
		return Frag1Search.newInstance();
	}

	@Override
	public int getItemCount() {
		return 2;	/* 初期設定と検索画面の2つ */
	}
}