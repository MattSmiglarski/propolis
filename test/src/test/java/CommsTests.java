import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import propolis.shared.Frames;
import test.HttpInputOutputStream;

public class CommsTests {

    private HttpInputOutputStream io = new HttpInputOutputStream();

    @After
    public void tearDown() {
        io.stop();
    }

    @Test(timeout = 5000)
    public void simpleTest() {
        io.clientSends(new Frames.PingFrame());
        Frames.Frame serverReceivedFrame = io.serverReceives();
        io.serverSends(new Frames.PingFrame());
        Frames.Frame clientReceivedFrame = io.clientReceives();
    }
}
