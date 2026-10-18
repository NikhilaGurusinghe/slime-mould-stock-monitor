package slimemouldstock.monitor.state;

import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;

// TODO
// handle a tick counter for when to take a photo -> deciding state
// handle states such as when the dish is not available/slime mould is deciding
// plan state diagram!!
public abstract class State {

    private final StateManager stateManager;

    public State(StateManager stateManager) {
        this.stateManager = stateManager;
    }

    public abstract Mat onCameraFrame(Mat frameRGBA, Mat frameRGB, Mat frameHSV, Mat frameGRAY,
                                       Mat slimeMouldRGB,
                                       Mat edgeDetectionMask, Mat slimeMouldDetectionMask,
                                       MatOfPoint petriDishContour);
    public abstract void onEnterState();
    public abstract void onExitState();
}
