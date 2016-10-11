package com.wiley.autotest.selenium.context;

import com.wiley.autotest.selenium.annotations.Report;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.openqa.selenium.By;
import org.openqa.selenium.Dimension;
import org.testng.Reporter;
import ru.yandex.qatools.allure.annotations.Step;

import static com.wiley.autotest.utils.DateUtils.waitForAssignmentDate;
import static org.apache.commons.lang.StringUtils.isNotBlank;

/**
 * User: dfedorov
 * Date: 1/5/12
 * Time: 12:34 PM
 */
public abstract class AbstractPage<P extends AbstractPage> extends AbstractPageElement<P> implements IPage {

    public static final By TABLE_LOCATOR = By.tagName("table");
    public static final By TR_LOCATOR = By.tagName("tr");
    public static final By TD_LOCATOR = By.tagName("td");
    public static final By TH_LOCATOR = By.tagName("th");
    public static final By SELECT_LOCATOR = By.tagName("select");
    public static final By SPAN_LOCATOR = By.tagName("span");
    public static final By DIV_LOCATOR = By.tagName("div");
    public static final By P_LOCATOR = By.tagName("p");
    public static final By A_LOCATOR = By.tagName("a");
    public static final By B_LOCATOR = By.tagName("b");
    public static final By INPUT_LOCATOR = By.tagName("input");
    public static final By IMG_LOCATOR = By.tagName("img");
    protected static final String CLASS_ATTRIBUTE = "class";

    private final String path;

    public AbstractPage() {
        path = null;
    }

    public void load() {
        load(path);
    }

    public void load(final String pathString) {
        if (isNotBlank(pathString)) {
            getDriver().get(pathString);
        }
    }

    protected final void log(final String message) {
        Reporter.log(message);
    }

    protected final void log(final String format, final Object... args) {
        log(String.format(format, args));
    }

    @Step
    @Report
    public <P extends AbstractPage> P closeCurrentWindow(final Class<P> target) {
        closeBrowserWindow();
        return redirectTo(target);
    }

    @Step
    @Report
    public <P extends AbstractPage> P closeCurrentWindowAndSwitchToLastWindow(final Class<P> target) {
        closeBrowserWindow();
        switchToLastWindow();
        return redirectTo(target);
    }

    @Step
    @Report
    public <P extends AbstractPage> P setBrowserDimensions(int width, int height) {
        Dimension dimension = new Dimension(width, height);
        getDriver().manage().window().setSize(dimension);
        return (P) this;
    }

    public static By getLinkByXpath(String linkText) {
        return By.xpath("//a[text()='" + linkText + "']");
    }

    @Step
    @Report
    public P waitForDate(DateTimeZone dateTimeZone, DateTime dueDate) {
        waitForAssignmentDate(dateTimeZone, dueDate);
        return (P) this;
    }

    @Step
    @Report
    public <T extends AbstractPage> T waitForDate(DateTimeZone dateTimeZone, DateTime dueDate, Class<T> target) {
        waitForAssignmentDate(dateTimeZone, dueDate);
        return redirectTo(target);
    }

    /**
     * This method has to be added in every test that fails because of bug
     * This method has to be added right before the method which fails because of bug
     * If after this method test does not fail, it probably means that the bug was fixed
     * and this method call has to be removed as well as bug annotation and group from the test
     *
     * @param bugId - id of a bug,
     *              in case when there's no bug but system behavior is different from test case
     *              ask for an approval of manual QA team; If they say "OK, keep it as is" use
     *              EXPECTED_FAILURE_AGREED_WITH_MANUAL_QA constant as an ID.
     * @return this page
     */
    @Step
    @Report
    public P bugInNextStepReportAlert(String bugId) {
        reportWithStep("The next step will fail because of bug with id '" + bugId + "'!");
        return (P) this;
    }

    @Step
    @Report
    public P checkTitleOfBrowserWindow(String expectedTitle) {
        postponedAssertEquals(getDriver().getTitle(), expectedTitle, "Incorrect title of browser window");
        return (P) this;
    }
}
