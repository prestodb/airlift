/*
 * Copyright (C) 2013 Facebook, Inc.
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
package com.facebook.drift.client.guice;

import com.facebook.drift.client.ExceptionClassifier;
import com.facebook.drift.client.MethodInvocationFilter;
import com.facebook.drift.client.address.AddressSelector;
import com.google.inject.Binder;
import com.google.inject.multibindings.Multibinder;

import java.lang.annotation.Annotation;

import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static java.util.Objects.requireNonNull;

public class DriftClientBindingBuilder
{
    private final Binder binder;
    private final Annotation annotation;
    private final String prefix;

    DriftClientBindingBuilder(Binder binder, Annotation annotation, String prefix)
    {
        this.binder = binder.skipSources(getClass());
        this.annotation = requireNonNull(annotation, "annotation is null");
        this.prefix = requireNonNull(prefix, "prefix is null");
        // add MethodInvocationFilter extension binding point
        filterBinder();
        // add ExceptionClassifier extension binding point
        classifierBinder();
    }

    public DriftClientBindingBuilder withMethodInvocationFilter(MethodInvocationFilterBinder filterBinder)
    {
        filterBinder.bind(filterBinder(), binder, annotation, prefix);
        return this;
    }

    public DriftClientBindingBuilder withAddressSelector(AddressSelector<?> addressSelector)
    {
        binder.bind(AddressSelector.class)
                .annotatedWith(annotation)
                .toInstance(addressSelector);
        return this;
    }

    public DriftClientBindingBuilder withAddressSelector(AddressSelectorBinder selectorBinder)
    {
        selectorBinder.bind(binder, annotation, prefix);
        return this;
    }

    public DriftClientBindingBuilder withExceptionClassifier(ExceptionClassifier exceptionClassifier)
    {
        classifierBinder().addBinding().toInstance(exceptionClassifier);
        return this;
    }

    public DriftClientBindingBuilder withExceptionClassifier(ExceptionClassifierBinder classifierBinder)
    {
        classifierBinder.bind(classifierBinder(), annotation, prefix);
        return this;
    }

    private Multibinder<MethodInvocationFilter> filterBinder()
    {
        return newSetBinder(binder, MethodInvocationFilter.class, annotation);
    }

    private Multibinder<ExceptionClassifier> classifierBinder()
    {
        return newSetBinder(binder, ExceptionClassifier.class, annotation);
    }
}
