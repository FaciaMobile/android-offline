package com.facia.faciasdk.Utils.Constants;
public class ApiConstants {

    /**
     * constant values
     */


    public static final String REQUEST_MODEL = "request_model";
    public static final String SDK_VERSION = "android-offline 1.0.0";
    public static final String MERCHANT_APP_LOGO = "ic_merchant_app_logo";
    public static final int MAX_RESULT_API_CALL_COUNT = 39; //+1

//    Server base URL
//    public static final String BASIC_URL = "https://app.facia.ai/";
    public static final String BASIC_URL = "https://api.facia.ai/";


//    public static final String BASIC_URL = "https://staging.facia.ai/";
//    public static final String BASIC_URL = "https://dev3.facia.ai/";
//    public static final String BASIC_URL = "https://demo.facia.ai/";
    public static final String GET_IP_URL = "https://ifconfig.me/ip";

    // API basic URL
//    public static final String API_BASIC_URL = "/backend/api/transaction/";
    // API end points
//    public static final String CREATE_TRANSACTION = API_BASIC_URL + "create-transaction";
    public static final String LIVENESS = "liveness";
    public static  final String PERFORM_KYC = "kyc";

    //    public static final String LIVENESS_RESULT = API_BASIC_URL + "liveness-result";
    public static final String LIVENESS_RESULT = "result";

//    public static final String CHECK_LIVENESS = API_BASIC_URL + "check-liveness";
    public static final String CHECK_LIVENESS = "check-liveness";

//    public static final String CREATE_FEEDBACK = API_BASIC_URL + "create-feedback";


    // photoIDMatch
    public static final String FACE_MATCH = "face-match";
//    upload Frames BG
    public static final String UPLOAD_FRAMES = "upload-frames";

    /**
     * Links for production web hooks
     */
    public static final String PRODUCTION_CRASH_REPORT_URL = "https://app.whistleit.io/api/webhooks/63c93851a45229315d5a74df";
    public static final String API_RESPONSE = "https://app.whistleit.io/api/webhooks/63c90daeeecb595fb3470d02";
    public static final String EXCEPTION_REPORT = "https://app.whistleit.io/api/webhooks/63cfc1bc454d6850e8764cb4";
    public static final String TESTING_WEBHOOKS = "https://app.whistleit.io/api/webhooks/63cfc1fedd9ed127aa4ce386";

//    private static final String DEV_LINK = "https://app.whistleit.io/api/webhooks/655e031d40fcfd2fcf2a5727";
//    /**
//     * Links for testing web hooks
//     */
//    public static final String PRODUCTION_CRASH_REPORT_URL = DEV_LINK;
//    public static final String API_RESPONSE = DEV_LINK;
//    public static final String EXCEPTION_REPORT = DEV_LINK;
//    public static final String TESTING_WEBHOOKS = DEV_LINK;

}
