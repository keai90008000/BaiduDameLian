package com.Liani.baidumap.app;

import android.app.Application;

import com.baidu.mapapi.SDKInitializer;

/**
 * Created by 陈驰 on 2017/4/23.
 */

public class App extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
// 在使用 SDK 各组间之前初始化 context 信息，传入 ApplicationContext //这样就不用每个Activity都初始化一遍了
         SDKInitializer.initialize(this);
    }
}
