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
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.Collections;
import java.util.List;

// code credits ->
// setup
// https://www.youtube.com/watch?v=6Nl3JYpEgeI&list=PLTuKYqpidPXZjtOEjOgeKxNAe4NzSgsg8&index=3
// https://docs.opencv.org/4.13.0/d5/df8/tutorial_dev_with_OCV_on_Android.html
// center of contouring (used to center the petri dish)
// https://pyimagesearch.com/2016/02/01/opencv-center-of-contour/
// inRange thresholding
// https://docs.opencv.org/3.4.13/da/d97/tutorial_threshold_inRange.html
// https://docs.opencv.org/4.12.0/javadoc/org/opencv/core/Core.html#inRange(org.opencv.core.Mat,org.opencv.core.Scalar,org.opencv.core.Scalar,org.opencv.core.Mat)
// https://pyimagesearch.com/2014/08/04/opencv-python-color-detection/
public class MainActivity extends CameraActivity {

    CameraBridgeViewBase cameraBridgeViewBase;
    Mat inRangeThresholdMask;
    Mat inputFrameRGB;
    Mat inputFrameHSV;
    Mat maskedRGB;

    // Looking to recognise slime mould coloured pixels (in our ideal lighting)
    // in HSV
    // lowering H value helps capture more orange
    // S value is more to do with getting rid of white light - good for getting rid of shadows
    //      reflections/hue leakage of yellow, but less colour info
    // V doesn't seem to do a whole lot
    Scalar inRangeLowerBoundHSV = new Scalar(22, 200, 0);
    Scalar inRangeUpperBoundHSV = new Scalar(50, 255, 255);

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
                inputFrameHSV = new Mat();
                inRangeThresholdMask = new Mat();
                maskedRGB = new Mat();
            }

            @Override
            public void onCameraViewStopped() {

            }

            @Override
            public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
                Mat inputFrameRGBA = inputFrame.rgba();

                // RGBA -> HSV
                Imgproc.cvtColor(inputFrameRGBA, inputFrameRGB, Imgproc.COLOR_RGBA2RGB);
                Imgproc.cvtColor(inputFrameRGB, inputFrameHSV, Imgproc.COLOR_RGB2HSV);

                // ranges in HSV
                Core.inRange(inputFrameHSV, inRangeLowerBoundHSV, inRangeUpperBoundHSV, inRangeThresholdMask);

                maskedRGB.setTo(Scalar.all(0));
                // dst = maskedRGB
                Core.bitwise_and(inputFrameRGB, inputFrameRGB, maskedRGB, inRangeThresholdMask);

                return maskedRGB;
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

    void getCameraPermission() {
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