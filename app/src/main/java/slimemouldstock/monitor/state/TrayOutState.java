package slimemouldstock.monitor.state;

import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.List;

public class TrayOutState extends State {

    public TrayOutState(StateManager stateManager) {
        super(stateManager);
    }

    @Override
    public Mat onCameraFrame(Mat frameRGBA, Mat frameRGB, Mat frameHSV, Mat frameGRAY,
                             Mat slimeMouldRGB, Mat edgeDetectionMask, Mat slimeMouldDetectionMask,
                             MatOfPoint petriDishContour) {
        int TRAY_IN_SLIME_MOULD_PERCENTAGE = 2;

        if (petriDishContour == null || petriDishContour.empty()) {
            return slimeMouldRGB;
        }

        Mat petriDishMask = Mat.zeros(slimeMouldDetectionMask.rows(),
                                        slimeMouldDetectionMask.cols(),
                                        CvType.CV_8UC1);
        Mat slimeMouldInsidePetriDishMask = new Mat();

        // draw contours as completely filled in petriDishMask
        Imgproc.drawContours(petriDishMask, List.of(petriDishContour), 0,
                new Scalar(255), Imgproc.FILLED);
        Core.bitwise_and(slimeMouldDetectionMask, petriDishMask, slimeMouldInsidePetriDishMask);

        // calculations
        // waiting for detection of ANY slime-coloured pixels
        int slimeMouldPixels = Core.countNonZero(slimeMouldInsidePetriDishMask);
        long totalDishPixels = Core.countNonZero(petriDishMask);
        long percentageOfSlimeMouldPixels = Math.round(
                totalDishPixels != 0 ? 100.0 * slimeMouldPixels / totalDishPixels : 0);

        // just some debug diagnostic on the screen
        Imgproc.drawContours(frameRGB, List.of(petriDishContour), 0,
                new Scalar(0, 255, 0), 2);
        Imgproc.putText(frameRGB,
                Math.round(percentageOfSlimeMouldPixels) + "%",
                new Point(1, 50), Imgproc.FONT_HERSHEY_SIMPLEX, 1,
                new Scalar(255, 255, 255), 2);

        // we want to see if greater than TRAY_IN_SLIME_MOULD_PERCENTAGE%
        if (percentageOfSlimeMouldPixels >= TRAY_IN_SLIME_MOULD_PERCENTAGE) {
            // transition to next state
            this.stateManager.setState(new GrowthState(this.stateManager));
        }

        petriDishMask.release();
        slimeMouldInsidePetriDishMask.release();

        return frameRGB;
    }

    @Override
    public void onEnterState() {

    }

    @Override
    public void onExitState() {

    }
}
