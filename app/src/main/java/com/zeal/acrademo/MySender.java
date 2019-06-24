package com.zeal.acrademo;


import android.content.Context;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.acra.ACRA;
import org.acra.ACRAConstants;
import org.acra.ReportField;
import org.acra.collections.ImmutableSet;
import org.acra.collector.CrashReportData;
import org.acra.config.ACRAConfiguration;
import org.acra.sender.HttpSender;
import org.acra.sender.ReportSender;
import org.acra.sender.ReportSenderException;
import org.acra.util.JSONReportBuilder;

import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.acra.ACRA.LOG_TAG;

public class MySender implements ReportSender {
    /**
     * Available HTTP methods to send data. Only POST and PUT are currently
     * supported.
     */
    public enum Method {
        POST, PUT
    }

    /**
     * Type of report data encoding, currently supports Html Form encoding and
     * JSON.
     */
    public enum Type {
        /**
         * Send data as a www form encoded list of key/values.
         * @see <a href="http://www.w3.org/TR/html401/interact/forms.html#h-17.13.4">Form content types</a>
         */
        FORM {
            @NonNull
            @Override
            public String getContentType() {
                return "application/x-www-form-urlencoded";
            }
        },
        /**
         * Send data as a structured JSON tree.
         */
        JSON {
            @NonNull
            @Override
            public String getContentType() {
                return "application/json";
            }
        };

        @NonNull
        public abstract String getContentType();
    }




    private final ACRAConfiguration config;
    @Nullable
    private final Uri mFormUri;
    private final Map<ReportField, String> mMapping;
    private final HttpSender.Method mMethod;
    private final HttpSender.Type mType;
    @Nullable
    private String mUsername;
    @Nullable
    private String mPassword;

    /**
     * <p>
     * Create a new HttpSender instance with its destination taken from the supplied config.
     * </p>
     *
     * @param config    AcraConfig declaring the
     * @param method
     *            HTTP {@link HttpSender.Method} to be used to send data. Currently only
     *            {@link HttpSender.Method#POST} and {@link HttpSender.Method#PUT} are available. If
     *            {@link HttpSender.Method#PUT} is used, the {@link ReportField#REPORT_ID}
     *            is appended to the formUri to be compliant with RESTful APIs.
     *
     * @param type
     *            {@link HttpSender.Type} of encoding used to send the report body.
     *            {@link HttpSender.Type#FORM} is a simple Key/Value pairs list as defined
     *            by the application/x-www-form-urlencoded mime type.
     *
     * @param mapping
     *            Applies only to {@link HttpSender.Method#POST} method parameter. If null,
     *            POST parameters will be named with {@link ReportField} values
     *            converted to String with .toString(). If not null, POST
     *            parameters will be named with the result of
     *            mapping.get(ReportField.SOME_FIELD);
     */
    public MySender(@NonNull ACRAConfiguration config, @NonNull HttpSender.Method method, @NonNull HttpSender.Type type, @Nullable Map<ReportField, String> mapping) {
        this(config, method, type, null, mapping);
    }

    /**
     * <p>
     * Create a new HttpPostSender instance with a fixed destination provided as
     * a parameter. Configuration changes to the formUri are not applied.
     * </p>
     *
     * @param config    AcraConfig declaring the
     * @param method
     *            HTTP {@link HttpSender.Method} to be used to send data. Currently only
     *            {@link HttpSender.Method#POST} and {@link HttpSender.Method#PUT} are available. If
     *            {@link HttpSender.Method#PUT} is used, the {@link ReportField#REPORT_ID}
     *            is appended to the formUri to be compliant with RESTful APIs.
     *
     * @param type
     *            {@link HttpSender.Type} of encoding used to send the report body.
     *            {@link HttpSender.Type#FORM} is a simple Key/Value pairs list as defined
     *            by the application/x-www-form-urlencoded mime type.
     * @param formUri
     *            The URL of your server-side crash report collection script.
     * @param mapping
     *            Applies only to {@link HttpSender.Method#POST} method parameter. If null,
     *            POST parameters will be named with {@link ReportField} values
     *            converted to String with .toString(). If not null, POST
     *            parameters will be named with the result of
     *            mapping.get(ReportField.SOME_FIELD);
     */
    public MySender(@NonNull ACRAConfiguration config, @NonNull HttpSender.Method method, @NonNull HttpSender.Type type, @Nullable String formUri, @Nullable Map<ReportField, String> mapping) {
        this.config = config;
        mMethod = method;
        mFormUri = (formUri == null) ? null : Uri.parse(formUri);
        mMapping = mapping;
        mType = type;
        mUsername = null;
        mPassword = null;
    }

    /**
     * <p>
     * Set credentials for this HttpSender that override (if present) the ones
     * set globally.
     * </p>
     *
     * @param username
     *            The username to set for HTTP Basic Auth.
     * @param password
     *            The password to set for HTTP Basic Auth.
     */
    @SuppressWarnings( "unused" )
    public void setBasicAuth(@Nullable String username, @Nullable String password) {
        mUsername = username;
        mPassword = password;
    }

    @Override
    public void send(@NonNull Context context, @NonNull CrashReportData report) throws ReportSenderException {

        try {
            URL reportUrl = mFormUri == null ? new URL(config.formUri()) : new URL(mFormUri.toString());
            if (ACRA.DEV_LOGGING) ACRA.log.d(LOG_TAG, "Connect to " + reportUrl.toString());

            final String login = mUsername != null ? mUsername : isNull(config.formUriBasicAuthLogin()) ? null : config.formUriBasicAuthLogin();
            final String password = mPassword != null ? mPassword : isNull(config.formUriBasicAuthPassword()) ? null : config.formUriBasicAuthPassword();

            final MyHttpRequest request = new MyHttpRequest(config);
            request.setConnectionTimeOut(config.connectionTimeout());
            request.setSocketTimeOut(config.socketTimeout());
            request.setLogin(login);
            request.setPassword(password);
            request.setHeaders(config.getHttpHeaders());

            // Generate report body depending on requested type
            final String reportAsString;
            switch (mType) {
                case JSON:
                    reportAsString = report.toJSON().toString();
                    break;
                case FORM:
                default:
                    final Map<String, String> finalReport = remap2(report);
                    reportAsString = MyHttpRequest.getParamsAsFormString(finalReport);
                    break;
            }

            // Adjust URL depending on method
            switch (mMethod) {
                case POST:
                    break;
                case PUT:
                    reportUrl = new URL(reportUrl.toString() + '/' + report.getProperty(ReportField.REPORT_ID));
                    break;
                default:
                    throw new UnsupportedOperationException("Unknown method: " + mMethod.name());
            }
            request.send(context, reportUrl, mMethod, reportAsString, mType);

        } catch (@NonNull IOException e) {
            throw new ReportSenderException("Error while sending " + config.reportType()
                    + " report via Http " + mMethod.name(), e);
        } catch (@NonNull JSONReportBuilder.JSONReportException e) {
            throw new ReportSenderException("Error while sending " + config.reportType()
                    + " report via Http " + mMethod.name(), e);
        }
    }


    @NonNull
    private Map<String, String> remap2(@NonNull CrashReportData report) {
        final Map<String, String> finalReport = new HashMap<String, String>(report.size());
        try {

            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
            finalReport.put("sign",MD5Util.crypt(dateFormat.format(new Date())+"5a2f956673127"));
            finalReport.put("context",report.toJSON().toString());
            finalReport.put("message","中文说明");
            finalReport.put("channel","渠道");

        }catch (Exception e){
            e.printStackTrace();
        }

        return finalReport;
    }

    @NonNull
    private Map<String, String> remap(@NonNull Map<ReportField, String> report) {

        Set<ReportField> fields = config.getReportFields();
        if (fields.isEmpty()) {
            fields = new ImmutableSet<ReportField>(ACRAConstants.DEFAULT_REPORT_FIELDS);
        }

        final Map<String, String> finalReport = new HashMap<String, String>(report.size());
        for (ReportField field : fields) {
            if (mMapping == null || mMapping.get(field) == null) {
                finalReport.put(field.toString(), report.get(field));
            } else {
                finalReport.put(mMapping.get(field), report.get(field));
            }
        }
        return finalReport;
    }

    private boolean isNull(@Nullable String aString) {
        return aString == null || ACRAConstants.NULL_VALUE.equals(aString);
    }
}
