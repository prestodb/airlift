/*
 * Copyright (C) 2017 Facebook, Inc.
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
package com.facebook.drift.codec.metadata;

import com.facebook.drift.codec.BonkBuilder;
import com.facebook.drift.codec.OneOfEverything;
import com.facebook.drift.codec.UnionBean;
import com.facebook.drift.codec.recursion.CoRecursive;
import com.facebook.drift.codec.recursion.CoRecursiveTree;
import org.testng.annotations.Test;

import static org.testng.Assert.assertNotNull;

public class TestToString
{
    @Test
    public void testToString()
    {
        assertToString(BonkBuilder.class);
        assertToString(UnionBean.class);
        assertToString(OneOfEverything.class);
        assertToString(CoRecursive.class);
        assertToString(CoRecursiveTree.class);
    }

    private static void assertToString(Class<?> clazz)
    {
        ThriftType type = new ThriftCatalog().getThriftType(clazz);
        assertNotNull(type.toString());
        assertNotNull(type.getStructMetadata().toString());
        for (ThriftFieldMetadata fieldMetadata : type.getStructMetadata().getFields()) {
            assertNotNull(fieldMetadata.toString());
        }
    }
}
