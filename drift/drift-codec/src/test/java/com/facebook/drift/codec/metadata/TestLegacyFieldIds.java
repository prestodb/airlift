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
package com.facebook.drift.codec.metadata;

import com.facebook.drift.annotations.ThriftField;
import com.facebook.drift.annotations.ThriftStruct;
import com.facebook.drift.annotations.ThriftUnion;
import com.facebook.drift.annotations.ThriftUnionId;
import com.google.common.collect.ImmutableSet;
import org.testng.annotations.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;

import static com.google.common.base.Preconditions.checkArgument;
import static org.assertj.core.api.Assertions.assertThat;

public class TestLegacyFieldIds
{
    @Test
    public void testLegacyIdCorrectlyAnnotated()
    {
        ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), LegacyIdCorrect.class);
        ThriftStructMetadata metadata = builder.build();

        Set<Integer> seen = new HashSet<>();
        for (ThriftFieldMetadata field : metadata.getFields()) {
            seen.add((int) field.getId());
        }

        assertThat(seen)
                .as("fields found in LegacyIdCorrect")
                .isEqualTo(LegacyIdCorrect.IDS);
    }

    @Test
    public void testLegacyIdCorrectlyAnnotatedWhiteBox()
    {
        ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), LegacyIdCorrect.class);

        Set<Integer> seen = new HashSet<>();

        for (FieldMetadata field : builder.fields) {
            String name = field.getName();
            short id = field.getId();
            Boolean legacy = field.isLegacyId();

            assertThat(name)
                    .as("name of field " + field)
                    .matches("^(notLegacy|legacy).*");
            if (name.startsWith("legacy")) {
                assertThat(id)
                        .as("id of field " + field)
                        .isLessThan((short) 0);
                assertThat(legacy)
                        .as("isLegacyId of field " + field)
                        .isTrue();
            }
            else {
                assertThat(id)
                        .as("id of field " + field)
                        .isGreaterThanOrEqualTo((short) 0);
                assertThat(legacy)
                        .as("isLegacyId of field " + field)
                        .isFalse();
            }

            seen.add((int) id);
        }

        assertThat(seen)
                .as("present fields in the struct")
                .isEqualTo(LegacyIdCorrect.IDS);
    }

    @ThriftStruct
    public static final class LegacyIdCorrect
    {
        private static final Set<Integer> IDS = ImmutableSet.of(-4, -3, -2, -1, 0, 1, 2);

        @ThriftField(0)
        public boolean notLegacyId0;
        @ThriftField(1)
        public boolean notLegacyId1;
        @ThriftField(2)
        public boolean notLegacyId2;
        @ThriftField(value = -1, isLegacyId = true)
        public boolean legacyIdOnField;

        @ThriftField(value = -2, isLegacyId = true)
        public boolean getLegacyIdOnGetterOnly()
        {
            return false;
        }

        @ThriftField
        public void setLegacyIdOnGetterOnly(boolean value)
        {
        }

        @ThriftField
        public boolean getLegacyIdOnSetterOnly()
        {
            return false;
        }

        @ThriftField(value = -3, isLegacyId = true)
        public void setLegacyIdOnSetterOnly(boolean value)
        {
        }

        @ThriftField(value = -4, isLegacyId = true)
        public boolean getLegacyIdOnBoth()
        {
            return false;
        }

        @ThriftField(value = -4, isLegacyId = true)
        public void setLegacyIdOnBoth(boolean value)
        {
        }
    }

    @Test
    public void testLegacyIdIncorrect()
    {
        // 1: Missing isLegacyId=true when necessary
        {
            ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), LegacyIdIncorrectlyMissing.class);

            MetadataErrors metadataErrors = builder.getMetadataErrors();

            assertThat(metadataErrors.getErrors())
                    .as("metadata errors")
                    .hasSize(1);

            assertThat(metadataErrors.getWarnings())
                    .as("metadata warnings")
                    .isEmpty();

            assertThat(metadataErrors.getErrors().get(0).getMessage())
                    .as("error message")
                    .containsIgnoringCase("has a negative field id but not isLegacyId=true");
        }

        // 2: Has isLegacyId=true when unnecessary
        {
            ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), LegacyIdIncorrectlyPresent.class);

            MetadataErrors metadataErrors = builder.getMetadataErrors();

            assertThat(metadataErrors.getErrors())
                    .as("metadata errors")
                    .hasSize(1);

            assertThat(metadataErrors.getWarnings())
                    .as("metadata warnings")
                    .isEmpty();

            assertThat(metadataErrors.getErrors().get(0).getMessage())
                    .as("error message")
                    .containsIgnoringCase("has isLegacyId=true but not a negative field id");
        }

        // 3: Must be consistent
        for (Class<?> invalidClass : Arrays.asList(LegacyIdInconsistent1.class, LegacyIdInconsistent2.class, LegacyIdInconsistent3.class, LegacyIdInconsistent4.class)) {
            ThriftStructMetadataBuilder builder = new ThriftStructMetadataBuilder(new ThriftCatalog(), invalidClass);

            MetadataErrors metadataErrors = builder.getMetadataErrors();

            assertThat(metadataErrors.getErrors())
                    .as("metadata errors")
                    .isNotEmpty();

            assertThat(metadataErrors.getWarnings())
                    .as("metadata warnings")
                    .isEmpty();

            assertThat(metadataErrors.getErrors().get(0).getMessage())
                    .as("error message")
                    .containsIgnoringCase("has both isLegacyId=true and isLegacyId=false");
        }
    }

    @ThriftStruct
    public static final class LegacyIdIncorrectlyMissing
    {
        @ThriftField(-4)
        public boolean field;
    }

    @ThriftStruct
    public static final class LegacyIdIncorrectlyPresent
    {
        @ThriftField(value = 4, isLegacyId = true)
        public boolean field;
    }

    /* legacy, getter correct, setter wrong */
    @ThriftStruct
    public static final class LegacyIdInconsistent1
    {
        @ThriftField(value = -4, isLegacyId = true)
        public boolean getField()
        {
            return false;
        }

        @ThriftField(-4)
        public void setField(boolean value)
        {
        }
    }

    /* legacy, setter correct, getter wrong */
    @ThriftStruct
    public static final class LegacyIdInconsistent2
    {
        @ThriftField(-4)
        public boolean getField()
        {
            return false;
        }

        @ThriftField(value = -4, isLegacyId = true)
        public void setField(boolean value)
        {
        }
    }

    /* not legacy, setter correct, getter wrong */
    @ThriftStruct
    public static final class LegacyIdInconsistent3
    {
        @ThriftField(value = 4, isLegacyId = true)
        public boolean getField()
        {
            return false;
        }

        @ThriftField(4)
        public void setField(boolean value)
        {
        }
    }

    /* not legacy, getter correct, setter wrong */
    @ThriftStruct
    public static final class LegacyIdInconsistent4
    {
        @ThriftField(4)
        public boolean getField()
        {
            return false;
        }

        @ThriftField(value = 4, isLegacyId = true)
        public void setField(boolean value)
        {
        }
    }

    @Test
    public void testGetThriftFieldIsLegacyId()
    {
        Function<ThriftField, FieldMetadata> makeFakeFieldMetadata = input -> new FieldMetadata(input, FieldKind.THRIFT_FIELD)
        {
            @Override
            public Type getJavaType()
            {
                throw new UnsupportedOperationException();
            }

            @Override
            public String extractName()
            {
                throw new UnsupportedOperationException();
            }
        };

        for (Field f : ReflectionHelper.findAnnotatedFields(SomeThriftFields.class, ThriftField.class)) {
            Optional<Boolean> expected;
            if (f.getName().startsWith("expectTrue")) {
                expected = Optional.of(true);
            }
            else if (f.getName().startsWith("expectFalse")) {
                expected = Optional.of(false);
            }
            else if (f.getName().startsWith("expectNothing")) {
                expected = Optional.empty();
            }
            else {
                checkArgument(f.getName().startsWith("broken"));
                continue;
            }

            Optional<Boolean> actual = makeFakeFieldMetadata.apply(f.getAnnotation(ThriftField.class)).getThriftFieldIsLegacyId();

            assertThat(actual)
                    .as("result of getThriftFieldIsLegacyId on " + f)
                    .isEqualTo(expected);
        }
    }

    private static class SomeThriftFields
    {
        @ThriftField(+1)
        boolean expectFalse1;
        @ThriftField(-1)
        boolean expectFalse2;
        @ThriftField
        boolean broken;  // see comments in impl.

        @ThriftField(value = +1, isLegacyId = true)
        boolean expectTrue1;
        @ThriftField(value = -1, isLegacyId = true)
        boolean expectTrue2;
        @ThriftField(isLegacyId = true)
        boolean expectTrue3;

        @ThriftField
        boolean expectNothing;
    }

    @Test
    public void testLegacyIdOnUnion()
    {
        ThriftUnionMetadataBuilder builder = new ThriftUnionMetadataBuilder(new ThriftCatalog(), LegacyIdUnionCorrect.class);
        ThriftStructMetadata metadata = builder.build();

        Set<Integer> seen = new HashSet<>();
        for (ThriftFieldMetadata field : metadata.getFields()) {
            seen.add((int) field.getId());
        }

        assertThat(seen)
                .as("fields found in LegacyIdUnionCorrect")
                .isEqualTo(LegacyIdUnionCorrect.IDS);
    }

    @ThriftUnion
    public static final class LegacyIdUnionCorrect
    {
        private static final Set<Integer> IDS = ImmutableSet.of(-4, -3, -2, -1, 0, 1, 2, (int) Short.MIN_VALUE);

        @ThriftUnionId
        public short unionId;

        @ThriftField(0)
        public boolean notLegacyId0;
        @ThriftField(1)
        public boolean notLegacyId1;
        @ThriftField(2)
        public boolean notLegacyId2;
        @ThriftField(value = -1, isLegacyId = true)
        public boolean legacyIdOnField;

        @ThriftField(value = -2, isLegacyId = true)
        public boolean getLegacyIdOnGetterOnly()
        {
            return false;
        }

        @ThriftField
        public void setLegacyIdOnGetterOnly(boolean value)
        {
        }

        @ThriftField
        public boolean getLegacyIdOnSetterOnly()
        {
            return false;
        }

        @ThriftField(value = -3, isLegacyId = true)
        public void setLegacyIdOnSetterOnly(boolean value)
        {
        }

        @ThriftField(value = -4, isLegacyId = true)
        public boolean getLegacyIdOnBoth()
        {
            return false;
        }

        @ThriftField(value = -4, isLegacyId = true)
        public void setLegacyIdOnBoth(boolean value)
        {
        }
    }
}
