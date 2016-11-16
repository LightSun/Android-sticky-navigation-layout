package com.heaven7.android.scroll;

import android.text.TextUtils;

/**
 * internal util class
 * Created by heaven7 on 2016/11/16.
 */
 class Util {

    public static void check(Object obj ,String msg){
        if(obj == null){
            throw TextUtils.isEmpty(msg) ? new NullPointerException(): new NullPointerException(msg);
        }
    }
    public static void check(Object obj ){
        if(obj == null){
            throw new NullPointerException();
        }
    }
}
