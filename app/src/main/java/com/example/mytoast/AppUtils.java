package com.example.mytoast;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.Application;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.TreeMap;

public class AppUtils {
    private static final String TAG = "AppUtils";
    private static Application sApplication;
    private ActivityLifecycleImpl mActivityLifecycle = new ActivityLifecycleImpl();
    private static AppUtils sInstance;

    private AppUtils() {}

    public static AppUtils getInstance() {
        if (sInstance == null) {
            synchronized (AppUtils.class) {
                if (sInstance == null) {
                    sInstance = new AppUtils();
                }
            }
        }
        return sInstance;
    }


    /*
      简单总结一下为了返回上层的Activity或者是App, 经过的过程。
      1. 首先我们需要自定义一个ActivityLifecycleCallback来回调管理Activity的生命周期, 在里面实现返回上层
         activity的函数. ActivityLifecycleCallback的主要工作就是使用一个list来管理Activity. 使用两个计数
         值来统计位于前台的Activity的数量和判断是否是因为configuration的改变而造成Activity的重建的.(然后说
         一个在这个过程比较重要的函数, 通过反射来获取top Activity)

      2. 如果上个步骤返回的是null, 这时我们就需要返回Application了. 使用一个static变量sApplication来存储,
         如果该值不是null, 那么就直接返回. 如果是null就通过反射来获取Application, 并对获取到的Application
         进行注册ActivityLifecycleCallback, 最后保存到Application的静态变量中

      3. 为了更好的使用1步骤, 我们需要判断一下当前应用是否是位于前台, 如果是的话进行1步骤, 如果不是的话进行2步骤
     */



    /**
     * 优先返回上层的Activity而不是Application, 如果上层的Activity是null就返回App
     */
    public Context getTopActivityOrApp() {
        if (isAppForeground()) {
            Log.d(TAG, "getTopActivityOrApp: is foreground");
            return mActivityLifecycle.getTopActivity();
        } else {
            Log.d(TAG, "getTopActivityOrApp: not foreground");
            return getApp();
        }
    }


    /**
     * 其实用这个方法来判断应用是否处于前台是有局限的, 如果这个应用的Service被设置成START_STICKY
     * `appInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND` 这个判断是
     * 始终成立的.
     * 还是比较推荐使用ActivityLifecycle来判断是否是前台应用的
     */
    /**
     * 通过runningProcess获取到一个当前正在运行的进程list, 我们遍历这个list的每一个进程, 判断这个进程的
     * importance属性是否是IMPORTANCE_FOREGROUND, 并且包名是否与我们的app的包名一样, 如果这两个条件都
     * 符合, 那么这个app就处于前台运行
     */
    public boolean isAppForeground(){
        ActivityManager am = (ActivityManager) getApp().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> info = am.getRunningAppProcesses();
        if (info == null || info.size() == 0) {
            return false;
        }

        for (ActivityManager.RunningAppProcessInfo appInfo : info) {
            if (appInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return appInfo.processName.equals(getApp().getPackageName());
            }
        }
        return false;
    }

    /**
     * 该方法是用来获取前台应用的进程名, 判断到底是那个应该在前台
     */
    public String getForegroundApp() {
        ActivityManager am = (ActivityManager) getApp().getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> infos = am.getRunningAppProcesses();
        if (infos == null || infos.size() == 0) {
            return null;
        }

        for (ActivityManager.RunningAppProcessInfo info : infos) {
            if (info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
                    || info.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_VISIBLE) {
                return info.processName;
            }
        }
        return null;
    }


    /**
     * android 5.0 以上(api > 22) 实现判断是否是前台应用的另一种方法, 只不过实现比较复杂, 还需要手动引导设置权限,
     * 并且还要添加声明
     * <uses-permission android:name="android.permission.PACKAGE_USAGE_STATS" />
     *
     * mLastEvent对应UsageEvents.Event, 前台应用需要拉取mLastEvent=1的应用
     *
     * 使用这个函数之前必须先调用checkUsageStateAccessPermission()来引导用户开启权限
     */
    public String isForeground() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            UsageStatsManager usm = (UsageStatsManager) getApp().getSystemService(Context.USAGE_STATS_SERVICE);
            long time = System.currentTimeMillis();

            List<UsageStats> list = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, time - 1000 * 10, time);
            if (list == null || list.size() == 0) {
                return null;
            }
            TreeMap<Long, UsageStats> sortedMap = new TreeMap<>();
            for (UsageStats stats : list) {
                sortedMap.put(stats.getLastTimeUsed(), stats);
            }
            if (!sortedMap.isEmpty()) {
                // 如果需要查询具体前台应用的包名, 就返回下面的packageName
                //return Objects.requireNonNull(sortedMap.get(sortedMap.lastKey())).getPackageName();
                //return getApp().getPackageName().equals(Objects.requireNonNull(sortedMap.get(sortedMap.lastKey())).getPackageName());

                Iterator iterator = sortedMap.values().iterator();
                Field lastField = null;
                String packageName = null;
                while (iterator.hasNext()) {

                    UsageStats stats = (UsageStats) iterator.next();

                    if (lastField == null) {
                        try {
                            lastField = UsageStats.class.getField("mLastEvent");
                        } catch (NoSuchFieldException e) {
                            e.printStackTrace();
                        }
                    }

                    if (lastField != null) {
                        int lastEvent = 0;
                        try {
                            lastEvent = lastField.getInt(stats);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                        if (lastEvent == 1) {
                            packageName = Objects.requireNonNull(stats).getPackageName();
                            break;
                        }
                    } else {
                        break;
                    }
                }

                if (packageName == null) {
                    packageName = Objects.requireNonNull(sortedMap.get(sortedMap.lastKey())).getPackageName();
                }
                return packageName;
            }
        }
        return null;
    }

    /**
     * 手动引导用户进行声明
     */
    public void checkUsageStateAccessPermission() {
        if (!checkAppUsagePermission()) {
            requestAppUsagePermission();
        }
    }

    public boolean checkAppUsagePermission(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            UsageStatsManager usm = (UsageStatsManager) getApp().getSystemService(Context.USAGE_STATS_SERVICE);
            if (usm == null) {
                return false;
            }

            long currentTime = System.currentTimeMillis();
            List<UsageStats> stats = usm.queryUsageStats(UsageStatsManager.INTERVAL_DAILY, currentTime - 6 * 1000, currentTime);
            return stats != null && stats.size() != 0;
        }
        return false;
    }

    private void requestAppUsagePermission(){
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
            Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            getApp().getApplicationContext().startActivity(intent);
        }
    }




    public Application getApp() {
        if (sApplication != null) {
            return sApplication;
        }
        Application application = getApplicationByReflect();
        init(application);
        return application;
    }


    /**
     * 通过反射来获取application
     * @return application
     */
    private Application getApplicationByReflect() {
        try {
            @SuppressLint("PrivateApi")
            Class<?> activityThread = Class.forName("android.app.ActivityThread");
            Object thread = activityThread.getMethod("currentActivityThread").invoke(null);
            Object app = activityThread.getMethod("getApplication").invoke(thread);
            return (Application) app;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        } catch (NoSuchMethodException e) {
            e.printStackTrace();
        }
        return null;
    }

    private void init(Application app) {
        if (sApplication == null) {
            if (app == null) {
                sApplication = getApplicationByReflect();
            } else {
                sApplication = app;
            }
            if (sApplication != null) {
                sApplication.registerActivityLifecycleCallbacks(mActivityLifecycle);
            }
        } else {
            if (app != null && app.getClass() != sApplication.getClass()) {
                // 重新注册
                sApplication.unregisterActivityLifecycleCallbacks(mActivityLifecycle);
                mActivityLifecycle.getActivityList().clear();
                sApplication = app;
                sApplication.registerActivityLifecycleCallbacks(mActivityLifecycle);
            }
        }
    }
}
