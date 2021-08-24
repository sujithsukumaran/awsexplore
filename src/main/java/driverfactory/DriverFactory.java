package driverfactory;

import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.firefox.FirefoxDriver;

import java.util.Map;
import java.util.function.Supplier;

public class DriverFactory {

    private static final Supplier<WebDriver> chromesupplier = () -> {
        System.setProperty("webdriver.chrome.driver", "C:\\Users\\A-6190\\chromedriver_win32\\chromedriver.exe");
        return new ChromeDriver();
    };

    private static final Supplier<WebDriver> firefoxsupplier = () -> {
        System.setProperty("webdriver.gecko.driver", "C:\\Users\\A-6190\\geckodriver-v0.29.1-win64\\geckodriver.exe");
        return new FirefoxDriver();
    };

    private static final Map<String, Supplier<WebDriver>> MAP = Map.of(
            "chrome",chromesupplier,
            "firefox", firefoxsupplier
    );

    public static WebDriver getWebDriver(String browser){

        return MAP.get(browser).get();
    }

}
