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
package com.facebook.airlift.http.server;

import com.facebook.airlift.configuration.AbstractConfigurationAwareModule;
import com.facebook.airlift.discovery.client.AnnouncementHttpServerInfo;
import com.facebook.airlift.http.server.HttpServer.ClientCertificate;
import com.facebook.airlift.http.server.HttpServerBinder.HttpResourceBinding;
import com.google.common.collect.ImmutableList;
import com.google.inject.Binder;
import com.google.inject.Provides;
import com.google.inject.Scopes;
import org.eclipse.jetty.util.ssl.SslContextFactory;

import javax.servlet.Filter;
import javax.servlet.Servlet;

import java.util.List;
import java.util.Set;

import static com.facebook.airlift.configuration.ConditionalModule.installModuleIf;
import static com.facebook.airlift.configuration.ConfigBinder.configBinder;
import static com.facebook.airlift.event.client.EventBinder.eventBinder;
import static com.google.inject.multibindings.MapBinder.newMapBinder;
import static com.google.inject.multibindings.Multibinder.newSetBinder;
import static com.google.inject.multibindings.OptionalBinder.newOptionalBinder;
import static org.weakref.jmx.guice.ExportBinder.newExporter;

/**
 * Provides a fully configured instance of an HTTP server,
 * ready to use with Guice.
 * <p>
 * Features:
 * <ul>
 * <li>HTTP/HTTPS</li>
 * <li>Basic Auth</li>
 * <li>Request logging</li>
 * <li>JMX</li>
 * </ul>
 * Configuration options are provided via {@link HttpServerConfig}
 * <p>
 * To enable JMX, an {@link javax.management.MBeanServer} must be bound elsewhere
 * <p>
 * To enable Basic Auth, a {@link org.eclipse.jetty.security.LoginService} must be bound elsewhere
 * <p>
 * To enable HTTPS, {@link HttpServerConfig#isHttpsEnabled()} must return true
 * and {@link HttpsConfig#getKeystorePath()}
 * and {@link HttpsConfig#getKeystorePassword()} must return the path to
 * the keystore containing the SSL cert and the password to the keystore, respectively.
 * The HTTPS port is specified via {@link HttpsConfig#getHttpsPort()}.
 */
public class HttpServerModule
        extends AbstractConfigurationAwareModule
{
    public static final String REALM_NAME = "Airlift";

    @Override
    protected void setup(Binder binder)
    {
        binder.disableCircularProxies();

        binder.bind(HttpServer.class).toProvider(HttpServerProvider.class).in(Scopes.SINGLETON);
        newOptionalBinder(binder, ClientCertificate.class).setDefault().toInstance(ClientCertificate.NONE);
        newExporter(binder).export(HttpServer.class).withGeneratedName();
        binder.bind(HttpServerInfo.class).in(Scopes.SINGLETON);
        binder.bind(RequestStats.class).in(Scopes.SINGLETON);
        newMapBinder(binder, String.class, Servlet.class, TheServlet.class);
        newSetBinder(binder, Filter.class, TheServlet.class);
        newSetBinder(binder, Filter.class, TheAdminServlet.class);
        newSetBinder(binder, HttpResourceBinding.class, TheServlet.class);
        newOptionalBinder(binder, SslContextFactory.Server.class);

        newExporter(binder).export(RequestStats.class).withGeneratedName();

        configBinder(binder).bindConfig(HttpServerConfig.class);
        newOptionalBinder(binder, HttpsConfig.class);

        eventBinder(binder).bindEventClient(HttpRequestEvent.class);

        binder.bind(AnnouncementHttpServerInfo.class).to(LocalAnnouncementHttpServerInfo.class).in(Scopes.SINGLETON);
        newSetBinder(binder, Filter.class, TheServlet.class).addBinding()
                .to(AuthenticationFilter.class).in(Scopes.SINGLETON);
        newSetBinder(binder, Authenticator.class);
        install(installModuleIf(HttpServerConfig.class, HttpServerConfig::isHttpsEnabled, moduleBinder ->
                configBinder(moduleBinder).bindConfig(HttpsConfig.class)));
    }

    @Provides
    List<Authenticator> getAuthenticatorList(Set<Authenticator> authenticators)
    {
        return ImmutableList.copyOf(authenticators);
    }
}
