package net.serenitybdd.core.photography;

import net.serenitybdd.core.photography.bluring.Blurer;
import net.serenitybdd.core.photography.resizing.Resizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

public class Darkroom {

    private final static Logger LOGGER = LoggerFactory.getLogger(Darkroom.class);
    private List<? extends PhotoFilter> processors = Arrays.asList(new Resizer(), new Blurer());
    private DarkroomProcessingLine processingLine;
    private Thread screenshotThread;

    public void isOpenForBusiness() {
        if (theDarkroomIsClosed()) {
            LOGGER.debug("Opening darkroom");
            start();
        }
    }

    public Darkroom() { }

    private boolean theDarkroomIsClosed() {
        return !theDarkroomIsOpen();
    }

    private boolean theDarkroomIsOpen() {
        return (processingLine != null && processingLine.openForBusiness);
    }

    public void waitUntilClose() {
        LOGGER.debug("Closing darkroom");
        if (theDarkroomIsOpen()) {
            terminate();
        }
    }

    public void start() {

        this.processingLine = new DarkroomProcessingLine(processors);
        screenshotThread = new Thread(processingLine,"Darkroom Processing Line");
        screenshotThread.setDaemon(true);
        screenshotThread.start();
    }

    public void terminate() {
        if (processingLine != null) {
            shutdownProcessingLine();
        }
        DarkroomFileSystem.close();
    }

    public ScreenshotReceipt submitForProcessing(ScreenshotNegative negative) {
        ensureThatTheProcessingLineIsRunning();
        return processingLine.addToProcessingQueue(negative);
    }

    private void ensureThatTheProcessingLineIsRunning() {
        if (processingLine == null) {
            start();
        }
    }

    private void shutdownProcessingLine() {
        processingLine.terminate();
        try {
            screenshotThread.join();
        } catch (InterruptedException e) {
            LOGGER.error("Screenshot processing interrupted",e);
        }
    }

    /**
     * Returns a receipt of the screenshot negative with the definitive destination path
     */
    public ScreenshotReceipt sendNegative(ScreenshotNegative screenshotNegative) {
        LOGGER.debug("Send negative for processing for " + screenshotNegative.getScreenshotPath());
        return submitForProcessing(screenshotNegative);
    }
}
