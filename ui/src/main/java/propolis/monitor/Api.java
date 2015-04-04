package propolis.monitor;

import propolis.server.Server;
import propolis.shared.Frames;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

@Path("/")
public class Api {

    private propolis.shared.Application application;
    private Server.TcpServer server;

    @javax.inject.Inject
    public Api(Server.TcpServer server) {
        this.server = server;
    }

    /*
    static class ProxiedApplication extends Application.UntypedAdapter {

        private EventOutput eventOutput;

        public ProxiedApplication(EventOutput eventOutput) {
            this.eventOutput = eventOutput;
        }

        public void onAnyFrame(Frames.Frame frame) {
            final OutboundEvent.Builder eventBuilder = new OutboundEvent.Builder();
            eventBuilder.name("message-to-client");
            eventBuilder.data(frame.toString());
            final OutboundEvent event = eventBuilder.build();
            try {
                eventOutput.write(event);
            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                try {
                    eventOutput.close();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @GET
    @Path("/start")
    @Produces(SseFeature.SERVER_SENT_EVENTS)
    public EventOutput start() throws IOException, URISyntaxException {
        EventOutput eventOutput = new EventOutput();

        application = new ProxiedApplication(eventOutput);
        propolis.client.Client.requestHttp2("http://localhost:" + server.port, application);
        return eventOutput;
    }
    */

    @GET
    @Path("/ping")
    public void ping() {
        application.sendFrame(new Frames.PingFrame());
    }

    @GET
    @Path("/test")
    public String test() {
        return "foo";
    }
}