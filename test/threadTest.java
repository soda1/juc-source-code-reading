import concurrent.TimeUnit;

import concurrent.locks.ReentrantReadWriteLock;
import org.junit.Test;

/**
 * @author eric
 * @date 11/10/2024
 */


public class threadTest {

    public static void main(String[] args) throws InterruptedException {
        ReentrantReadWriteLock reentrantReadWriteLock = new ReentrantReadWriteLock();
        ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();
        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(4);
                writeLock.lock();
            } catch (Exception e) {
                e.printStackTrace();
            }finally {
                writeLock.unlock();
            }
        }).start();
        readLock.lock();
        TimeUnit.SECONDS.sleep(5);
        readLock.lock();
        System.out.println("finish");
    }
}
