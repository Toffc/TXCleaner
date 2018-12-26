package com.inject.app;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CountDownLatch;

import com.util.ILog;

public class SafeLocker {
//
//    public static int MAX_WAIT_THREAD_NUM = 200;
//    private static int MAX_WAIT_TIMEUP = 3 * 60 * 1000;

	//mark 原先的配置
    public static int MAX_WAIT_THREAD_NUM = 10;
    private static int MAX_WAIT_TIMEUP = 60 * 1000;

    private static String TAG = SafeLocker.class.getSimpleName();

    public String key;
    public CountDownLatch mCountDownLatch;
    private boolean locked = false;
    public boolean mTimeUp;
    private Timer timer;
    public volatile static int resumeCount = 0; //用于onResume计数用。

    public SafeLocker(String keyName) {
        key = keyName;
        mCountDownLatch = new CountDownLatch(MAX_WAIT_THREAD_NUM);
        mTimeUp = false;
        long start = System.currentTimeMillis();
        //end 计算结束时间
//        final long end = start + min * 60 * 1000;
        final long end = start + MAX_WAIT_TIMEUP;
        timer = new Timer();
        //延迟0毫秒（即立即执行）开始，每隔1000毫秒执行一次
        timer.schedule(new TimerTask() {
            public void run() {
                //show是剩余时间，即要显示的时间
                long show = end - System.currentTimeMillis();
                long h = show / 1000 / 60 / 60;//时
                long m = show / 1000 / 60 % 60;//分
                long s = show / 1000 % 60;//秒
                if (0 == m && s % 10 == 0) {
                	ILog.i(TAG,"remain time:" + h + "h:" + m + "m:" + s + "s");
                }
                mTimeUp = false;
            }
        }, 0, 1000);

        //计时结束时候，停止全部timer计时计划任务
        timer.schedule(new TimerTask() {
            public void run() {
            	ILog.i(TAG,"time up");
                timer.cancel();
                mTimeUp = true;
//               如果没有减至0，超时后主动减至0，释放锁
                while (getCount() != 0) {
                	ILog.i(TAG,"time up,count down :" + getCount());
                    countDown();
                }
            }
        }, new Date(end));
    }

    public SafeLocker(String keyName, int count, int timeUp) {
        key = keyName;
        mCountDownLatch = new CountDownLatch(count);
        mTimeUp = false;

        long start = System.currentTimeMillis();
        //end 计算结束时间
        final long end = start + timeUp * 1000;

        final Timer timer = new Timer();
        //延迟0毫秒（即立即执行）开始，每隔1000毫秒执行一次
        timer.schedule(new TimerTask() {
            public void run() {
                //show是剩余时间，即要显示的时间
                long show = end - System.currentTimeMillis();
                long h = show / 1000 / 60 / 60;//时
                long m = show / 1000 / 60 % 60;//分
                long s = show / 1000 % 60;//秒
                if (0 == m && s % 10 == 0) {
                	ILog.i(TAG,"remain time:" + h + "h:" + m + "m:" + s + "s");
                }
                mTimeUp = false;
            }
        }, 0, 1000);

        //计时结束时候，停止全部timer计时计划任务
        timer.schedule(new TimerTask() {
            public void run() {
                timer.cancel();
                mTimeUp = true;
                if (getCount() != 0) {
                	ILog.i(TAG,"time up,count down :" + getCount());
                    countDown();
                }
            }
        }, new Date(end));
    }

    public void resetTimer() {
    	ILog.i(TAG,"reset timer");
        mTimeUp = false;
        long start = System.currentTimeMillis();
        //end 计算结束时间
//        final long end = start + min * 60 * 1000;
        final long end = start + 5000;

        timer.cancel();
        timer.purge();

        timer = new Timer();
        //延迟0毫秒（即立即执行）开始，每隔1000毫秒执行一次
        timer.schedule(new TimerTask() {
            public void run() {
                //show是剩余时间，即要显示的时间
                long show = end - System.currentTimeMillis();
                long h = show / 1000 / 60 / 60;//时
                long m = show / 1000 / 60 % 60;//分
                long s = show / 1000 % 60;//秒
                ILog.i(TAG,"after reset timer,time not up,remain time:" + h + "h:" + m + "m:" + s + "s");
                mTimeUp = false;
            }
        }, 0, 1000);

        //计时结束时候，停止全部timer计时计划任务
        timer.schedule(new TimerTask() {
            public void run() {
            	ILog.i(TAG,"after reset timer,time up");
                timer.cancel();
                mTimeUp = true;
            }
        }, new Date(end));
    }

    public void countDown() {
        mCountDownLatch.countDown();
        long currentCount = mCountDownLatch.getCount();
        ILog.i(TAG,"after countDown,current count:" + currentCount);
        if (0 == currentCount) {
        	ILog.i(TAG,"after countDown,current count to 0,init");
        }
    }

    public long getCount() {
        return mCountDownLatch.getCount();
    }

}