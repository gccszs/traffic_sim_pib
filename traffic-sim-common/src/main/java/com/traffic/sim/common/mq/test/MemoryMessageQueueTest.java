package com.traffic.sim.common.mq;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class MemoryMessageQueueTest {

    private static int passed = 0;
    private static int failed = 0;

    public static void main(String[] args) throws Exception {
        System.out.println("========== MQ Module Function Test ==========\n");

        testBasicPublishAndSubscribe();
        testDelayedMessage();
        testBroadcast();
        testMultipleConsumers();
        testUnsubscribe();
        testQueueStats();

        System.out.println("\n========== Test Results ==========");
        System.out.println("Passed: " + passed);
        System.out.println("Failed: " + failed);
        System.out.println("Total: " + (passed + failed));

        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void testBasicPublishAndSubscribe() throws Exception {
        System.out.println("Test 1: Basic Publish and Subscribe");
        MemoryMessageQueue queue = new MemoryMessageQueue(1000);
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger received = new AtomicInteger(0);

        queue.subscribe("test-topic", new MessageQueue.MessageConsumer<String>() {
            @Override
            public void consume(Message<String> message) {
                received.incrementAndGet();
                System.out.println("  Received message: " + message.getPayload());
            }
        });

        queue.publish("test-topic", "Hello");
        queue.publish("test-topic", "World");

        boolean success = latch.await(1, TimeUnit.SECONDS);

        if (received.get() == 2) {
            System.out.println("  OK: Received 2 messages");
            passed++;
        } else {
            System.out.println("  FAIL: Expected 2, actual " + received.get());
            failed++;
        }

        queue.shutdown();
    }

    private static void testDelayedMessage() throws Exception {
        System.out.println("\nTest 2: Delayed Message");
        MemoryMessageQueue queue = new MemoryMessageQueue(1000);
        CountDownLatch latch = new CountDownLatch(1);
        long startTime = System.currentTimeMillis();

        queue.subscribe("delayed-topic", new MessageQueue.MessageConsumer<String>() {
            @Override
            public void consume(Message<String> message) {
                long elapsed = System.currentTimeMillis() - startTime;
                System.out.println("  Received delayed message: " + message.getPayload() + ", elapsed: " + elapsed + "ms");
                latch.countDown();
            }
        });

        queue.publishDelayed("delayed-topic", "Delayed", 500);

        boolean success = latch.await(2, TimeUnit.SECONDS);
        long elapsed = System.currentTimeMillis() - startTime;

        if (success && elapsed >= 500 && elapsed < 700) {
            System.out.println("  OK: Delayed message works correctly, delay about " + elapsed + "ms");
            passed++;
        } else {
            System.out.println("  FAIL: Delay time abnormal, elapsed " + elapsed + "ms");
            failed++;
        }

        queue.shutdown();
    }

    private static void testBroadcast() throws Exception {
        System.out.println("\nTest 3: Broadcast Message");
        MemoryMessageQueue queue = new MemoryMessageQueue(1000);
        AtomicInteger count1 = new AtomicInteger(0);
        AtomicInteger count2 = new AtomicInteger(0);

        queue.subscribe("broadcast-topic", new MessageQueue.MessageConsumer<String>() {
            @Override
            public void consume(Message<String> message) {
                count1.incrementAndGet();
            }
        });

        queue.subscribe("broadcast-topic", new MessageQueue.MessageConsumer<String>() {
            @Override
            public void consume(Message<String> message) {
                count2.incrementAndGet();
            }
        });

        Message<String> broadcastMsg = Message.broadcast("broadcast-topic", "Broadcast Test");
        queue.publish(broadcastMsg);

        Thread.sleep(200);

        if (count1.get() == 1 && count2.get() == 1) {
            System.out.println("  OK: Both consumers received broadcast message");
            passed++;
        } else {
            System.out.println("  FAIL: Consumer 1 received " + count1.get() + " messages, Consumer 2 received " + count2.get() + " messages");
            failed++;
        }

        queue.shutdown();
    }

    private static void testMultipleConsumers() throws Exception {
        System.out.println("\nTest 4: Multiple Consumers (Load Balancing)");
        MemoryMessageQueue queue = new MemoryMessageQueue(10000);
        AtomicInteger count1 = new AtomicInteger(0);

        CountDownLatch latch = new CountDownLatch(100);

        queue.subscribe("multi-topic", new MessageQueue.MessageConsumer<Integer>() {
            @Override
            public void consume(Message<Integer> message) {
                count1.incrementAndGet();
                latch.countDown();
            }
        }, 2);

        for (int i = 0; i < 100; i++) {
            queue.publish("multi-topic", i);
        }

        boolean success = latch.await(3, TimeUnit.SECONDS);

        if (success && count1.get() == 100) {
            System.out.println("  OK: 100 messages consumed correctly");
            System.out.println("  Consumer: " + count1.get() + " messages");
            passed++;
        } else {
            System.out.println("  FAIL: Messages lost or timeout");
            failed++;
        }

        queue.shutdown();
    }

    private static void testUnsubscribe() throws Exception {
        System.out.println("\nTest 5: Unsubscribe");
        MemoryMessageQueue queue = new MemoryMessageQueue(1000);
        AtomicInteger count = new AtomicInteger(0);

        MessageQueue.MessageConsumer<String> consumer = new MessageQueue.MessageConsumer<String>() {
            @Override
            public void consume(Message<String> message) {
                count.incrementAndGet();
            }
        };

        queue.subscribe("unsubscribe-topic", consumer);
        queue.publish("unsubscribe-topic", "Before unsubscribe");

        Thread.sleep(200);

        queue.unsubscribe("unsubscribe-topic");
        queue.publish("unsubscribe-topic", "After unsubscribe");

        Thread.sleep(200);

        if (count.get() == 1) {
            System.out.println("  OK: No more messages after unsubscribe");
            passed++;
        } else {
            System.out.println("  FAIL: Received " + count.get() + " messages");
            failed++;
        }

        queue.shutdown();
    }

    private static void testQueueStats() throws Exception {
        System.out.println("\nTest 6: Queue Stats");
        MemoryMessageQueue queue = new MemoryMessageQueue(1000);

        queue.publish("stats-topic", "msg1");
        queue.publish("stats-topic", "msg2");
        queue.publish("stats-topic", "msg3");

        System.out.println("  Topics: " + queue.getTopics());
        System.out.println("  Queue size: " + queue.getQueueSize("stats-topic"));

        queue.clear("stats-topic");
        System.out.println("  Size after clear: " + queue.getQueueSize("stats-topic"));

        queue.publish("stats-topic", "msg4");
        queue.clearAll();
        System.out.println("  Size after clearAll: " + queue.getQueueSize("stats-topic"));

        if (queue.getQueueSize("stats-topic") == 0) {
            System.out.println("  OK: Queue stats and clear functions work correctly");
            passed++;
        } else {
            System.out.println("  FAIL: Queue stats abnormal");
            failed++;
        }

        queue.shutdown();
    }
}