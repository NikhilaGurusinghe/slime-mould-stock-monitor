package slimemouldstock.monitor.utils;

import org.opencv.core.Rect;

public class OpenCVUtils {
    public static Rect intersectRect(Rect a, Rect b) {
        int left = Math.max(a.x, b.x);
        int top = Math.max(a.y, b.y);
        int right = Math.min(a.x + a.width, b.x + b.width);
        int bottom = Math.min(a.y + a.height, b.y + b.height);

        return new Rect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }
}
