package test.server;

import propolis.server.Http11;
import propolis.server.Server;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import propolis.client.Client;

public class Http11ServerTests {

    Server.Daemon daemon;

    @Before
    public void setUp() {
        daemon = new Server.Daemon(
                new Server.TcpServer(8003, Http11::handleHttp11Connection));
        daemon.start();
        System.out.println("Server started");
    }

    @After
    public void tearDown() {
        daemon.stop();
    }

    @Test
    public void sanityTest() {
        assertGetResponseEquals("http://localhost:8003", "This server is correctly installed");
    }

    static void assertGetResponseEquals(String url, String expected) {
        Assert.assertEquals(expected, Client.quickGetResponseBody(url));
    }
}