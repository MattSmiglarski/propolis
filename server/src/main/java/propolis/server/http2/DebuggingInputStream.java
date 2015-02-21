package propolis.server.http2;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.LinkedTransferQueue;

public class DebuggingInputStream extends InputStream {

    private static final Logger log = LoggerFactory.getLogger(DebuggingInputStream.class);

    private InputStream is;
    private LinkedTransferQueue<Integer> read = new LinkedTransferQueue<>();

    public DebuggingInputStream(InputStream is) {
        this.is = is;

        Runnable runnable = new Runnable() {

            @Override
            public void run() {
                List<Integer> buffer = new ArrayList<>(16);

                try {
                    buffer.add(read.take());
                    if (buffer.size() == 16) {
                        log.info(String.format("%X%X %X%X %X%X %X%X %X%X %X%X %X%X %X%X", buffer.toArray()));
                        buffer.clear();
                    }
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
            }
        };
        new Thread(runnable).start();
    }

    @Override
    public int read() throws IOException {
        int i = is.read();
        read.put(i);
        return i;
    }
}
