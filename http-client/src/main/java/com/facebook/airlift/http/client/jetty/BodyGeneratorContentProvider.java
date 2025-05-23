package com.facebook.airlift.http.client.jetty;

import com.facebook.airlift.http.client.BodyGenerator;
import com.google.common.collect.AbstractIterator;
import org.eclipse.jetty.io.Content.Chunk;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicReference;

import static com.google.common.base.Throwables.throwIfUnchecked;
import static org.eclipse.jetty.io.Content.Chunk.EOF;

class BodyGeneratorContentProvider
        extends AbstractContentProvider
{
    private static final ByteBuffer DONE = ByteBuffer.allocate(0);
    private static final ByteBuffer EXCEPTION = ByteBuffer.allocate(0);

    private final BodyGenerator bodyGenerator;
    private final Executor executor;
    private final Iterator<ByteBuffer> iterator;

    public BodyGeneratorContentProvider(BodyGenerator bodyGenerator, Executor executor)
    {
        this.bodyGenerator = bodyGenerator;
        this.executor = executor;
        iterator = iterator();
    }

    @Override
    public Chunk read()
    {
        if (failed.get() != null) {
            return Chunk.from(failed.get());
        }

        return iterator.hasNext() ? Chunk.from(iterator.next(), !iterator.hasNext()) : EOF;
    }

    @Override
    public void fail(Throwable throwable)
    {
        super.fail(throwable);
        iterator.forEachRemaining(chunk -> {});
    }

    public Iterator<ByteBuffer> iterator()
    {
        final BlockingQueue<ByteBuffer> chunks = new ArrayBlockingQueue<>(16);
        final AtomicReference<Exception> exception = new AtomicReference<>();

        executor.execute(() -> {
            try (BodyGeneratorOutputStream out = new BodyGeneratorOutputStream(chunks)) {
                bodyGenerator.write(out);
            }
            catch (Exception e) {
                exception.set(e);
                chunks.add(EXCEPTION);
            }
        });

        return new AbstractIterator<>()
        {
            @Override
            protected ByteBuffer computeNext()
            {
                ByteBuffer chunk;
                try {
                    chunk = chunks.take();
                }
                catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Interrupted", e);
                }

                if (chunk == EXCEPTION) {
                    throwIfUnchecked(exception.get());
                    throw new RuntimeException(exception.get());
                }
                if (chunk == DONE) {
                    return endOfData();
                }
                return chunk;
            }
        };
    }

    private static final class BodyGeneratorOutputStream
            extends OutputStream
    {
        private final BlockingQueue<ByteBuffer> chunks;

        private BodyGeneratorOutputStream(BlockingQueue<ByteBuffer> chunks)
        {
            this.chunks = chunks;
        }

        @Override
        public void write(int b)
                throws IOException
        {
            try {
                // must copy array since it could be reused
                chunks.put(ByteBuffer.wrap(new byte[] {(byte) b}));
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
        }

        @Override
        public void write(byte[] b, int off, int len)
                throws IOException
        {
            try {
                // must copy array since it could be reused
                byte[] copy = Arrays.copyOfRange(b, off, len);
                chunks.put(ByteBuffer.wrap(copy));
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
        }

        @Override
        public void close()
                throws IOException
        {
            try {
                chunks.put(DONE);
            }
            catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new InterruptedIOException();
            }
        }
    }
}
