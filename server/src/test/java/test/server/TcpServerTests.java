package test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import propolis.server.Utils;

import java.io.IOException;
import java.net.Socket;

import static propolis.server.Server.Daemon;
import static propolis.server.Server.TcpServer;

public class TcpServerTests {

    private TcpServer server = new TcpServer(Utils::tcpEcho);
    private Daemon daemon = new Daemon(server);
    private Socket client;

    public String input = Utils.generateString(4097);

    @Before
    public void setUp() throws IOException {
        daemon.start();
        client = new Socket("localhost", server.port);
    }

    @After
    public void tearDown() throws IOException {
        client.close();
        daemon.stop();
    }

    @Test
    public void shouldDecodeStringOverTcp() throws IOException {
        Assert.assertEquals(input, Utils.requestResponseOverSocket(client, input));
    }

    @Test(timeout = 4000)
    public void shouldRestartDaemonLotsOfTimes() {
        for (int i=0; i<10; i++) {
            daemon.stop();
            daemon.start();
        }
    }
}
