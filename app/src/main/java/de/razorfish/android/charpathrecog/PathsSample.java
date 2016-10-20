package de.razorfish.android.charpathrecog;

import android.graphics.PointF;

import com.google.gson.annotations.SerializedName;

import java.util.List;

public class PathsSample {
    public final List<List<PointF>> paths;
    @SerializedName("class")
    public final Character klass;

    public PathsSample(List<List<PointF>> paths, Character klass) {
        this.paths = paths;
        this.klass = klass;
    }
}
