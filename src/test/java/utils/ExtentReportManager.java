package utils;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.reporter.ExtentSparkReporter;

/** One Extent report per run: test-output/ExtentReport.html. */
public final class ExtentReportManager {

    private static ExtentReports extent;
    private static final ThreadLocal<ExtentTest> CURRENT = new ThreadLocal<>();

    private ExtentReportManager() {
    }

    public static synchronized ExtentReports getReportInstance() {
        if (extent == null) {
            ExtentSparkReporter spark = new ExtentSparkReporter("test-output/ExtentReport.html");
            spark.config().setDocumentTitle("ApiDemos Appium Tests");
            spark.config().setReportName("ApiDemos — Appium + TestNG");
            extent = new ExtentReports();
            extent.attachReporter(spark);
            extent.setSystemInfo("App", "ApiDemos-debug.apk");
            extent.setSystemInfo("Automation", "Appium UiAutomator2");
        }
        return extent;
    }

    public static synchronized ExtentTest createTest(String name) {
        ExtentTest test = getReportInstance().createTest(name);
        CURRENT.set(test);
        return test;
    }

    public static ExtentTest current() {
        return CURRENT.get();
    }
}
