package com.facia.faciasdk.Camera.FaceDetection;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.os.Handler;

import com.facia.faciasdk.Activity.Helpers.Enums.OvalSize;
import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.R;
import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ThresholdConstants;
import com.facia.faciasdk.Utils.Constants.TimeConstants;
import com.google.mlkit.vision.face.FaceContour;

import java.util.List;

public class DetectionUiUpdater {
    private final Boolean blinkTimeout;
    private final OvalSize ovalSize;

    public DetectionUiUpdater(Boolean blinkTimeout, OvalSize ovalSize) {
        this.blinkTimeout = blinkTimeout;
        this.ovalSize = ovalSize;
    }

    /**
     * handling UI if face found precisely (edge to edge) in case of DL
     *
     * @param faceContourList list of facial landmarks
     */
    protected void faceFoundEdgeToEdgeDl(List<FaceContour> faceContourList) {
        try {
            if (SingletonData.getInstance().isOvalSizeIncreased()) {
                if (!SingletonData.getInstance().isSizeIncreasing()) {
                    SingletonData.getInstance().getFragmentCameraBinding().faceDetectInst.setText(R.string.blink_your_eyes);
                    detectEyesBlinking(faceContourList);
                } else {
                    SingletonData.getInstance().getCameraListeners().frameProcessed();
                }
            } else {
                SingletonData.getInstance().getFragmentCameraBinding().faceDetectInst.setText(R.string.hold_steady);

                if (!SingletonData.getInstance().isVideoRecording()) {
                    SingletonData.getInstance().getCameraListeners().startVideoRecording();
                }

                SingletonData.getInstance().setCurrentTimeSmallOval(System.currentTimeMillis());
                if (!SingletonData.getInstance().isSmallOvalSteadyTimerOn()) {
                    SingletonData.getInstance().setSmallOvalSteadyTimerOn(true);
                    SingletonData.getInstance().setPreviousTimeSmallOval(System.currentTimeMillis());
                } else if ((SingletonData.getInstance().getCurrentTimeSmallOval() -
                        SingletonData.getInstance().getPreviousTimeSmallOval()) >= TimeConstants.HOLD_STEADY_IN_OVAL_DL) {
                    SingletonData.getInstance().setIncreasedFrameCount(0);
                    SingletonData.getInstance().setBlinkCount(0);
                    if (!SingletonData.getInstance().isSizeIncreasing()) {
                        SingletonData.getInstance().setDetectionState("bigOval");//timer
                        SingletonData.getInstance().setDetectionTimerOn(false);//timer
                        SingletonData.getInstance().setSizeIncreasing(true);
                        changeOvalHeight(true);
                        changeOvalWidth(true);
                        SingletonData.getInstance().setOvalSizeIncreased(true);
                    }
                }
                SingletonData.getInstance().getCameraListeners().frameProcessed();
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "MediaPipe/FaceMeshResultImageView/faceFoundEdgeToEdge");
        }
    }


    /**
     * handling UI if face found precisely (edge to edge) in case of QL
     */
    protected void faceFoundEdgeToEdgeQl(Bitmap bitmap) {
        try {
            if (!SingletonData.getInstance().isQlReqInProcess()) {
                SingletonData.getInstance().setCurrentTimeSmallOval(System.currentTimeMillis());
                if (!SingletonData.getInstance().isSmallOvalSteadyTimerOn()) {
                    SingletonData.getInstance().setSmallOvalSteadyTimerOn(true);
                    SingletonData.getInstance().setPreviousTimeSmallOval(System.currentTimeMillis());
                    SingletonData.getInstance().getCameraListeners().frameProcessed();
//            } else if (SingletonData.getInstance().getQuickLivenessFrameCount() == ThresholdConstants.QL_MIN_FACE_DETECTION) {
                } else if (SingletonData.getInstance().getQlFrameList().size() == ThresholdConstants.QL_MIN_FACE_DETECTION) {
                    //                if ((SingletonData.getInstance().getCurrentTimeSmallOval() -
//                    SingletonData.getInstance().getPreviousTimeSmallOval()) >= TimeConstants.HOLD_STEADY_IN_OVAL_QL) {

//                SingletonData.getInstance().getFragmentCameraBinding().faceBorder.setBackgroundResource(
//                        R.drawable.face_border_bg_success);
                    SingletonData.getInstance().getFragmentCameraBinding().faceDetectInst.setText(R.string.perfect);
                    SingletonData.getInstance().getCameraListeners().processQuickLiveness(bitmap);
                } else {
//                SingletonData.getInstance().getFragmentCameraBinding().faceBorder.setBackgroundResource(
//                        R.drawable.face_border_bg_success);
                    SingletonData.getInstance().getFragmentCameraBinding().faceDetectInst.setText(R.string.hold_steady);

//                if (!SingletonData.getInstance().isVideoRecording()) {
//                    SingletonData.getInstance().getCameraListeners().startVideoRecording();
//                }

                    SingletonData.getInstance().getCameraListeners().frameProcessed();
                }
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "MediaPipe/FaceMeshResultImageView/faceFoundEdgeToEdge");
        }
    }

    /**
     * method to handle UI if face is not detected precisely
     * invoking further method to stop recording too
     *
     * @param error error to be displayed
     */
    protected void faceNotFitted(String error) {
        try {
            SingletonData.getInstance().setBlinkCount(0);
//            SingletonData.getInstance().getFragmentCameraBinding().faceBorder.setBackgroundResource(R.drawable.face_border_bg_red);
            SingletonData.getInstance().getFragmentCameraBinding().faceDetectInst.setText(error);
            if (!SingletonData.getInstance().isOvalSizeIncreased()) {
                SingletonData.getInstance().getCameraListeners().stopVideoRecording();
            }
            SingletonData.getInstance().getCameraListeners().frameProcessed();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "MediaPipe/FaceMeshResultImageView/faceNotFitted");
        }
    }

    /**
     * method to handle UI
     * if no face is detected in the frame
     * will stop recording, resetting blink count
     * if oval size is increased, will reduce it
     */
    protected void noFaceDetectedInTheImage(String state) {
        try {
            SingletonData.getInstance().setBlinkCount(0);
            SingletonData.getInstance().getFragmentCameraBinding().faceDetectInst.setText(
                    state.equals("noFace") ? R.string.no_face_found : R.string.position_face_in_oval);
            if (SingletonData.getInstance().isOvalSizeIncreased() &&
                    !SingletonData.getInstance().isSizeIncreasing()) {
                SingletonData.getInstance().setSizeIncreasing(true);
                changeOvalHeight(false);
                changeOvalWidth(false);
                SingletonData.getInstance().setOvalSizeIncreased(false);

                SingletonData.getInstance().setDetectionTimerOn(false);//timer
                SingletonData.getInstance().setDetectionState("smallOval");//timer
            }
            SingletonData.getInstance().getCameraListeners().stopVideoRecording();
            SingletonData.getInstance().getCameraListeners().frameProcessed();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "MediaPipe/FaceMeshResultImageView/noFaceDetectedInTheImage");
        }
    }

    /**
     * method to detect eyes blinking
     * if eye blinked, oval size will be increased and UI will be updated
     */
    private void detectEyesBlinking(List<FaceContour> faceContourList) {
        try {
            //timer
            if (blinkTimeout) {
                SingletonData.getInstance().setDetectionState("blink");
                SingletonData.getInstance().setCurrent(System.currentTimeMillis());
                if (!SingletonData.getInstance().isDetectionTimerOn()) {
                    SingletonData.getInstance().setDetectionTimerOn(true);
                    SingletonData.getInstance().setPrevious(System.currentTimeMillis());
                } else if (SingletonData.getInstance().getCurrent() - SingletonData.getInstance().getPrevious() >=
                        TimeConstants.BLINK_TIMEOUT) {
                    SingletonData.getInstance().getCameraListeners().cameraTimeOut();
                    return;
                }
            }
            //timer
            SingletonData.getInstance().setBlinkCurrent(System.currentTimeMillis());
            if (!SingletonData.getInstance().isBlinkTimerOn()) {
                SingletonData.getInstance().setBlinkTimerOn(true);
                SingletonData.getInstance().setBlinkPrevious(System.currentTimeMillis());
            }

            float currentOpenDist = faceContourList.get(FaceContour.LEFT_EYE - 1).getPoints().get(12).y - faceContourList.get(FaceContour.LEFT_EYE - 1).getPoints().get(4).y;
            if (SingletonData.getInstance().getMaxOpenDist() == 0.0f) {
                SingletonData.getInstance().setMaxOpenDist(currentOpenDist);
                SingletonData.getInstance().getCameraListeners().frameProcessed();
            } else {
                if (currentOpenDist > SingletonData.getInstance().getMaxOpenDist()) {
                    SingletonData.getInstance().setMaxOpenDist(currentOpenDist);
                } else if (currentOpenDist < SingletonData.getInstance().getMaxOpenDist() / 3 ||
                        (faceContourList.get(FaceContour.LEFT_EYE - 1).getPoints().get(12).y - faceContourList.get(FaceContour.LEFT_EYE - 1).getPoints().get(4).y <
                                (faceContourList.get(FaceContour.LEFT_EYE - 1).getPoints().get(3).x - faceContourList.get(FaceContour.LEFT_EYE - 1).getPoints().get(0).x) / ThresholdConstants.EYE_BLINK_DIVISOR &&
                                faceContourList.get(FaceContour.RIGHT_EYE - 1).getPoints().get(12).y - faceContourList.get(FaceContour.RIGHT_EYE - 1).getPoints().get(4).y <
                                        (faceContourList.get(FaceContour.RIGHT_EYE - 1).getPoints().get(3).x - faceContourList.get(FaceContour.RIGHT_EYE - 1).getPoints().get(0).x) / ThresholdConstants.EYE_BLINK_DIVISOR)) {
                    SingletonData.getInstance().setBlinkCount(SingletonData.getInstance().getBlinkCount() + 1);
                }
                if (SingletonData.getInstance().getBlinkCount() >= ThresholdConstants.MIN_EYE_BLINK_DETECTION &&
                        SingletonData.getInstance().getBlinkCurrent() - SingletonData.getInstance().getBlinkPrevious() >=
                                TimeConstants.MIN_BLINK_DETECTION_TIMER) {
//                if ((SingletonData.getInstance().getBlinkCount() >= ThresholdConstants.MIN_EYE_BLINK_DETECTION &&
//                        SingletonData.getInstance().getBlinkCurrent() - SingletonData.getInstance().getBlinkPrevious() >=
//                                TimeConstants.MIN_BLINK_DETECTION_TIMER) ||
//                        SingletonData.getInstance().getBlinkCurrent() - SingletonData.getInstance().getBlinkPrevious() >=
//                                TimeConstants.BLINK_DETECTION_TIMER) {
                    SingletonData.getInstance().getFragmentCameraBinding().faceDetectInst.setText(R.string.perfect);
                    SingletonData.getInstance().setFaceFinalized(true);
                    SingletonData.getInstance().getFragmentCameraBinding().showInstBtn.setEnabled(false);
                    SingletonData.getInstance().getCameraListeners().stopVideoRecording();
                    new Handler().postDelayed(() -> {
//                        SingletonData.getInstance().setBlinkCount(0);
//                        SingletonData.getInstance().setOvalSizeIncreased(false);
//                        SingletonData.getInstance().setDetectionTimerOn(false);//timer
//                        SingletonData.getInstance().setDetectionState("smallOval");//timer

                        try {
                            if (SingletonData.getInstance().isFaceFinalized()) {
                                SingletonData.getInstance().getFragmentCameraBinding().showInstBtn.setEnabled(true);
                                SingletonData.getInstance().setFaceFinalized(false);
                                noFaceDetectedInTheImage("noFace");
                            }
                        }catch (Exception e){
                            Webhooks.exceptionReport(e, "DetectionUiUpdater/detectEyesBlinking/Handler");
                        }
                    }, 2000);

                } else {
                    SingletonData.getInstance().getCameraListeners().frameProcessed();
                }
            }
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "DetectionUiUpdater/detectEyesBlinking");
        }
    }

    private double getHeightPercentage(Boolean isToIncreaseOvalSize) {
        double smallHeightPercent, largeHeightPercent, xLargeHeightPercent, defaultHeightPercent;
        if (isToIncreaseOvalSize) {
            if (ovalSize == OvalSize.SMALL) {
                smallHeightPercent = ThresholdConstants.EXTENDED_SMALL_OVAL_HEIGHT_SMALL_SCR;
                largeHeightPercent = ThresholdConstants.EXTENDED_SMALL_OVAL_HEIGHT_LARGE_SCR;
                xLargeHeightPercent = ThresholdConstants.EXTENDED_SMALL_OVAL_HEIGHT_XLARGE_SCR;
                defaultHeightPercent = ThresholdConstants.EXTENDED_SMALL_OVAL_HEIGHT_DEFAULT_SCR;
            } else if (ovalSize == OvalSize.LARGE) {
                smallHeightPercent = ThresholdConstants.EXTENDED_LARGE_OVAL_HEIGHT_SMALL_SCR;
                largeHeightPercent = ThresholdConstants.EXTENDED_LARGE_OVAL_HEIGHT_LARGE_SCR;
                xLargeHeightPercent = ThresholdConstants.EXTENDED_LARGE_OVAL_HEIGHT_XLARGE_SCR;
                defaultHeightPercent = ThresholdConstants.EXTENDED_LARGE_OVAL_HEIGHT_DEFAULT_SCR;
            } else {
                smallHeightPercent = ThresholdConstants.EXTENDED_MEDIUM_OVAL_HEIGHT_SMALL_SCR;
                largeHeightPercent = ThresholdConstants.EXTENDED_MEDIUM_OVAL_HEIGHT_LARGE_SCR;
                xLargeHeightPercent = ThresholdConstants.EXTENDED_MEDIUM_OVAL_HEIGHT_XLARGE_SCR;
                defaultHeightPercent = ThresholdConstants.EXTENDED_MEDIUM_OVAL_HEIGHT_DEFAULT_SCR;
            }
        } else {
            if (ovalSize == OvalSize.SMALL) {
                smallHeightPercent = ThresholdConstants.SMALL_OVAL_HEIGHT_SMALL_SCR;
                largeHeightPercent = ThresholdConstants.SMALL_OVAL_HEIGHT_LARGE_SCR;
                xLargeHeightPercent = ThresholdConstants.SMALL_OVAL_HEIGHT_XLARGE_SCR;
                defaultHeightPercent = ThresholdConstants.SMALL_OVAL_HEIGHT_DEFAULT_SCR;
            } else if (ovalSize == OvalSize.LARGE) {
                smallHeightPercent = ThresholdConstants.LARGE_OVAL_HEIGHT_SMALL_SCR;
                largeHeightPercent = ThresholdConstants.LARGE_OVAL_HEIGHT_LARGE_SCR;
                xLargeHeightPercent = ThresholdConstants.LARGE_OVAL_HEIGHT_XLARGE_SCR;
                defaultHeightPercent = ThresholdConstants.LARGE_OVAL_HEIGHT_DEFAULT_SCR;
            } else {
                smallHeightPercent = ThresholdConstants.MEDIUM_OVAL_HEIGHT_SMALL_SCR;
                largeHeightPercent = ThresholdConstants.MEDIUM_OVAL_HEIGHT_LARGE_SCR;
                xLargeHeightPercent = ThresholdConstants.MEDIUM_OVAL_HEIGHT_XLARGE_SCR;
                defaultHeightPercent = ThresholdConstants.MEDIUM_OVAL_HEIGHT_DEFAULT_SCR;
            }
        }

        if ((SingletonData.getInstance().getActivity().getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_SMALL) {
            return smallHeightPercent;
        } else if ((SingletonData.getInstance().getActivity().getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_LARGE) {
            return largeHeightPercent;
        } else if ((SingletonData.getInstance().getActivity().getResources().getConfiguration().screenLayout &
                Configuration.SCREENLAYOUT_SIZE_MASK) == Configuration.SCREENLAYOUT_SIZE_XLARGE) {
            return xLargeHeightPercent;
        } else {
            return defaultHeightPercent;
        }
    }

    /**
     * method to change (increase/decrease) oval's height
     *
     * @param isToIncrease is to increase height or to decrease (true/false)
     */
    protected void changeOvalHeight(Boolean isToIncrease) {
        try {
            int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
            int ovalHeight = (int) (screenHeight * getHeightPercentage(isToIncrease));
            ValueAnimator va;
            va = ValueAnimator.ofInt(SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getHeight(), ovalHeight);
            va.setDuration(TimeConstants.OVAL_ANIMATION_TIME);
            va.addUpdateListener(animation -> {
                SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getLayoutParams().height =
                        (Integer) animation.getAnimatedValue();
                SingletonData.getInstance().getFragmentCameraBinding().faceBorder.requestLayout();
            });
            va.start();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "MediaPipe/FaceMeshResultImageView/changeOvalHeight");
        }
    }

    /**
     * method to change (increase/decrease) oval's width
     *
     * @param isToIncrease is to increase width or to decrease (true/false)
     */
    protected void changeOvalWidth(Boolean isToIncrease) {
        try {
            int screenHeight = Resources.getSystem().getDisplayMetrics().heightPixels;
            int screenWidth = Resources.getSystem().getDisplayMetrics().widthPixels;
            int ovalHeight = (int) (screenHeight * getHeightPercentage(isToIncrease));
            int ovalWidth = (int) (ovalHeight * ThresholdConstants.OVAL_HEIGHT_MULTIPLIER_FOR_WIDTH);
            ValueAnimator va;
            va = ValueAnimator.ofInt(
                    SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getWidth(),
                    ovalWidth >= screenWidth ? (int) (screenWidth * ThresholdConstants.OVAL_WIDTH_MULTIPLIER_FOR_WIDTH) : ovalWidth);
            va.setDuration(TimeConstants.OVAL_ANIMATION_TIME);
            va.addUpdateListener(animation -> {
                SingletonData.getInstance().getFragmentCameraBinding().faceBorder.getLayoutParams().width =
                        (Integer) animation.getAnimatedValue();
                SingletonData.getInstance().getFragmentCameraBinding().faceBorder.requestLayout();
            });

            va.addListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    SingletonData.getInstance().setSizeIncreasing(false);
                }
            });
            va.start();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "MediaPipe/FaceMeshResultImageView/changeOvalWidth");
        }
    }
}
