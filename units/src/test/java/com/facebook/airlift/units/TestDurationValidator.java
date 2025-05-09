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
package com.facebook.airlift.units;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.ValidationException;
import jakarta.validation.Validator;
import org.hibernate.validator.HibernateValidator;
import org.testng.annotations.Test;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import static com.facebook.airlift.units.ConstraintValidatorAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TestDurationValidator
{
    private static final Validator VALIDATOR = Validation.byProvider(HibernateValidator.class).configure().buildValidatorFactory().getValidator();

    @Test
    public void testMaxDurationValidator()
    {
        MaxDurationValidator maxValidator = new MaxDurationValidator();
        maxValidator.initialize(new MockMaxDuration(new Duration(5, TimeUnit.SECONDS)));

        assertThat(maxValidator).isValidFor(new Duration(0, TimeUnit.SECONDS));
        assertThat(maxValidator).isValidFor(new Duration(5, TimeUnit.SECONDS));
        assertThat(maxValidator).isInvalidFor(new Duration(6, TimeUnit.SECONDS));
    }

    @Test
    public void testMinDurationValidator()
    {
        MinDurationValidator minValidator = new MinDurationValidator();
        minValidator.initialize(new MockMinDuration(new Duration(5, TimeUnit.SECONDS)));

        assertThat(minValidator).isValidFor(new Duration(5, TimeUnit.SECONDS));
        assertThat(minValidator).isValidFor(new Duration(6, TimeUnit.SECONDS));
        assertThat(minValidator).isInvalidFor(new Duration(0, TimeUnit.SECONDS));
    }

    @Test
    public void testAllowsNullMinAnnotation()
    {
        VALIDATOR.validate(new NullMinAnnotation());
    }

    @Test
    public void testAllowsNullMaxAnnotation()
    {
        VALIDATOR.validate(new NullMaxAnnotation());
    }

    @Test
    public void testDetectsBrokenMinAnnotation()
    {
        assertThatThrownBy(() -> VALIDATOR.validate(new BrokenMinAnnotation()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("java.lang.IllegalArgumentException: duration is not a valid data duration string: broken");

        assertThatThrownBy(() -> VALIDATOR.validate(new MinAnnotationOnOptional()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("HV000030: No validator could be found for constraint 'com.facebook.airlift.units.MinDuration' validating type 'java.util.Optional<com.facebook.airlift.units.Duration>'. Check configuration for 'constrainedByMin'");

        assertThatThrownBy(() -> VALIDATOR.validate(new BrokenOptionalMinAnnotation()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("java.lang.IllegalArgumentException: duration is not a valid data duration string: broken");
    }

    @Test
    public void testDetectsBrokenMaxAnnotation()
    {
        assertThatThrownBy(() -> VALIDATOR.validate(new BrokenMaxAnnotation()))
                .isInstanceOf(ValidationException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessage("java.lang.IllegalArgumentException: duration is not a valid data duration string: broken");

        assertThatThrownBy(() -> VALIDATOR.validate(new MaxAnnotationOnOptional()))
                .isInstanceOf(ValidationException.class)
                .hasMessage("HV000030: No validator could be found for constraint 'com.facebook.airlift.units.MaxDuration' validating type 'java.util.Optional<com.facebook.airlift.units.Duration>'. Check configuration for 'constrainedByMin'");

        assertThatThrownBy(() -> VALIDATOR.validate(new BrokenOptionalMaxAnnotation()))
                .isInstanceOf(ValidationException.class)
                .hasRootCauseInstanceOf(IllegalArgumentException.class)
                .hasMessage("java.lang.IllegalArgumentException: duration is not a valid data duration string: broken");
    }

    @Test
    public void testPassesValidation()
    {
        assertThat(VALIDATOR.validate(new ConstrainedDuration(new Duration(7, TimeUnit.SECONDS)))).isEmpty();

        assertThat(VALIDATOR.validate(new ConstrainedOptionalDuration(Optional.of(new Duration(7, TimeUnit.SECONDS))))).isEmpty();

        assertThat(VALIDATOR.validate(new ConstrainedOptionalDuration(Optional.empty()))).isEmpty();

        assertThat(VALIDATOR.validate(new ConstrainedOptionalDuration(null))).isEmpty();
    }

    @Test
    public void testFailsMaxDurationConstraint()
    {
        Set<? extends ConstraintViolation<?>> violations = VALIDATOR.validate(new ConstrainedDuration(new Duration(11, TimeUnit.SECONDS)));
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(violation -> violation.getConstraintDescriptor().getAnnotation())
                .allMatch(MaxDuration.class::isInstance);
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsOnly("constrainedByMax", "constrainedByMinAndMax");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsOnly("must be less than or equal to 10s", "must be less than or equal to 10000ms");

        violations = VALIDATOR.validate(new ConstrainedOptionalDuration(Optional.of(new Duration(11, TimeUnit.SECONDS))));
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(violation -> violation.getConstraintDescriptor().getAnnotation())
                .allMatch(MaxDuration.class::isInstance);
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsOnly("constrainedByMax", "constrainedByMinAndMax");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsOnly("must be less than or equal to 10s", "must be less than or equal to 10000ms");
    }

    @Test
    public void testFailsMinDurationConstraint()
    {
        Set<? extends ConstraintViolation<?>> violations = VALIDATOR.validate(new ConstrainedDuration(new Duration(1, TimeUnit.SECONDS)));
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(violation -> violation.getConstraintDescriptor().getAnnotation())
                .allMatch(MinDuration.class::isInstance);
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsOnly("constrainedByMin", "constrainedByMinAndMax");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsOnly("must be greater than or equal to 5000ms", "must be greater than or equal to 5s");

        violations = VALIDATOR.validate(new ConstrainedOptionalDuration(Optional.of(new Duration(1, TimeUnit.SECONDS))));
        assertThat(violations).hasSize(2);
        assertThat(violations)
                .extracting(violation -> violation.getConstraintDescriptor().getAnnotation())
                .allMatch(MinDuration.class::isInstance);
        assertThat(violations)
                .extracting(violation -> violation.getPropertyPath().toString())
                .containsOnly("constrainedByMin", "constrainedByMinAndMax");
        assertThat(violations)
                .extracting(ConstraintViolation::getMessage)
                .containsOnly("must be greater than or equal to 5000ms", "must be greater than or equal to 5s");
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class ConstrainedDuration
    {
        private final Duration duration;

        public ConstrainedDuration(Duration duration)
        {
            this.duration = duration;
        }

        @MinDuration("5s")
        public Duration getConstrainedByMin()
        {
            return duration;
        }

        @MaxDuration("10s")
        public Duration getConstrainedByMax()
        {
            return duration;
        }

        @MinDuration("5000ms")
        @MaxDuration("10000ms")
        public Duration getConstrainedByMinAndMax()
        {
            return duration;
        }
    }

    @SuppressWarnings("UnusedDeclaration")
    public static class ConstrainedOptionalDuration
    {
        private final Optional<Duration> duration;

        public ConstrainedOptionalDuration(Optional<Duration> duration)
        {
            this.duration = duration;
        }

        public Optional<@MinDuration("5s") Duration> getConstrainedByMin()
        {
            return duration;
        }

        public Optional<@MaxDuration("10s") Duration> getConstrainedByMax()
        {
            return duration;
        }

        public Optional<@MinDuration("5000ms") @MaxDuration("10000ms") Duration> getConstrainedByMinAndMax()
        {
            return duration;
        }
    }

    public static class NullMinAnnotation
    {
        @SuppressWarnings("UnusedDeclaration")
        @MinDuration("1s")
        public Duration getConstrainedByMin()
        {
            return null;
        }
    }

    public static class NullMaxAnnotation
    {
        @SuppressWarnings("UnusedDeclaration")
        @MaxDuration("1s")
        public Duration getConstrainedByMin()
        {
            return null;
        }
    }

    public static class BrokenMinAnnotation
    {
        @SuppressWarnings("UnusedDeclaration")
        @MinDuration("broken")
        public Duration getConstrainedByMin()
        {
            return new Duration(10, TimeUnit.SECONDS);
        }
    }

    public static class BrokenMaxAnnotation
    {
        @SuppressWarnings("UnusedDeclaration")
        @MinDuration("broken")
        public Duration getConstrainedByMin()
        {
            return new Duration(10, TimeUnit.SECONDS);
        }
    }

    public static class MinAnnotationOnOptional
    {
        @SuppressWarnings("UnusedDeclaration")
        @MinDuration("1s")
        public Optional<Duration> getConstrainedByMin()
        {
            return Optional.empty();
        }
    }

    public static class MaxAnnotationOnOptional
    {
        @SuppressWarnings("UnusedDeclaration")
        @MaxDuration("1s")
        public Optional<Duration> getConstrainedByMin()
        {
            return Optional.empty();
        }
    }

    public static class BrokenOptionalMinAnnotation
    {
        @SuppressWarnings("UnusedDeclaration")
        public Optional<@MinDuration("broken") Duration> getConstrainedByMin()
        {
            return Optional.empty();
        }
    }

    public static class BrokenOptionalMaxAnnotation
    {
        @SuppressWarnings("UnusedDeclaration")
        public Optional<@MaxDuration("broken") Duration> getConstrainedByMax()
        {
            return Optional.empty();
        }
    }
}
