package com.inject.app;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.util.Random;

import com.util.FileUtil;
import com.util.ILog;
import com.util.SystemLib;

import android.os.Bundle;
import android.os.Looper;
import android.os.Message;
import android.util.Log;

public class HandlerHooker extends BaseHooker {
	
	private static HandlerHooker instance;
	private static String Tag = "HandlerHooker";
	
	JavaMethodHook.Callback methodHookCallback = new JavaMethodHook.Callback() {
		
		@Override
		public void beforeHookedMethod(Member member, Object o,	Object[] objects) {
			//三个参数代表method, thisObject, args
//			String method = "postDelayed";
//            String hookMethod = "android.os.Handler.postDelayed";
			String method = "sendMessageDelayed";
			String hookMethod = "android.os.Handler.sendMessageDelayed";
            StackTraceElement[] stackElements = Thread.currentThread().getStackTrace();
            String stack = "";
            if (stackElements != null) {
                for (int i = 0; i < stackElements.length; i++) {
                    stack = stack + stackElements[i].toString() + "\n";
                }
            }

            if (SystemLib.isAllJavaSysStack(stack)) {
            } else {
            	//key为除去系统堆栈之外的应用堆栈
                final String key = SystemLib.getKeyFromStack(stack, hookMethod);
                if ("" == key) {
                    Log.i(Tag,"postDelayed app key is null");
                } else {
                	Log.i(Tag,"hook before method:" + method + " stack:\n" + stack);
                	Log.i(Tag,"#######################");
                	Log.i(Tag,hookMethod + " key:" + key);
                    Long para2 = (Long) objects[1];
                    Log.i(Tag,"hook before sendMessageDelayed delay time:" + para2);
//                  若是0，考虑加延时
                    if(para2 == 0) {
                    	if(Thread.currentThread() != Looper.getMainLooper().getThread()  ){
                    		objects[1] = new Double(Math.random()*(2000 - 1000) + 1000).longValue();
                    	}
                    }
//                  小于10秒的乘以20倍，否则延迟时间太长，怕搞出问题
                    else if (para2 < 10000) {
                        objects[1] = para2 * 20;
                    }
                }
            }
		}

		@Override
		public void afterHookedMethod(Member member, Object o, Object[] objects, Object[] objects2) {
           Long para2 = (Long) objects[1];
           Log.i(Tag,"hook after postDelayed delay time:" + para2);
		}
	};
	
	private HandlerHooker() {}
	
	public static HandlerHooker getInstance() {
		if (instance == null) {
			instance = new HandlerHooker();
		}
		return instance;
	}

	public void start() {		
		Constructor<?> constractor;
		Method hookMethod;
		Method onWindowFocusChanged;
		
		ILog.i(Tag, "enter start()");

		try {
			Class hookClass = Class.forName("android.os.Handler");
			// method Runnable Long  
	        Class[] cArg = new Class[2];  
	        cArg[0] = Runnable.class;  
	        cArg[1] = long.class; 
//	        hookMethod = hookClass.getDeclaredMethod("postDelayed", new Class[]{Runnable.class,long.class});
	        hookMethod = hookClass.getDeclaredMethod("sendMessageDelayed", new Class[]{Message.class,long.class});
	        if (hookMethod != null ) {
				methodHook(hookMethod, methodHookCallback);
			}
		} catch (Exception e) {
				Log.e("inject", "method Hook failed :android.os.Handler.postDelayed");
			e.printStackTrace();
		}	
	
	}	
}