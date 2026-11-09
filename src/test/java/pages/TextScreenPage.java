package pages;

import io.appium.java_client.android.AndroidDriver;
import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.WebDriverWait;

import java.time.Duration;

/**
 * Views › Controls › 1. Light Theme — the ApiDemos screen with a plain EditText.
 * It exists on every ApiDemos build, so text and keyboard tests never need to skip.
 */
public class TextScreenPage {

    public static final By EDIT_FIELD = By.id("io.appium.android.apis:id/edit");

    private final AndroidDriver driver;
    private final WebDriverWait wait;

    public TextScreenPage(AndroidDriver driver) {
        this.driver = driver;
        this.wait = new WebDriverWait(driver, Duration.ofSeconds(15));
    }

    /** Opens the screen from the main menu. */
    public TextScreenPage open() {
        MainScreenPage menu = new MainScreenPage(driver);
        menu.scrollToAndClick("Views");
        menu.clickMenuItemByText("Controls");
        menu.clickMenuItemByText("1. Light Theme");
        wait.until(ExpectedConditions.visibilityOfElementLocated(EDIT_FIELD));
        return this;
    }

    public WebElement field() {
        return wait.until(ExpectedConditions.visibilityOfElementLocated(EDIT_FIELD));
    }

    public boolean isEditTextPresent() {
        return !driver.findElements(EDIT_FIELD).isEmpty();
    }

    public void enterText(String text) {
        WebElement f = field();
        f.clear();
        f.sendKeys(text);
    }

    public void clear() {
        field().clear();
    }

    public String getEnteredText() {
        return field().getText();
    }
}
