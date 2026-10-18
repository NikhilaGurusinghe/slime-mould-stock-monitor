package slimemouldstock.monitor.state;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

public class TrayInState extends State {
    public TrayInState(StateManager stateManager) {
        super(stateManager);
    }

    @Override
    public Mat onCameraFrame(Mat frameRGBA, Mat frameRGB, Mat frameHSV, Mat frameGRAY,
                             Mat slimeMouldRGB, Mat edgeDetectionMask, Mat slimeMouldDetectionMask,
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
