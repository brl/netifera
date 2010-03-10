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
package org.jboss.netty.handler.stream;

import org.jboss.netty.buffer.ChannelBuffer;

/**
 * A large data stream which is consumed by {@link ChunkedWriteHandler}.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 *
 * @apiviz.landmark
 */
public interface ChunkedInput {

    /**
     * Returns {@code true} if and only if there is any data left in the
     * stream.
     */
    boolean hasNextChunk() throws Exception;

    /**
     * Fetches a chunked data from the stream.  The returned chunk is usually
     * a {@link ChannelBuffer}, but you could extend an existing implementation
     * to convert the {@link ChannelBuffer} into a different type that your
     * handler or encoder understands.
     *
     * @return the fetched chunk, which is usually {@link ChannelBuffer}.
     *         {@code null} if there is no data left in the stream.
     */
    Object nextChunk() throws Exception;

    /**
     * Releases the resources associated with the stream.
     */
    void close() throws Exception;
}
