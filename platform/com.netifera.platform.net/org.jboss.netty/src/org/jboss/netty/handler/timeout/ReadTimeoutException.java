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
package org.jboss.netty.handler.timeout;

/**
 * A {@link TimeoutException} raised by {@link ReadTimeoutHandler} when no data
 * was read within a certain period of time.
 *
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 */
public class ReadTimeoutException extends TimeoutException {

    private static final long serialVersionUID = -4596059237992273913L;

    /**
     * Creates a new instance.
     */
    public ReadTimeoutException() {
        super();
    }

    /**
     * Creates a new instance.
     */
    public ReadTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Creates a new instance.
     */
    public ReadTimeoutException(String message) {
        super(message);
    }

    /**
     * Creates a new instance.
     */
    public ReadTimeoutException(Throwable cause) {
        super(cause);
    }
}
