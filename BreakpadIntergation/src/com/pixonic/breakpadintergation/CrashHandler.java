/**
 * Copyright (c) 2013 Pixonic.
 * All rights reserved.
 */
package com.pixonic.breakpadintergation;

import java.io.File;

import java.util.HashMap;
import java.util.Map;

import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.Header;
import org.apache.http.HttpResponse;

import com.pixonic.breakpadintergation.R;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.os.Handler;
import android.os.Build;
import android.os.Looper;
import android.util.Log;

import org.json.JSONObject;

/**
 *
 */
public class CrashHandler
{
	private static final String TAG = "CrashHandler";
	private static CrashHandler msSingletonInstance;

	private Activity mActivity;

	private ProgressDialog mSendCrashReportDialog;
	private static String msApplicationName = null;
	
	private static HashMap< String, String > optionalFilesToSend = null;
	private static JSONObject optionalParameters = null;

	public static void init(Activity activity)
	{
		if(msSingletonInstance == null)
		{
			msSingletonInstance = new CrashHandler(activity);
		}
		else
		{
			msSingletonInstance.mActivity = activity;
		}
	}

	private CrashHandler(Activity activity)
	{
		mActivity = activity;
		if(msApplicationName == null)
		{
			msApplicationName = mActivity.getApplicationContext().getPackageName();
		}

		nativeInit(mActivity.getFilesDir().getAbsolutePath());
	}

	/**
	 * Sets a name of the application
	 * 
	 * @param appName
	 *            application name
	 */
	public static void setApplicationName(String appName)
	{
		assert (appName != null);
		msApplicationName = appName;
	}

	///  Sets additional file to send to server with `file`
	///  File path is relative to 
	public static void includeFile(String name, String file)
	{
		if(optionalFilesToSend == null)
			optionalFilesToSend = new HashMap< String, String >();
			
		optionalFilesToSend.put(name, file);
	}
	
	///  Sets additional request data for dump as json object `params`
	public static void includeJsonData(JSONObject params)
	{
		optionalParameters = params;
	}

	///  NATIVE IMPLEMENTATION GLUE  ///

	private native void nativeInit(String path);

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

		RuntimeException exception = new RuntimeException(
				"crashed here (native trace should follow after the Java trace)");
		exception.printStackTrace();
		throw exception;
	}

	///  CRASH HANDLING PROCESS  ///

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

		Log.i(TAG, "exit");
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
				CrashHandler.this.onCancelDialog(dialog);
			}
		});

		builder.setCancelable(true);
		builder.setOnCancelListener(new DialogInterface.OnCancelListener()
		{
			@Override
			public void onCancel(DialogInterface dialog)
			{
				CrashHandler.this.onCancelDialog(dialog);
			}
		});

		builder.show();
	}

	private void onCancelDialog(DialogInterface dialog)
	{
		dialog.dismiss();
		finish();
	}

	private void finish()
	{
		synchronized(this)
		{
			// release crashed thread
			notifyAll();
		}

		new Handler().post(new Runnable()
		{
			@Override
			public void run()
			{
				Looper.myLooper().quit();
			}
		});
	}

	private void sendCrashReport(final String dumpFile)
	{
		createSendDialog();
		sendCrashReportImpl(dumpFile);
		desptorySendDialog();
	}

	private void createSendDialog()
	{
		mSendCrashReportDialog = new ProgressDialog(mActivity);
		mSendCrashReportDialog.setMax(100);
		mSendCrashReportDialog.setMessage(mActivity.getText(R.string.sending_crash_report));
		mSendCrashReportDialog.setCancelable(false);
		mSendCrashReportDialog.show();
	}

	protected String getVersionCode()
	{
		PackageInfo pInfo = null;
		try
		{
			pInfo = mActivity.getPackageManager().getPackageInfo(mActivity.getPackageName(), 0);
		}
		catch (Throwable e)
		{
			e.printStackTrace();
			pInfo = null;
		}
		if(pInfo == null) return "UnknownVersion";
		return String.valueOf(pInfo.versionCode);
	}

	protected String getDeviceName()
	{
		String device = Build.MANUFACTURER + "," + Build.MODEL;
		
		return device.replaceAll("\\W", "_");
	}

	private void sendCrashReportImpl(final String dumpFile)
	{
		try
		{
			HttpClient httpclient = new DefaultHttpClient();
			HttpPost httppost = new HttpPost("http://dwarves-analize.pixonic.ru/breakpad.php");
			//HttpPost httppost = new HttpPost("http://192.168.0.117/index.php");

			MultipartHttpEntity mpfr = new MultipartHttpEntity();
			mpfr.addValue("device", getDeviceName());
			mpfr.addValue("version", getVersionCode());
			mpfr.addValue("product_name", msApplicationName);
			mpfr.addFile("symbol_file", dumpFile, new File(mActivity.getFilesDir().getAbsolutePath() + "/" + dumpFile));
			
			if(optionalParameters != null)
			{
				mpfr.addValue("optional", optionalParameters.toString());
			}
			
			if(optionalFilesToSend != null)
			{
				for(Map.Entry<String, String> file : optionalFilesToSend.entrySet())
				{
					File f = new File(file.getValue());
					mpfr.addFile(file.getKey(), f.getName(), f);
				}
			}
			
			mpfr.end();

			httppost.setEntity(mpfr);

			// Execute HTTP Post Request
			HttpResponse resp = httpclient.execute(httppost);

			Log.v(TAG, "request complete, code = " + String.valueOf(resp.getStatusLine().getStatusCode()) );
		}
		catch(final Throwable t)
		{
			Log.e(TAG, "failed to send file", t);
		}

	}

	private void desptorySendDialog()
	{
		final ProgressDialog dialog = mSendCrashReportDialog;
		if(dialog != null)
		{
			new Handler().post(new Runnable()
			{
				@Override
				public void run()
				{
					dialog.dismiss();
				}
			});
		}

		finish();
	}
}
