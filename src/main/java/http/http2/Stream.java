package http.http2;

/**
 * Stub -- not yet required.
 */
public class Stream {

    static enum State {
        IDLE, OPEN, CLOSED, RESERVED_LOCAL, RESERVED_REMOTE, HALF_CLOSED_LOCAL, HALF_CLOSED_REMOTE
    }

    State state = State.IDLE;

}
