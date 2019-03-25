package com.org.life.weather.gson;

import com.google.gson.annotations.SerializedName;

public class ForeCast {
    public String date;

    @SerializedName("cond")
    public More more;

    @SerializedName("tmp")
    public Temperature temperature;

    public class More {
        @SerializedName("txt_d")
        public String into;
    }

    public class Temperature {
        public String max;
        public String min;
    }

}
