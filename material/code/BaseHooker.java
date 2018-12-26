package com.inject.app;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

import android.util.Log;

public abstract class BaseHooker {
	protected final static String TAG = "BaseHooker";
	
	protected Class<?>[] classLoader(String... classNames) {
		int numToLoad = 0;
		if (classNames == null || classNames.length == 0) {
			return null;
		} else {
			numToLoad = classNames.length;
		}
		Class<?>[] loaders = new Class<?>[numToLoad];
		try {
			for(int i = 0; i < numToLoad; ++i) {
				loaders[i] = Class.forName(classNames[i]);
			}
		} catch (ClassNotFoundException e) {
			Log.e(TAG, "", e);
		}
		return loaders;
	}
	
	protected Method getMethodReflection(Class<?> loadClass, String methodName, Class<?>... parameterTypes) {
		Method method;
		try {
			method = loadClass.getDeclaredMethod(methodName, parameterTypes);
		} catch (NoSuchMethodException e2) {
			Log.e(TAG, "", e2);
			return null;
		}
		return method;
	}
	
	protected void methodHook(Method method, JavaMethodHook.Callback callback) {
		JavaMethodHook.hook(method, callback);
	}
	
	protected void constructorHook(Constructor<?> method, JavaMethodHook.Callback callback) {
		JavaMethodHook.constructorhook(method, callback);
	}
	
	public abstract void start(); 
}