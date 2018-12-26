package com.inject.app;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.util.FileUtil;
import com.util.ILog;
public class LockerFactory {

    public volatile static ConcurrentHashMap<String, SafeLocker> safeLockerMap = new ConcurrentHashMap<String, SafeLocker>();
    private static int sCount = 0;
    private static String TAG = LockerFactory.class.getSimpleName();

    public synchronized static SafeLocker getLock(String key) {
        ++sCount;
        ILog.i(TAG,"hook time:" + sCount);

        if (safeLockerMap.containsKey(key)) {
            SafeLocker safeLocker = safeLockerMap.get(key);
            ILog.i(TAG,"safeLockerMap have key:" + key);
            safeLocker.countDown();
            return safeLockerMap.get(key);
        } else {
        	ILog.i(TAG,"safeLockerMap don't have key:" + key);
            SafeLocker safeLocker = new SafeLocker(key);
            safeLockerMap.put(key, safeLocker);
//            FileUtil.writeStringToTmpFile("/data/local/tmp/" + InjectApp.time + "_lock.txt", key + '\n');
            safeLocker.countDown();
            return safeLocker;
        }
    }

    public static SafeLocker getLock(String key, int count, int timeUp) {
        ++sCount;
        ILog.i(TAG,"hook time:" + sCount + " count：" + count + " timeUp:" + timeUp);
        if (safeLockerMap.containsKey(key)) {
        	ILog.i(TAG,"safeLockerMap have key:" + key);
            safeLockerMap.get(key).countDown();
            return safeLockerMap.get(key);
        } else {
        	ILog.i(TAG,"safeLockerMap don't have key:" + key);
            SafeLocker safeLocker = new SafeLocker(key, count, timeUp);
            safeLockerMap.put(key, safeLocker);
            safeLockerMap.get(key).countDown();
            return safeLocker;
        }
    }
    
    public synchronized static void releaseLock(String key) {
    	if (safeLockerMap.containsKey(key)) {
    		while(safeLockerMap.get(key).getCount() > 0) {
    			safeLockerMap.get(key).countDown();
    		}
    	}
    }

    public synchronized static boolean deleteLock(String key) {
        if (safeLockerMap.containsKey(key)) {
        	ILog.i(TAG,"delete locker:" + key);
//        	FileUtil.writeStringToTmpFile("/data/local/tmp/" + InjectApp.time + "_dele.txt", key + '\n');
            safeLockerMap.remove(key);
            return true;
        } else {
//            XposedBridge.log("delete locker failed:" + key);
            return false;
        }
    }
    
    public synchronized static boolean isLockExist(String key) {
    	ILog.w(TAG, "key :" + key + " " + key.toString());
    	for(String lockKey : safeLockerMap.keySet()) {
    		ILog.i(TAG, "safeLockerMap key:" + lockKey + " " + lockKey.toString());
    	}
    	ILog.i(TAG, "isLockExist:" + safeLockerMap.containsKey(key));
    	return safeLockerMap.containsKey(key);
    }
}