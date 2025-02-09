import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ForkJoinTask;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

/**
 * @author eric
 * @date 11/10/2024
 */


public class threadTest2 {
    public static void main(String[] args) throws InterruptedException {
        StampedLock stampedLock = new StampedLock();
        long l = stampedLock.writeLock();
        stampedLock.unlock(l);
    }

    @Test
    public void testReaderFlow() throws InterruptedException {

        StampedLock stampedLock = new StampedLock();
        for (int i = 0; i < 127; i++) {
            new Thread(() -> {
                stampedLock.tryReadLock();
                while (true) {

                }
            }).start();
        }
        TimeUnit.SECONDS.sleep(5);
        stampedLock.tryReadLock();
    }

    /**
     * 读锁不按顺序释放会不会有问题
     */
    @Test
    public void testUnlockRead() throws InterruptedException {
        StampedLock stampedLock = new StampedLock();
        long l = stampedLock.tryReadLock();
        new Thread(() -> {
            long l1 = stampedLock.tryReadLock();
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            stampedLock.unlockRead(l1);
        }).start();
        TimeUnit.SECONDS.sleep(1);
        stampedLock.unlockRead(l);

    }

    @Test
    public void testTryConvertToWriteLock() throws InterruptedException {
        StampedLock stampedLock = new StampedLock();
        long l = stampedLock.tryReadLock();
        new Thread(() -> {
            long l1 = stampedLock.tryConvertToWriteLock(l + 1);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            stampedLock.unlock(l1);
            // stampedLock.unlockRead(l1);
        }).start();
        while (true) {

        }
    }

    @Test
    @DisplayName("测试写转读")
    public void testTryConvertToReadLock() throws InterruptedException {
        StampedLock stampedLock = new StampedLock();
        long l = stampedLock.tryWriteLock();
        new Thread(() -> {
            long l1 = stampedLock.tryConvertToReadLock(l);
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            stampedLock.unlock(l1);
            // stampedLock.unlockRead(l1);
        }).start();
        while (true) {
        }
    }

    @Test
    @DisplayName("测试线程池")
    public void testThreadPool() throws InterruptedException {
        int COUNT_BITS = Integer.SIZE - 3;
        int CAPACITY = (1 << COUNT_BITS) - 1;
        int RUNNING    = -1 << COUNT_BITS;
        int SHUTDOWN   =  0 << COUNT_BITS;
        int STOP       =  1 << COUNT_BITS;
        int TIDYING    =  2 << COUNT_BITS;
        int TERMINATED =  3 << COUNT_BITS;

        System.out.println(Integer.toBinaryString(RUNNING));
        System.out.println(Integer.toBinaryString(SHUTDOWN));
        System.out.println(Integer.toBinaryString(STOP));
        System.out.println(Integer.toBinaryString(TIDYING));
        System.out.println(Integer.toBinaryString(TERMINATED));
    }

    @Test
    @DisplayName("测试forkJoin")
    public void testForkJoin() {
        ForkJoinPool forkJoinPool = new ForkJoinPool();
        forkJoinPool.invoke(ForkJoinTask.adapt(() -> {
            System.out.println("hello");
        }));
        while (true) {
            System.out.println(forkJoinPool.getQueuedTaskCount());
        }
    }

    @Test
    @DisplayName("测试forkJoin Scan")
    public void testForkJoinScan() {
        ForkJoinPool forkJoinPool = new ForkJoinPool(1);
        for (int i = 0; i < 10000; i++) {
            forkJoinPool.execute(ForkJoinTask.adapt(() -> {
                System.out.println("hello");
            }));
        }
        new Thread(() -> {
            forkJoinPool.execute(ForkJoinTask.adapt(() -> {
                System.out.println("hello2");
            }));
        }).start();
        while (true) {
            System.out.println(forkJoinPool.getQueuedTaskCount());
        }
    }
}
