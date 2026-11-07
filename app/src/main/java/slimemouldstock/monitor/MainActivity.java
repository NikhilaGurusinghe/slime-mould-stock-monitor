package slimemouldstock.monitor;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import org.eclipse.paho.client.mqttv3.MqttException;
import org.opencv.android.CameraActivity;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;

import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

import slimemouldstock.monitor.mqtt.MqttClient;
import slimemouldstock.monitor.mqtt.MqttUtils;
import slimemouldstock.monitor.state.StateManager;

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
// run method every X seconds in android
// https://stackoverflow.com/questions/6242268/repeat-a-task-with-a-time-delay
public class MainActivity extends CameraActivity {

    // TODO a lot of this CV stuff can just be packed into an object
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
    private Mat lastFrameRGB;

    // Looking to recognise slime mould coloured pixels (in our ideal lighting)
    // in HSV
    // lowering H value helps capture more orange
    // S value is more to do with getting rid of white light - good for getting rid of shadows
    //      reflections/hue leakage of yellow, but less colour info
    // V doesn't seem to do a whole lot
    private final Scalar inRangeLowerBoundHSV = new Scalar(20, 175, 0);
    private final Scalar inRangeUpperBoundHSV = new Scalar(80, 255, 255);

    // this is used to dictate if a new frame should be analyzed from the camera
    private volatile boolean analyzeThisFrame = false;
    // how often to analyze frame (in millseconds)
    private final long ANALYZE_FRAME_DELAY_MS = 1_000;
    private final long LED_ON_TO_ANALYZABLE_FRAME_DELAY_MS = 500;

    // this is to do with analyzing the frame every X ms
    private final Handler taskHandler = new android.os.Handler(Looper.getMainLooper());
    private final Runnable updateAnalyzeThisFrameRunnable = () -> analyzeThisFrame = true;
    private final Runnable frameAnalysisCycleRunnable = new Runnable() {
        @Override
        public void run() {
            MqttUtils.sendTurnLedOn(mqttClient);

            // wait 1 second after led turn on
            taskHandler.postDelayed(updateAnalyzeThisFrameRunnable,
                    LED_ON_TO_ANALYZABLE_FRAME_DELAY_MS);
            taskHandler.postDelayed(frameAnalysisCycleRunnable, ANALYZE_FRAME_DELAY_MS);
        }
    };

    // State stuff
    StateManager stateManager;

    // MQTT Stuff, this should probably be a singleton
    MqttClient mqttClient;

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
                lastFrameRGB = new Mat();
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
                if (stateManager == null) {
                    return inputFrame.rgba();
                }

                if (!analyzeThisFrame) {
                    return lastFrameRGB.empty() ? inputFrame.rgba() : lastFrameRGB;
                }

                Log.e("slimemouldstock.monitor/StateManager",
                        String.valueOf(System.currentTimeMillis()));

                Mat inputFrameRGBA = inputFrame.rgba();

                // RGBA -> RGB
                Imgproc.cvtColor(inputFrameRGBA, inputFrameRGB, Imgproc.COLOR_RGBA2RGB);
                // RGB -> RGB + Gaussian blur
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

                        return (int) (Imgproc.contourArea(c1) - Imgproc.contourArea(c2));
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

                // LED will be activated a second before this frame via a runnable

                Mat outputFrame = stateManager.getState().onCameraFrame(
                        inputFrameRGBA, inputFrameRGB, inputFrameHSV,
                        inputFrameGRAY, maskedRGB,
                        edgeDetectionThresholdMask, inRangeThresholdMask, petriDishContour
                );

                // deactivate led - we have to do this here
                MqttUtils.sendTurnLedOff(mqttClient);

                // last frame is expecting RGB and our onCameraFrame might output anything
                if (outputFrame.channels() == 4) {
                    Imgproc.cvtColor(outputFrame, lastFrameRGB, Imgproc.COLOR_RGBA2RGB);
                } else {
                    outputFrame.copyTo(lastFrameRGB);
                }

                // reset analyze this frame flag
                analyzeThisFrame = false;
                return outputFrame;
            }
        });

        try {
            stateManager = new StateManager(null);
        } catch (MalformedURLException e) {
            Log.e("slimemouldstock.monitor/StateManager",
                    "Failed to initialize state management");
            return;
        }

        // init OpenCV
        if (OpenCVLoader.initLocal()) {
            Log.d("slimemouldstock.monitor/OpenCV", "OpenCV loaded successfully");

            // init -> on success code
            cameraBridgeViewBase.enableView();

        } else {
            Log.e("slimemouldstock.monitor/OpenCV", "OpenCV initialization failed!");
            (Toast.makeText(this, "OpenCV initialization failed!", Toast.LENGTH_LONG)).show();
            return;
        }

        // init mqtt client
        try {
            mqttClient = new MqttClient(getApplicationContext(),
                    "tcp://broker.emqx.io:1883");
        } catch (MqttException e) {
            throw new RuntimeException(e);
        }

        // testing connection
        mqttClient.publishToTopic("slime-mould-stock-monitor/led_relay/command", 2,
                true, "start");
    }

    private void getCameraPermission() {
        if (checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[]{Manifest.permission.CAMERA}, 101);
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

    @Override
    protected void onStop() {
        // handler stuff
        taskHandler.removeCallbacks(updateAnalyzeThisFrameRunnable);
        taskHandler.removeCallbacks(frameAnalysisCycleRunnable);
        super.onStop();
    }

    @Override
    protected void onStart() {
        super.onStart();
        // handler stuff
        taskHandler.removeCallbacks(updateAnalyzeThisFrameRunnable);
        taskHandler.removeCallbacks(frameAnalysisCycleRunnable);
        taskHandler.postDelayed(frameAnalysisCycleRunnable,
                ANALYZE_FRAME_DELAY_MS - LED_ON_TO_ANALYZABLE_FRAME_DELAY_MS);

    }
}