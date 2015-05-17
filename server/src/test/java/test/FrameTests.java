package test;

import org.junit.Assert;
import org.junit.Test;
import propolis.server.FrameFactory;
import propolis.server.Frames;

public class FrameTests {

    private FrameFactory frameFactory = new FrameFactory(); // Object under test.

    @Test
    public void shouldAllBeIdempotent() {
        Frames.DataFrame expectedDataFrame = new Frames.DataFrame();
        Frames.HeadersFrame expectedHeadersFrame = new Frames.HeadersFrame();
        Frames.PriorityFrame expectedPriorityFrame = new Frames.PriorityFrame();
        Frames.ResetFrame expectedResetFrame = new Frames.ResetFrame();
        Frames.SettingsFrame expectedSettingsFrame = new Frames.SettingsFrame();
        Frames.PushPromiseFrame expectedPushPromiseFrame = new Frames.PushPromiseFrame();
        Frames.PingFrame expectedPingFrame = new Frames.PingFrame();
        Frames.GoAwayFrame expectedGoAwayFrame = new Frames.GoAwayFrame();
        Frames.WindowUpdateFrame expectedWindowUpdateFrame = new Frames.WindowUpdateFrame();
        Frames.ContinuationFrame expectedContinuationFrame = new Frames.ContinuationFrame();

        Frames.DataFrame actualDataFrame = frameFactory.createDataFrame(expectedDataFrame.asHttpFrame());
        Frames.HeadersFrame actualHeadersFrame = frameFactory.createHeadersFrame(expectedHeadersFrame.asHttpFrame());
        Frames.PriorityFrame actualPriorityFrame = frameFactory.createPriorityFrame(expectedPriorityFrame.asHttpFrame());
        Frames.ResetFrame actualResetFrame = frameFactory.createResetStreamFrame(expectedResetFrame.asHttpFrame());
        Frames.SettingsFrame actualSettingsFrame = frameFactory.createSettingsFrame(expectedSettingsFrame.asHttpFrame());
        Frames.PushPromiseFrame actualPushPromiseFrame = frameFactory.createPushPromiseFrame(expectedPushPromiseFrame.asHttpFrame());
        Frames.PingFrame actualPingFrame = frameFactory.createPingFrame(expectedPingFrame.asHttpFrame());
        Frames.GoAwayFrame actualGoAwayFrame = frameFactory.createGoAwayFrame(expectedGoAwayFrame.asHttpFrame());
        Frames.WindowUpdateFrame actualWindowUpdateFrame = frameFactory.createWindowUpdateFrame(expectedWindowUpdateFrame.asHttpFrame());
        Frames.ContinuationFrame actualContinuationFrame = frameFactory.createContinuationFrame(expectedContinuationFrame.asHttpFrame());

        Assert.assertEquals(expectedDataFrame, actualDataFrame);
        Assert.assertEquals(expectedHeadersFrame, actualHeadersFrame);
        Assert.assertEquals(expectedPriorityFrame, actualPriorityFrame);
        Assert.assertEquals(expectedResetFrame, actualResetFrame);
        Assert.assertEquals(expectedSettingsFrame, actualSettingsFrame);
        Assert.assertEquals(expectedPushPromiseFrame, actualPushPromiseFrame);
        Assert.assertEquals(expectedPingFrame, actualPingFrame);
        Assert.assertEquals(expectedGoAwayFrame, actualGoAwayFrame);
        Assert.assertEquals(expectedWindowUpdateFrame, actualWindowUpdateFrame);
        Assert.assertEquals(expectedContinuationFrame, actualContinuationFrame);
    }

}
