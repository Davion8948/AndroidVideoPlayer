package com.timeslily.utils;

import android.content.Context;
import android.util.DisplayMetrics;

public class DensityUtil {
    // 当前屏幕的densityDpi
    private static float dmDensityDpi = 0.0f;
    private static DisplayMetrics dm;
    private static float scale = 0.0f;
 
    
    public DensityUtil(Context context) {
        // 获取当前屏幕
        dm = new DisplayMetrics();
        //返回当前资源对象的DispatchMetrics信息。
        dm = context.getApplicationContext().getResources().getDisplayMetrics();
        // 设置DensityDpi
        setDmDensityDpi(dm.densityDpi);
        // 密度因子
        scale = getDmDensityDpi() / 160;//等于 scale=dm.density;
    }
 
    
    public static float getDmDensityDpi() {
        return dmDensityDpi;
    }
 
    
    public static void setDmDensityDpi(float dmDensityDpi) {
        DensityUtil.dmDensityDpi = dmDensityDpi;
    }
 
    
    public int dip2px(float dipValue) {
        return (int) (dipValue * scale + 0.5f);
 
    }
 
    
    public int px2dip(float pxValue) {
        return (int) (pxValue / scale + 0.5f);
    }
 
    @Override
    public String toString() {
        return " dmDensityDpi:" + dmDensityDpi;
    }
}