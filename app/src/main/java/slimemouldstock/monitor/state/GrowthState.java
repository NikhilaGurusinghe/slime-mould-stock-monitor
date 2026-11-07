package slimemouldstock.monitor.state;

import static slimemouldstock.monitor.utils.OpenCVUtils.intersectRect;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;
import org.opencv.imgproc.Moments;

import java.util.Arrays;
import java.util.List;
import java.util.UUID;

import slimemouldstock.monitor.http.api.APIUtils;
import slimemouldstock.monitor.http.api.status.SlimeMouldDecision;
import slimemouldstock.monitor.http.api.status.SlimeMouldState;
import slimemouldstock.monitor.utils.OpenCVUtils;

public class GrowthState extends State {

    private final int CYCLE_COUNT = 2;

    private final int DETECTION_AREA_THRESHOLD_PERCENTAGE = 10;

    public GrowthState(StateManager stateManager) {
        super(stateManager);
    }

    // only frameRGB here is assumed as being non-empty/everything else here might be empty
    @Override
    public Mat onCameraFrame(Mat frameRGBA, Mat frameRGB, Mat frameHSV, Mat frameGRAY,
                             Mat slimeMouldRGB, Mat edgeDetectionMask, Mat slimeMouldDetectionMask,
                             MatOfPoint petriDishContour) {
        if (petriDishContour == null) {
            return frameRGB;
        }

        // draw centroid of petri dish
        Moments petriDishMoment = Imgproc.moments(petriDishContour);

        if (petriDishMoment.m00 == 0) {
            return frameRGB;
        }

        int centroidX = (int) (petriDishMoment.m10 / petriDishMoment.m00);
        int centroidY = (int) (petriDishMoment.m01 / petriDishMoment.m00);

        // camera calibration using the petri dish's size
        // we get the bounding rectangle to calculate the width of the dish in pixels
        Rect dishBoundingRect = Imgproc.boundingRect(petriDishContour);
        Imgproc.rectangle(frameRGB, dishBoundingRect.tl(),
                dishBoundingRect.br(), new Scalar(0, 0, 255), 3);
        double petriDishDiameterMm = 90.0; // 9cm petri dish
        double petriDishDiameterPx =
                (dishBoundingRect.width + dishBoundingRect.height) / 2.0;
        double pointOfInterestShiftMm = 25;
        double pixelsPerMm = petriDishDiameterPx / petriDishDiameterMm;
        int pointOfInterestShiftPx = (int)
                Math.round(pointOfInterestShiftMm * pixelsPerMm);

        // centre of dish contour
        Imgproc.circle(frameRGB, new Point(centroidX, centroidY), 30,
                new Scalar(255, 0, 0), 5);
        // drawing largest contour
        Imgproc.drawContours(frameRGB, List.of(petriDishContour), 0,
                new Scalar(0, 255, 0), 2);

        // drawing detection squares
        int regionHalfSize = 40;
        Scalar color = new Scalar(255, 125, 0);
        int thickness = 2;

        Rect southEastRect = new Rect(
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(Math.PI / 4) - regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(Math.PI / 4) - regionHalfSize),
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(Math.PI / 4) + regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(Math.PI / 4) + regionHalfSize)
        );
        Rect northEastRect = new Rect(
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(7 * Math.PI / 4) - regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(7 * Math.PI / 4) - regionHalfSize),
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(7 * Math.PI / 4) + regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(7 * Math.PI / 4) + regionHalfSize)
        );
        Rect northWestRect = new Rect(new Point(centroidX + pointOfInterestShiftPx * Math.cos(5 * Math.PI / 4) - regionHalfSize,
                centroidY + pointOfInterestShiftPx * Math.sin(5 * Math.PI / 4) - regionHalfSize),
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(5 * Math.PI / 4) + regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(5 * Math.PI / 4) + regionHalfSize)
        );
        Rect southWestRect = new Rect(
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(3 * Math.PI / 4) - regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(3 * Math.PI / 4) - regionHalfSize),
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(3 * Math.PI / 4) + regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(3 * Math.PI / 4) + regionHalfSize)
        );
        Rect thresholdRect = new Rect(0, 0,
                slimeMouldDetectionMask.cols(), slimeMouldDetectionMask.rows());

        // this is hard coded for 0 and 1 so change this if CYCLE_COUNT changes
        List<Rect> allRects;
        switch (stateManager.growthCycleNumber) {
            case 0:
                allRects = Arrays.asList(northEastRect, southWestRect);
                break;
            case 1:
                allRects = Arrays.asList(northWestRect, southEastRect);
                break;
            default:
                allRects = Arrays.asList(southEastRect, northEastRect,
                        northWestRect, southWestRect);
                break;
        }

        // TODO this tie breaks based on order if slime mould reaches both at the same time
        //  this tie breaking strategy is not very random and could influence randomness
        //  in a situation where the slime mould reaches both at the same time
        //  could shuffle based on random number generated by image hash
        for (int i = 0; i < allRects.size(); i++) {
            Rect rect = allRects.get(i);

            // clip rectangles into our threshold mask
            Rect clippedRect = intersectRect(rect, thresholdRect);

            if (clippedRect.width <= 0 || clippedRect.height <= 0) {
                continue;
            }

            Mat rectMask = slimeMouldDetectionMask.submat(clippedRect);
            int numberSlimeMouldPixels = Core.countNonZero(rectMask);
            double percentageSlimeMouldPixels =
                    (double) numberSlimeMouldPixels / clippedRect.area();

            // TODO make a decision here and send API request, depending on if decision
            if (percentageSlimeMouldPixels >= DETECTION_AREA_THRESHOLD_PERCENTAGE) {
                // slime mould was successful

                // send all the things we need to send over the wire
                // this returns the unedited frame (frameRGBA)
                byte[] jpegImage = OpenCVUtils.convertMatToJPEGBytes(frameRGBA);

                if (jpegImage.length > 0) {
                    APIUtils.sendStatusUpdate(SlimeMouldState.SLIME_MOULD_DECISION,
                            i == 0 ? SlimeMouldDecision.A : SlimeMouldDecision.B,
                            jpegImage,
                            System.currentTimeMillis() + "_" + UUID.randomUUID() + ".jpg");
                }

                // increment our number of cycles
                stateManager.growthCycleNumber = (stateManager.growthCycleNumber + 1) % CYCLE_COUNT;
                // leave this state
                this.stateManager.setState(new TrayOutState(this.stateManager));
            }

            Imgproc.rectangle(frameRGB, rect.tl(), rect.br(), color, thickness);
            Imgproc.putText(frameRGB,
                    Math.round(percentageSlimeMouldPixels * 100) + "%",
                    rect.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);

            rectMask.release();
        }

        return frameRGB;
    }

    @Override
    public void onEnterState() {

    }

    @Override
    public void onExitState() {

    }
}
