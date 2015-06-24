package com.epam.gepard.selenium;

/*==========================================================================
 Copyright 2004-2015 EPAM Systems

 This file is part of Gepard.

 Gepard is free software: you can redistribute it and/or modify
 it under the terms of the GNU General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 Gepard is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU General Public License for more details.

 You should have received a copy of the GNU General Public License
 along with Gepard.  If not, see <http://www.gnu.org/licenses/>.
===========================================================================*/

import com.epam.gepard.annotations.TestClass;
import com.epam.gepard.common.NATestCaseException;
import com.epam.gepard.generic.GepardTestClass;
import com.epam.gepard.selenium.annotation.GepardSeleniumTestClass;
import com.epam.gepard.selenium.browsers.BrowserEnum;
import com.epam.gepard.selenium.browsers.SeleniumUtil;
import com.epam.gepard.selenium.helper.EnvironmentHelper;
import com.epam.gepard.util.FileUtil;
import com.epam.gepard.util.Util;
import com.thoughtworks.selenium.Selenium;
import com.thoughtworks.selenium.webdriven.WebDriverBackedSelenium;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.Augmenter;
import org.openqa.selenium.remote.DesiredCapabilities;
import org.openqa.selenium.remote.RemoteWebDriver;

import java.awt.event.KeyEvent;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * This class represents a TestCase, which supports Selenium WebDriver.
 *
 * @author Tamas_Kohegyi
 */
public abstract class SeleniumTestCase implements GepardTestClass {
    public static final String DEFAULT_TIMEOUT = "30000";
    public static final String SELENIUM_PORT = "gepard.selenium.port";
    public static final String SELENIUM_HOST = "gepard.selenium.host";

    public static final String SELENIUM_BROWSER_FIREFOX = "gepard.selenium.browserString.FF";
    public static final String SELENIUM_BROWSER_INTERNET_EXPLORER = "gepard.selenium.browserString.IE";
    public static final String SELENIUM_BROWSER_GOOGLE_CHROME = "gepard.selenium.browserString.GoogleChrome";
    public static final String SELENIUM_BROWSER_SAFARI = "gepard.selenium.browserString.Safari";
    private static int dumpFileCount;

    private SeleniumUtil seleniumUtil = new SeleniumUtil();

    /**
     * Selenium main object.
     */
    private Selenium selenium;

    private Util util = new Util();

    /**
     * WebDriver main object.
     */
    private WebDriver webDriver;

    /**
     * Environment extender.
     */
    private EnvironmentHelper environmentHelper;

    private String browserString;

    private String baseURL;

    private NATestCaseException setupException;

    /**
     * Determines if selenium should remember the traffic or not to get it with the captureNetworkTraffic method.
     * Its default value is <b>false</b>.
     * <p>
     * Set this to <b>true</b> in case you would like to use the captureNetworkTraffic method of selenium.
     * Be aware that captureNetworkTraffic method might cause exceptions in selenium (especially on Linux OS),
     * because its selenium implementation is not bug-free.
     */
    private boolean needCaptureNetworkTraffic;

    /**
     * Determines if selenium should allow adding custom headers to requests with selenium's addCustomRequestHeader method.
     * Its default value is <b>false</b>.
     * <p>
     * Set this to <b>true</b> in case you would like to use the addCustomRequestHeader method of selenium.
     */
    private boolean addCustomRequestHeaders;

    /**
     * Contains the command line flags that will be passed to the browser
     * Default value is <b>--disable-web-security</b> for *googlechrome, null for others.
     */
    private String commandLineFlags;

    /**
     * Default constructor, loads the basic starting points, the browser string, and the initial/base url.
     */
    public SeleniumTestCase() {
        environmentHelper = new EnvironmentHelper(getTestClassExecutionData().getEnvironment());
        setBaseURL(environmentHelper.getTestEnvironmentURL());
        setBrowserString(environmentHelper.getTestEnvironmentBrowser());
        setNeedCaptureNetworkTraffic(true);
    }

    public SeleniumUtil getSeleniumUtil() {
        return seleniumUtil;
    }

    public Selenium getSelenium() {
        return selenium;
    }

    public WebDriver getWebDriver() {
        return webDriver;
    }

    public EnvironmentHelper getEnvironmentHelper() {
        return environmentHelper;
    }

    public String getBrowserString() {
        return browserString;
    }

    public String getBaseURL() {
        return baseURL;
    }

    public void setBaseURL(String baseURL) {
        this.baseURL = baseURL;
    }

    public boolean getNeedCaptureNetworkTraffic() {
        return needCaptureNetworkTraffic;
    }

    public void setNeedCaptureNetworkTraffic(final boolean needCaptureNetworkTraffic) {
        this.needCaptureNetworkTraffic = needCaptureNetworkTraffic;
    }

    public void setAddCustomRequestHeaders(boolean addCustomRequestHeaders) {
        this.addCustomRequestHeaders = addCustomRequestHeaders;
    }

    public boolean getAddCustomRequestHeaders() {
        return addCustomRequestHeaders;
    }

    public void setCommandLineFlags(String commandLineFlags) {
        this.commandLineFlags = commandLineFlags;
    }

    public String getCommandLineFlags() {
        return commandLineFlags;
    }

    private void throwNAExceptionInSetup(final String reason) {
        setupException = new NATestCaseException(reason);
        naTestCase(reason);
    }

    /**
     * Initialization of the test class, we use this beforeTestCase method to setup selenium.
     */
    public void beforeTestCase() {
        if (this.getClass().isAnnotationPresent(TestClass.class)) {
            if (this.getClass().isAnnotationPresent(GepardSeleniumTestClass.class)) {
                if (!GepardSeleniumTestClass.UNDEFINED.equals(this.getClass().getAnnotation(GepardSeleniumTestClass.class).baseUrl())) {
                    setBaseURL(this.getClass().getAnnotation(GepardSeleniumTestClass.class).baseUrl());
                }
                if (!GepardSeleniumTestClass.UNDEFINED.equals(this.getClass().getAnnotation(GepardSeleniumTestClass.class).browser())) {
                    setBrowserString(this.getClass().getAnnotation(GepardSeleniumTestClass.class).browser());
                }
            } else {
                throwNAExceptionInSetup("When using Gepard SeleniumTestCase, you must annotate your class as 'GepardSeleniumTestClass'.");
            }
        } else {
            throwNAExceptionInSetup("When using Gepard SeleniumTestCase, you must annotate your class as 'TestClass'.");
        }
        setNeedCaptureNetworkTraffic(true);
        initiateSelenium();
    }

    /**
     * Initialization code executed on a dummy instance when the test case method ends.
     * Use this as POSTCONDITION.
     */
    public void afterTestCase() {
        if (webDriver != null) {
            webDriver.quit(); //close all opened browser window
            webDriver = null;
            selenium = null;
        }
    }

    private void initiateSelenium() {
        if (browserString == null) {
            throwNAExceptionInSetup("No browser to be used was specified.");
        }
        if (browserString.endsWith("*googlechrome")) {
            commandLineFlags = "--disable-web-security";
        }

        buildSeleniumInstance();

        //TO ENABLE THIS, NEED SELENIUM FIX FIRST! (to get rid of the WARNING messages)
        //selenium.clearCustomRequestHeader(); //otherwise it keeps other custom headers, forever
    }

    private DesiredCapabilities detectCapabilities() {
        DesiredCapabilities capabilities = null;
        try {
            if (browserString.compareTo(environmentHelper.getProperty(SELENIUM_BROWSER_GOOGLE_CHROME)) == 0) {
                capabilities = DesiredCapabilities.chrome();
                capabilities.setBrowserName("chrome");
            }
            if (browserString.compareTo(environmentHelper.getProperty(SELENIUM_BROWSER_FIREFOX)) == 0) {
                capabilities = DesiredCapabilities.firefox();
                capabilities.setBrowserName("firefox");
                capabilities.setVersion("ANY");
            }
            if (browserString.compareTo(environmentHelper.getProperty(SELENIUM_BROWSER_INTERNET_EXPLORER)) == 0) {
                capabilities = DesiredCapabilities.internetExplorer();
                capabilities.setBrowserName("internetExplorer");
            }
            if (browserString.compareTo(environmentHelper.getProperty(SELENIUM_BROWSER_SAFARI)) == 0) {
                capabilities = DesiredCapabilities.safari();
                capabilities.setBrowserName("safari");
            }
            if (capabilities == null) {
                throwNAExceptionInSetup("Specified browser:'" + browserString + "' is not supported.");
            }
        } catch (NullPointerException e) {
            throwNAExceptionInSetup("Gepard property values for Selenium Browsers are not available.");
        }
        return capabilities;
    }

    private void buildSeleniumInstance() {
        DesiredCapabilities capabilities = detectCapabilities();

        try {
            webDriver = new RemoteWebDriver(buildWebDriverUrl(), capabilities);
        } catch (WebDriverException e) {
            throwNAExceptionInSetup(e.getLocalizedMessage());
        }
        selenium = new WebDriverBackedSelenium(webDriver, baseURL);
        selenium.setTimeout(DEFAULT_TIMEOUT); //set the default timeout
        // hide all open windows on Mac, it is necessary to hide 'always on top' windows, otherwise useless screenshots will be created from desktop
        if (seleniumUtil.getBrowserType(this) == BrowserEnum.Safari) {
            selenium.keyPressNative(String.valueOf(KeyEvent.VK_F11));
        }
    }

    private URL buildWebDriverUrl() {
        URL retVal;

        try {
            retVal = new URIBuilder().setScheme(getWebDriverScheme()).setHost(getSeleniumHost()).setPort(getSeleniumPort())
                    .setPath(getWebDriverPath()).build().toURL();
        } catch (MalformedURLException | URISyntaxException e) {
            throw new IllegalStateException("Unable to build RemoteWebDriver's url!");
        }

        return retVal;
    }

    private String getWebDriverScheme() {
        return "http";
    }

    private String getSeleniumHost() {
        return environmentHelper.getProperty(SELENIUM_HOST, "localhost");
    }

    private int getSeleniumPort() {
        return Integer.valueOf(environmentHelper.getProperty(SELENIUM_PORT, "4444"));
    }

    private String getWebDriverPath() {
        return "/wd/hub";
    }

    /**
     * Creates and returns with a string that can be used as parameter at selenium.start().
     *
     * @return with the start string to be used at selenium.start, be aware that it can be null value as well.
     */
    private String getBrowserStartString() {
        String retVal = "";

        if (!StringUtils.isBlank(commandLineFlags)) {
            retVal = retVal + "commandLineFlags=" + commandLineFlags;
        }
        if (needCaptureNetworkTraffic) {
            retVal = retVal + ("".equals(retVal) ? "" : ";") + "captureNetworkTraffic=true";
        }
        //needCaptureNetworkTraffic is stronger if it is set to true addCustomRequestHeaders will be set to true inclusively
        if (addCustomRequestHeaders && !needCaptureNetworkTraffic) {
            retVal = retVal + ("".equals(retVal) ? "" : ";") + "addCustomRequestHeaders=true";
        }

        return retVal;
    }

    /**
     * Sets the actually used browser.
     *
     * @param browserString is the string that identified the browser.
     * @return with the browser string (like: *chrome).
     */
    public String setBrowserString(String browserString) {
        this.browserString = browserString;
        return this.browserString;
    }

    /**
     * Restarts the actual browser.
     */
    public final void restartBrowser() {
        //restart the browser
        try {
            logComment("Stopping Actual Browser session...");
            selenium.stop();
            logComment("Starting New Browser session...");
            selenium.start(getBrowserStartString());
        } catch (Exception e) {
            naTestCase("Start browser is failed. Reason: " + e.getMessage());
        }
    }

    /**
     * Write an event message to the log.
     *
     * @param text     Event message
     * @param makeDump if true, page source dump will be created.
     */
    public void logEvent(final String text, final boolean makeDump) {
        String addStr = "";
        if (!text.startsWith("<font")) {
            getTestClassExecutionData().addSysOut(text);
        }
        if (getTestClassExecutionData().getHtmlRunReporter().getTestMethodHtmlLog() != null) {
            if (makeDump) {
                try {
                    String dumpFileName = dumpSource(true);
                    File dumpFile = new File(dumpFileName);
                    String screenshotFileName = dumpFileName + ".png";
                    if (selenium != null) {
                        selenium.windowMaximize();
                        WebDriver augmentedDriver = new Augmenter().augment(webDriver);
                        File screenShot = ((TakesScreenshot) augmentedDriver).getScreenshotAs(OutputType.FILE);
                        FileUtil fileUtil = new FileUtil();
                        File screenShotFile = new File(screenshotFileName);
                        fileUtil.copy(screenShot, screenShotFile);
                    }
                    String dumpFileName2 = dumpSource(false);
                    File dumpFile2 = new File(dumpFileName2);
                    addStr = " <small>[<a href=\"" + dumpFile.getName() + "\" target=\"_new\">source</a>]" + " [<a href=\"" + dumpFile2.getName()
                            + "\" target=\"_new\">view</a>]" + " [<a href=\"" + dumpFile.getName() + ".png\" target=\"_new\">screenshot</a>]</small>";
                } catch (Exception e) {
                    addStr = " <small>[Dump failed]</small>";
                }
            }
            getTestClassExecutionData().getHtmlRunReporter().getTestMethodHtmlLog().insertText("<tr><td>&nbsp;</td><td bgcolor=\"#F0F0F0\">" + text + addStr + "</td></tr>\n");
        }
    }

    /**
     * Write current HTML response data to file. This is not the XML representation, therefore
     * may be used only after explicit page load.
     *
     * @param fileName   Target file path
     * @param escapeHTML HTML output will be escaped if true
     * @throws FileNotFoundException when file is not available.
     */
    public void dumpSource(final String fileName, final boolean escapeHTML) throws FileNotFoundException {
        PrintWriter out = new PrintWriter(new FileOutputStream(fileName));
        out.println("<!-- Dumped on " + (new SimpleDateFormat()).format(new Date()) + ", URL: " + selenium.getLocation() + " -->");

        String source = selenium.getHtmlSource();

        if (escapeHTML) {
            source = util.escapeHTML(source);
            out.println("<html><body><pre>");
            out.println(source);
            out.println("</pre></body></html>");
        } else {
            out.print(source);
        }
        out.flush();
    }

    /**
     * Write current HTML response data to a file in the current test case's log directory.
     *
     * @param escapeHTML HTML output will be escaped if true
     * @return Path of the created file
     * @throws FileNotFoundException when problem occurred.
     */
    public String dumpSource(boolean escapeHTML) throws FileNotFoundException {
        String newFilePath;
        String logPath = getTestClassExecutionData().getHtmlRunReporter().getTestMethodHtmlLog().getLogPath();
        String logPathCanonical = logPath.replace('\\', '/');
        int pos = logPathCanonical.lastIndexOf('/');
        dumpFileCount++;

        if (pos == -1) {
            newFilePath = "dump" + Integer.toString(dumpFileCount) + ".html";
        } else {
            newFilePath = logPath.substring(0, pos) + "/dump" + Integer.toString(dumpFileCount) + ".html";
        }

        dumpSource(newFilePath, escapeHTML);

        return newFilePath;
    }

}
