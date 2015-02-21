package test.server;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import propolis.shared.Utils;

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
        System.out.println(input);
        Assert.assertEquals(input, Utils.requestResponseOverSocket(client, input));
    }
}
