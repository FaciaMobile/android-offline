package com.facia.faciasdk.Logs;

import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;


import com.facia.faciasdk.Singleton.SingletonData;
import com.facia.faciasdk.Utils.Constants.ApiConstants;
import com.facia.faciasdk.Utils.Utilities;

import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class Webhooks {

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
                                        Utilities.SimilarMethods.getDeviceDetails() + " (hardware: " + Build.HARDWARE + ")").appendQueryParameter("Device_ID:",
                                        "Device_Id: " + Build.ID + " Android_Version: " + Build.VERSION.SDK_INT)
                                .appendQueryParameter("URLs:", ApiConstants.BASIC_URL);
                        setUrlConnection(ApiConstants.PRODUCTION_CRASH_REPORT_URL, builder);
                    } catch (Exception e) {
                        Webhooks.exceptionReport(e, "ErrorLogs : sendStacktraceReport/inner-catch");
                    }
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ErrorLogs : sendStacktraceReport-catch");
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
     * To send Api response issues
     */
    public static void apiReport(String issue, String errorMessage, String endpoint) {
        try {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    try {
                        Uri.Builder builder = new Uri.Builder().appendQueryParameter("Title", "Facia_API_Report, SDK Version: " +
                                        ApiConstants.SDK_VERSION)
                                .appendQueryParameter("Error Message", errorMessage)
                                .appendQueryParameter("Issue", issue)
                                .appendQueryParameter("Reference ID", SingletonData.getInstance().getReferenceId())
                                .appendQueryParameter("Device Name", Utilities.SimilarMethods.getDeviceDetails() + " (hardware: " + Build.HARDWARE + ")")
                                .appendQueryParameter("Device_ID", "Device_Id: " + Build.ID + " Android_Version: " +
                                        Build.VERSION.SDK_INT).appendQueryParameter("API", ApiConstants.BASIC_URL + endpoint);
                        setUrlConnection(ApiConstants.API_RESPONSE, builder);
                    } catch (Exception e) {
                        Webhooks.exceptionReport(e, "ErrorLogs : apiReport/inner-catch");
                    }
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ErrorLogs : apiReport-catch");
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
                                .appendQueryParameter("Device Name:", Utilities.SimilarMethods.getDeviceDetails() + " (hardware: " + Build.HARDWARE + ")")
                                .appendQueryParameter("Device_ID:", "Device_Id: " + Build.ID + " Android_Version: " +
                                        Build.VERSION.SDK_INT)
                                .appendQueryParameter("URLs:", ApiConstants.BASIC_URL)
                                .appendQueryParameter("ReferenceID:", SingletonData.getInstance().getReferenceId());
                        setUrlConnection(ApiConstants.EXCEPTION_REPORT, builder);
                    } catch (Exception e) {
                        Webhooks.exceptionReport(e, "ErrorLogs : FaciaException/inner-catch");
                    }
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ErrorLogs : exceptionReport-catch");
        }
    }

    /**
     * To send Exceptions (caught through try catch)
     */
    public static void testingValues(final String message) {
        try {
            new AsyncTask<Void, Void, String>() {
                @Override
                protected String doInBackground(Void... voids) {
                    try {
                        Uri.Builder builder = new Uri.Builder().appendQueryParameter("Title: ", "Testing_Values, " +
                                        "App Version: " + ApiConstants.SDK_VERSION)
                                .appendQueryParameter("Message:", message)
                                .appendQueryParameter("Device Name:", Utilities.SimilarMethods.getDeviceDetails() + " (hardware: " + Build.HARDWARE + ")")
                                .appendQueryParameter("Device_ID:", "Device_Id: " + Build.ID + " Android_Version: " +
                                        Build.VERSION.SDK_INT)
                                .appendQueryParameter("URLs:", ApiConstants.BASIC_URL)
                                .appendQueryParameter("ReferenceID:", SingletonData.getInstance().getReferenceId());
                        setUrlConnection(ApiConstants.TESTING_WEBHOOKS, builder);
                    } catch (Exception e) {
                        Webhooks.exceptionReport(e, "ErrorLogs : testingValues/inner-catch");
                    }
                    return null;
                }
            }.execute();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ErrorLogs : testingValues-catch");
        }
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
            byte[] outputBytes = query.getBytes(StandardCharsets.UTF_8);
            OutputStream os = connection.getOutputStream();
            os.write(outputBytes);
            ((HttpURLConnection) connection).getResponseCode();
            os.close();
            connection.connect();
        } catch (Exception e) {
            Webhooks.exceptionReport(e, "ErrorLogs : setUrlConnection-catch");
        }
    }

}