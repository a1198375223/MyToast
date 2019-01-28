package com.example.mytoast;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Application;
import android.os.Bundle;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

/**
 * activity 生命周期回调发方法
 */
public class ActivityLifecycleImpl implements Application.ActivityLifecycleCallbacks {
    private static final String TAG = "ActivityLifecycleImpl";

    private LinkedList<Activity> mActivityList = new LinkedList<>();

    private HashMap<Object, OnAppStatusChangedListener> mStatusListenerMap = new HashMap<>();

    private int mForegroundCount = 0; // 记录前台activity的数量

    private int mConfigCount = 0; // 使用这个属性来判断activity是否是因为config的改变从而发生销毁的


    public void addListener(Object object, OnAppStatusChangedListener listener) {
        mStatusListenerMap.put(object, listener);
    }

    public void removeListener(Object object) {
        mStatusListenerMap.remove(object);
    }


    public LinkedList<Activity> getActivityList() {
        return mActivityList;
    }

    @Override
    public void onActivityCreated(Activity activity, Bundle savedInstanceState) {
        Log.d(TAG, "onActivityCreated: ");
        // 当activity启动的时候,把这个被展现的activity设置到顶层
        setTopActivity(activity);
    }

    @Override
    public void onActivityStarted(Activity activity) {
        Log.d(TAG, "onActivityStarted: ");
        setTopActivity(activity);

        if (mForegroundCount <= 0) {
            postStatus(true);
        }
        if (mConfigCount < 0) {
            ++mConfigCount;
        } else {
            ++mForegroundCount;
        }
    }

    @Override
    public void onActivityResumed(Activity activity) {
        Log.d(TAG, "onActivityResumed: ");
        setTopActivity(activity);
    }

    @Override
    public void onActivityPaused(Activity activity) {
        Log.d(TAG, "onActivityPaused: ");

    }

    @Override
    public void onActivityStopped(Activity activity) {
        Log.d(TAG, "onActivityStopped: ");
        if (activity.isChangingConfigurations()) {
            --mConfigCount;
        } else {
            --mForegroundCount;
            if (mForegroundCount <= 0) {
                postStatus(false);
            }
        }
    }

    @Override
    public void onActivitySaveInstanceState(Activity activity, Bundle outState) {
        Log.d(TAG, "onActivitySaveInstanceState: ");

    }

    @Override
    public void onActivityDestroyed(Activity activity) {
        Log.d(TAG, "onActivityDestroyed: ");
        mActivityList.remove(activity);
    }


    /**
     * 让activity回到顶层
     */
    private void setTopActivity(Activity activity) {
        Log.d(TAG, "setTopActivity: ");
        if (mActivityList.contains(activity)) {
            if (!mActivityList.getLast().equals(activity)) {
                mActivityList.remove(activity);
                mActivityList.addLast(activity);
            }
        } else {
            mActivityList.addLast(activity);
        }
    }

    /**
     * 得到顶层的activity
     */
    public Activity getTopActivity(){
        if (!mActivityList.isEmpty()) {
            Activity topActivity = mActivityList.getLast();
            if (topActivity != null) {
                return topActivity;
            }
        }

        Log.d(TAG, "getTopActivity: Activity list is null");
        Activity topActivityByReflect = getTopActivityByReflect();
        if (topActivityByReflect != null) {
            setTopActivity(topActivityByReflect);
        }
        return topActivityByReflect;
    }

    /**
     * 通过反射来获取activity
     */
    @SuppressLint("PrivateApi")
    private Activity getTopActivityByReflect() {
        Class<?> activityThreadClass = null;
        try {
            activityThreadClass = Class.forName("android.app.ActivityThread");
            Object thread = activityThreadClass.getMethod("currentActivityThread").invoke(null);
            Field activityFields = activityThreadClass.getDeclaredField("mActivities");
            activityFields.setAccessible(true);
            Map activities = (Map) activityFields.get(thread);
            if (activities == null) {
                return null;
            }
            for (Object activityRecord : activities.values()) {
                Class activityRecordClass = activityRecord.getClass();
                Field pausedField = activityRecordClass.getDeclaredField("paused");
                pausedField.setAccessible(true);
                if (!pausedField.getBoolean(activityRecord)) {
                    Field activityField = activityRecordClass.getDeclaredField("activity");
                    activityField.setAccessible(true);
                    return (Activity) activityField.get(activityRecord);
                }
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * 发送activity状态通知
     */
    private void postStatus(boolean isForeground) {
        if (mStatusListenerMap.isEmpty()) {
            return;
        }
        for (OnAppStatusChangedListener listener : mStatusListenerMap.values()) {
            if (listener == null) {
                return;
            }

            if (isForeground) {
                listener.onForeground();
            } else {
                listener.onBackground();
            }
        }
    }
}
