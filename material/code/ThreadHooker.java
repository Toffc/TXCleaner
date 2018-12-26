package com.inject.app;

import java.io.File;
import java.lang.reflect.Constructor;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadPoolExecutor;

import com.util.FileUtil;
import com.util.ILog;
import com.util.SystemLib;

import android.os.Bundle;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.os.Process;

public class ThreadHooker extends BaseHooker {
	
	private static ThreadHooker instance;
	private static String Tag = "ThreadHooker";
	
//  保存调用Thread  start run 和线程id start堆栈特征
    final ConcurrentHashMap<Long, String> startCallThreadIdMap = new ConcurrentHashMap<Long, String>();
	
//  保存手Q调用ThreadPoolExecutor executor()方法的类名
    final Vector<String> haveHookedRunnableClass = new Vector<String>();

//    保存手Q通过线程池 execute runnable实例hashcode 和堆栈特征
    final ConcurrentHashMap<Integer, String> executorRunnableHashCodeAndKeyMap = new ConcurrentHashMap<Integer, String>();
	
	JavaMethodHook.Callback threadPoolHookCallback = new JavaMethodHook.Callback() {
		//获取Runnable类型的线程的调用堆栈
		
		@Override
		public void beforeHookedMethod(Member member, Object o,	Object[] objects) {
			ThreadPoolExecutor threadPoolExecutor = (ThreadPoolExecutor) o;
            Runnable runnable = (Runnable) objects[0];
            String runnableClassName = runnable.getClass().getName();
            int hashCode = runnable.hashCode();

//            pool name
//            XposedBridge.log("ThreadPoolExecutor pool name:" + threadPoolExecutor.toString());
//            XposedBridge.log("runnable getClass:" + runnableClassName);
//            XposedBridge.log("runnable hashcode before execute:" + hashCode);
            String method = "execute";
            String hookMethod = "java.util.concurrent.ThreadPoolExecutor.execute";
            StackTraceElement[] stackElements = Thread.currentThread().getStackTrace();
            String stack = "";
            if (stackElements != null) {
                for (int i = 0; i < stackElements.length; i++) {
                	stack = stack + stackElements[i].toString() + "##";
                }
            }
            if (SystemLib.isAllJavaSysStack(stack)  || isWhiteStack(stack)) {
//                XposedBridge.log("execute is all java system stack,not hook");
            } else {
                final String key = SystemLib.getKeyFromStack(stack, hookMethod);
                if ("" == key) {
                    ILog.i(Tag,"execute app key is null");
                } else {
//                	ILog.i(Tag,"hook before method:" + method + " stack:\n" + stack);
//                	ILog.i(Tag,"#######################");
//                	ILog.i(Tag,"ThreadPoolExecutor execute key:" + key);

                    if (executorRunnableHashCodeAndKeyMap.containsKey(hashCode)) {
                    	ILog.i(Tag,"runnable hashcode is in map:" + hashCode);
                    } else {
                    	ILog.i(Tag,"add runnable hashcode to map:" + hashCode);
                        executorRunnableHashCodeAndKeyMap.put(hashCode, key);
                    }
                }
            }
		}

		@Override
		public void afterHookedMethod(Member member, Object o, Object[] objects, Object[] objects2) {
			
		}
	};
	
	
	JavaMethodHook.Callback threadPoolBeforeExecuteCallback = new JavaMethodHook.Callback() {
		//获取Runnable类型的线程的真正执行的地方
		
		@Override
		public void beforeHookedMethod(Member member, Object o,	Object[] objects) {
			if (executorRunnableHashCodeAndKeyMap.containsKey(objects[1].hashCode())) {
				int runnableHashcode = objects[1].hashCode();
                String key = executorRunnableHashCodeAndKeyMap.get(runnableHashcode);
                
                ILog.i(Tag, "runnable hashcode in map,lock:" + runnableHashcode);
                ILog.i(Tag,"before BeforeExecute thread id:" + Thread.currentThread().getId());
                if (Thread.currentThread().getId() == 1 || Thread.currentThread().getId() == 0 || Looper.getMainLooper().getThread().getId() == Thread.currentThread().getId()) {
                	ILog.i(Tag,"main thread return");
                    return;
                }
                SafeLocker safeLocker = LockerFactory.getLock(key);
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                ILog.i(Tag,"before lock time:" + df.format(new Date())+ " runnable hashcode:"+runnableHashcode);
                try{
                	safeLocker.mCountDownLatch.await();
                }catch(InterruptedException e){
                	ILog.e(Tag,"await() exception");
                }
                ILog.i(Tag,"runnable hashcode:"+runnableHashcode +" unlock key:" + safeLocker.key);
                LockerFactory.deleteLock(key);
                ILog.i(Tag,"after unlock time:" + df.format(new Date()) + " runnable hashcode:"+runnableHashcode);
            } else {
//               XposedBridge.log("runnable hashcode not in map,not lock:" + param2.thisObject.hashCode());
            }
		}

		@Override
		public void afterHookedMethod(Member member, Object o, Object[] objects, Object[] objects2) {
            Runnable runnable = (Runnable) objects[1];
            int hashCode = runnable.hashCode();
            if (executorRunnableHashCodeAndKeyMap.containsKey(hashCode)) {
                executorRunnableHashCodeAndKeyMap.remove(hashCode);
                ILog.i(Tag,"runnable hashcode in map delete:" + hashCode);
            }
		}
	};
	
	
	JavaMethodHook.Callback threadStartHookCallback = new JavaMethodHook.Callback() {
		//获取Thread类型的线程的调用堆栈
		
		@Override
		public void beforeHookedMethod(Member member, Object o,	Object[] objects) {
			String method = "start";
            StackTraceElement[] stackElements = Thread.currentThread().getStackTrace();
            String stack = "";
            if (stackElements != null) {
                for (int i = 0; i < stackElements.length; i++) {
//                    XposedBridge.log("hook before stack trace method:" + method + "  " + stackElements[i].toString());
//                    stack = stack + stackElements[i].toString() + "\n";
                	stack = stack + stackElements[i].toString() + "##";
//                    XposedBridge.log("ClassName:" + stackElements[i].getClassName());
//                    XposedBridge.log("FileName:" + stackElements[i].getFileName());
//                    XposedBridge.log("LineNum:" + stackElements[i].getLineNumber());
//                    XposedBridge.log("MethodName:" + stackElements[i].getMethodName());
                }
//                XposedBridge.log("hook before stack trace method:" + method + " end");
            }
//            XposedBridge.log("hook before method stack:\n" + stack);
            Thread thread = (Thread) o;
            int processId = Process.myPid();
            long threadId = thread.getId();
            ILog.i(Tag, "hook process id:" + Process.myPid() + " thisObject thread id:" + threadId + " before method:" + method);
            if (SystemLib.isAllJavaSysStack(stack) || isWhiteStack(stack)) {
//                XposedBridge.log("process id:" + processId + " thread id:" + threadId + " is all java system stack,not hook");
            }
            else {
                String hookMethod = "java.lang.Thread.start";
                String key = SystemLib.getKeyFromStack(stack, hookMethod);
                if ("" == key) {
                	ILog.i(Tag, "process id:" + processId + " thread id:" + threadId + " key is null");
                } else {
//                	ILog.i(Tag, "hook before method:" + method + " stack:\n" + stack);
//                	ILog.i(Tag, "#######################");
//                	ILog.i(Tag, "Thread start key:" + key);
                    if (startCallThreadIdMap.containsKey(threadId)) {
                    	ILog.i(Tag, "thread id is already in map:" + threadId + ",hook before method:" + method);
                    } else {
                    	ILog.i(Tag, "may don't thread id add to map:" + threadId + ",hook before method:" + method);
                        startCallThreadIdMap.put(threadId, key);
                    }
                }
            }
			
		}

		@Override
		public void afterHookedMethod(Member member, Object o, Object[] objects, Object[] objects2) {
			
		}
	};
	
	
	JavaMethodHook.Callback threadRunHookCallback = new JavaMethodHook.Callback() {
		//获取Thread类型的线程真正执行的地方
		
		@Override
		public void beforeHookedMethod(Member member, Object o,	Object[] objects) {
			String method = "run";
            Thread t = (Thread) o;
            long threadId = t.getId();
            if (Thread.currentThread().getId() == 1 || Thread.currentThread().getId() == 0 || Looper.getMainLooper().getThread().getId() == Thread.currentThread().getId()) {
                ILog.i(Tag, "main thread return");
                return;
            }
            ILog.i(Tag,"hook process id:" + Process.myPid() + " thisObject thread id:" + t.getId() + " before method:" + method);
            if (startCallThreadIdMap.containsKey(threadId)) {
                SafeLocker safeLocker = LockerFactory.getLock(startCallThreadIdMap.get(threadId));
                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
                ILog.i(Tag,"before lock time:" + df.format(new Date())+" thread id:"+ threadId);
                try{
                	safeLocker.mCountDownLatch.await();
                }catch(InterruptedException e){
                	ILog.e(Tag,"await exception");
                }
                ILog.i(Tag,"thread id:"+ threadId+" unlock key:" + safeLocker.key);
                LockerFactory.deleteLock(startCallThreadIdMap.get(threadId));
                ILog.i(Tag,"after unlock time:" + df.format(new Date())+" thread id:"+ threadId);
            } else {
            	ILog.i(Tag,"thread id is not app,not hook,brefore method:" + method + " thread id:" + threadId);
            }			
		}

		@Override
		public void afterHookedMethod(Member member, Object o, Object[] objects, Object[] objects2) {
            String method = "run";
            Thread t = (Thread) o;
            long threadId = t.getId();
//            ILog.i(Tag,"hook process id:" + Process.myPid() + " thisObject thread id:" + threadId + " after method:" + method);
            if (startCallThreadIdMap.containsKey(threadId)) {
            	ILog.i(Tag,"thread id is in map delete:" + threadId);
                startCallThreadIdMap.remove(threadId);
            }
		}
	};
	
	
	private ThreadHooker() {}
	
	public static ThreadHooker getInstance() {
		if (instance == null) {
			instance = new ThreadHooker();
		}
		return instance;
	}

	public void start() {		
		Constructor<?> constractor;
		Method onWindowFocusChanged;
		
		ILog.i(Tag, "enter start()");
		try {
			Class hookClass = Class.forName("java.util.concurrent.ThreadPoolExecutor");
	        Method hookMethod = hookClass.getDeclaredMethod("execute", new Class[]{Runnable.class});
			if (hookMethod != null ) {
				methodHook(hookMethod, threadPoolHookCallback);
			}
	        Method hookBeforeExecute = hookClass.getDeclaredMethod("beforeExecute", new Class[]{Thread.class,Runnable.class});
			if (hookBeforeExecute != null ) {
				methodHook(hookBeforeExecute, threadPoolBeforeExecuteCallback);
			}						
		} catch (Exception e) {
			Log.e("inject", "method Hook failed :java.util.concurrent.ThreadPoolExecutor execute");
			e.printStackTrace();
		}	
		try {
			Class hookClass = Class.forName("java.lang.Thread");
	        Method threadStartMethod = hookClass.getDeclaredMethod("start");
			if (threadStartMethod != null ) {
				methodHook(threadStartMethod, threadStartHookCallback);
			}
			
			Class hookThreadClass = Class.forName("java.lang.Thread");
	        Method threadRunMethod = hookThreadClass.getDeclaredMethod("run");
			if (threadRunMethod != null ) {
				methodHook(threadRunMethod, threadRunHookCallback);
			}
			
		} catch (Exception e) {
			Log.e("inject", "method Hook failed :java.lang.Thread");
			e.printStackTrace();
		}
	}
	
	public boolean isWhiteStack(String key) {

        String sTackA = "com.android.okhttp.ConnectionPool";
        String sTackB = "android.app.SharedPreferencesImpl"; 
        if(key.contains(sTackA)  || key.contains(sTackB)) {
        	return true;
        }
        return false;
	}
	
	private class RunnableHooker extends BaseHooker {
		
		private String className;
		
		JavaMethodHook.Callback methodHookCallback = new JavaMethodHook.Callback() {
			
			@Override
			public void beforeHookedMethod(Member member, Object o,	Object[] objects) {
				if (executorRunnableHashCodeAndKeyMap.containsKey(o.hashCode())) {
	            	ILog.i(Tag,"runnable hashcode in map,lock:" + o.hashCode());
	            	ILog.i(Tag,"thread id:" + Thread.currentThread().getId());
	                if (Thread.currentThread().getId() == 0) {
	                	ILog.i(Tag,"main thread return");
	                    return;
	                }
	                SafeLocker safeLocker = LockerFactory.getLock(executorRunnableHashCodeAndKeyMap.get(o.hashCode()));
	                SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");//设置日期格式
	                ILog.i(Tag,"before lock time:" + df.format(new Date()));
	                try{
	                	safeLocker.mCountDownLatch.await();
	                }catch(InterruptedException e){
	                	ILog.e(Tag,"await exception");
	                }
	                ILog.i(Tag,"unlock key:" + safeLocker.key);
	                LockerFactory.deleteLock(executorRunnableHashCodeAndKeyMap.get(o.hashCode()));
	                ILog.i(Tag,"after unlock time:" + df.format(new Date()));
	            } else {
//	                XposedBridge.log("runnable hashcode not in map,not lock:" + param2.thisObject.hashCode());
	            }
			}

			@Override
			public void afterHookedMethod(Member member, Object o, Object[] objects, Object[] objects2) {
				Runnable runnable = (Runnable) o;
	            int hashCode = runnable.hashCode();
//	            XposedBridge.log("runnable hashcode after run:" + param2.thisObject.hashCode());
	            if (executorRunnableHashCodeAndKeyMap.containsKey(hashCode)) {
	                executorRunnableHashCodeAndKeyMap.remove(hashCode);
//	                XposedBridge.log("runnable hashcode in map delete:" + hashCode);
	            } else {
//	                XposedBridge.log("runnable hashcode not in map,don't delete:" + hashCode);
	            }
				
			}
		};
		
		private RunnableHooker() {}
		
		public RunnableHooker(String className){
			this.className = className;
		}
		
//		public static RunnableHooker getInstance() {
//			if (instance == null) {
//				instance = new RunnableHooker();
//			}
//			return instance;
//		}

		public void start() {		
			Method hookMethod;
			ILog.i(Tag, "Runnable enter start()");
			try {
				ILog.i(Tag, "process id:" + Process.myPid()+",thread id:"+Process.myTid());
				Class hookClass = ClassUtils.getClass(InjectApp.classLoader, className);
		        hookMethod = hookClass.getDeclaredMethod("run");
				
//				Class hookClass = Class.forName(className);
//		        hookMethod = hookClass.getDeclaredMethod("run");
				if (hookMethod != null ) {
					methodHook(hookMethod, methodHookCallback);
				}
				} catch (Exception e) {
					Log.e("inject", "method Hook failed :" + className);
				e.printStackTrace();
			}	
		}
	} 
	
}

