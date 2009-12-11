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
package org.jboss.netty.channel;


/**
 * The {@link ReceiveBufferSizePredictor} that always yields the same buffer
 * size prediction.  This predictor ignores the feed back from the I/O thread.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 *
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 */
public class FixedReceiveBufferSizePredictor implements
        ReceiveBufferSizePredictor {

    private final int bufferSize;

    /**
     * Creates a new predictor that always returns the same prediction of
     * the specified buffer size.
     */
    public FixedReceiveBufferSizePredictor(int bufferSize) {
        if (bufferSize <= 0) {
            throw new IllegalArgumentException(
                    "bufferSize must greater than 0: " + bufferSize);
        }
        this.bufferSize = bufferSize;
    }

    public int nextReceiveBufferSize() {
        return bufferSize;
    }

    public void previousReceiveBufferSize(int previousReceiveBufferSize) {
        // Ignore
    }
}
