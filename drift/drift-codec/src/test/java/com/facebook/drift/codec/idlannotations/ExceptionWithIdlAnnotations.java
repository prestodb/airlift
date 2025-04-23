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

import com.facebook.drift.annotations.ThriftConstructor;
import com.facebook.drift.annotations.ThriftField;
import com.facebook.drift.annotations.ThriftIdlAnnotation;
import com.facebook.drift.annotations.ThriftStruct;

@ThriftStruct(idlAnnotations = @ThriftIdlAnnotation(key = "message", value = "message"))
public class ExceptionWithIdlAnnotations
        extends Exception
{
    private final int type;

    @ThriftConstructor
    public ExceptionWithIdlAnnotations(@ThriftField(1) String message,
            @ThriftField(2) int type)
    {
        super(message);
        this.type = type;
    }

    @Override
    @ThriftField(1)
    public String getMessage()
    {
        return super.getMessage();
    }

    @ThriftField(2)
    public int getType()
    {
        return type;
    }
}
