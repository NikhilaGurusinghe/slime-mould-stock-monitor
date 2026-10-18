package slimemouldstock.monitor.State;

import org.opencv.android.CameraBridgeViewBase;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

// TODO
// handle a tick counter for when to take a photo -> deciding state
// handle states such as when the dish is not available/slime mould is deciding
// plan state diagram!!
public abstract class State {
    public abstract void onCameraFrame(Mat frameRGBA, Mat frameRGB, Mat frameHSV, Mat frameGRAY,
                                       Mat edgeDetectionMask, Mat slimeMouldDetectionMask,
                                       MatOfPoint petriDishContour);
    public abstract void onEnterState();
    public abstract void onExitState();
}
