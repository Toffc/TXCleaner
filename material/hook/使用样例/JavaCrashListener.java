package com.tencent.automationlib.impl.crash;

import android.app.ActivityThread;
import android.content.Context;
import android.util.Log;

import com.qq.android.dexposed.DexposedBridge;
import com.qq.android.dexposed.XC_MethodHook;
import com.tencent.automationlib.util.ProcessUtil;

public class JavaCrashListener {

    public static Class<Thread> ThreadClass;
    public static String TAG = JavaCrashListener.class.getSimpleName();

    public void thread_hook_set() {
        try {
            //如果被测应用本身没有crash的捕获流程。
            Log.i(TAG,"setUncaughtExceptionHandler");
            Thread.setDefaultUncaughtExceptionHandler(JavaCrashHandler.getInstance());

            ThreadClass = Thread.class;
            String pname = "";
            int pid = android.os.Process.myPid();
            Context context = ActivityThread.currentActivityThread().getApplication();
            pname = ProcessUtil.getProcessName(context);
            Log.i(TAG, "hook procrss pname:" + pname + ",pid:" + pid);

            DexposedBridge.findAndHookMethod(ThreadClass, "setDefaultUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class,
                    new XC_MethodHook() {
                        @Override
                        public void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            Thread.UncaughtExceptionHandler originalHandler = (Thread.UncaughtExceptionHandler)param.args[0];
                            param.args[0] = JavaCrashHandler.getInstance(originalHandler);
                        }

                        @Override
                        public void afterHookedMethod(MethodHookParam param) throws Throwable {
                        }
                    });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
