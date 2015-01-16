package test.server;

import org.junit.Assert;
import org.junit.Test;
import propolis.shared.Frames;
import propolis.shared.Messages;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
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
        Frames.SettingsFrame receivedFrame = (Frames.SettingsFrame) Frames.Frame.readSync(bais);
    }
}
