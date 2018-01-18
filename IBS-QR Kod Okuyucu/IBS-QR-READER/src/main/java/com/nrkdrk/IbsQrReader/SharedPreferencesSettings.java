package com.nrkdrk.IbsQrReader;

import android.content.Context;
import android.content.SharedPreferences;

public class SharedPreferencesSettings {

    public static void SaveKey(Context context, String tag, String value) {
        SharedPreferences mSharedPrefs = context.getSharedPreferences("xmlFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putString(tag ,value);
        editor.commit();
    }

    public static void SaveKey(Context context, String tag,int value) {
        SharedPreferences mSharedPrefs = context.getSharedPreferences("xmlFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.putInt(tag ,value);
        editor.commit();
    }

    public static String GetKey(Context context, String tag) {
        SharedPreferences mSharedPrefs = context.getSharedPreferences("xmlFile", Context.MODE_PRIVATE);
        return mSharedPrefs.getString(tag,"");
    }
    public static int GetIntKey(Context context, String tag) {
        SharedPreferences mSharedPrefs = context.getSharedPreferences("xmlFile", Context.MODE_PRIVATE);
        return mSharedPrefs.getInt(tag,0);
    }
    public static  void DeleteKey(Context context, String tag) {
        SharedPreferences mSharedPrefs = context.getSharedPreferences("xmlFile", Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSharedPrefs.edit();
        editor.remove(tag);
        editor.commit();
    }
}
