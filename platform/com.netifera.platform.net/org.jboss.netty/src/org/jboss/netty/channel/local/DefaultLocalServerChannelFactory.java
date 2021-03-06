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
package org.jboss.netty.channel.local;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelSink;

/**
 * The default {@link LocalServerChannelFactory} implementation.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Andy Taylor (andy.taylor@jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 */
public class DefaultLocalServerChannelFactory implements LocalServerChannelFactory {

    private final ChannelSink sink = new LocalServerChannelSink();

    /**
     * Creates a new instance.
     */
    public DefaultLocalServerChannelFactory() {
        super();
    }

    public LocalServerChannel newChannel(ChannelPipeline pipeline) {
        return new DefaultLocalServerChannel(this, pipeline, sink);
    }

    /**
     * Does nothing because this implementation does not require any external
     * resources.
     */
    public void releaseExternalResources() {
        // Unused
    }
}
