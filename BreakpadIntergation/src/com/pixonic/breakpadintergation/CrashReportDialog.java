package com.pixonic.breakpadintergation;

import com.pixonic.breakpabintergation.R;

import android.os.Bundle;
import android.app.Activity;
import android.view.Menu;

public class CrashReportDialog extends Activity
{

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_crash_report_dialog);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_crash_report_dialog, menu);
		return true;
	}

}
