package com.sample.testdrawer;

import androidx.appcompat.app.ActionBarDrawerToggle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.drawerlayout.widget.DrawerLayout;

import android.os.Bundle;
import android.view.Gravity;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.google.android.material.navigation.NavigationView;

import java.util.jar.Attributes;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		ListView listView = findViewById(R.id.task_list_view_inside_nav);
		DrawerListAdapter drawerlistAdapter = new DrawerListAdapter(getApplicationContext());
		for(int lpct = 0; lpct < 5; lpct++) {
			int inum = lpct;
			switch(lpct) {
				case 0: drawerlistAdapter.add(new PeripheralInfo(){{Name="aa"+inum; UUID = "aaaa-0000"+inum; resID = android.R.drawable.ic_dialog_email;}});	break;
				case 1: drawerlistAdapter.add(new PeripheralInfo(){{Name="bb"+inum; UUID = "bbbb-0000"+inum; resID = android.R.drawable.ic_dialog_map;}});	break;
				case 2: drawerlistAdapter.add(new PeripheralInfo(){{Name="cc"+inum; UUID = "cccc-0000"+inum; resID = android.R.drawable.ic_input_add;}});	break;
				case 3: drawerlistAdapter.add(new PeripheralInfo(){{Name="dd"+inum; UUID = "dddd-0000"+inum; resID = android.R.drawable.ic_input_delete;}});	break;
				case 4: drawerlistAdapter.add(new PeripheralInfo(){{Name="ee"+inum; UUID = "eeee-0000"+inum; resID = android.R.drawable.ic_lock_lock;}});	break;
			}
		}
		listView.setAdapter(drawerlistAdapter);

//		ArrayAdapter<String> arrayAdapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, lists);
//		listView.setAdapter(arrayAdapter);

		DrawerLayout naviview = findViewById(R.id.lay_drawer);
		naviview.openDrawer(Gravity.LEFT);

	}

	private static final String lists[] = { "TASK1", "TASK2", "TASK3"};

}