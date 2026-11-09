package base;

import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.android.options.UiAutomator2Options;
import org.testng.annotations.AfterClass;
import org.testng.annotations.AfterSuite;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Optional;
import org.testng.annotations.Parameters;
import utils.ExtentReportManager;

import java.io.File;
import java.net.HttpURLConnection;
import java.net.URL;
import java.time.Duration;

/**
 * One Appium session per test class.
 * <ul>
 *   <li>Appium URL: {@code -DappiumUrl} (default http://127.0.0.1:4723)</li>
 *   <li>Device: the {@code deviceName} TestNG parameter (default emulator-5554); it is also used as the udid,
 *       so the cross-device suite can target two emulators</li>
 *   <li>App: ApiDemos-debug.apk in the project root, installed fresh for every class</li>
 * </ul>
 */
public class BaseTest {

    public static final String APP_PACKAGE = "io.appium.android.apis";
    public static final String APP_ACTIVITY = "io.appium.android.apis.ApiDemos";
    public static final File APK = new File("ApiDemos-debug.apk").getAbsoluteFile();

    protected AndroidDriver driver;
    protected String deviceName;

    @BeforeClass(alwaysRun = true)
    @Parameters({"deviceName"})
    public void setUp(@Optional("emulator-5554") String deviceName) throws Exception {
        this.deviceName = deviceName;
        URL appium = appiumUrl();
        checkAppiumIsRunning(appium);

        UiAutomator2Options options = new UiAutomator2Options()
                .setDeviceName(deviceName)
                .setUdid(deviceName)
                .setApp(APK.getPath())
                .setAppPackage(APP_PACKAGE)
                .setAppActivity(APP_ACTIVITY)
                .setAutoGrantPermissions(true)
                .setNewCommandTimeout(Duration.ofSeconds(120))
                .setUiautomator2ServerLaunchTimeout(Duration.ofSeconds(90))
                .setUiautomator2ServerInstallTimeout(Duration.ofSeconds(90));

        driver = new AndroidDriver(appium, options);
    }

    public AndroidDriver getDriver() {
        return driver;
    }

    public static URL appiumUrl() throws Exception {
        return new URL(System.getProperty("appiumUrl", "http://127.0.0.1:4723"));
    }

    /** Fails fast with a readable message instead of a connection stack trace. */
    public static void checkAppiumIsRunning(URL appium) {
        try {
            HttpURLConnection c = (HttpURLConnection) new URL(appium, "/status").openConnection();
            c.setConnectTimeout(5000);
            if (c.getResponseCode() != 200) {
                throw new IllegalStateException("Appium answered " + c.getResponseCode() + " at " + appium);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Appium server is not running at " + appium
                    + " — start it with `appium` first", e);
        }
    }

    @AfterClass(alwaysRun = true)
    public void tearDown() {
        if (driver != null) {
            driver.quit();
        }
    }

    @AfterSuite(alwaysRun = true)
    public void flushReport() {
        ExtentReportManager.getReportInstance().flush();
    }
}
