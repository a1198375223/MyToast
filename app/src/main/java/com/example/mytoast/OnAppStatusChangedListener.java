package com.example.mytoast;

/**
 * 监听app的状态变化
 */
public interface OnAppStatusChangedListener {

    // app在前台
    void onForeground();

    // app在后台
    void onBackground();
}

