package org.ming.ui.activity;

import org.ming.R;
import org.ming.util.MyLogger;
import org.ming.util.Util;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.KeyEvent;

public class SplashActivity extends Activity
{
	private static final MyLogger logger = MyLogger.getLogger("SplashActivity");
	private final Handler mSplashHandler = new Handler()
	{
		// 处理匿名信息
		public void handleMessage(Message paramAnonymousMessage)
		{
			SplashActivity.logger.v("handleMessage() ---> Enter : "
					+ paramAnonymousMessage.what);
			switch (paramAnonymousMessage.what)
			{
			case 0:
			{
				SplashActivity.this.turnToMainActivity();
				SplashActivity.this.finish();
			}
			default:
				SplashActivity.logger.v("handleMessage() ---> Exit");
			}
		}
	};

	private void enterMonbileMusicMainActivty()
	{
		Message localMessage = new Message();
		localMessage.what = 0;
		this.mSplashHandler.sendMessageDelayed(localMessage, 800L);
	}

	private void turnToMainActivity()
	{
		logger.v("turnToMainActivity() ---> Enter");
		if (Util.isNeedForUserLead(this))
		{
			logger.v("startActivity------>UserLeadActivity");
			startActivity(new Intent(this, UserLeadActivity.class));
			Util.setNoNeedUserLead(this);
		} else
		{
			startActivity(new Intent(this, UserLeadActivity.class));
			// Intent localIntent = new Intent(getBaseContext(),
			// MobileMusicMainActivity.class);
			// localIntent.putExtra("TABINDEX", this.mTabIndex);
			// startActivity(localIntent);
		}
		logger.v("turnToMainActivity() ---> Exit");
	}

	@Override
	public void onCreate(Bundle paramBundle)
	{
		logger.v("onCreate() ---> Enter");
		super.onCreate(paramBundle);
		requestWindowFeature(1);
		setContentView(R.layout.splash);

		logger.v("onCreate() ---> Exit");

		enterMonbileMusicMainActivty();
	}

	@Override
	public boolean onKeyDown(int paramInt, KeyEvent paramKeyEvent)
	{
		if (paramInt == 4)
		{
			// 在当前Activity的消息队列中删除消息号码为0的没有处理的消息
			this.mSplashHandler.removeMessages(0);
			Util.exitMobileMusicApp(false);
		}
		return super.onKeyDown(paramInt, paramKeyEvent);
	}

	@Override
	protected void onPause()
	{
		logger.v("onPause() ---> Enter");
		super.onPause();
		logger.v("onPause() ---> Exit");
	}

	@Override
	protected void onResume()
	{
		logger.v("onResume() ---> Enter");
		super.onResume();
		logger.v("onResume() ---> Exit");
	}
}