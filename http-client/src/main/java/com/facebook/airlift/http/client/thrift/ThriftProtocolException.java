/*
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
package com.facebook.airlift.http.client.thrift;

import java.io.IOException;

import static java.util.Objects.requireNonNull;

public class ThriftProtocolException
        extends IOException
{
    private final Class<?> type;

    public ThriftProtocolException(Class<?> type, Throwable cause)
    {
        super("Invalid thrift object for Java type " + requireNonNull(type, "type is null").getName(), cause);
        this.type = type;
    }

    /**
     * Returns the type of object that failed Thrift .
     */
    public Class<?> getType()
    {
        return type;
    }
}
