package com.zeal.acrademo;

import android.app.Application;
import android.content.Context;

import org.acra.ACRA;
import org.acra.ReportField;
import org.acra.config.ACRAConfiguration;
import org.acra.config.ConfigurationBuilder;
import org.acra.sender.HttpSender;


//@ReportsCrashes(
//        formUri = "http://www.backendofyourchoice.com/reportpath"
//)
public class AcraApplication extends Application {
    @Override
    protected void attachBaseContext(Context base) {
        super.attachBaseContext(base);
        try {
            final ACRAConfiguration config = new ConfigurationBuilder(this)
                    .setReportSenderFactoryClasses(MySenderfactory.class)
                    .setFormUri("http://10.207.26.159:8080/AcraDemo/CrashApiAction")
//                    .setFormUri("http://zygw.bch.leju.com/api/Applog/writeLog")
                    .setHttpMethod(HttpSender.Method.POST)
                    .setReportType(HttpSender.Type.FORM)
                    .setCustomReportContent(ReportField.APP_VERSION_CODE
                            , ReportField.APP_VERSION_NAME
                            , ReportField.ANDROID_VERSION
                            , ReportField.STACK_TRACE
                            , ReportField.CUSTOM_DATA)
                    .build();
            ACRA.init(this,config);
//            ACRA.getErrorReporter().putCustomData("testKey1","testValue1");
            ACRA.DEV_LOGGING =true;
        }catch (Exception e){

        }

    }
}
