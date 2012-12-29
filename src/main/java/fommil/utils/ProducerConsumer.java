// Copyright Samuel Halliday 2012
package fommil.utils;

import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

/**
 * A very clean `Iterator` realisation of the
 * Producer / Consumer pattern where the producer and
 * consumer can run in separate threads.
 * <p/>
 * Both the `hasNext` and `next` methods of the `Iterator`
 * may block (e.g. when the consumer catches up with the
 * producer).
 * <p/>
 * If the client wishes to cancel iteration early, the
 * `stop` method may be called to free up resources.
 * <p/>
 * This Java backport has a more monolithic API and is
 * not as feature-rich as the Scala original. The producer
 * will continue to fill up the buffer unless the consumer
 * takes – this could lead to OOMs.
 *
 * @author Sam Halliday
 * @see <a href="https://github.com/fommil/scalad/blob/master/src/main/scala/org/cakesolutions/scalad/mongo/ProducerConsumer.scala">ProducerConsumer.scala</a>
 */
public final class ProducerConsumer<T> implements Iterator<T> {

    private final AtomicBoolean stopSignal = new AtomicBoolean();

    private final Queue<T> queue = new ConcurrentLinkedQueue<T>();

    private final AtomicBoolean closed = new AtomicBoolean();

    private final ReentrantLock lock = new ReentrantLock();

    private final Condition change = lock.newCondition();

    /**
     * Instruct the implementation to truncate at its
     * earliest convenience and dispose of resources.
     * Should only be used by the consumer.
     */
    public void stop() {
        stopSignal.set(true);
    }

    /**
     * Make an element available for the consumer.
     * Should only be used by the producer.
     */
    public void produce(T el) {
        queue.add(el);
        lock.lock();
        try {
            change.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Finish producing.
     * Should only be used by the producer.
     */
    public void close() {
        lock.lock();
        try {
            closed.set(true);
            change.signal();
        } finally {
            lock.unlock();
        }
    }

    /**
     * Should only be used by the producer.
     *
     * @return `true` if the consumer instructed the producer to stop.
     */
    public boolean stopped() {
        return stopSignal.get();
    }


    @Override
    public boolean hasNext() {
        if (!queue.isEmpty()) return true;
        else if (closed.get()) return !queue.isEmpty(); // non-locking optimisation
        lock.lock();
        try {
            if (closed.get()) return !queue.isEmpty();
            try {
                change.await();
            } catch (InterruptedException ignored) {
            }
        } finally {
            lock.unlock();
        }
        return hasNext(); // recursive, but should never go too deep
    }

    @Override
    public T next() {
        return queue.poll();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException("remove not supported");
    }
}
