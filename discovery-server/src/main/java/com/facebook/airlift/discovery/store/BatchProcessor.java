/*
 * Copyright 2010 Proofpoint, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.facebook.airlift.discovery.store;

import com.facebook.airlift.log.Logger;
import com.google.common.base.Preconditions;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.weakref.jmx.Managed;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

import static com.facebook.airlift.concurrent.Threads.threadsNamed;
import static java.util.Objects.requireNonNull;
import static java.util.concurrent.Executors.newSingleThreadExecutor;

public class BatchProcessor<T>
{
    private static final Logger log = Logger.get(BatchProcessor.class);

    private final BatchHandler<T> handler;
    private final int maxBatchSize;
    private final BlockingQueue<T> queue;
    private final String name;

    private ExecutorService executor;
    private volatile Future<?> future;

    private final AtomicLong processedEntries = new AtomicLong();
    private final AtomicLong droppedEntries = new AtomicLong();
    private final AtomicLong errors = new AtomicLong();

    public BatchProcessor(String name, BatchHandler<T> handler, int maxBatchSize, int queueSize)
    {
        requireNonNull(name, "name is null");
        requireNonNull(handler, "handler is null");
        Preconditions.checkArgument(queueSize > 0, "queue size needs to be a positive integer");
        Preconditions.checkArgument(maxBatchSize > 0, "max batch size needs to be a positive integer");

        this.name = name;
        this.handler = handler;
        this.maxBatchSize = maxBatchSize;
        this.queue = new ArrayBlockingQueue<T>(queueSize);
    }

    @PostConstruct
    public synchronized void start()
    {
        if (future == null) {
            executor = newSingleThreadExecutor(threadsNamed("batch-processor-" + name));

            future = executor.submit(new Runnable()
            {
                public void run()
                {
                    while (!Thread.interrupted()) {
                        final List<T> entries = new ArrayList<T>(maxBatchSize);

                        try {
                            T first = queue.take();
                            entries.add(first);
                            queue.drainTo(entries, maxBatchSize - 1);

                            handler.processBatch(Collections.unmodifiableList(entries));

                            processedEntries.addAndGet(entries.size());
                        }
                        catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        catch (Throwable t) {
                            errors.incrementAndGet();
                            log.warn(t, "Error handling batch");
                        }

                        // TODO: expose timestamp of last execution via jmx
                    }
                }
            });
        }
    }

    @Managed
    public long getProcessedEntries()
    {
        return processedEntries.get();
    }

    @Managed
    public long getDroppedEntries()
    {
        return droppedEntries.get();
    }

    @Managed
    public long getErrors()
    {
        return errors.get();
    }

    @Managed
    public long getQueueSize()
    {
        return queue.size();
    }

    @PreDestroy
    public synchronized void stop()
    {
        if (future != null) {
            future.cancel(true);
            executor.shutdownNow();

            future = null;
        }
    }

    public void put(T entry)
    {
        Preconditions.checkState(!future.isCancelled(), "Processor is not running");
        requireNonNull(entry, "entry is null");

        while (!queue.offer(entry)) {
            // throw away oldest and try again
            if (queue.poll() != null) {
                droppedEntries.incrementAndGet();
            }
        }
    }

    public static interface BatchHandler<T>
    {
        void processBatch(Collection<T> entries);
    }
}
