package com.wiley.autotest.selenium.driver;

import com.google.common.base.Function;
import org.openqa.selenium.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Stack;

import static com.google.common.collect.Collections2.transform;
import static java.lang.String.format;
import static java.util.Collections.emptyList;
import static org.apache.commons.collections.CollectionUtils.isNotEmpty;
import static org.testng.collections.Lists.newArrayList;

public class FramesTransparentWebDriver extends WebDriverDecorator {
    private final Stack<WebElement> currentFramesPath;
    private final Function<WebElement, WebElement> toFrameAwareWebElements;

    //VE Do we really need a ThreadLocal here? Seems that this is an extra one
    private ThreadLocal<String> mainWindowHandle = new ThreadLocal<String>();

    public FramesTransparentWebDriver(final WebDriver driver) {
        super(driver);
        currentFramesPath = new Stack<WebElement>();
        toFrameAwareWebElements = new FrameAwareWebElementTransformer(driver, currentFramesPath);
        mainWindowHandle.set(driver.getWindowHandle());
    }

    @Override
    public String getCurrentUrl() {
        switchToDefaultContext();
        return getDriver().getCurrentUrl();
    }

    @Override
    public List<WebElement> findElements(final By by) {
        List<WebElement> found = driverFindElements(by);
        if (found.isEmpty()) {
            switchToDefaultContext();
            currentFramesPath.clear();
            found = findElementsInFrames(by);
        }

        return newArrayList(transform(found, toFrameAwareWebElements));
    }

    @Override
    public WebElement findElement(final By by) {
        try {
            return findElements(by).get(0);
        } catch (IndexOutOfBoundsException e) {
            throw new NoSuchElementException("Unable to locate element " + by + ", Exception - " + e);
        }
    }

    @Override
    public void close() {
        getDriver().close();
        switchToMainWindow();
    }

    @Override
    public TargetLocator switchTo() {
        return new FramesTransparentTargetLocator(getDriver().switchTo());
    }

    private void switchToDefaultContext() {
        try {
            getDriver().switchTo().defaultContent();
        } catch (WebDriverException e) {
            switchToMainWindow();
            getDriver().switchTo().defaultContent();
        }
    }

    private void switchToMainWindow() {
        try {
            switchTo().window(mainWindowHandle.get());
        } catch (SwitchToWindowException e) {
            switchTo().window(getDriver().getWindowHandles().iterator().next());
        }
    }

    private List<WebElement> findElementsInFrames(final By by) {
        final List<WebElement> foundInCurrentFrame = driverFindElements(by);
        if (isNotEmpty(foundInCurrentFrame)) {
            return foundInCurrentFrame;
        }

        final List<WebElement> currentFrames = driverFindElements(By.tagName("iframe"));
        currentFrames.addAll(driverFindElements(By.tagName("frame")));
        for (final WebElement frame : currentFrames) {
            if (!switchToFrame(frame)) {
                continue;
            }

            currentFramesPath.push(frame);
            final List<WebElement> foundInFrames = findElementsInFrames(by);
            if (isNotEmpty(foundInFrames)) {
                return foundInFrames;
            }

            currentFramesPath.pop();
            getDriver().switchTo().defaultContent();
            for (final WebElement each : currentFramesPath) {
                switchToFrame(each);
            }
        }
        return emptyList();
    }

    private List<WebElement> driverFindElements(By by) {
        try {
            return getDriver().findElements(by);
        } catch (NoSuchWindowException e) {
            //chrome driver throws this exception if current window is closed
            try {
                switchToMainWindow();
                return getDriver().findElements(by);
            } catch (Exception e1) {
                return new ArrayList<WebElement>();
            }
        } catch (WebDriverException e) {
            //this exception can be thrown if current frame is already detached.
            return new ArrayList<WebElement>();
        } catch (Exception e) {
            //In some cases IE driver can throw InvalidSelectorException or NullPointerException
            return new ArrayList<WebElement>();
        }
    }

    private boolean switchToFrame(WebElement frame) {
        try {
            getDriver().switchTo().frame(frame);
        } catch (WebDriverException ignored) {
            return false;
        }
        return true;
    }

    private class FramesTransparentTargetLocator implements TargetLocator {
        private final TargetLocator targetLocator;

        public FramesTransparentTargetLocator(final TargetLocator targetLocator) {
            this.targetLocator = targetLocator;
        }

        @Override
        public WebDriver frame(final int index) {
            throw new SwitchToFrameOperationNotAllowedException();
        }

        @Override
        public WebDriver frame(final String nameOrId) {
            throw new SwitchToFrameOperationNotAllowedException();
        }

        @Override
        public WebDriver frame(final WebElement frameElement) {
            throw new SwitchToFrameOperationNotAllowedException();
        }

        @Override
        public WebDriver parentFrame() {
            throw new SwitchToFrameOperationNotAllowedException();
        }

        @Override
        public WebDriver window(final String nameOrHandle) {
            try {
                return targetLocator.window(nameOrHandle);
            } catch (NoSuchWindowException e) {
                if (getWindowHandles().size() == 1) {
                    return targetLocator.window(getWindowHandles().iterator().next());
                } else {
                    throw new SwitchToWindowException(format("Unable to switch to window by handler: '%s'", nameOrHandle), e);
                }
            } catch (NullPointerException e) {
                throw new SwitchToWindowException("Unable to switch to window: handler is null ", e);
            }
        }

        @Override
        public WebDriver defaultContent() {
            return targetLocator.defaultContent();
        }

        @Override
        public WebElement activeElement() {
            return targetLocator.activeElement();
        }

        @Override
        public Alert alert() {
            return targetLocator.alert();
        }
    }
}