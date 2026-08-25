package com.facia.faciasdk.Camera.FaceDetection;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Rect;
import android.util.Log;
import android.view.View;

import androidx.appcompat.widget.AppCompatImageView;

import com.facia.faciasdk.Activity.Helpers.Enums.FaceDetectionThreshold;
import com.facia.faciasdk.Activity.Helpers.Enums.OvalSize;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ThresholdConstants;
import com.google.gson.JsonArray;
import com.google.mlkit.vision.face.Face;
import com.google.mlkit.vision.face.FaceContour;

import java.util.List;

@SuppressLint("ViewConstructor")
public class FaceDetectionHelper extends AppCompatImageView {
    private final Boolean blinkTimeout;
    private final FaceDetectionThreshold faceDetectionThreshold;
    private final OvalSize ovalSize;

    public FaceDetectionHelper(Context context, Boolean blinkTimeout,
                               FaceDetectionThreshold faceDetectionThreshold, OvalSize ovalSize) {
        super(context);
        this.blinkTimeout = blinkTimeout;
        this.faceDetectionThreshold = faceDetectionThreshold;
        this.ovalSize = ovalSize;
        setScaleType(ScaleType.FIT_CENTER);
    }

    /**
     * check if face detected
     * if yes then calling further method to detect face angle, position etc
     */
    @SuppressLint({"SetTextI18n", "ResourceAsColor"})
    public void setFaceMeshResult(Face face, Bitmap bitmap, Boolean isDimLight) {
        try {
            if (face != null) {
                checkFacePosition(face.getAllContours(), bitmap);
            } else {
                if (SingletonData.getInstance().isQuickLiveness() && !SingletonData.getInstance().isQuickRequestInProcess()) {
                    SingletonData.getInstance().setQuickLivenessFrameCount(0);
                    SingletonData.getInstance().getActivity().runOnUiThread(() -> {
                        SingletonData.getInstance().getFragmentCameraBinding().quickLivenessInstTxt.setText(R.string.position_face_in_front_of_camera);
                        SingletonData.getInstance().getFragmentCameraBinding().quickLivenessInst.setVisibility(View.GONE);
//                        SingletonData.getInstance().getFragmentCameraBinding().quickLivenessInst.setVisibility(View.VISIBLE);
                    });
                }
                if (isDimLight){
                    updateUiOnFacePositionChanged("dimLight", null, null);
                }else {
                    updateUiOnFacePositionChanged("noFace", null, null);
                }
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/FaceDetectionHelper/setFaceMeshResult");
        }
    }

    /**
     * method to check whether the face is tilted or moved toward left or right
     *
     * @return returning if the face is tilted or moved towards left or right (true/false)
     */
    private Boolean isFaceTiltedOrMovedQuickLiveness(List<FaceContour> faceContourList) {
        float faceRightSide, faceLeftSide, greaterDistSide, marginForTiltedFace;
        faceRightSide = (faceContourList.get(0).getPoints().get(8).x) -
                (faceContourList.get(0).getPoints().get(18).x);
        faceLeftSide = (faceContourList.get(0).getPoints().get(18).x) -
                (faceContourList.get(0).getPoints().get(28).x);

        greaterDistSide = Float.max(faceRightSide, faceLeftSide);
        marginForTiltedFace = (float) (greaterDistSide - (greaterDistSide / 2.3));
        return !(faceRightSide > marginForTiltedFace) || !(faceLeftSide > marginForTiltedFace);
    }

    /**
     * method to check face position and invoking further method to
     * set UI according to face position (oval color, text etc)
     *
     * @param faceContourList contour list
     */
    @SuppressLint("ResourceAsColor")
    private void checkFacePosition(List<FaceContour> faceContourList, Bitmap bitmap) {
        try {
            Rect ovalRect = getOvalRect();
            Rect faceRect = getFaceRect(faceContourList);
            if (SingletonData.getInstance().isQuickLiveness()) {
                Rect ovalRectWithMargin = getOvalRectWithMargin();
                checkFacePositionQl(faceRect, ovalRect, ovalRectWithMargin, faceContourList, bitmap);
            } else if (ovalRect.contains(faceRect)) {
                faceWithinOval(faceContourList, faceRect, ovalRect, bitmap);
            } else if (ovalRect.intersect(faceRect)) {
                if (faceRect.height() > getOvalRect().height() && faceRect.width() > getOvalRect().width()) {
                    updateUiOnFacePositionChanged("moveAway", faceContourList, bitmap);
                } else {
                    updateUiOnFacePositionChanged("fitFace", faceContourList, bitmap);
                }
            } else {
                updateUiOnFacePositionChanged("positionFace", faceContourList, bitmap);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/FaceDetectionHelper/drawLandmarksOnCanvas");
        }
    }

    /**
     * method to check face position in case of QL
     * verifying if face is straight and fitted
     *
     * @param faceRect        Points of face position
     * @param faceContourList facial contours
     */
    private void checkFacePositionQl(Rect faceRect, Rect ovalRect, Rect ovalRectWithMargin, List<FaceContour> faceContourList, Bitmap bitmap) {
        try {
            if (ovalRectWithMargin.contains(faceRect)){
                if (faceRect.width() > ovalRect.width() / ThresholdConstants.QL_OVAL_THRESHOLD_LOW &&
                        faceRect.height() > ovalRect.height() / ThresholdConstants.QL_OVAL_THRESHOLD_LOW) {
                    if (!isFaceTiltedOrMovedQuickLiveness(faceContourList)) {
                        SingletonData.getInstance().setQuickLivenessFrameCount(
                                SingletonData.getInstance().getQuickLivenessFrameCount() + 1);
                        SingletonData.getInstance().getCameraListeners().convertBitmapToBase64(bitmap);
                        updateUiOnFacePositionChanged("faceFound", faceContourList, bitmap);
                    }else {
                        SingletonData.getInstance().setQuickLivenessFrameCount(0);
                        updateUiOnFacePositionChanged("straightFace", faceContourList, bitmap);
                        SingletonData.getInstance().getCameraListeners().frameProcessed();
                    }
                } else {
                    SingletonData.getInstance().setQuickLivenessFrameCount(0);
                    updateUiOnFacePositionChanged("moveCloser", faceContourList, bitmap);
                }
            }else if (ovalRectWithMargin.intersect(faceRect)){
                if (faceRect.height() > ovalRectWithMargin.height() && faceRect.width() > ovalRectWithMargin.width()) {
                    SingletonData.getInstance().setQuickLivenessFrameCount(0);
                    updateUiOnFacePositionChanged("moveAway", faceContourList, bitmap);
                } else {
                    SingletonData.getInstance().setQuickLivenessFrameCount(0);
                    updateUiOnFacePositionChanged("fitFace", faceContourList, bitmap);
                }
            }else {
                SingletonData.getInstance().setQuickLivenessFrameCount(0);
                updateUiOnFacePositionChanged("positionFace", faceContourList, bitmap);
            }


//            if (getScreenRect().contains(faceRect)) {
//                if (!isFaceTiltedOrMovedQuickLiveness(faceContourList)) {
//                    SingletonData.getInstance().setQuickLivenessFrameCount(
//                            SingletonData.getInstance().getQuickLivenessFrameCount() + 1);
//                    SingletonData.getInstance().getActivity().runOnUiThread(() ->
//                            SingletonData.getInstance().getFragmentCameraBinding().quickLivenessInst.setVisibility(View.GONE));
//                    if (SingletonData.getInstance().getQuickLivenessFrameCount() == ThresholdConstants.QL_MIN_FACE_DETECTION) {
//                        SingletonData.getInstance().getCameraListeners().processQuickLiveness();
//                    } else {
//                        SingletonData.getInstance().getCameraListeners().frameProcessed();
//                    }
//                } else {
//                    SingletonData.getInstance().setQuickLivenessFrameCount(0);
//                    SingletonData.getInstance().getActivity().runOnUiThread(() -> {
//                        SingletonData.getInstance().getFragmentCameraBinding().quickLivenessInstTxt.setText(R.string.straighten_your_face);
//                        SingletonData.getInstance().getFragmentCameraBinding().quickLivenessInst.setVisibility(View.VISIBLE);
//                    });
//                    SingletonData.getInstance().getCameraListeners().frameProcessed();
//                }
//            } else {
//                SingletonData.getInstance().setQuickLivenessFrameCount(0);
//                SingletonData.getInstance().getCameraListeners().frameProcessed();
//            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "Camera/FaceDetectionHelper/checkFacePositionQl");
        }
    }

//    private void checkFacePositionQl(Rect faceRect, List<FaceContour> faceContourList){
//        try{
//            if (getScreenRect().contains(faceRect)) {
//                if (!isFaceTiltedOrMovedQuickLiveness(faceContourList)) {
//                    SingletonData.getInstance().setQuickLivenessFrameCount(
//                            SingletonData.getInstance().getQuickLivenessFrameCount() + 1);
//                    SingletonData.getInstance().getActivity().runOnUiThread(() ->
//                            SingletonData.getInstance().getFragmentCameraBinding().quickLivenessInst.setVisibility(View.GONE));
//                    if (SingletonData.getInstance().getQuickLivenessFrameCount() == ThresholdConstants.QL_MIN_FACE_DETECTION) {
//                        SingletonData.getInstance().getCameraListeners().processQuickLiveness();
//                    } else {
//                        SingletonData.getInstance().getCameraListeners().frameProcessed();
//                    }
//                } else {
//                    SingletonData.getInstance().setQuickLivenessFrameCount(0);
//                    SingletonData.getInstance().getActivity().runOnUiThread(() -> {
//                        SingletonData.getInstance().getFragmentCameraBinding().quickLivenessInstTxt.setText(R.string.straighten_your_face);
//                        SingletonData.getInstance().getFragmentCameraBinding().quickLivenessInst.setVisibility(View.VISIBLE);
//                    });
//                    SingletonData.getInstance().getCameraListeners().frameProcessed();
//                }
//            } else {
//                SingletonData.getInstance().setQuickLivenessFrameCount(0);
//                SingletonData.getInstance().getCameraListeners().frameProcessed();
//            }
//        } catch (Exception e) {
//            Webhooks.exceptionReport(e, "Camera/FaceDetectionHelper/checkFacePositionQl");
//        }
//    }

    /**
     * method to get Rect of face
     */
    private Rect getFaceRect(List<FaceContour> faceContourList) {
        return new Rect((int) (faceContourList.get(0).getPoints().get(28).x),
                (int) (faceContourList.get(0).getPoints().get(0).y),
                (int) (faceContourList.get(0).getPoints().get(8).x),
                (int) (faceContourList.get(0).getPoints().get(18).y));
    }

    /**
     * method to get Rect of oval
     */
    private Rect getOvalRect() {
        return new Rect((int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getX(),
                (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getY(),
                (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getX() +
                        (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getWidth(),
                (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getY() +
                        (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getHeight());
    }

    /**
     * method to get Rect of oval with extra margins to sides
     */
    private Rect getOvalRectWithMargin() {
        int percentage = 10;
        int widthMargin = (int) (SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getWidth() * (percentage / 100.0f));
        int heightMargin = (int) (SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getHeight() * (percentage / 100.0f));

        return new Rect((int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getX() - widthMargin,
                (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getY() - heightMargin,
                (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getX() +
                        (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getWidth() + widthMargin,
                (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getY() +
                        (int) SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getHeight() + heightMargin);
    }

    /**
     * method to get Rect of screen size
     */
    private Rect getScreenRect() {
        float widthMargin, heightMargin;
        widthMargin = (10 / 100) * SingletonData.getInstance().getFragmentCameraBinding().previewView.getWidth();
        heightMargin = (15 / 100) * SingletonData.getInstance().getFragmentCameraBinding().previewView.getHeight();

        return new Rect((int) (SingletonData.getInstance().getFragmentCameraBinding().previewView.getX() + widthMargin),
                (int) (SingletonData.getInstance().getFragmentCameraBinding().previewView.getY() + heightMargin),
                (int) (SingletonData.getInstance().getFragmentCameraBinding().previewView.getWidth() - widthMargin),
                (int) (SingletonData.getInstance().getFragmentCameraBinding().previewView.getHeight() - heightMargin));
    }

    /**
     * method to check whether the face is tilted or moved toward left or right
     *
     * @return returning if the face is tilted or moved towards left or right (true/false)
     */
    private Boolean isFaceTiltedOrMoved(List<FaceContour> faceContourList) {
        float faceRightSide, faceLeftSide, greaterDistSide, marginForTiltedFace;
        faceRightSide = (faceContourList.get(0).getPoints().get(8).x) -
                (faceContourList.get(0).getPoints().get(18).x);
        faceLeftSide = (faceContourList.get(0).getPoints().get(18).x) -
                (faceContourList.get(0).getPoints().get(28).x);

        greaterDistSide = Float.max(faceRightSide, faceLeftSide);
        marginForTiltedFace = greaterDistSide - (greaterDistSide / ThresholdConstants.FACE_TILTED_DIVISOR);
        return !(faceRightSide > marginForTiltedFace) || !(faceLeftSide > marginForTiltedFace);
    }

    /**
     * method to check whether the face is fitted in the oval (edge to edge)
     *
     * @param faceContourList list of facial landmarks
     * @param faceRect        Rect of face
     * @param ovalRect        Rect of oval (UI view)
     */
    private void faceWithinOval(List<FaceContour> faceContourList, Rect faceRect, Rect ovalRect, Bitmap bitmap) {
        try {
            if (!isFaceTiltedOrMoved(faceContourList)) {
                double smallThresholdDivisor = ThresholdConstants.SMALL_OVAL_THRESHOLD_MEDIUM;
                double largeThresholdDivisor = ThresholdConstants.LARGE_OVAL_THRESHOLD_MEDIUM;
                switch (faceDetectionThreshold) {
                    case LOW:
                        smallThresholdDivisor = ThresholdConstants.SMALL_OVAL_THRESHOLD_LOW;
                        largeThresholdDivisor = ThresholdConstants.LARGE_OVAL_THRESHOLD_LOW;
                        break;
                    case HIGH:
                        smallThresholdDivisor = ThresholdConstants.SMALL_OVAL_THRESHOLD_HIGH;
                        largeThresholdDivisor = ThresholdConstants.LARGE_OVAL_THRESHOLD_HIGH;
                        break;
                }
                if (SingletonData.getInstance().isOvalSizeIncreased()) {
                    if (faceRect.width() > ovalRect.width() / largeThresholdDivisor &&
                            faceRect.height() > ovalRect.height() / largeThresholdDivisor) {
                        updateUiOnFacePositionChanged("faceFound", faceContourList, bitmap);
                    } else {
                        updateUiOnFacePositionChanged("moveCloser", faceContourList, bitmap);
                    }
                } else {
                    if (faceRect.width() > ovalRect.width() / smallThresholdDivisor &&
                            faceRect.height() > ovalRect.height() / smallThresholdDivisor) {
                        updateUiOnFacePositionChanged("faceFound", faceContourList, bitmap);
                    } else {
                        updateUiOnFacePositionChanged("moveCloser", faceContourList, bitmap);
                    }
                }
            } else {
                updateUiOnFacePositionChanged("straightFace", faceContourList, bitmap);
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/FaceDetectionHelper/faceWithinOval");
        }
    }

    /**
     * handling UI according to the face position (comparing to the oval)
     *
     * @param state           state according to face position
     * @param faceContourList list of facial landmarks
     */
    private void updateUiOnFacePositionChanged(final String state, List<FaceContour> faceContourList, Bitmap bitmap) {
        try {
            if (!SingletonData.getInstance().isQuickRequestInProcess() && !SingletonData.getInstance().isFaceFinalized()) {
                DetectionUiUpdater cameraInterface = new DetectionUiUpdater(blinkTimeout, ovalSize);
                SingletonData.getInstance().getActivity().runOnUiThread(() -> {
//                    if (!SingletonData.getInstance().isQuickLiveness()) {
//                        SingletonData.getInstance().getFragmentCameraBinding().cameraInstParentLayout.setVisibility(VISIBLE);
//                    }
                    switch (state) {
                        case "faceFound" -> {
                            //timer
                            if (!SingletonData.getInstance().getDetectionState().equals("bigOval")) {
                                if (!SingletonData.getInstance().isFaceDetectedFirstTime()) {
                                    SingletonData.getInstance().setFaceDetectedFirstTime(true);
                                    SingletonData.getInstance().setDetectionTimerOn(false);
                                }
                            }
                            if (SingletonData.getInstance().isQuickLiveness()) {
                                cameraInterface.faceFoundEdgeToEdgeQl(bitmap);
                            } else {
                                cameraInterface.faceFoundEdgeToEdgeDl(faceContourList);
                            }
                        }
                        case "moveCloser" -> {
                            //timer
                            if (!SingletonData.getInstance().getDetectionState().equals("bigOval") &&
                                    !SingletonData.getInstance().getDetectionState().equals("smallOval")) {
                                SingletonData.getInstance().setDetectionTimerOn(false);
                                SingletonData.getInstance().setDetectionState("smallOval");
                            }
                            SingletonData.getInstance().setBlinkTimerOn(false);
                            SingletonData.getInstance().setSmallOvalSteadyTimerOn(false);
                            cameraInterface.faceNotFitted(SingletonData.getInstance().getActivity().getString(R.string.move_closer));
                            SingletonData.getInstance().setQlFrameList(new JsonArray());
                        }
                        case "moveAway" -> {
                            //timer
                            if (!SingletonData.getInstance().getDetectionState().equals("bigOval") &&
                                    !SingletonData.getInstance().getDetectionState().equals("smallOval")) {
                                SingletonData.getInstance().setDetectionTimerOn(false);
                                SingletonData.getInstance().setDetectionState("smallOval");
                            }
                            SingletonData.getInstance().setBlinkTimerOn(false);
                            SingletonData.getInstance().setSmallOvalSteadyTimerOn(false);
                            cameraInterface.faceNotFitted(SingletonData.getInstance().getActivity().getString(R.string.move_away));
                            SingletonData.getInstance().setQlFrameList(new JsonArray());
                        }
                        case "fitFace" -> {
                            if (!SingletonData.getInstance().getDetectionState().equals("bigOval") &&
                                    !SingletonData.getInstance().getDetectionState().equals("smallOval")) {
                                SingletonData.getInstance().setDetectionTimerOn(false);
                                SingletonData.getInstance().setDetectionState("smallOval");
                            }
                            SingletonData.getInstance().setBlinkTimerOn(false);
                            SingletonData.getInstance().setSmallOvalSteadyTimerOn(false);
                            cameraInterface.faceNotFitted(SingletonData.getInstance().getActivity().getString(R.string.fit_face_in_oval));
                            SingletonData.getInstance().setQlFrameList(new JsonArray());
                        }
                        case "straightFace" -> {
                            if (!SingletonData.getInstance().getDetectionState().equals("bigOval") &&
                                    !SingletonData.getInstance().getDetectionState().equals("smallOval")) {
                                SingletonData.getInstance().setDetectionTimerOn(false);
                                SingletonData.getInstance().setDetectionState("smallOval");
                            }
                            SingletonData.getInstance().setBlinkTimerOn(false);
                            SingletonData.getInstance().setSmallOvalSteadyTimerOn(false);
                            cameraInterface.faceNotFitted(SingletonData.getInstance().getActivity().getString(R.string.straighten_your_face));
                            SingletonData.getInstance().setQlFrameList(new JsonArray());
                        }
                        case "dimLight" -> {
                            if (!SingletonData.getInstance().getDetectionState().equals("bigOval") &&
                                    !SingletonData.getInstance().getDetectionState().equals("smallOval")) {
                                SingletonData.getInstance().setDetectionTimerOn(false);
                                SingletonData.getInstance().setDetectionState("smallOval");
                            }
                            SingletonData.getInstance().setBlinkTimerOn(false);
                            SingletonData.getInstance().setSmallOvalSteadyTimerOn(false);
                            cameraInterface.faceNotFitted(getContext().getString(R.string.increase_light_for_better_scanning));
                            SingletonData.getInstance().setQlFrameList(new JsonArray());
                        }
                        case "positionFace", "noFace" -> {
                            if (!SingletonData.getInstance().getDetectionState().equals("bigOval") &&
                                    !SingletonData.getInstance().getDetectionState().equals("smallOval")) {
                                SingletonData.getInstance().setDetectionTimerOn(false);
                                SingletonData.getInstance().setDetectionState("smallOval");
                            }
                            SingletonData.getInstance().setBlinkTimerOn(false);
                            SingletonData.getInstance().setSmallOvalSteadyTimerOn(false);
                            cameraInterface.noFaceDetectedInTheImage(state);
                            SingletonData.getInstance().setQlFrameList(new JsonArray());
                        }
                    }
                });
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "CameraFragment/FaceDetectionHelper/updateUiOnFacePositionChanged");
        }
    }
}
