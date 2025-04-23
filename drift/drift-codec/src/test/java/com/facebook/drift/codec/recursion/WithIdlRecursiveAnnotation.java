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
package com.facebook.drift.codec.recursion;

import com.facebook.drift.annotations.ThriftField;
import com.facebook.drift.annotations.ThriftIdlAnnotation;
import com.facebook.drift.annotations.ThriftStruct;

import java.util.Objects;

import static com.facebook.drift.annotations.ThriftField.Requiredness;
import static com.facebook.drift.annotations.ThriftIdlAnnotation.RECURSIVE_REFERENCE_ANNOTATION_KEY;

@ThriftStruct
public class WithIdlRecursiveAnnotation
{
    @ThriftField(
            value = 1,
            requiredness = Requiredness.OPTIONAL,
            idlAnnotations = @ThriftIdlAnnotation(key = RECURSIVE_REFERENCE_ANNOTATION_KEY, value = "true"))
    public WithIdlRecursiveAnnotation child;

    @ThriftField(2)
    public String data;

    @Override
    public boolean equals(Object o)
    {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        WithIdlRecursiveAnnotation that = (WithIdlRecursiveAnnotation) o;
        return Objects.equals(child, that.child) &&
                Objects.equals(data, that.data);
    }

    @Override
    public int hashCode()
    {
        return Objects.hash(child, data);
    }
}
