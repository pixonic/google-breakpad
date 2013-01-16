/**
 * Copyright (c) 2013 Pixonic.
 * All rights reserved.
 */
package com.pixonic.breakpabintergation;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Looper;
import android.util.Log;

/**
 *
 */
public class CrashHandler
{
	private static final String TAG = "CrashHandler";
	private static CrashHandler msSingletonInstance;

	private final Activity mActivity;

	private ProgressDialog mSendCrashReportDialog;

	public static void init(Activity activity)
	{
		if(msSingletonInstance == null)
		{
			msSingletonInstance = new CrashHandler(activity);
		}
	}

	private CrashHandler(Activity activity)
	{
		mActivity = activity;
	}

	/**
	 * A signal handler in native code has been triggered. As our last gasp,
	 * launch the crash handler (in its own process), because when we return
	 * from this function the process will soon exit.
	 */
	static public void nativeCrashed(String dumpFile)
	{
		if(msSingletonInstance != null)
		{
			msSingletonInstance.onCrashed(dumpFile);
		}
		else
		{
			new RuntimeException("crashed here (native trace should follow after the Java trace)").printStackTrace();
		}
	}

	private void onCrashed(String dumpFile)
	{
		try
		{
			createUploadPromtAlert(dumpFile);
			synchronized(this)
			{
				// lock crashed thread				
				wait();
			}
		}
		catch(final Throwable t)
		{
			Log.e(TAG, "Error.", t);
		}
	}

	private void createUploadPromtAlert(final String dumpFile)
	{
		new Thread(new Runnable()
		{			
			@Override
			public void run()
			{
				// create looper 
				Looper.prepare();
				createUploadPromtAlertImpl(dumpFile);
				
				Looper.loop();
			}
		}).start();
	}

	private void createUploadPromtAlertImpl(final String dumpFile)
	{
		AlertDialog.Builder builder = new AlertDialog.Builder(mActivity);
		builder.setTitle(R.string.promt_title);
		builder.setMessage(R.string.promt_message);

		builder.setPositiveButton(R.string.button_send, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				sendCrashReport(dumpFile);
			}
		});

		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener()
		{
			@Override
			public void onClick(DialogInterface dialog, int which)
			{
				Looper.myLooper().quit();
				
				// release crashed thread
				CrashHandler.this.notify();
				dialog.dismiss();
			}
		});

		builder.setCancelable(true);
		builder.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				Looper.myLooper().quit();
				
				// release crashed thread
				CrashHandler.this.notify();
				dialog.dismiss();
			}
		});

		builder.show();
	}

	private void sendCrashReport(final String dumpFile)
	{
		mSendCrashReportDialog = new ProgressDialog(mActivity);
		mSendCrashReportDialog.setMax(100);
		mSendCrashReportDialog.setTitle(R.string.sending_crash_report);
		mSendCrashReportDialog.show();
	}
}
