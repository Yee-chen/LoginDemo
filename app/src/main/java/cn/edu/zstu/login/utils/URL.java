package cn.edu.zstu.login.utils;

import android.content.Context;
import android.content.SharedPreferences;

/**
 * Created by Lei Chen on 7/20/2017.
 */
public class URL {
    public static String URL = "http://192.168.168.123:3003";

    private static SharedPreferences preferences;
    private static SharedPreferences.Editor editor;

    public static SharedPreferences getSharedPreferences(Context context) {
        preferences = context.getSharedPreferences("session", Context.MODE_PRIVATE);
        return preferences;
    }

    public static void setUrl(Context context, String url) {
        editor = getSharedPreferences(context).edit();
        editor.putString("url", url).apply();
    }

    public static String getURL(Context context) {
        return getSharedPreferences(context).getString("url", URL);
    }
}
