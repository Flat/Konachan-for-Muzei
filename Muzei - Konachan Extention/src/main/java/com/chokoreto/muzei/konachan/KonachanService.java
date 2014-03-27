package com.chokoreto.muzei.konachan;

import java.util.ArrayList;
import java.util.List;
import retrofit.http.GET;
import retrofit.http.Path;
import retrofit.http.Query;

interface KonachanService {
    @GET("/post.json")
    List<Posts> getPopularPosts(@Query("tags") String tags, @Query("limit") String limit);

    static class Posts{
        int id;
        String file_url;
        String tags;
        String author;
    }

}
