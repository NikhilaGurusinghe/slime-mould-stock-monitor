package slimemouldstock.monitor.State;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

public class TrayInState extends State {
    @Override
    public Mat onCameraFrame(Mat frameRGBA, Mat frameRGB, Mat frameHSV, Mat frameGRAY,
                             Mat edgeDetectionMask, Mat slimeMouldDetectionMask,
                             MatOfPoint petriDishContour) {

        return new Mat();
    }

    @Override
    public void onEnterState() {

    }

    @Override
    public void onExitState() {

    }
}
