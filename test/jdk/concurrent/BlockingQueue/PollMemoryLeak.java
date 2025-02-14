package jdk.concurrent.BlockingQueue;/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

/*
 * This file is available under and governed by the GNU General Public
 * License version 2 only, as published by the Free Software Foundation.
 * However, the following notice accompanied the original version of this
 * file:
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

/*
 * @test
 * @bug 6236036 6264015
 * @compile PollMemoryLeak.java
 * @run main/othervm -Xmx8m PollMemoryLeak
 * @summary  Checks for OutOfMemoryError when an unbounded
 * number of aborted timed waits occur without a signal.
 */

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;

public class PollMemoryLeak {
    public static void main(String[] args) throws InterruptedException {
        final BlockingQueue[] qs = {
            new LinkedBlockingQueue(10),
            new LinkedTransferQueue(),
            new ArrayBlockingQueue(10),
            new SynchronousQueue(),
            new SynchronousQueue(true),
        };
        final long start = System.currentTimeMillis();
        final long end = start + 10 * 1000;
        while (System.currentTimeMillis() < end)
            for (BlockingQueue q : qs)
                q.poll(1, TimeUnit.NANOSECONDS);
    }
}
