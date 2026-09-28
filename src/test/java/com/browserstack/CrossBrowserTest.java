package com.browserstack;

import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.testng.Assert;
import org.testng.annotations.Test;

public class CrossBrowserTest extends SeleniumTest{
    @Test
    public void addProductToCart() throws Exception {
        // navigate to bstackdemo
        driver.get("https://www.bstackdemo.com");

        JavascriptExecutor jse = (JavascriptExecutor)driver;

        // Click on add to cart button
        jse.executeScript("browserstack_executor: {\"action\":" +
                " \"ai\", \"arguments\": [\"Click 'Add to cart'.\"]}");

        jse.executeScript("browserstack_executor: {\"action\":" +
                " \"ai\", \"arguments\": [\"Click 'CHECKOUT'.\"]}");

        jse.executeScript("browserstack_executor: {\"action\":" +
                " \"ai\", \"arguments\": [\"Click 'Select Username' .\"]}");

        jse.executeScript("browserstack_executor: {\"action\":" +
                " \"ai\", \"arguments\": [\"Click 'demouser' .\"]}");

        jse.executeScript("browserstack_executor: {\"action\":" +
                " \"ai\", \"arguments\": [\"Click 'Select Password' .\"]}");

        jse.executeScript("browserstack_executor: {\"action\":" +
                " \"ai\", \"arguments\": [\"Click 'testingisfun99' .\"]}");

        jse.executeScript("browserstack_executor: {\"action\":" +
                " \"ai\", \"arguments\": [\"Click 'LOG IN'.\"]}");
    }
}
