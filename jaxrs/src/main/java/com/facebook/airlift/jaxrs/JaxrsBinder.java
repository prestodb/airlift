package com.facebook.airlift.jaxrs;

import com.google.common.collect.ImmutableMap;
import com.google.inject.Binder;
import com.google.inject.Key;
import com.google.inject.TypeLiteral;
import com.google.inject.multibindings.MapBinder;
import com.google.inject.multibindings.Multibinder;

import java.util.Map;

import static com.google.inject.Scopes.SINGLETON;
import static com.google.inject.multibindings.MapBinder.newMapBinder;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static java.util.Objects.requireNonNull;

public class JaxrsBinder
{
    private final Multibinder<Object> resourceBinder;
    private final Binder binder;
    private final MapBinder<Class<?>, Map<String, String>> resourceRolesMappingBinder;

    private JaxrsBinder(Binder binder)
    {
        this.binder = requireNonNull(binder, "binder is null").skipSources(getClass());
        this.resourceBinder = newSetBinder(binder, Object.class, JaxrsResource.class).permitDuplicates();
        this.resourceRolesMappingBinder = newMapBinder(
                binder,
                new TypeLiteral<Class<?>>() {},
                new TypeLiteral<Map<String, String>>() {},
                RoleMapping.class);
    }

    public static JaxrsBinder jaxrsBinder(Binder binder)
    {
        return new JaxrsBinder(binder);
    }

    public ResourceBinding bind(Class<?> implementation)
    {
        binder.bind(implementation).in(SINGLETON);
        resourceBinder.addBinding().to(implementation).in(SINGLETON);
        return new ResourceBinding(implementation, resourceRolesMappingBinder);
    }

    public void bind(TypeLiteral<?> implementation)
    {
        binder.bind(implementation).in(SINGLETON);
        resourceBinder.addBinding().to(implementation).in(SINGLETON);
    }

    public void bind(Key<?> targetKey)
    {
        binder.bind(targetKey).in(SINGLETON);
        resourceBinder.addBinding().to(targetKey).in(SINGLETON);
    }

    public ResourceBinding bindInstance(Object instance)
    {
        resourceBinder.addBinding().toInstance(instance);
        return new ResourceBinding(instance.getClass(), resourceRolesMappingBinder);
    }

    public static class ResourceBinding
    {
        private final Class<?> implementation;
        private final MapBinder<Class<?>, Map<String, String>> resourceRolesMappingBinder;

        public ResourceBinding(
                Class<?> implementation,
                MapBinder<Class<?>, Map<String, String>> resourceRolesMappingBinder)
        {
            this.implementation = implementation;
            this.resourceRolesMappingBinder = resourceRolesMappingBinder;
        }

        public ResourceBinding withRolesMapping(Map<String, String> rolesMapping)
        {
            resourceRolesMappingBinder.addBinding(implementation).toInstance(ImmutableMap.copyOf(rolesMapping));
            return this;
        }
    }
}
