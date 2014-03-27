package com.chokoreto.muzei.konachan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.preference.Preference;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

public class Config {
  private final Context context;
  private final SharedPreferences prefs;
  public static final String limit = "100";

  public Config (Context context) {
        this.context = context;
        prefs  = PreferenceManager.getDefaultSharedPreferences(context);
    }
   public String strBuilder(){
      String tagz = prefs.getString("tags", "hatsune_miku");
      String sort = prefs.getString("pref_sort_order","order:score");
      Boolean restrictContent = prefs.getBoolean("pref_restrict_content", true);
      if (restrictContent == true){
          String maturity = "rating:safe";
          String rtnString = tagz + " " + sort + " " + maturity;
          return rtnString;
      }
      else{
          String rtnString = tagz + " " + sort;
          return rtnString;
      }
   }
    public int getTimeSet(){
        int time = Integer.parseInt(prefs.getString("pref_refresh_time", "90"));
        return time;
    }
    public String TAGS = "order:score rating:safe";
    public Boolean prettyflyforaWifi()
    {
        return prefs.getBoolean("pref_wifi", false);
    }
}
