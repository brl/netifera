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

/**
 * The core channel API which is asynchronous and event-driven abstraction of
 * various transports such as a
 * <a href="http://en.wikipedia.org/wiki/New_I/O#Channels">NIO Channel</a>.
 *
 * @apiviz.landmark
 * @apiviz.exclude ^java
 * @apiviz.exclude ^org\.jboss\.netty\.channel\.[^\.]+\.
 * @apiviz.exclude ^org\.jboss\.netty\.(bootstrap|handler|util)\.
 * @apiviz.exclude \.(Abstract|Default).*$
 * @apiviz.exclude \.(Downstream|Upstream).*Event$
 * @apiviz.exclude \.[A-Za-z]+ChannelFuture$
 * @apiviz.exclude \.ChannelPipelineFactory$
 * @apiviz.exclude \.ChannelHandlerContext$
 * @apiviz.exclude \.ChannelSink$
 * @apiviz.exclude \.ChannelLocal$
 * @apiviz.exclude \.[^\.]+ReceiveBufferSizePredictor$
 */
package org.jboss.netty.channel;
