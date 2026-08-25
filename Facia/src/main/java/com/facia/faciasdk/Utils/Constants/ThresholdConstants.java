package com.facia.faciasdk.Utils.Constants;

public class ThresholdConstants {
    public static final double MIN_CARD_DETECTION_CONFIDENCE = 0.84;
    public static final double CARD_BOX_DIVIDER = 2;
    // Document auto-capture stability gate: the document must stay valid (in-box, correct size,
    // confident) for at least this many consecutive frames AND this long before it is captured.
    // Both must be satisfied; any invalid frame resets the accumulator. Reduces blurry captures
    // by giving the camera/document time to stabilize (autofocus to converge).
    public static final int CARD_STABILITY_MIN_FRAMES = 6;
    public static final long CARD_STABILITY_MIN_DURATION_MS = 800;
    public static final double QL_MIN_FACE_DETECTION = 9;
//    public static final double QL_MIN_FACE_DETECTION = 5;
    public static final int SUGGESTIONS_TEXT_MAX_LIMIT = 500;
    public static final int RECURSIVE_QL = 3;
    public static final int FACE_TILTED_DIVISOR = 3;
    public static final int MIN_FACE_DETECTION_BIG_OVAL = 2;
    public static final int MIN_FACE_DETECTION_SMALL_OVAL = 5;
    public static final double EYE_BLINK_DIVISOR = 1.25;
    public static final int MIN_EYE_BLINK_DETECTION = 1;
//    public static final double OVAL_HEIGHT_MULTIPLIER_FOR_WIDTH = 0.72;
    public static final double OVAL_HEIGHT_MULTIPLIER_FOR_WIDTH = 0.67;//new val acc to SP
    public static final double OVAL_WIDTH_MULTIPLIER_FOR_WIDTH = 0.9;

    public static final double QL_OVAL_THRESHOLD_LOW = 1.75;

    //Face detection Threshold
    public static final double SMALL_OVAL_THRESHOLD_LOW = 1.65;
    public static final double SMALL_OVAL_THRESHOLD_MEDIUM = 1.5;
    public static final double SMALL_OVAL_THRESHOLD_HIGH = 1.3;

    public static final double LARGE_OVAL_THRESHOLD_LOW = 1.55;
    public static final double LARGE_OVAL_THRESHOLD_MEDIUM = 1.4;
    public static final double LARGE_OVAL_THRESHOLD_HIGH = 1.3;


    //To set Oval Height (multiplier)
    //according to small, large, xlarge, medium screens
    //for simple and expanded oval, (expanded oval: big oval, after eye blink detection)
    public static final double SMALL_OVAL_HEIGHT_SMALL_SCR = 0.4;
    public static final double SMALL_OVAL_HEIGHT_DEFAULT_SCR = 0.3;
    public static final double SMALL_OVAL_HEIGHT_LARGE_SCR = 0.25;
    public static final double SMALL_OVAL_HEIGHT_XLARGE_SCR = 0.2;

    public static final double MEDIUM_OVAL_HEIGHT_SMALL_SCR = 0.5;
    public static final double MEDIUM_OVAL_HEIGHT_DEFAULT_SCR = 0.4;
    public static final double MEDIUM_OVAL_HEIGHT_LARGE_SCR = 0.35;
    public static final double MEDIUM_OVAL_HEIGHT_XLARGE_SCR = 0.3;

    public static final double LARGE_OVAL_HEIGHT_SMALL_SCR = 0.6;
    public static final double LARGE_OVAL_HEIGHT_DEFAULT_SCR = 0.5;
    public static final double LARGE_OVAL_HEIGHT_LARGE_SCR = 0.45;
    public static final double LARGE_OVAL_HEIGHT_XLARGE_SCR = 0.4;

    public static final double EXTENDED_SMALL_OVAL_HEIGHT_SMALL_SCR = 0.5;
    public static final double EXTENDED_SMALL_OVAL_HEIGHT_DEFAULT_SCR = 0.4;
    public static final double EXTENDED_SMALL_OVAL_HEIGHT_LARGE_SCR = 0.35;
    public static final double EXTENDED_SMALL_OVAL_HEIGHT_XLARGE_SCR = 0.3;

    public static final double EXTENDED_MEDIUM_OVAL_HEIGHT_SMALL_SCR = 0.6;
    public static final double EXTENDED_MEDIUM_OVAL_HEIGHT_DEFAULT_SCR = 0.5;
    public static final double EXTENDED_MEDIUM_OVAL_HEIGHT_LARGE_SCR = 0.45;
    public static final double EXTENDED_MEDIUM_OVAL_HEIGHT_XLARGE_SCR = 0.4;

    public static final double EXTENDED_LARGE_OVAL_HEIGHT_SMALL_SCR = 0.7;
    public static final double EXTENDED_LARGE_OVAL_HEIGHT_DEFAULT_SCR = 0.6;
    public static final double EXTENDED_LARGE_OVAL_HEIGHT_LARGE_SCR = 0.55;
    public static final double EXTENDED_LARGE_OVAL_HEIGHT_XLARGE_SCR = 0.5;

    /**
     * camera permission
     */
    public static final String[] REQUIRED_PERMISSIONS_CAMERA = new String[]{"android.permission.CAMERA"};
    public static final int CAMERA_PERMISSION_CODE_CONSENT = 200;
    public static final int CAMERA_PERMISSION_CODE_FACE_LIVENESS = 201;
    public static final int CAMERA_PERMISSION_CODE_MATCH_ID = 202;
    public static final int CAMERA_PERMISSION_CODE_CAPTURE = 203;
    public static final int CAMERA_PERMISSION_CODE_DOC_LIVENESS = 204;
    public static final int CAMERA_PERMISSION_CODE_ID_CARD_DOC = 205;
    public static final int CAMERA_PERMISSION_CODE_PASSPORT_DOC = 206;
}
