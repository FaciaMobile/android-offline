package com.facia.faciademo;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;

import com.facia.faciasdk.Logs.Webhooks;
import com.facia.faciasdk.Utils.Constants.ApiConstants;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ErrorLogs {

    /**
     * To send App crash report
     *
     * @param message Exception containing all detail about crash
     * @param name    class name where crash happened
     */
    public static void sendStacktraceReport(final Exception message, final String name) {
        try {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    try {
                        Uri.Builder builder = new Uri.Builder().appendQueryParameter("Title: ", "<b>Facia_Crash_Report</b>, SDK Version: "
                                        + ApiConstants.SDK_VERSION).appendQueryParameter("Error_Message:", "<b>" + message.toString() + "</b>")
                                .appendQueryParameter("Stack_Trace:", getCrashPath(message)).appendQueryParameter("Device Name:",
                                        Build.FINGERPRINT + " (harware: " + Build.HARDWARE + ")").appendQueryParameter("Device_ID:",
                                        "Device_Id: " + Build.ID + " Android_Version: " + String.valueOf(android.os.Build.VERSION.SDK_INT))
                                .appendQueryParameter("URLs:", ApiConstants.BASIC_URL);
                        setUrlConnection(ApiConstants.PRODUCTION_CRASH_REPORT_URL, builder);
                    } catch (Exception e) {
                        Webhooks.exceptionReport(e, "ErrorLogs : sendStacktraceReport/inner-catch");
                    }
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            ErrorLogs.exceptionReport(e, "ErrorLogs : sendStacktraceReport-catch");
        }
    }

    /**
     * To send Api response issues
     *
     * @param message Api Issue
     * @param name    where it happened
     */
    public static void apiReport(String email, String message, final String name) {
        try {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    try {
                        Uri.Builder builder = new Uri.Builder().appendQueryParameter("Title: ", "App_API_Report, APP_version:" + ApiConstants.SDK_VERSION)
                                .appendQueryParameter("Error_Message:", message)
                                .appendQueryParameter("Name:", name)
                                .appendQueryParameter("Url:", ApiConstants.BASIC_URL)
                                .appendQueryParameter("Device Name:", Build.FINGERPRINT + " (harware: " + Build.HARDWARE + ")")
                                .appendQueryParameter("Device_ID:", "Device_Id: " + Build.ID + " Android_Version: " +
                                        String.valueOf(Build.VERSION.SDK_INT))
                                .appendQueryParameter("UserInfo:", email);
                        setUrlConnection(ApiConstants.API_RESPONSE, builder);
                    } catch (Exception e) {
                        exceptionReport(e, "ErrorLogs : apiReport-catch-inner");
                    }
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            exceptionReport(e, "ErrorLogs : apiReport-catch");
        }
    }

    /**
     * To send testing values
     *
     * @param message Message to send to webhook
     * @param path    where it happened
     */
    public static void testMessage(String message, final String path) {
        try {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    try {
                        Uri.Builder builder = new Uri.Builder().appendQueryParameter("Title: ", "App_Test_Message, APP_version:" + ApiConstants.SDK_VERSION)
                                .appendQueryParameter("Message:", message)
                                .appendQueryParameter("Path:", path)
                                .appendQueryParameter("Url:", ApiConstants.BASIC_URL)
                                .appendQueryParameter("Device Name:", Build.FINGERPRINT + " (harware: " + Build.HARDWARE + ")")
                                .appendQueryParameter("Device_ID:", "Device_Id: " + Build.ID + " Android_Version: " +
                                        String.valueOf(Build.VERSION.SDK_INT));
                        setUrlConnection(ApiConstants.TESTING_WEBHOOKS, builder);
                    } catch (Exception e) {
                        exceptionReport(e, "ErrorLogs : testMessage-catch-inner");
                    }
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            exceptionReport(e, "ErrorLogs : testMessage-catch");
        }
    }

    /**
     * To send Exceptions (caught through try catch)
     *
     * @param exception Exception that we caught
     * @param name    where it happened
     */
    public static void exceptionReport(final Exception exception, final String name) {
        try {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    try {
                        Uri.Builder builder = new Uri.Builder().appendQueryParameter("Title: ", "Facia_Exception_Report, " +
                                        "App Version: " + ApiConstants.SDK_VERSION)
                                .appendQueryParameter("Error_Message:", exception.toString())
                                .appendQueryParameter("Class Name:", name)
                                .appendQueryParameter("Stack_Trace:", getCrashPath(exception))
                                .appendQueryParameter("Device Name:", Build.FINGERPRINT + " (harware: " + Build.HARDWARE + ")")
                                .appendQueryParameter("Device_ID:", "Device_Id: " + Build.ID + " Android_Version: " +
                                        String.valueOf(android.os.Build.VERSION.SDK_INT))
                                .appendQueryParameter("URLs:", ApiConstants.BASIC_URL);
                        setUrlConnection(ApiConstants.EXCEPTION_REPORT, builder);
                    } catch (Exception e) {
                        Webhooks.exceptionReport(e, "ErrorLogs : moodyTryCatchReport/inner-catch");
                    }
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            exceptionReport(e, "ErrorLogs : exceptionReport-catch");
        }
    }

    /**
     * method to get the crash path
     *
     * @param message message to show
     * @return the crash path
     */
    private static String getCrashPath(Exception message) {
        StringBuilder crashPath = new StringBuilder();
        for (int i = 0; i < message.getStackTrace().length; i++) {
            crashPath.append(message.getStackTrace()[i]).append("\n");
        }
        return crashPath.toString();
    }

    /**
     * Method to set the URL connection
     *
     * @param CrashReportUrl path to send the data
     * @param builder        data to send
     */
    private static void setUrlConnection(String CrashReportUrl, Uri.Builder builder) {
        try {
            URL url = new URL(CrashReportUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setDoOutput(true);
            connection.setDoInput(true);
            connection.setAllowUserInteraction(false);
            connection.setRequestProperty("Connection", "Keep-Alive");
            connection.setConnectTimeout(90000);
            connection.setReadTimeout(90000);
            connection.setRequestMethod("POST");
            String query = builder.build().getEncodedQuery();
            byte[] outputBytes = query.getBytes("UTF-8");
            OutputStream os = connection.getOutputStream();
            os.write(outputBytes);
            ((HttpURLConnection) connection).getResponseCode();
            os.close();
            connection.connect();
        } catch (Exception e) {
            exceptionReport(e, "ErrorLog : setURLConnection-catch");
        }
    }

}