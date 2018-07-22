package com.example.stickyindicator;

import android.app.Activity;
import android.content.Context;
import android.util.DisplayMetrics;
import android.view.WindowManager;

public class ScreenUtil {
    public static int pxTodp(Context context,float px){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(px / scale + 0.5f);
    }

    public static int dipTopx(Context context,float dp){
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int)(dp * scale + 0.5f);
    }

    public static int getWindowWidth(Activity activty){
        WindowManager manager = activty.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.widthPixels;
    }

    public static int getWindowHeight(Activity activty){
        WindowManager manager = activty.getWindowManager();
        DisplayMetrics outMetrics = new DisplayMetrics();
        manager.getDefaultDisplay().getMetrics(outMetrics);
        return outMetrics.heightPixels;
    }
}