package slimemouldstock.monitor.State;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

public class TrayOutState extends State {
    @Override
    public void onCameraFrame(Mat frameRGBA, Mat frameRGB, Mat frameHSV, Mat frameGRAY,
                              Mat edgeDetectionMask, Mat slimeMouldDetectionMask,
                              MatOfPoint petriDishContour) {

    }

    @Override
    public void onEnterState() {

    }

    @Override
    public void onExitState() {

    }
}
