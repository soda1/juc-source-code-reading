import concurrent.TimeUnit;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

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
}
