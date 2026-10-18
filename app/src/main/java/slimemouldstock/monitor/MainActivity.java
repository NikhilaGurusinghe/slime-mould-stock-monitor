package slimemouldstock.monitor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

// code credits ->
// setup
// https://www.youtube.com/watch?v=6Nl3JYpEgeI&list=PLTuKYqpidPXZjtOEjOgeKxNAe4NzSgsg8&index=3
// https://docs.opencv.org/4.13.0/d5/df8/tutorial_dev_with_OCV_on_Android.html
// center of contouring (used to find center of the petri dish)
// https://pyimagesearch.com/2016/02/01/opencv-center-of-contour/
// aruco marker detection
// https://docs.opencv.org/3.4.20/d5/dae/tutorial_aruco_detection.html
// inRange thresholding
// https://docs.opencv.org/3.4.13/da/d97/tutorial_threshold_inRange.html
// https://docs.opencv.org/4.12.0/javadoc/org/opencv/core/Core.html#inRange(org.opencv.core.Mat,org.opencv.core.Scalar,org.opencv.core.Scalar,org.opencv.core.Mat)
// https://pyimagesearch.com/2014/08/04/opencv-python-color-detection/
public class MainActivity extends CameraActivity {

    // cv2 camera stuff
    private CameraBridgeViewBase cameraBridgeViewBase;

    // cv2 frame colour matrices
    private Mat inRangeThresholdMask;
    private Mat edgeDetectionThresholdMask;
    private Mat inputFrameRGB;
    private Mat inputFrameRGBBlurred;
    private Mat inputFrameHSV;
    private Mat inputFrameGRAY;
    private Mat inputFrameGRAYBlurred;
    private Mat maskedRGB;

    // Looking to recognise slime mould coloured pixels (in our ideal lighting)
    // in HSV
    // lowering H value helps capture more orange
    // S value is more to do with getting rid of white light - good for getting rid of shadows
    //      reflections/hue leakage of yellow, but less colour info
    // V doesn't seem to do a whole lot
    Scalar inRangeLowerBoundHSV = new Scalar(20, 175, 0);
    Scalar inRangeUpperBoundHSV = new Scalar(80, 255, 255);

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        // android os camera permissions
        getCameraPermission();
        cameraBridgeViewBase = findViewById(R.id.cameraView);
        // for some reason this doesn't work as you'd expect
        // this sets it to a higher resolution 1280x2560 (even though it's 100x100)
        // default is 768 x 1024
//        cameraBridgeViewBase.setMaxFrameSize(100, 100);
        cameraBridgeViewBase.setCvCameraViewListener(new CameraBridgeViewBase.CvCameraViewListener2() {
            @Override
            public void onCameraViewStarted(int width, int height) {
                // init these here after OpenCV has initialized
                inputFrameRGB = new Mat();
                inputFrameRGBBlurred = new Mat();
                inputFrameHSV = new Mat();
                inputFrameGRAY = new Mat();
                inputFrameGRAYBlurred = new Mat();
                inRangeThresholdMask = new Mat();
                edgeDetectionThresholdMask = new Mat();
                maskedRGB = new Mat();
            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat inputFrameRGBA = inputFrame.rgba();

                // RGBA -> RGB
                Imgproc.cvtColor(inputFrameRGBA, inputFrameRGB, Imgproc.COLOR_RGBA2RGB);
                // RGB -> RGB + guassian blur
                // guassian blur on main RGB, if we want more blur use a 5x5 kernel
                Imgproc.GaussianBlur(inputFrameRGB, inputFrameRGBBlurred,
                        new Size(3, 3), 0, 0);
                // RGB + guassian blur -> HSV
                Imgproc.cvtColor(inputFrameRGBBlurred, inputFrameHSV, Imgproc.COLOR_RGB2HSV);

                // RGBA -> Gray
                Imgproc.cvtColor(inputFrameRGBA, inputFrameGRAY, Imgproc.COLOR_RGBA2GRAY);
                Imgproc.GaussianBlur(inputFrameGRAY, inputFrameGRAYBlurred,
                        new Size(5, 5), 0, 0);
                // shapes are white after thresholding
                Imgproc.threshold(inputFrameGRAYBlurred, edgeDetectionThresholdMask,
                        60, 255, Imgproc.THRESH_BINARY);
                // finding contours
                // can make this more efficient by doing this once and storing list of contours
                //      assumes that the camera does not move
                List<MatOfPoint> contours = new ArrayList<>();
                Imgproc.findContours(edgeDetectionThresholdMask, contours,
                        new Mat(), Imgproc.RETR_CCOMP, Imgproc.CHAIN_APPROX_NONE);
                // get largest contour (by area) -> assume petri dish is the largest thing in frame
                // https://stackoverflow.com/questions/40669684/opencv-sorting-contours-by-area-in-java
                Optional<MatOfPoint> largest = contours.stream().max(new Comparator<MatOfPoint>() {
                    public int compare(MatOfPoint c1, MatOfPoint c2) {

                        return (int) (Imgproc.contourArea(c1)- Imgproc.contourArea(c2));
                    }
                });
                MatOfPoint petriDishContour = largest.orElse(null);

                // ranges in HSV
                Core.inRange(inputFrameHSV, inRangeLowerBoundHSV,
                        inRangeUpperBoundHSV, inRangeThresholdMask);

                maskedRGB.setTo(Scalar.all(0));
                // dst = maskedRGB
                // apply threshold mask to original RGB
                Core.bitwise_and(inputFrameRGB, inputFrameRGB, maskedRGB, inRangeThresholdMask);

                // draw centroid of petri dish
                if (petriDishContour != null) {
                    Moments petriDishMoment = Imgproc.moments(petriDishContour);

                    if (petriDishMoment.m00 != 0) {
                        int centroidX = (int) (petriDishMoment.m10 / petriDishMoment.m00);
                        int centroidY = (int) (petriDishMoment.m01 / petriDishMoment.m00);

                        // camera calibration using the petri dish's size
                        // we get the bounding rectangle to calculate the width of the dish in pixels
                        Rect dishBoundingRect = Imgproc.boundingRect(petriDishContour);
                        Imgproc.rectangle(inputFrameRGB, dishBoundingRect.tl(),
                                dishBoundingRect.br(), new Scalar(0, 0, 255), 3);
                        double petriDishDiameterMm = 90.0; // 9cm petri dish
                        double petriDishDiameterPx =
                                (dishBoundingRect.width + dishBoundingRect.height) / 2.0;
                        double pointOfInterestShiftMm = 37;
                        double pixelsPerMm = petriDishDiameterPx / petriDishDiameterMm;
                        int pointOfInterestShiftPx = (int)
                                Math.round(pointOfInterestShiftMm * pixelsPerMm);

                        // centre of dish contour
                        Imgproc.circle(inputFrameRGB, new Point(centroidX, centroidY), 30,
                                new Scalar(255, 0, 0), 5);
                        // drawing largest contour
                        Imgproc.drawContours(inputFrameRGB, List.of(petriDishContour), 0,
                                new Scalar(0, 255, 0), 2);

                        // drawing detection squares
                        int halfSize = 20;
                        Scalar color = new Scalar(255, 125, 0);
                        int thickness = 2;
                        Rect southRect = new Rect(
                                new Point(centroidX - halfSize,
                                        centroidY + pointOfInterestShiftPx - halfSize),
                                new Point(centroidX + halfSize,
                                        centroidY + pointOfInterestShiftPx + halfSize)
                        );
                        Rect southEastRect = new Rect(
                                new Point(centroidX + pointOfInterestShiftPx * Math.cos(Math.PI / 4) - halfSize,
                                        centroidY + pointOfInterestShiftPx * Math.sin(Math.PI / 4) - halfSize),
                                new Point(centroidX + pointOfInterestShiftPx * Math.cos(Math.PI / 4) + halfSize,
                                        centroidY + pointOfInterestShiftPx * Math.sin(Math.PI / 4) + halfSize)
                        );
                        Rect northRect = new Rect(
                                new Point(centroidX - halfSize,
                                        centroidY - pointOfInterestShiftPx - halfSize),
                                new Point(centroidX + halfSize,
                                        centroidY - pointOfInterestShiftPx + halfSize)
                        );
                        Rect northEastRect = new Rect(
                                new Point(centroidX + pointOfInterestShiftPx * Math.cos(7 * Math.PI / 4) - halfSize,
                                        centroidY + pointOfInterestShiftPx * Math.sin(7 * Math.PI / 4) - halfSize),
                                new Point(centroidX + pointOfInterestShiftPx * Math.cos(7 * Math.PI / 4) + halfSize,
                                        centroidY + pointOfInterestShiftPx * Math.sin(7 * Math.PI / 4) + halfSize)
                        );
                        Rect eastRect = new Rect(
                                new Point(centroidX + pointOfInterestShiftPx - halfSize,
                                centroidY - halfSize),
                                new Point(centroidX + pointOfInterestShiftPx + halfSize,
                                        centroidY + halfSize)
                        );
                        Rect northWestRect = new Rect(new Point(centroidX + pointOfInterestShiftPx * Math.cos(5 * Math.PI / 4) - halfSize,
                                        centroidY + pointOfInterestShiftPx * Math.sin(5 * Math.PI / 4) - halfSize),
                                new Point(centroidX + pointOfInterestShiftPx * Math.cos(5 * Math.PI / 4) + halfSize,
                                        centroidY + pointOfInterestShiftPx * Math.sin(5 * Math.PI / 4) + halfSize)
                        );
                        Rect westRect = new Rect(
                                new Point(centroidX - pointOfInterestShiftPx - halfSize,
                                centroidY - halfSize),
                                new Point(centroidX - pointOfInterestShiftPx + halfSize,
                                        centroidY + halfSize)
                        );
                        Rect southWestRect = new Rect(
                                new Point(centroidX + pointOfInterestShiftPx * Math.cos(3 * Math.PI / 4) - halfSize,
                                        centroidY + pointOfInterestShiftPx * Math.sin(3 * Math.PI / 4) - halfSize),
                                new Point(centroidX + pointOfInterestShiftPx * Math.cos(3 * Math.PI / 4) + halfSize,
                                        centroidY + pointOfInterestShiftPx * Math.sin(3 * Math.PI / 4) + halfSize)
                        );


                        List<Rect> allRects = Arrays.asList(
                                southRect, southEastRect, northRect, northEastRect,
                                eastRect, northWestRect, westRect, southWestRect);

                        for (Rect rect : allRects) {
                            Imgproc.rectangle(inputFrameRGB, rect.tl(), rect.br(), color, thickness);
                        }

                    }

                }




                return inputFrameRGB;
            }
        });

        // init OpenCV
        if (OpenCVLoader.initLocal()) {
            Log.d("SlimeMouldStockMonitor/OpenCV", "OpenCV loaded successfully");

            // init -> on success code
            cameraBridgeViewBase.enableView();

        } else {
            Log.e("SlimeMouldStockMonitor/OpenCV", "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }



    }

    private void getCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{ Manifest.permission.CAMERA }, 101);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (grantResults.length > 0 && grantResults[0] != PackageManager.PERMISSION_GRANTED) {
            getCameraPermission();
        }
    }

    @Override
    protected List<? extends CameraBridgeViewBase> getCameraViewList() {
        return Collections.singletonList(cameraBridgeViewBase);
    }
}