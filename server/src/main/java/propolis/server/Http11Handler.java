package propolis.server;

import propolis.shared.Messages;

import java.io.InputStream;

@FunctionalInterface
public interface Http11Handler {

    Handlers.ResponseBodyCallback handle(
            Messages.Request header,
            Messages.Response response,
            InputStream inputStream);
}
