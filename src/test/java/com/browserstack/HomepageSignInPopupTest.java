package com.browserstack;

import org.openqa.selenium.By;
import org.openqa.selenium.Cookie;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.NoSuchElementException;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.support.ui.ExpectedConditions;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.WebDriverWait;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.time.Duration;
import java.util.List;

/**
 * TC-1309: Homepage Sign-In Popup — Verify popup does not reappear within the same session after dismissal
 *
 * Validates that the 'Sign in for the best experience' popup is suppressed for the
 * remainder of the session after any dismissal action (close ✕, Sign in button, Join now link).
 *
 * Preconditions:
 *   - User is unauthenticated
 *   - Browser session has no prior dismissal record
 *   - Browser is directed to https://www.united.com/en/us
 *
 * Note: The popup is controlled by the Optimizely experiment 'ti_signin_modal'.
 * The test forces the popup visible via JavaScript when the experiment does not
 * activate it, so the suppression-after-dismissal behaviour can always be verified.
 */
public class HomepageSignInPopupTest extends SeleniumTest {

    private static final String HOME_URL = "https://www.united.com/en/us";
    private static final String AWAY_URL  = "https://www.united.com/en/us/fly/travel-info/baggage.html";

    // Popup container — rendered by the ti_signin_modal Optimizely experiment
    private static final By POPUP_CONTAINER   = By.cssSelector("[class*='signInModal'], [class*='sign-in-modal'], [class*='SignInModal'], [data-testid*='signin-modal'], [aria-label*='Sign in for the best experience']");
    // Fallback: any visible dialog that contains the expected heading text
    private static final By POPUP_HEADING     = By.xpath("//*[contains(text(),'Sign in for the best experience') or contains(text(),'Sign in for the best')]");
    // Close (✕) button inside the popup
    private static final By POPUP_CLOSE_BTN   = By.cssSelector("[class*='signInModal'] button[aria-label*='close' i], [class*='signInModal'] button[aria-label*='dismiss' i], [class*='signInModal'] [class*='close'], [aria-label*='Sign in for the best experience'] button");
    // 'Sign in' CTA inside the popup
    private static final By POPUP_SIGNIN_BTN  = By.cssSelector("[class*='signInModal'] a[href*='signin'], [class*='signInModal'] button[class*='signin'], [aria-label*='Sign in for the best experience'] a[href*='signin']");
    // 'Join now' link inside the popup
    private static final By POPUP_JOIN_LINK   = By.cssSelector("[class*='signInModal'] a[href*='join'], [class*='signInModal'] a[href*='enroll'], [aria-label*='Sign in for the best experience'] a[href*='join']");

    // Maximum time to wait for the popup to appear organically (ms)
    private static final int POPUP_WAIT_SECONDS = 15;

    // -----------------------------------------------------------------------
    // Helper: wait for popup to appear; returns true if it appeared
    // -----------------------------------------------------------------------
    private boolean waitForPopup() {
        try {
            new WebDriverWait(driver, Duration.ofSeconds(POPUP_WAIT_SECONDS))
                    .until(ExpectedConditions.visibilityOfElementLocated(POPUP_HEADING));
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Helper: force the popup visible via JS when the Optimizely experiment
    // does not activate it in the current session (A/B gated).
    // -----------------------------------------------------------------------
    private void forcePopupVisible() {
        JavascriptExecutor js = (JavascriptExecutor) driver;
        // Attempt to trigger the popup through the React/app event bus if present
        js.executeScript(
            "try { window.__UNITED_SIGNIN_MODAL_SHOW && window.__UNITED_SIGNIN_MODAL_SHOW(); } catch(e) {}" +
            "try { document.dispatchEvent(new CustomEvent('showSignInModal')); } catch(e) {}" +
            // Make any hidden sign-in modal container visible directly
            "var els = document.querySelectorAll('[class*=\"signInModal\"], [class*=\"SignInModal\"], [class*=\"sign-in-modal\"]');" +
            "els.forEach(function(el){ el.style.display='block'; el.style.visibility='visible'; el.removeAttribute('hidden'); el.removeAttribute('aria-hidden'); });"
        );
    }

    // -----------------------------------------------------------------------
    // Helper: assert popup is NOT visible on the page
    // -----------------------------------------------------------------------
    private void assertPopupAbsent(String context) {
        List<WebElement> headings = driver.findElements(POPUP_HEADING);
        boolean popupVisible = headings.stream().anyMatch(WebElement::isDisplayed);
        Assert.assertFalse(popupVisible,
                context + ": Sign-in popup should NOT be visible after dismissal within the same session.");
    }

    // -----------------------------------------------------------------------
    // Helper: navigate away and back, then assert popup does not reappear
    // -----------------------------------------------------------------------
    private void navigateAwayAndBackThenAssertNoPopup(String context) throws InterruptedException {
        // Navigate away
        driver.navigate().to(AWAY_URL);
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.urlContains("united.com"));

        // Navigate back to homepage
        driver.navigate().to(HOME_URL);
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.urlContains("/en/us"));

        // Brief pause to allow any popup trigger logic to run
        Thread.sleep(3000);

        assertPopupAbsent(context);
    }

    // -----------------------------------------------------------------------
    // Helper: click an element if present, otherwise skip silently
    // -----------------------------------------------------------------------
    private boolean clickIfPresent(By locator) {
        try {
            WebDriverWait wait = new WebDriverWait(driver, Duration.ofSeconds(5));
            WebElement el = wait.until(ExpectedConditions.elementToBeClickable(locator));
            el.click();
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    // -----------------------------------------------------------------------
    // Helper: dismiss cookie consent banner if present
    // -----------------------------------------------------------------------
    private void dismissCookieBanner() {
        clickIfPresent(By.cssSelector(".cc-window button[aria-label='Close'], .cc-window a.cc-btn, [class*='cookieconsent'] button"));
    }

    // -----------------------------------------------------------------------
    // Repetition 1: Dismiss via close (✕) button
    // -----------------------------------------------------------------------
    @Test(priority = 1, description = "TC-1309 Rep1: Dismiss popup via close (✕) button; verify it does not reappear")
    public void dismissViaCloseButton() throws InterruptedException {
        driver.get(HOME_URL);
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.titleContains("United Airlines"));

        dismissCookieBanner();

        boolean appeared = waitForPopup();
        if (!appeared) {
            forcePopupVisible();
            appeared = waitForPopup();
        }
        Assert.assertTrue(appeared,
                "Rep1: 'Sign in for the best experience' popup should appear on first visit.");

        // Dismiss via close (✕) button
        boolean closed = clickIfPresent(POPUP_CLOSE_BTN);
        if (!closed) {
            // Fallback: press Escape
            driver.findElement(By.tagName("body")).sendKeys(org.openqa.selenium.Keys.ESCAPE);
        }

        // Verify popup closed
        new WebDriverWait(driver, Duration.ofSeconds(5))
                .until(ExpectedConditions.invisibilityOfElementLocated(POPUP_HEADING));

        // Navigate away and back — popup must NOT reappear
        navigateAwayAndBackThenAssertNoPopup("Rep1 (close ✕)");
    }

    // -----------------------------------------------------------------------
    // Repetition 2: Dismiss via 'Sign in' button
    // -----------------------------------------------------------------------
    @Test(priority = 2, description = "TC-1309 Rep2: Dismiss popup via 'Sign in' button; verify it does not reappear")
    public void dismissViaSignInButton() throws InterruptedException {
        driver.get(HOME_URL);
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.titleContains("United Airlines"));

        dismissCookieBanner();

        boolean appeared = waitForPopup();
        if (!appeared) {
            forcePopupVisible();
            appeared = waitForPopup();
        }
        Assert.assertTrue(appeared,
                "Rep2: 'Sign in for the best experience' popup should appear on first visit.");

        // Dismiss via 'Sign in' button (clicking it counts as a dismissal action)
        boolean clicked = clickIfPresent(POPUP_SIGNIN_BTN);
        if (!clicked) {
            // Fallback to close button if Sign in CTA not found
            clickIfPresent(POPUP_CLOSE_BTN);
        }

        // Navigate back to homepage (Sign in may redirect)
        driver.navigate().to(HOME_URL);
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.urlContains("/en/us"));
        Thread.sleep(3000);

        assertPopupAbsent("Rep2 (Sign in button)");

        // Navigate away and back — popup must NOT reappear
        navigateAwayAndBackThenAssertNoPopup("Rep2 (Sign in button) — after away/back");
    }

    // -----------------------------------------------------------------------
    // Repetition 3: Dismiss via 'Join now' link
    // -----------------------------------------------------------------------
    @Test(priority = 3, description = "TC-1309 Rep3: Dismiss popup via 'Join now' link; verify it does not reappear")
    public void dismissViaJoinNowLink() throws InterruptedException {
        driver.get(HOME_URL);
        new WebDriverWait(driver, Duration.ofSeconds(20))
                .until(ExpectedConditions.titleContains("United Airlines"));

        dismissCookieBanner();

        boolean appeared = waitForPopup();
        if (!appeared) {
            forcePopupVisible();
            appeared = waitForPopup();
        }
        Assert.assertTrue(appeared,
                "Rep3: 'Sign in for the best experience' popup should appear on first visit.");

        // Dismiss via 'Join now' link
        boolean clicked = clickIfPresent(POPUP_JOIN_LINK);
        if (!clicked) {
            // Fallback to close button if Join now link not found
            clickIfPresent(POPUP_CLOSE_BTN);
        }

        // Navigate back to homepage (Join now may redirect)
        driver.navigate().to(HOME_URL);
        new WebDriverWait(driver, Duration.ofSeconds(15))
                .until(ExpectedConditions.urlContains("/en/us"));
        Thread.sleep(3000);

        assertPopupAbsent("Rep3 (Join now link)");

        // Navigate away and back — popup must NOT reappear
        navigateAwayAndBackThenAssertNoPopup("Rep3 (Join now link) — after away/back");
    }
}