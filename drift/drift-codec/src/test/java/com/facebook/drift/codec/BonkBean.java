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
package com.facebook.drift.codec;

import com.facebook.drift.annotations.ThriftField;
import com.facebook.drift.annotations.ThriftStruct;

import java.util.Objects;

import static com.google.common.base.MoreObjects.toStringHelper;

@ThriftStruct("Bonk")
public final class BonkBean
{
    private String message;
    private int type;

    public BonkBean()
    {
    }

    public BonkBean(String message, int type)
    {
        this.message = message;
        this.type = type;
    }

    @ThriftField(1)
    public String getMessage()
    {
        return message;
    }

    @ThriftField
    public void setMessage(String message)
    {
        this.message = message;
    }

    @ThriftField(2)
    public int getType()
    {
        return type;
    }

    @ThriftField
    public void setType(int type)
    {
        this.type = type;
    }

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        BonkBean bonkBean = (BonkBean) o;
        return type == bonkBean.type &&
                Objects.equals(message, bonkBean.message);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(message, type);
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .add("message", message)
                .add("type", type)
                .toString();
    }
}
