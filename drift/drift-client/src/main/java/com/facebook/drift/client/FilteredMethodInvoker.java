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
package com.facebook.drift.client;

import com.facebook.drift.transport.client.InvokeRequest;
import com.facebook.drift.transport.client.MethodInvoker;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.ListenableFuture;
import io.airlift.units.Duration;

import java.util.List;

import static java.util.Objects.requireNonNull;

class FilteredMethodInvoker
        implements MethodInvoker
{
    public static MethodInvoker createFilteredMethodInvoker(List<MethodInvocationFilter> filters, MethodInvoker methodInvoker)
    {
        for (MethodInvocationFilter filter : Lists.reverse(filters)) {
            methodInvoker = new FilteredMethodInvoker(filter, methodInvoker);
        }
        return methodInvoker;
    }

    private final MethodInvocationFilter filter;
    private final MethodInvoker next;

    private FilteredMethodInvoker(MethodInvocationFilter filter, MethodInvoker next)
    {
        this.filter = requireNonNull(filter, "filter is null");
        this.next = requireNonNull(next, "next is null");
    }

    @Override
    public ListenableFuture<Object> invoke(InvokeRequest request)
    {
        return filter.invoke(request, next);
    }

    @Override
    public ListenableFuture<?> delay(Duration duration)
    {
        return next.delay(duration);
    }
}
