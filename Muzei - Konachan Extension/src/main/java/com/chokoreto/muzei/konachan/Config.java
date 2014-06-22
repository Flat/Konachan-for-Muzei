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
      String sort;
      if (prefs.getString("pref_booru","konachan.com").equals("gelbooru.com"))
       {
           sort = "sort";
       }
       else
      {
          sort = "order";
      }
      String sortValue = prefs.getString("pref_sort_order","score");
      sort = sort + ":" + sortValue;
      Boolean restrictContent = prefs.getBoolean("pref_restrict_content", true);
      if (restrictContent){
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
        return Integer.parseInt(prefs.getString("pref_refresh_time", "90"));
    }
    public String getURLForLink(String booru){
        if(booru.equals("konachan.com")){
            return "http://konachan.net/post/show/";
        }
        else if(booru.equals("gelbooru.com")){
            return "http://gelbooru.com/index.php?page=post&s=view&id=";
        }
        else{
            return "http://konachan.net/post/show/";
        }
    }
    public String getAPIString(String booru){
        if (booru.equals("konachan.com")){
            return "post.json";
        }
        else if (booru.equals("gelbooru.com")){
            return "index.php";
        }
        else
        {
            return "post.json";
        }
    }
    public String getHyperTextProtocol(String booru){
        if(booru.equals("gelbooru.com")){
            return "http://";
        }
        else
        {
            return "https://";
        }
    }
    public String TAGS = "order:score rating:safe";
    public String getBooru(){return prefs.getString("pref_booru","konachan.com");}
    public Boolean prettyflyforaWifi()
    {
        return prefs.getBoolean("pref_wifi", false);
    }
}
