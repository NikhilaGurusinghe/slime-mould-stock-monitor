package slimemouldstock.monitor.utils;

import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.Rect;
import org.opencv.imgcodecs.Imgcodecs;

public class OpenCVUtils {
    public static Rect intersectRect(Rect a, Rect b) {
        int left = Math.max(a.x, b.x);
        int top = Math.max(a.y, b.y);
        int right = Math.min(a.x + a.width, b.x + b.width);
        int bottom = Math.min(a.y + a.height, b.y + b.height);

        return new Rect(left, top, Math.max(0, right - left), Math.max(0, bottom - top));
    }

    // may return an empty byte[] if encoding fails!
    // https://academyeurope.org/courses-4/certificate-courses/opencv-image-processing-techniques-in-java/lessons/3-0-core-data-structures-and-image-i-o/
    public static byte[] convertMatToJPEGBytes(Mat image) {
        MatOfByte matOfByte = new MatOfByte();

        try {
            boolean wasEncoded = Imgcodecs.imencode(".jpg", image, matOfByte);

            if (!wasEncoded) {
                // return empty byte array if we failed to encode
                return new byte[0];
            }

            return matOfByte.toArray();
        } finally {
            matOfByte.release();
        }
    }
}
