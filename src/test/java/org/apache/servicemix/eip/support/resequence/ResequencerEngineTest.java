/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.eip.support.resequence;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

import junit.framework.TestCase;

public class ResequencerEngineTest extends TestCase {

    private static final boolean IGNORE_LOAD_TESTS = true;
    
    private ResequencerEngine<Integer> resequencer;
    
    private LinkedBlockingQueue<Integer> queue;
    
    public void tearDown() throws Exception {
        if (resequencer != null) {
            resequencer.stop();
        }
    }

    public void testTimeout1() throws InterruptedException {
        initResequencer(500, 10);
        resequencer.put(4);
        assertNull(queue.poll(250, TimeUnit.MILLISECONDS));
        assertEquals((Integer)4, queue.take());
        assertEquals((Integer)4, resequencer.getLastDelivered());
    }
    
    public void testTimeout2() throws InterruptedException {
        initResequencer(500, 10);
        resequencer.setLastDelivered(2);
        resequencer.put(4);
        assertNull(queue.poll(250, TimeUnit.MILLISECONDS));
        assertEquals((Integer)4, queue.take());
        assertEquals((Integer)4, resequencer.getLastDelivered());
    }
    
    public void testTimeout3() throws InterruptedException {
        initResequencer(500, 10);
        resequencer.setLastDelivered(3);
        resequencer.put(4);
        assertEquals((Integer)4, queue.poll(250, TimeUnit.MILLISECONDS));
        assertEquals((Integer)4, resequencer.getLastDelivered());
    }
    
    public void testTimout4() throws InterruptedException {
        initResequencer(500, 10);
        resequencer.setLastDelivered(2);
        resequencer.put(4);
        resequencer.put(3);
        assertEquals((Integer)3, queue.poll(125, TimeUnit.MILLISECONDS));
        assertEquals((Integer)4, queue.poll(125, TimeUnit.MILLISECONDS));
        assertEquals((Integer)4, resequencer.getLastDelivered());
    }
    
    public void testRandom() throws InterruptedException {
        if (IGNORE_LOAD_TESTS) {
            return;
        }
        int input = 1000;
        initResequencer(1000, 1000);
        List<Integer> list = new LinkedList<Integer>();
        for (int i = 0; i < input; i++) {
            list.add(i);
        }
        Random random = new Random(System.currentTimeMillis());
        System.out.println("Input sequence:");
        long millis = System.currentTimeMillis();
        for (int i = input; i > 0; i--) {
            int r = random.nextInt(i);
            int next = list.remove(r);
            System.out.print(next + " ");
            resequencer.put(next); 
        }
        System.out.println("\nOutput sequence:");
        for (int i = 0; i < input; i++) {
            System.out.print(queue.take() + " ");
        }
        millis = System.currentTimeMillis() - millis;
        System.out.println("\nDuration = " + millis + " ms");
    }
    
    public void testReverse1() throws InterruptedException {
        if (IGNORE_LOAD_TESTS) {
            return;
        }
        testReverse(10);
    }
    
    public void testReverse2() throws InterruptedException {
        if (IGNORE_LOAD_TESTS) {
            return;
        }
        testReverse(100);
    }
    
    private void testReverse(int capacity) throws InterruptedException {
        initResequencer(1, capacity);
        for (int i = 99; i >= 0; i--) {
            resequencer.put(i);
        }
        System.out.println("\nOutput sequence:");
        for (int i = 0; i < 100; i++) {
            System.out.print(queue.take() + " ");
        }
    }
    
    private void initResequencer(long timeout, int capacity) {
        queue = new LinkedBlockingQueue<Integer>();
        resequencer = new ResequencerEngine<Integer>(new IntegerComparator(), capacity);
        resequencer.setOutQueue(queue);
        resequencer.setTimeout(timeout);
    }
    
}
