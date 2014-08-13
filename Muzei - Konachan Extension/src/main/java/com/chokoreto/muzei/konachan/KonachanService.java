package com.chokoreto.muzei.konachan;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;
import retrofit.http.QueryMap;

interface KonachanService {
    @GET("/{API}")
    List<Posts> getPopularPosts(@Path("API") String APIURL, @QueryMap Map<String, String> filters);
   // List<Posts> getPopularPosts(@Path("API") String APIURL, @Query("tags") String tags, @Query("limit") String limit);

    static class Posts {
        int id;
        String file_url;
        String tags;
        String author;
        String md5;
        String hash;
    }

}
