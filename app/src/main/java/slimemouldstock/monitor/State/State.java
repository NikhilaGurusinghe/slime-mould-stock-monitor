package slimemouldstock.monitor.State;

import org.opencv.android.CameraBridgeViewBase;

// TODO
// handle a tick counter for when to take a photo -> deciding state
// handle states such as when the dish is not available/slime mould is deciding
// plan state diagram!!
public abstract class State {
    public abstract void onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame);
    public abstract void onEnterState();
    public abstract void onExitState();
}
