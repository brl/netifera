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
package org.jboss.netty.util.internal;

import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * @author The Netty Project (netty-dev@lists.jboss.org)
 * @author Trustin Lee (trustin@gmail.com)
 * @version $Rev: 1783 $, $Date: 2009-10-14 14:46:40 +0900 (수, 14 10 2009) $
 */
public class CombinedIterator<E> implements Iterator<E> {

    private final Iterator<E> i1;
    private final Iterator<E> i2;
    private Iterator<E> currentIterator;

    public CombinedIterator(Iterator<E> i1, Iterator<E> i2) {
        if (i1 == null) {
            throw new NullPointerException("i1");
        }
        if (i2 == null) {
            throw new NullPointerException("i2");
        }
        this.i1 = i1;
        this.i2 = i2;
        currentIterator = i1;
    }

    public boolean hasNext() {
        boolean hasNext = currentIterator.hasNext();
        if (hasNext) {
            return true;
        }

        if (currentIterator == i1) {
            currentIterator = i2;
            return hasNext();
        } else {
            return false;
        }
    }

    public E next() {
        try {
            E e = currentIterator.next();
            return e;
        } catch (NoSuchElementException e) {
            if (currentIterator == i1) {
                currentIterator = i2;
                return next();
            } else {
                throw e;
            }
        }
    }

    public void remove() {
        currentIterator.remove();
    }

}
