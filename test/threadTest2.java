import concurrent.TimeUnit;

/**
 * @author eric
 * @date 11/10/2024
 */


public class threadTest2 {
    public static void main(String[] args) throws InterruptedException {
        java.util.concurrent.locks.ReentrantReadWriteLock reentrantReadWriteLock = new java.util.concurrent.locks.ReentrantReadWriteLock();
        java.util.concurrent.locks.ReentrantReadWriteLock.ReadLock readLock = reentrantReadWriteLock.readLock();
        java.util.concurrent.locks.ReentrantReadWriteLock.WriteLock writeLock = reentrantReadWriteLock.writeLock();
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
