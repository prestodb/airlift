/*
 * Copyright (C) 2012 Facebook, Inc.
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
package com.facebook.drift.codec.idlannotations;

import com.facebook.drift.annotations.ThriftField;
import com.facebook.drift.annotations.ThriftIdlAnnotation;
import com.facebook.drift.annotations.ThriftUnion;
import com.facebook.drift.annotations.ThriftUnionId;

@ThriftUnion(
        idlAnnotations = {
                @ThriftIdlAnnotation(key = "testkey1", value = "testvalue1"),
                @ThriftIdlAnnotation(key = "testkey2", value = "testvalue2")})
public class UnionWithIdlAnnotations
{
    private short unionType;
    private Object value;

    @ThriftUnionId
    public short getUnionType()
    {
        return unionType;
    }

    @ThriftField(1)
    public void setMessage(String message)
    {
        this.value = message;
        this.unionType = 1;
    }

    @ThriftField(1)
    public String getMessage()
    {
        if (unionType != 1) {
            throw new IllegalStateException("not a message");
        }
        return (String) value;
    }

    @ThriftField(2)
    public void setType(int type)
    {
        this.value = type;
        this.unionType = 2;
    }

    @ThriftField(2)
    public int getType()
    {
        if (unionType != 2) {
            throw new IllegalStateException("not a type");
        }
        return (int) value;
    }
}
