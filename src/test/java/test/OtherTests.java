package test;

import http.Messages;
import http.http2.Application;
import http.http2.Frames;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class OtherTests {

    @Test
    public void shouldCreateAndParseResponse() throws IOException {
        Messages.Response response = new Messages.Response();
        response.headers.put("Server", "Unit tests");

        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        Messages.writeResponse(baos, response);
        byte[] responseData = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(responseData);

        Messages.Response readResponse = Messages.readResponse(bais);

        Assert.assertEquals(response.version, readResponse.version);
        Assert.assertEquals("Unit tests", readResponse.headers.get("Server"));
    }

    @Test
    public void shouldCreateAndParseRequest() throws IOException {
        Messages.Request request = new Messages.Request();
        request.method = "GET";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        Messages.writeRequest(baos, request);
        byte[] requestData = baos.toByteArray();

        ByteArrayInputStream bais = new ByteArrayInputStream(requestData);

        Messages.Request readRequest = Messages.readRequest(bais);

        Assert.assertEquals("GET", readRequest.method);
    }

    @Test
    public void shouldCreateAndParseFrame() throws IOException {
        Frames.Frame frame = new Frames.SettingsFrame();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        frame.write(baos);
        byte[] frameData = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(frameData);
        List<Frames.SettingsFrame> receivedFrames = new ArrayList<>();
        Frames.Frame.read(bais, new Application.Adapter() {
            @Override
            public void onFrame(Frames.SettingsFrame frame) {
                receivedFrames.add(frame);
            }
        });
        Assert.assertTrue(receivedFrames.size() == 1);
        Frames.SettingsFrame receivedFrame = receivedFrames.get(0);
    }

    @Test
    public void shouldDetermineTheFrameLength() throws IOException {
        Frames.PingFrame frame = new Frames.PingFrame();
        frame.data = "1234567".getBytes();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        frame.write(baos);
        byte[] frameData = baos.toByteArray();
        ByteArrayInputStream bais = new ByteArrayInputStream(frameData);
        List<Frames.PingFrame> receivedFrames = new ArrayList<>();
        Frames.Frame.read(bais, new Application.Adapter() {
            @Override
            public void onFrame(Frames.PingFrame frame) {
                receivedFrames.add(frame);
            }
        });
        Assert.assertTrue(receivedFrames.size() == 1);
        Frames.PingFrame receivedFrame = receivedFrames.get(0);
        Assert.assertEquals(frame.data.length, receivedFrame.data.length);
    }

    @Test
    public void howDoesItWork() {
        for (int type = 0; type < (2 << 15) - 1; type++) {
            byte[] a24BitInt = new byte[3];
            a24BitInt[0] = (byte) ((0xFF0000 & type) >> 16);
            a24BitInt[1] = (byte) ((0x00FF00 & type) >> 8);
            a24BitInt[2] = (byte) (0x0000FF & type);

            int decoded = (0xFF0000 & (a24BitInt[0] << 16))
                    | (0x00FF00 & (a24BitInt[1] << 8))
                    | (0x0000FF & (a24BitInt[2]));

            Assert.assertEquals(type, decoded);
        }
    }

    public static void main(String[] args) throws IOException {
        byte[] output = new byte[4];
        byte[] input = {(byte) (2)};
        new ByteArrayInputStream(input).read(output);
        ByteBuffer wrap = ByteBuffer.wrap(output).order(ByteOrder.LITTLE_ENDIAN);

        System.out.println(String.format("%X", wrap.getInt()));
    }
}
