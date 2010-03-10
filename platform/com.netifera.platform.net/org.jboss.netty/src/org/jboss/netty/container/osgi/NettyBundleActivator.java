/*
 * Copyright 2009 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package org.jboss.netty.container.osgi;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalClientChannelFactory;
import org.jboss.netty.channel.local.DefaultLocalServerChannelFactory;
import org.jboss.netty.channel.local.LocalClientChannelFactory;
import org.jboss.netty.channel.local.LocalServerChannelFactory;
import org.jboss.netty.channel.socket.ClientSocketChannelFactory;
import org.jboss.netty.channel.socket.DatagramChannelFactory;
import org.jboss.netty.channel.socket.ServerSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.nio.NioDatagramChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioClientSocketChannelFactory;
import org.jboss.netty.channel.socket.oio.OioDatagramChannelFactory;
import org.jboss.netty.channel.socket.oio.OioServerSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.OsgiLoggerFactory;
import org.jboss.netty.util.internal.ExecutorUtil;
import org.osgi.framework.BundleActivator;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceRegistration;

/**
 * An OSGi {@link BundleActivator} that configures logging and registered
 * all {@link ChannelFactory} implementations as OSGi services.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 */
public class NettyBundleActivator implements BundleActivator {

	private static NettyBundleActivator instance;
	
	public static NettyBundleActivator getInstance() {
		return instance;
	}

    private final List<ServiceRegistration> registrations =
        new ArrayList<ServiceRegistration>();

    private Executor executor;
//    private Timer timer;
    private OsgiLoggerFactory loggerFactory;

    public void start(BundleContext ctx) throws Exception {
		instance = this;
		
        // Switch the internal logger to the OSGi LogService.
        loggerFactory = new OsgiLoggerFactory(ctx);
        InternalLoggerFactory.setDefaultFactory(loggerFactory);

        // Prepare the resources required for creating ChannelFactories.
        Executor executor = this.executor = Executors.newCachedThreadPool();

/*
        // The Timer
        timer = new HashedWheelTimer();
        register(ctx,
                timer,
                Timer.class);
*/
        // The default transport is NIO.
        register(ctx,
                 new NioClientSocketChannelFactory(executor, executor),
                 ClientSocketChannelFactory.class);
        register(ctx,
                 new NioServerSocketChannelFactory(executor, executor),
                 ServerSocketChannelFactory.class);
        // ... except for the datagram transport.
        register(ctx,
                new OioDatagramChannelFactory(executor),
                DatagramChannelFactory.class);

        // Local transports
        register(ctx,
                new DefaultLocalClientChannelFactory(),
                LocalClientChannelFactory.class);
        register(ctx,
                new DefaultLocalServerChannelFactory(),
                LocalServerChannelFactory.class);

        // Miscellaneous transports
        register(ctx, new OioClientSocketChannelFactory(executor));
        register(ctx, new OioServerSocketChannelFactory(executor, executor));
        register(ctx, new NioDatagramChannelFactory(executor));
    }

    public void stop(BundleContext ctx) throws Exception {
        unregisterAll();
        if (executor != null) {
            ExecutorUtil.terminate(executor);
            executor = null;
        }/*
        if (timer != null) {
        	timer.stop();
        	timer = null;
        }*/
        if (loggerFactory != null) {
            InternalLoggerFactory.setDefaultFactory(loggerFactory.getFallback());
            loggerFactory.destroy();
            loggerFactory = null;
        }
    }

    private void register(BundleContext ctx, Object service, Class<?>... serviceTypes) {
        Properties props = new Properties();
        props.setProperty("category", "netty");

        registrations.add(ctx.registerService(service.getClass().getName(), service, props));

        for (Class<?> t: serviceTypes) {
            registrations.add(ctx.registerService(t.getName(), service, props));
        }
    }

    private void unregisterAll() {
        List<ServiceRegistration> registrationsCopy =
            new ArrayList<ServiceRegistration>(registrations);
        registrations.clear();
        for (ServiceRegistration r: registrationsCopy) {
            r.unregister();
        }
    }
}
