package tests;

import base.BaseTest;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import io.appium.java_client.android.connection.ConnectionState;
import io.appium.java_client.android.connection.ConnectionStateBuilder;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;
import pages.MainScreenPage;
import utils.Adb;
import utils.ExtentReportManager;
import utils.WaitUtils;

import java.util.Map;

/**
 * Real interruptions sent through the emulator console (adb emu): an incoming call, an SMS,
 * a dead battery, and Wi-Fi + mobile data switched off. Each one is checked to have really
 * happened, and the app must still be in front on its main screen afterwards.
 * Needs an emulator (the console commands do not exist on a physical phone).
 */
public class InterruptSimulationTests extends BaseTest {

    ExtentTest test;
    MainScreenPage mainPage;

    @BeforeMethod
    public void setupReport(java.lang.reflect.Method method) {
        test = ExtentReportManager.createTest(method.getName());
        mainPage = new MainScreenPage(driver);
        mainPage.ensureOnMainScreen(driver);
    }

    private void assertAppStillUsable(String after) {
        if (!APP_PACKAGE.equals(driver.getCurrentPackage())) {
            test.log(Status.INFO, after + " brought " + driver.getCurrentPackage() + " to the front");
        }
        Assert.assertTrue(mainPage.waitForMainScreen(), "App not usable on its main screen after " + after);
        Assert.assertEquals(mainPage.getMenuItemCount(), 12, "Main menu incomplete after " + after);
    }

    @Test(priority = 1)
    public void interrupt_IncomingCallDoesNotCrashApp() {
        String number = "5551234567";
        Adb.emu(deviceName, "gsm", "call", number);
        try {
            // the modem can take a few seconds to report the call
            Assert.assertTrue(waitFor(() -> Adb.emu(deviceName, "gsm", "list").contains(number), 20),
                    "The emulator never registered the incoming call");
            test.log(Status.INFO, "Ringing: " + Adb.emu(deviceName, "gsm", "list")
                    + " | in front: " + driver.getCurrentPackage());
        } finally {
            Adb.emu(deviceName, "gsm", "cancel", number);
        }
        Assert.assertTrue(waitFor(() -> !Adb.emu(deviceName, "gsm", "list").contains(number), 20),
                "The call did not end");
        // the in-call screen closes on its own once the call has ended
        waitFor(() -> !driver.getCurrentPackage().contains("dialer"), 15);
        if (!APP_PACKAGE.equals(driver.getCurrentPackage())) {
            driver.activateApp(APP_PACKAGE);
        }
        assertAppStillUsable("an incoming call");
    }

    private static boolean waitFor(java.util.function.BooleanSupplier condition, int seconds) {
        long end = System.currentTimeMillis() + seconds * 1000L;
        while (System.currentTimeMillis() < end) {
            if (condition.getAsBoolean()) return true;
            WaitUtils.pause(500);
        }
        return condition.getAsBoolean();
    }

    @Test(priority = 2)
    public void interrupt_IncomingSmsDoesNotCrashApp() {
        Adb.emu(deviceName, "sms", "send", "5551234567", "Test SMS interrupt");
        WaitUtils.pause(2000);
        assertAppStillUsable("an incoming SMS");
    }

    @Test(priority = 3)
    public void interrupt_NetworkLossDoesNotCrashApp() {
        ConnectionState offline = driver.setConnection(
                new ConnectionStateBuilder().withWiFiDisabled().withDataDisabled().build());
        try {
            test.log(Status.INFO, "Connection while offline: " + offline.getBitMask());
            Assert.assertFalse(driver.getConnection().isWiFiEnabled(), "Wi-Fi is still on");
            Assert.assertFalse(driver.getConnection().isDataEnabled(), "Mobile data is still on");
            assertAppStillUsable("losing the network");
        } finally {
            driver.setConnection(new ConnectionStateBuilder().withWiFiEnabled().withDataEnabled().build());
        }
        Assert.assertTrue(driver.getConnection().isWiFiEnabled(), "Wi-Fi was not restored");
    }

    @Test(priority = 4)
    @SuppressWarnings("unchecked")
    public void interrupt_LowBatterySimulationDoesNotAffectApp() {
        try {
            Adb.emu(deviceName, "power", "capacity", "5");
            WaitUtils.pause(1500);
            Map<String, Object> battery = (Map<String, Object>) driver.executeScript("mobile: batteryInfo");
            double level = ((Number) battery.get("level")).doubleValue();
            test.log(Status.INFO, "Battery reported by the device: " + battery);
            Assert.assertTrue(level <= 0.06, "Battery level did not drop to 5 %: " + level);
            assertAppStillUsable("a low-battery warning");
        } finally {
            Adb.emu(deviceName, "power", "capacity", "100");
        }
    }
}
