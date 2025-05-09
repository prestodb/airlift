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
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.smile.SmileFactory;
import com.google.common.collect.Streams;
import jakarta.inject.Inject;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.Options;
import org.iq80.leveldb.impl.Iq80DBFactory;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;

import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.ImmutableList.toImmutableList;

public class PersistentStore
        implements LocalStore
{
    private static final Logger log = Logger.get(PersistentStore.class);
    private final DB db;
    private final ObjectMapper mapper = new ObjectMapper(new SmileFactory());

    @Inject
    public PersistentStore(PersistentStoreConfig config)
            throws IOException
    {
        db = Iq80DBFactory.factory.open(config.getLocation(), new Options().createIfMissing(true));
    }

    @Override
    public void put(Entry entry)
    {
        byte[] dbEntry;
        try {
            dbEntry = mapper.writeValueAsBytes(entry);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }

        db.put(entry.getKey(), dbEntry);
    }

    @Override
    public Entry get(byte[] key)
    {
        try {
            return mapper.readValue(db.get(key), Entry.class);
        }
        catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    @Override
    public void delete(byte[] key, Version version)
    {
        db.delete(key);
    }

    @Override
    public Iterable<Entry> getAll()
    {
        return Streams.stream(db)
                .map(dbEntry -> {
                    try {
                        return mapper.readValue(dbEntry.getValue(), Entry.class);
                    }
                    catch (IOException e) {
                        byte[] key = dbEntry.getKey();
                        log.error(e, "Corrupt entry " + Arrays.toString(key));

                        // delete the corrupt entry... if another node has a non-corrupt version it will be replicated
                        db.delete(key);

                        // null if filtered below
                        return null;
                    }
                })
                .filter(notNull())
                .collect(toImmutableList());
    }
}
