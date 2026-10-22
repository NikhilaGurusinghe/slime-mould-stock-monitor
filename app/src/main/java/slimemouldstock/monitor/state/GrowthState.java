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
        double pointOfInterestShiftMm = 35;
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
        int regionHalfSize = 20;
        Scalar color = new Scalar(255, 125, 0);
        int thickness = 2;
        Rect southRect = new Rect(
                new Point(centroidX - regionHalfSize,
                        centroidY + pointOfInterestShiftPx - regionHalfSize),
                new Point(centroidX + regionHalfSize,
                        centroidY + pointOfInterestShiftPx + regionHalfSize)
        );
        Rect southEastRect = new Rect(
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(Math.PI / 4) - regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(Math.PI / 4) - regionHalfSize),
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(Math.PI / 4) + regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(Math.PI / 4) + regionHalfSize)
        );
        Rect northRect = new Rect(
                new Point(centroidX - regionHalfSize,
                        centroidY - pointOfInterestShiftPx - regionHalfSize),
                new Point(centroidX + regionHalfSize,
                        centroidY - pointOfInterestShiftPx + regionHalfSize)
        );
        Rect northEastRect = new Rect(
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(7 * Math.PI / 4) - regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(7 * Math.PI / 4) - regionHalfSize),
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(7 * Math.PI / 4) + regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(7 * Math.PI / 4) + regionHalfSize)
        );
        Rect eastRect = new Rect(
                new Point(centroidX + pointOfInterestShiftPx - regionHalfSize,
                        centroidY - regionHalfSize),
                new Point(centroidX + pointOfInterestShiftPx + regionHalfSize,
                        centroidY + regionHalfSize)
        );
        Rect northWestRect = new Rect(new Point(centroidX + pointOfInterestShiftPx * Math.cos(5 * Math.PI / 4) - regionHalfSize,
                centroidY + pointOfInterestShiftPx * Math.sin(5 * Math.PI / 4) - regionHalfSize),
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(5 * Math.PI / 4) + regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(5 * Math.PI / 4) + regionHalfSize)
        );
        Rect westRect = new Rect(
                new Point(centroidX - pointOfInterestShiftPx - regionHalfSize,
                        centroidY - regionHalfSize),
                new Point(centroidX - pointOfInterestShiftPx + regionHalfSize,
                        centroidY + regionHalfSize)
        );
        Rect southWestRect = new Rect(
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(3 * Math.PI / 4) - regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(3 * Math.PI / 4) - regionHalfSize),
                new Point(centroidX + pointOfInterestShiftPx * Math.cos(3 * Math.PI / 4) + regionHalfSize,
                        centroidY + pointOfInterestShiftPx * Math.sin(3 * Math.PI / 4) + regionHalfSize)
        );
        Rect thresholdRect = new Rect(0, 0,
                slimeMouldDetectionMask.cols(), slimeMouldDetectionMask.rows());


        List<Rect> allRects = Arrays.asList(
                southRect, southEastRect, northRect, northEastRect,
                eastRect, northWestRect, westRect, southWestRect);
        List<String> allRectsLabels = Arrays.asList("southRect", "southEastRect", "northRect",
                "northEastRect",
                "eastRect", "northWestRect", "westRect", "southWestRect");

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

            Imgproc.rectangle(frameRGB, rect.tl(), rect.br(), color, thickness);
            Imgproc.putText(frameRGB,
                    Math.round(percentageSlimeMouldPixels * 100) + "%",
                    rect.tl(), Imgproc.FONT_HERSHEY_SIMPLEX, 1,
                    new Scalar(255, 255, 255), 2);
        }

        // TODO make a decision here and send API request, depending on if decision
        // was successful
        // this.stateManager.setState(new State(this.stateManager));
        // TODO decision here is for testing only and is hardcoded
        // this returns the unedited frame (frameRGBA)
        byte[] jpegImage = OpenCVUtils.convertMatToJPEGBytes(frameRGBA);

        if (jpegImage.length > 0) {
            APIUtils.sendStatusUpdate(SlimeMouldState.SLIME_MOULD_DECISION,
                    SlimeMouldDecision.A,
                    jpegImage,
                    System.currentTimeMillis() + "_" + UUID.randomUUID() + ".jpg");
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
