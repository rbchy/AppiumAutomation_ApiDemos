package listeners;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.OutputType;
import org.testng.ITestListener;
import org.testng.ITestResult;
import utils.ExtentReportManager;

import java.nio.file.Files;
import java.nio.file.Path;

/**
 * On failure: saves a screenshot to test-output/screenshots, attaches it to the Extent report
 * and prints the screen's text to the log, so a CI failure can be read without artifacts.
 * On success / skip: records the status in the Extent report.
 */
public class ScreenshotListener implements ITestListener {

    @Override
    public void onTestSuccess(ITestResult result) {
        ExtentTest t = ExtentReportManager.current();
        if (t != null) t.log(Status.PASS, "Passed");
    }

    @Override
    public void onTestSkipped(ITestResult result) {
        ExtentTest t = ExtentReportManager.current();
        if (t != null) t.log(Status.SKIP, String.valueOf(result.getThrowable()));
    }

    @Override
    public void onTestFailure(ITestResult result) {
        ExtentTest t = ExtentReportManager.current();
        if (t != null) t.log(Status.FAIL, result.getThrowable());

        if (!(result.getInstance() instanceof BaseTest)) return;
        AndroidDriver driver = ((BaseTest) result.getInstance()).getDriver();
        if (driver == null) return;
        try {
            Path dir = Path.of("test-output", "screenshots");
            Files.createDirectories(dir);
            Path file = dir.resolve(result.getMethod().getMethodName() + ".png");
            Files.write(file, driver.getScreenshotAs(OutputType.BYTES));
            if (t != null) t.addScreenCaptureFromPath(Path.of("screenshots", file.getFileName().toString()).toString());

            System.out.println("---- FAILED: " + result.getMethod().getMethodName()
                    + " | package=" + driver.getCurrentPackage() + " activity=" + driver.currentActivity());
            String source = driver.getPageSource()
                    .replaceAll("(?s).*?text=\"([^\"]*)\"", "$1 | ")
                    .replaceAll("(?s)\\|[^|]*$", "");
            System.out.println("Screen text: " + (source.length() > 800 ? source.substring(0, 800) : source));
        } catch (Exception e) {
            System.out.println("Could not capture failure details: " + e.getMessage());
        }
    }
}
