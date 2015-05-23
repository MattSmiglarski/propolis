package test;

import org.junit.Assert;
import org.junit.Test;
import propolis.server.FrameFactory;
import propolis.server.Frames;

import java.util.LinkedHashMap;

public class FrameTests {

    private FrameFactory frameFactory = new FrameFactory(); // Object under test.

    @Test
    public void shouldEncodeDecodeDataFrame() {
        Frames.DataFrame expectedDataFrame = new Frames.DataFrame();
        Frames.DataFrame actualDataFrame = frameFactory.createDataFrame(expectedDataFrame.asHttpFrame());

        Assert.assertEquals(expectedDataFrame.flagEndStream, actualDataFrame.flagEndStream);
        Assert.assertEquals(expectedDataFrame.flagPadded, actualDataFrame.flagPadded);

        Assert.assertArrayEquals(expectedDataFrame.data, actualDataFrame.data);
        Assert.assertArrayEquals(expectedDataFrame.asHttpFrame().payload, actualDataFrame.asHttpFrame().payload);
    }

    @Test
    public void shouldEncodeDecodeHeadersFrame() {
        Frames.HeadersFrame expectedHeadersFrame = new Frames.HeadersFrame();
        Frames.HeadersFrame actualHeadersFrame = frameFactory.createHeadersFrame(expectedHeadersFrame.asHttpFrame());

        Assert.assertEquals(expectedHeadersFrame.exclusive, actualHeadersFrame.exclusive);
        Assert.assertEquals(expectedHeadersFrame.padLength, actualHeadersFrame.padLength);
        Assert.assertEquals(expectedHeadersFrame.streamDependency, actualHeadersFrame.streamDependency);
        Assert.assertEquals(expectedHeadersFrame.weight, actualHeadersFrame.weight);
        Assert.assertEquals(expectedHeadersFrame.padLength, actualHeadersFrame.padLength);

        Assert.assertEquals(expectedHeadersFrame.flagEndStream, actualHeadersFrame.flagEndStream);
        Assert.assertEquals(expectedHeadersFrame.flagEndHeaders, actualHeadersFrame.flagEndHeaders);
        Assert.assertEquals(expectedHeadersFrame.flagPadded, actualHeadersFrame.flagPadded);
        Assert.assertEquals(expectedHeadersFrame.flagPriority, actualHeadersFrame.flagPriority);

        Assert.assertEquals(expectedHeadersFrame.headers, actualHeadersFrame.headers);
        Assert.assertArrayEquals(expectedHeadersFrame.asHttpFrame().payload, actualHeadersFrame.asHttpFrame().payload);
    }

    @Test
    public void shouldEncodeDecodePriorityFrame() {
        Frames.PriorityFrame expectedPriorityFrame = new Frames.PriorityFrame();
        Frames.PriorityFrame actualPriorityFrame = frameFactory.createPriorityFrame(expectedPriorityFrame.asHttpFrame());

        Assert.assertEquals(expectedPriorityFrame.exclusive, actualPriorityFrame.exclusive);
        Assert.assertEquals(expectedPriorityFrame.streamDependency, actualPriorityFrame.streamDependency);
        Assert.assertEquals(expectedPriorityFrame.weight, actualPriorityFrame.weight);

        Assert.assertEquals(0, actualPriorityFrame.asHttpFrame().flags);
        Assert.assertArrayEquals(expectedPriorityFrame.asHttpFrame().payload, actualPriorityFrame.asHttpFrame().payload);
    }

    @Test
    public void shouldEncodeDecodeResetFrame() {
        Frames.ResetFrame expectedResetFrame = new Frames.ResetFrame();
        expectedResetFrame.error = Frames.Error.COMPRESSION_ERROR;
        Frames.ResetFrame actualResetFrame = frameFactory.createResetStreamFrame(expectedResetFrame.asHttpFrame());

        Assert.assertEquals(expectedResetFrame.error, actualResetFrame.error);
        Assert.assertArrayEquals(expectedResetFrame.asHttpFrame().payload, actualResetFrame.asHttpFrame().payload);
    }

    @Test
    public void shouldEncodeDecodeSettingsFrame() {
        Frames. SettingsFrame expectedSettingsFrame = new Frames.SettingsFrame();
        Frames.SettingsFrame actualSettingsFrame = frameFactory.createSettingsFrame(expectedSettingsFrame.asHttpFrame());

        Assert.assertEquals(expectedSettingsFrame.ack, actualSettingsFrame.ack);
        Assert.assertEquals(expectedSettingsFrame.settings, actualSettingsFrame.settings);

        Assert.assertArrayEquals(expectedSettingsFrame.asHttpFrame().payload, actualSettingsFrame.asHttpFrame().payload);
    }

    @Test
    public void shouldEncodeDecodePushPromiseFrame() {
        Frames.PushPromiseFrame expectedPushPromiseFrame = new Frames.PushPromiseFrame();
        expectedPushPromiseFrame.headers = new LinkedHashMap<>();
        Frames.PushPromiseFrame actualPushPromiseFrame = frameFactory.createPushPromiseFrame(expectedPushPromiseFrame.asHttpFrame());

        Assert.assertEquals(expectedPushPromiseFrame.padLength, actualPushPromiseFrame.padLength);
        Assert.assertEquals(expectedPushPromiseFrame.reserved, actualPushPromiseFrame.reserved);
        Assert.assertEquals(expectedPushPromiseFrame.promisedStreamId, actualPushPromiseFrame.promisedStreamId);
        Assert.assertEquals(expectedPushPromiseFrame.headers, actualPushPromiseFrame.headers);
        Assert.assertEquals(expectedPushPromiseFrame.flagEndHeaders, actualPushPromiseFrame.flagEndHeaders);
        Assert.assertEquals(expectedPushPromiseFrame.flagPadded, actualPushPromiseFrame.flagPadded);

        Assert.assertArrayEquals(expectedPushPromiseFrame.asHttpFrame().payload, actualPushPromiseFrame.asHttpFrame().payload);
    }

    @Test
    public void shouldEncodeDecodePingFrame() {
        Frames.PingFrame expectedPingFrame = new Frames.PingFrame();
        Frames.PingFrame actualPingFrame = frameFactory.createPingFrame(expectedPingFrame.asHttpFrame());
        Assert.assertEquals(expectedPingFrame.data, actualPingFrame.data);
        Assert.assertEquals(expectedPingFrame.ack, actualPingFrame.ack);
        Assert.assertArrayEquals(expectedPingFrame.asHttpFrame().payload, actualPingFrame.asHttpFrame().payload);
    }

    @Test
    public void shouldEncodeDecodeGoAwayFrame() {
        Frames.GoAwayFrame expectedGoAwayFrame = new Frames.GoAwayFrame();
        expectedGoAwayFrame.data = "foobar blarr blarr".getBytes();
        expectedGoAwayFrame.error = Frames.Error.FLOW_CONTROL_ERROR;
        expectedGoAwayFrame.lastStreamId = 42;
        Frames.GoAwayFrame actualGoAwayFrame = frameFactory.createGoAwayFrame(expectedGoAwayFrame.asHttpFrame());

        Assert.assertEquals(expectedGoAwayFrame.lastStreamId, actualGoAwayFrame.lastStreamId);
        Assert.assertEquals(expectedGoAwayFrame.error, actualGoAwayFrame.error);
        Assert.assertArrayEquals(expectedGoAwayFrame.data, actualGoAwayFrame.data);

        Assert.assertArrayEquals(expectedGoAwayFrame.asHttpFrame().payload, actualGoAwayFrame.asHttpFrame().payload);
    }

    @Test
    public void shouldEncodeDecodeWindowUpdateFrame() {
        Frames.WindowUpdateFrame expectedWindowUpdateFrame = new Frames.WindowUpdateFrame();
        Frames.WindowUpdateFrame actualWindowUpdateFrame = frameFactory.createWindowUpdateFrame(expectedWindowUpdateFrame.asHttpFrame());

        Assert.assertEquals(expectedWindowUpdateFrame.canTransmit, actualWindowUpdateFrame.canTransmit);

        Assert.assertArrayEquals(expectedWindowUpdateFrame.asHttpFrame().payload, actualWindowUpdateFrame.asHttpFrame().payload);
    }

    @Test
    public void shouldEncodeDecodeContinuationFrame() {
        Frames.ContinuationFrame expectedContinuationFrame = new Frames.ContinuationFrame();
        expectedContinuationFrame.headers = new LinkedHashMap<>();
        Frames.ContinuationFrame actualContinuationFrame = frameFactory.createContinuationFrame(expectedContinuationFrame.asHttpFrame());

        Assert.assertEquals(expectedContinuationFrame.flagEndHeaders, actualContinuationFrame.flagEndHeaders);
        Assert.assertEquals(expectedContinuationFrame.headers, actualContinuationFrame.headers);

        Assert.assertArrayEquals(expectedContinuationFrame.asHttpFrame().payload, actualContinuationFrame.asHttpFrame().payload);
    }
}
