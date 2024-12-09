/*
 * ORACLE PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */

/*
 *
 *
 *
 *
 *
 * Written by Doug Lea with assistance from members of JCP JSR-166
 * Expert Group and released to the public domain, as explained at
 * http://creativecommons.org/publicdomain/zero/1.0/
 */

package concurrent.locks;

import concurrent.TimeUnit;

/**
 * A capability-based lock with three modes for controlling read/write
 * access.  The state of a StampedLock consists of a version and mode.
 * Lock acquisition methods return a stamp that represents and
 * controls access with respect to a lock state; "try" versions of
 * these methods may instead return the special value zero to
 * represent failure to acquire access. Lock release and conversion
 * methods require stamps as arguments, and fail if they do not match
 * the state of the lock. The three modes are:
 *
 * <ul>
 *
 *  <li><b>Writing.</b> Method {@link #writeLock} possibly blocks
 *   waiting for exclusive access, returning a stamp that can be used
 *   in method {@link #unlockWrite} to release the lock. Untimed and
 *   timed versions of {@code tryWriteLock} are also provided. When
 *   the lock is held in write mode, no read locks may be obtained,
 *   and all optimistic read validations will fail.  </li>
 *
 *  <li><b>Reading.</b> Method {@link #readLock} possibly blocks
 *   waiting for non-exclusive access, returning a stamp that can be
 *   used in method {@link #unlockRead} to release the lock. Untimed
 *   and timed versions of {@code tryReadLock} are also provided. </li>
 *
 *  <li><b>Optimistic Reading.</b> Method {@link #tryOptimisticRead}
 *   returns a non-zero stamp only if the lock is not currently held
 *   in write mode. Method {@link #validate} returns true if the lock
 *   has not been acquired in write mode since obtaining a given
 *   stamp.  This mode can be thought of as an extremely weak version
 *   of a read-lock, that can be broken by a writer at any time.  The
 *   use of optimistic mode for short read-only code segments often
 *   reduces contention and improves throughput.  However, its use is
 *   inherently fragile.  Optimistic read sections should only read
 *   fields and hold them in local variables for later use after
 *   validation. Fields read while in optimistic mode may be wildly
 *   inconsistent, so usage applies only when you are familiar enough
 *   with data representations to check consistency and/or repeatedly
 *   invoke method {@code validate()}.  For example, such steps are
 *   typically required when first reading an object or array
 *   reference, and then accessing one of its fields, elements or
 *   methods. </li>
 *
 * </ul>
 *
 * <p>This class also supports methods that conditionally provide
 * conversions across the three modes. For example, method {@link
 * #tryConvertToWriteLock} attempts to "upgrade" a mode, returning
 * a valid write stamp if (1) already in writing mode (2) in reading
 * mode and there are no other readers or (3) in optimistic mode and
 * the lock is available. The forms of these methods are designed to
 * help reduce some of the code bloat that otherwise occurs in
 * retry-based designs.
 *
 * <p>StampedLocks are designed for use as internal utilities in the
 * development of thread-safe components. Their use relies on
 * knowledge of the internal properties of the data, objects, and
 * methods they are protecting.  They are not reentrant, so locked
 * bodies should not call other unknown methods that may try to
 * re-acquire locks (although you may pass a stamp to other methods
 * that can use or convert it).  The use of read lock modes relies on
 * the associated code sections being side-effect-free.  Unvalidated
 * optimistic read sections cannot call methods that are not known to
 * tolerate potential inconsistencies.  Stamps use finite
 * representations, and are not cryptographically secure (i.e., a
 * valid stamp may be guessable). Stamp values may recycle after (no
 * sooner than) one year of continuous operation. A stamp held without
 * use or validation for longer than this period may fail to validate
 * correctly.  StampedLocks are serializable, but always deserialize
 * into initial unlocked state, so they are not useful for remote
 * locking.
 *
 * <p>The scheduling policy of StampedLock does not consistently
 * prefer readers over writers or vice versa.  All "try" methods are
 * best-effort and do not necessarily conform to any scheduling or
 * fairness policy. A zero return from any "try" method for acquiring
 * or converting locks does not carry any information about the state
 * of the lock; a subsequent invocation may succeed.
 *
 * <p>Because it supports coordinated usage across multiple lock
 * modes, this class does not directly implement the {@link Lock} or
 * {@link ReadWriteLock} interfaces. However, a StampedLock may be
 * viewed {@link #asReadLock()}, {@link #asWriteLock()}, or {@link
 * #asReadWriteLock()} in applications requiring only the associated
 * set of functionality.
 *
 * <p><b>Sample Usage.</b> The following illustrates some usage idioms
 * in a class that maintains simple two-dimensional points. The sample
 * code illustrates some try/catch conventions even though they are
 * not strictly needed here because no exceptions can occur in their
 * bodies.<br>
 *
 *  <pre>{@code
 * class Point {
 *   private double x, y;
 *   private final StampedLock sl = new StampedLock();
 *
 *   void move(double deltaX, double deltaY) { // an exclusively locked method
 *     long stamp = sl.writeLock();
 *     try {
 *       x += deltaX;
 *       y += deltaY;
 *     } finally {
 *       sl.unlockWrite(stamp);
 *     }
 *   }
 *
 *   double distanceFromOrigin() { // A read-only method
 *     long stamp = sl.tryOptimisticRead();
 *     double currentX = x, currentY = y;
 *     if (!sl.validate(stamp)) {
 *        stamp = sl.readLock();
 *        try {
 *          currentX = x;
 *          currentY = y;
 *        } finally {
 *           sl.unlockRead(stamp);
 *        }
 *     }
 *     return Math.sqrt(currentX * currentX + currentY * currentY);
 *   }
 *
 *   void moveIfAtOrigin(double newX, double newY) { // upgrade
 *     // Could instead start with optimistic, not read mode
 *     long stamp = sl.readLock();
 *     try {
 *       while (x == 0.0 && y == 0.0) {
 *         long ws = sl.tryConvertToWriteLock(stamp);
 *         if (ws != 0L) {
 *           stamp = ws;
 *           x = newX;
 *           y = newY;
 *           break;
 *         }
 *         else {
 *           sl.unlockRead(stamp);
 *           stamp = sl.writeLock();
 *         }
 *       }
 *     } finally {
 *       sl.unlock(stamp);
 *     }
 *   }
 * }}</pre>
 *
 * @since 1.8
 * @author Doug Lea
 */
public class StampedLock implements java.io.Serializable {
    /*
     * Algorithmic notes:
     *
     * The design employs elements of Sequence locks
     * (as used in linux kernels; see Lameter's
     * http://www.lameter.com/gelato2005.pdf
     * and elsewhere; see
     * Boehm's http://www.hpl.hp.com/techreports/2012/HPL-2012-68.html)
     * and Ordered RW locks (see Shirako et al
     * http://dl.acm.org/citation.cfm?id=2312015)
     *
     * Conceptually, the primary state of the lock includes a sequence
     * number that is odd when write-locked and even otherwise.
     * However, this is offset by a reader count that is non-zero when
     * read-locked.  The read count is ignored when validating
     * "optimistic" seqlock-reader-style stamps.  Because we must use
     * a small finite number of bits (currently 7) for readers, a
     * supplementary reader overflow word is used when the number of
     * readers exceeds the count field. We do this by treating the max
     * reader count value (RBITS) as a spinlock protecting overflow
     * updates.
     *
     * Waiters use a modified form of CLH lock used in
     * AbstractQueuedSynchronizer (see its internal documentation for
     * a fuller account), where each node is tagged (field mode) as
     * either a reader or writer. Sets of waiting readers are grouped
     * (linked) under a common node (field cowait) so act as a single
     * node with respect to most CLH mechanics.  By virtue of the
     * queue structure, wait nodes need not actually carry sequence
     * numbers; we know each is greater than its predecessor.  This
     * simplifies the scheduling policy to a mainly-FIFO scheme that
     * incorporates elements of Phase-Fair locks (see Brandenburg &
     * Anderson, especially http://www.cs.unc.edu/~bbb/diss/).  In
     * particular, we use the phase-fair anti-barging rule: If an
     * incoming reader arrives while read lock is held but there is a
     * queued writer, this incoming reader is queued.  (This rule is
     * responsible for some of the complexity of method acquireRead,
     * but without it, the lock becomes highly unfair.) Method release
     * does not (and sometimes cannot) itself wake up cowaiters. This
     * is done by the primary thread, but helped by any other threads
     * with nothing better to do in methods acquireRead and
     * acquireWrite.
     *
     * These rules apply to threads actually queued. All tryLock forms
     * opportunistically try to acquire locks regardless of preference
     * rules, and so may "barge" their way in.  Randomized spinning is
     * used in the acquire methods to reduce (increasingly expensive)
     * context switching while also avoiding sustained memory
     * thrashing among many threads.  We limit spins to the head of
     * queue. A thread spin-waits up to SPINS times (where each
     * iteration decreases spin count with 50% probability) before
     * blocking. If, upon wakening it fails to obtain lock, and is
     * still (or becomes) the first waiting thread (which indicates
     * that some other thread barged and obtained lock), it escalates
     * spins (up to MAX_HEAD_SPINS) to reduce the likelihood of
     * continually losing to barging threads.
     *
     * Nearly all of these mechanics are carried out in methods
     * acquireWrite and acquireRead, that, as typical of such code,
     * sprawl out because actions and retries rely on consistent sets
     * of locally cached reads.
     *
     * As noted in Boehm's paper (above), sequence validation (mainly
     * method validate()) requires stricter ordering rules than apply
     * to normal volatile reads (of "state").  To force orderings of
     * reads before a validation and the validation itself in those
     * cases where this is not already forced, we use
     * Unsafe.loadFence.
     *
     * The memory layout keeps lock state and queue pointers together
     * (normally on the same cache line). This usually works well for
     * read-mostly loads. In most other cases, the natural tendency of
     * adaptive-spin CLH locks to reduce memory contention lessens
     * motivation to further spread out contended locations, but might
     * be subject to future improvements.
     */

    private static final long serialVersionUID = -6001602636862214147L;

    /** Number of processors, for spin control */
    private static final int NCPU = Runtime.getRuntime().availableProcessors();

    /** Maximum number of retries before enqueuing on acquisition */
    // 自旋次数 64
    private static final int SPINS = (NCPU > 1) ? 1 << 6 : 0;

    /** Maximum number of retries before blocking at head on acquisition */
    // 头部自旋次数 1024
    private static final int HEAD_SPINS = (NCPU > 1) ? 1 << 10 : 0;

    /** Maximum number of retries before re-blocking */
    // 头部最大重试次数65536
    private static final int MAX_HEAD_SPINS = (NCPU > 1) ? 1 << 16 : 0;

    /** The period for yielding when waiting for overflow spinlock */
    private static final int OVERFLOW_YIELD_RATE = 7; // must be power 2 - 1

    /** The number of bits to use for reader count before overflowing */
    // 最大读127
    private static final int LG_READERS = 7;

    // Values for lock state and stamp operations
    // 读锁单位值
    private static final long RUNIT = 1L;
    // 写mask位
    private static final long WBIT  = 1L << LG_READERS;
    // 读的bit位十进制值
    private static final long RBITS = WBIT - 1L;
    // 读最大值 126
    private static final long RFULL = RBITS - 1L;
    // 读写所有bit位值
    private static final long ABITS = RBITS | WBIT;
    private static final long SBITS = ~RBITS; // note overlap with ABITS

    // Initial value for lock state; avoid failure value zero
    // state 0 表示失败
    private static final long ORIGIN = WBIT << 1;

    // Special value from cancelled acquire methods so caller can throw IE
    private static final long INTERRUPTED = 1L;

    // Values for node status; order matters
    private static final int WAITING   = -1;
    private static final int CANCELLED =  1;

    // Modes for nodes (int not boolean to allow arithmetic)
    private static final int RMODE = 0;
    private static final int WMODE = 1;

    /** Wait nodes */
    static final class WNode {
        volatile WNode prev;
        volatile WNode next;
        volatile WNode cowait;    // list of linked readers
        volatile Thread thread;   // non-null while possibly parked
        volatile int status;      // 0, WAITING, or CANCELLED
        final int mode;           // RMODE or WMODE
        WNode(int m, WNode p) { mode = m; prev = p; }
    }

    /** Head of CLH queue */
    private transient volatile WNode whead;
    /** Tail (last) of CLH queue */
    private transient volatile WNode wtail;

    // views
    transient ReadLockView readLockView;
    transient WriteLockView writeLockView;
    transient ReadWriteLockView readWriteLockView;

    /** Lock sequence/state */
    private transient volatile long state;
    /** extra reader count when state read count saturated */
    private transient int readerOverflow;

    /**
     * Creates a new lock, initially in unlocked state.
     */
    public StampedLock() {
        state = ORIGIN;
    }

    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a stamp that can be used to unlock or convert mode
     */
    public long writeLock() {
        long s, next;  // bypass acquireWrite in fully unlocked case only
        // 存在写锁和读锁返回0， 如果cas写锁成功，返回新的state值，否则自旋获取写锁
        return ((((s = state) & ABITS) == 0L &&
                 U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
                next : acquireWrite(false, 0L));
    }

    /**
     * Exclusively acquires the lock if it is immediately available.
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    public long tryWriteLock() {
        long s, next;
        return ((((s = state) & ABITS) == 0L &&
                 U.compareAndSwapLong(this, STATE, s, next = s + WBIT)) ?
                next : 0L);
    }

    /**
     * Exclusively acquires the lock if it is available within the
     * given time and the current thread has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long tryWriteLock(long time, TimeUnit unit)
        throws InterruptedException {
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            long next, deadline;
            if ((next = tryWriteLock()) != 0L)
                return next;
            if (nanos <= 0L)
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            if ((next = acquireWrite(true, deadline)) != INTERRUPTED)
                return next;
        }
        throw new InterruptedException();
    }

    /**
     * Exclusively acquires the lock, blocking if necessary
     * until available or the current thread is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link Lock#lockInterruptibly()}.
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long writeLockInterruptibly() throws InterruptedException {
        long next;
        if (!Thread.interrupted() &&
            (next = acquireWrite(true, 0L)) != INTERRUPTED)
            return next;
        throw new InterruptedException();
    }

    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available.
     *
     * @return a stamp that can be used to unlock or convert mode
     */
    public long readLock() {
        long s = state, next;  // bypass acquireRead on common uncontended case
        // 没有队列且读锁未饱和且cas成功，返回新的state值，否则自旋获取读锁
        return ((whead == wtail && (s & ABITS) < RFULL &&
                 U.compareAndSwapLong(this, STATE, s, next = s + RUNIT)) ?
                next : acquireRead(false, 0L));
    }

    /**
     * Non-exclusively acquires the lock if it is immediately available.
     *
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     */
    public long tryReadLock() {
        for (;;) {
            long s, m, next;
            if ((m = (s = state) & ABITS) == WBIT)
                return 0L;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                    return next;
            }
            else if ((next = tryIncReaderOverflow(s)) != 0L)
                return next;
        }
    }

    /**
     * Non-exclusively acquires the lock if it is available within the
     * given time and the current thread has not been interrupted.
     * Behavior under timeout and interruption matches that specified
     * for method {@link Lock#tryLock(long, TimeUnit)}.
     *
     * @param time the maximum time to wait for the lock
     * @param unit the time unit of the {@code time} argument
     * @return a stamp that can be used to unlock or convert mode,
     * or zero if the lock is not available
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long tryReadLock(long time, TimeUnit unit)
        throws InterruptedException {
        long s, m, next, deadline;
        long nanos = unit.toNanos(time);
        if (!Thread.interrupted()) {
            if ((m = (s = state) & ABITS) != WBIT) {
                if (m < RFULL) {
                    if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                        return next;
                }
                else if ((next = tryIncReaderOverflow(s)) != 0L)
                    return next;
            }
            if (nanos <= 0L)
                return 0L;
            if ((deadline = System.nanoTime() + nanos) == 0L)
                deadline = 1L;
            if ((next = acquireRead(true, deadline)) != INTERRUPTED)
                return next;
        }
        throw new InterruptedException();
    }

    /**
     * Non-exclusively acquires the lock, blocking if necessary
     * until available or the current thread is interrupted.
     * Behavior under interruption matches that specified
     * for method {@link Lock#lockInterruptibly()}.
     *
     * @return a stamp that can be used to unlock or convert mode
     * @throws InterruptedException if the current thread is interrupted
     * before acquiring the lock
     */
    public long readLockInterruptibly() throws InterruptedException {
        long next;
        if (!Thread.interrupted() &&
            (next = acquireRead(true, 0L)) != INTERRUPTED)
            return next;
        throw new InterruptedException();
    }

    /**
     * Returns a stamp that can later be validated, or zero
     * if exclusively locked.
     *
     * @return a stamp, or zero if exclusively locked
     */
    public long tryOptimisticRead() {
        long s;
        // 没有写锁，则取读锁位置0后的state值
        return (((s = state) & WBIT) == 0L) ? (s & SBITS) : 0L;
    }

    /**
     * Returns true if the lock has not been exclusively acquired
     * since issuance of the given stamp. Always returns false if the
     * stamp is zero. Always returns true if the stamp represents a
     * currently held lock. Invoking this method with a value not
     * obtained from {@link #tryOptimisticRead} or a locking method
     * for this lock has no defined effect or result.
     *
     * @param stamp a stamp
     * @return {@code true} if the lock has not been exclusively acquired
     * since issuance of the given stamp; else false
     */
    public boolean validate(long stamp) {
        U.loadFence();
        return (stamp & SBITS) == (state & SBITS);
    }

    /**
     * If the lock state matches the given stamp, releases the
     * exclusive lock.
     *
     * @param stamp a stamp returned by a write-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlockWrite(long stamp) {
        WNode h;
        if (state != stamp || (stamp & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        // 释放锁会加版本，state = stamp + WBIT，避免ABA问题，溢出了会使用origin，避免使用0
        state = (stamp += WBIT) == 0L ? ORIGIN : stamp;
        if ((h = whead) != null && h.status != 0)
            release(h);
    }

    /**
     * If the lock state matches the given stamp, releases the
     * non-exclusive lock.
     *
     * @param stamp a stamp returned by a read-lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlockRead(long stamp) {
        long s, m; WNode h;
        for (;;) {
            // stamp/state与SBITS相与是为了看读锁位之外的bit位是不是一致的，不一致则表示无效 或stamp中读锁位=0 或 （state中读锁位为0或有写锁）
            if (((s = state) & SBITS) != (stamp & SBITS) ||
                (stamp & ABITS) == 0L || (m = s & ABITS) == 0L || m == WBIT)
                throw new IllegalMonitorStateException();
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    break;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                break;
        }
    }

    /**
     * If the lock state matches the given stamp, releases the
     * corresponding mode of the lock.
     *
     * @param stamp a stamp returned by a lock operation
     * @throws IllegalMonitorStateException if the stamp does
     * not match the current state of this lock
     */
    public void unlock(long stamp) {
        long a = stamp & ABITS, m, s; WNode h;
        // 读锁位以外的bit位一致
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            // 读锁位为0，break
            if ((m = s & ABITS) == 0L)
                break;
            else if (m == WBIT) {
                // m为写锁，如果a不是，break
                if (a != m)
                    break;
                state = (s += WBIT) == 0L ? ORIGIN : s;
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return;
            }
            else if (a == 0L || a >= WBIT) //stamp invalid
                break;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return;
        }
        throw new IllegalMonitorStateException();
    }

    /**
     * If the lock state matches the given stamp, performs one of
     * the following actions. If the stamp represents holding a write
     * lock, returns it.  Or, if a read lock, if the write lock is
     * available, releases the read lock and returns a write stamp.
     * Or, if an optimistic read, returns a write stamp only if
     * immediately available. This method returns zero in all other
     * cases.
     *
     * @param stamp a stamp
     * @return a valid write stamp, or zero on failure
     */
    public long tryConvertToWriteLock(long stamp) {
        long a = stamp & ABITS, m, s, next;
        // stamp/state与SBITS相与是为了看读锁位之外的bit位是不是一致的，不一致则表示无效
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            // 读写锁位0
            if ((m = s & ABITS) == 0L) {
                // 如果a不是0，break
                if (a != 0L)
                    break;
                // cas获取写锁
                if (U.compareAndSwapLong(this, STATE, s, next = s + WBIT))
                    return next;
            }
            else if (m == WBIT) {
                // m为写锁，如果a不是，break，表明有其他线程持有写锁
                if (a != m)
                    break;
                // 否则就是当前线程持有，直接返回
                return stamp;
            }
            else if (m == RUNIT && a != 0L) { //只有一个读锁，cas转成写锁，如果读锁不是当前线程持有的话，会存在问题的
                if (U.compareAndSwapLong(this, STATE, s,
                                         next = s - RUNIT + WBIT))
                    return next;
            }
            else
                break;
        }
        return 0L;
    }

    /**
     * If the lock state matches the given stamp, performs one of
     * the following actions. If the stamp represents holding a write
     * lock, releases it and obtains a read lock.  Or, if a read lock,
     * returns it. Or, if an optimistic read, acquires a read lock and
     * returns a read stamp only if immediately available. This method
     * returns zero in all other cases.
     *
     * @param stamp a stamp
     * @return a valid read stamp, or zero on failure
     */
    public long tryConvertToReadLock(long stamp) {
        long a = stamp & ABITS, m, s, next; WNode h;
        // stamp/state与SBITS相与是为了看读锁位之外的bit位是不是一致的，不一致则表示无效
        while (((s = state) & SBITS) == (stamp & SBITS)) {
            // 读写锁位0
            if ((m = s & ABITS) == 0L) {
                // 如果a不是0，invalid，break
                if (a != 0L)
                    break;
                else if (m < RFULL) { //cas获取读锁
                    if (U.compareAndSwapLong(this, STATE, s, next = s + RUNIT))
                        return next;
                }
                else if ((next = tryIncReaderOverflow(s)) != 0L)
                    return next;
            }
            else if (m == WBIT) {
                // m为写锁，如果a不是，break，表明有其他线程持有写锁
                if (a != m)
                    break;
                //增加写锁版本及读锁+1
                state = next = s + (WBIT + RUNIT);
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return next;
            }
            else if (a != 0L && a < WBIT)
                return stamp;
            else
                break;
        }
        return 0L;
    }

    /**
     * If the lock state matches the given stamp then, if the stamp
     * represents holding a lock, releases it and returns an
     * observation stamp.  Or, if an optimistic read, returns it if
     * validated. This method returns zero in all other cases, and so
     * may be useful as a form of "tryUnlock".
     *
     * @param stamp a stamp
     * @return a valid optimistic read stamp, or zero on failure
     */
    public long tryConvertToOptimisticRead(long stamp) {
        long a = stamp & ABITS, m, s, next; WNode h;
        // 限制指令重排啥的
        U.loadFence();
        for (;;) {
            // stamp/state与SBITS相与是为了看读锁位之外的bit位是不是一致的，不一致则表示无效
            if (((s = state) & SBITS) != (stamp & SBITS))
                break;
            // 读写锁位0
            if ((m = s & ABITS) == 0L) {
                // 如果a不是0，break
                if (a != 0L)
                    break;
                return s;
            }
            else if (m == WBIT) {
                if (a != m)
                    break;
                state = next = (s += WBIT) == 0L ? ORIGIN : s;
                if ((h = whead) != null && h.status != 0)
                    release(h);
                return next;
            }
            else if (a == 0L || a >= WBIT)
                break;
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, next = s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return next & SBITS;
                }
            }
            else if ((next = tryDecReaderOverflow(s)) != 0L)
                return next & SBITS;
        }
        return 0L;
    }

    /**
     * Releases the write lock if it is held, without requiring a
     * stamp value. This method may be useful for recovery after
     * errors.
     *
     * @return {@code true} if the lock was held, else false
     */
    public boolean tryUnlockWrite() {
        long s; WNode h;
        if (((s = state) & WBIT) != 0L) {
            // 释放锁会加版本，state = stamp + WBIT，避免ABA问题，溢出了会使用origin，避免使用0
            state = (s += WBIT) == 0L ? ORIGIN : s;
            if ((h = whead) != null && h.status != 0)
                release(h);
            return true;
        }
        return false;
    }

    /**
     * Releases one hold of the read lock if it is held, without
     * requiring a stamp value. This method may be useful for recovery
     * after errors.
     *
     * @return {@code true} if the read lock was held, else false
     */
    public boolean tryUnlockRead() {
        long s, m; WNode h;
        while ((m = (s = state) & ABITS) != 0L && m < WBIT) {
            if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    return true;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                return true;
        }
        return false;
    }

    // status monitoring methods

    /**
     * Returns combined state-held and overflow read count for given
     * state s.
     */
    private int getReadLockCount(long s) {
        long readers;
        if ((readers = s & RBITS) >= RFULL)
            readers = RFULL + readerOverflow;
        return (int) readers;
    }

    /**
     * Returns {@code true} if the lock is currently held exclusively.
     *
     * @return {@code true} if the lock is currently held exclusively
     */
    public boolean isWriteLocked() {
        return (state & WBIT) != 0L;
    }

    /**
     * Returns {@code true} if the lock is currently held non-exclusively.
     *
     * @return {@code true} if the lock is currently held non-exclusively
     */
    public boolean isReadLocked() {
        return (state & RBITS) != 0L;
    }

    /**
     * Queries the number of read locks held for this lock. This
     * method is designed for use in monitoring system state, not for
     * synchronization control.
     * @return the number of read locks held
     */
    public int getReadLockCount() {
        return getReadLockCount(state);
    }

    /**
     * Returns a string identifying this lock, as well as its lock
     * state.  The state, in brackets, includes the String {@code
     * "Unlocked"} or the String {@code "Write-locked"} or the String
     * {@code "Read-locks:"} followed by the current number of
     * read-locks held.
     *
     * @return a string identifying this lock, as well as its lock state
     */
    public String toString() {
        long s = state;
        return super.toString() +
            ((s & ABITS) == 0L ? "[Unlocked]" :
             (s & WBIT) != 0L ? "[Write-locked]" :
             "[Read-locks:" + getReadLockCount(s) + "]");
    }

    // views

    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #readLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link
     * Lock#newCondition()} throws {@code
     * UnsupportedOperationException}.
     *
     * @return the lock
     */
    public Lock asReadLock() {
        ReadLockView v;
        return ((v = readLockView) != null ? v :
                (readLockView = new ReadLockView()));
    }

    /**
     * Returns a plain {@link Lock} view of this StampedLock in which
     * the {@link Lock#lock} method is mapped to {@link #writeLock},
     * and similarly for other methods. The returned Lock does not
     * support a {@link Condition}; method {@link
     * Lock#newCondition()} throws {@code
     * UnsupportedOperationException}.
     *
     * @return the lock
     */
    public Lock asWriteLock() {
        WriteLockView v;
        return ((v = writeLockView) != null ? v :
                (writeLockView = new WriteLockView()));
    }

    /**
     * Returns a {@link ReadWriteLock} view of this StampedLock in
     * which the {@link ReadWriteLock#readLock()} method is mapped to
     * {@link #asReadLock()}, and {@link ReadWriteLock#writeLock()} to
     * {@link #asWriteLock()}.
     *
     * @return the lock
     */
    public ReadWriteLock asReadWriteLock() {
        ReadWriteLockView v;
        return ((v = readWriteLockView) != null ? v :
                (readWriteLockView = new ReadWriteLockView()));
    }

    // view classes

    final class ReadLockView implements Lock {
        public void lock() { readLock(); }
        public void lockInterruptibly() throws InterruptedException {
            readLockInterruptibly();
        }
        public boolean tryLock() { return tryReadLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
            return tryReadLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockRead(); }
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class WriteLockView implements Lock {
        public void lock() { writeLock(); }
        public void lockInterruptibly() throws InterruptedException {
            writeLockInterruptibly();
        }
        public boolean tryLock() { return tryWriteLock() != 0L; }
        public boolean tryLock(long time, TimeUnit unit)
            throws InterruptedException {
            return tryWriteLock(time, unit) != 0L;
        }
        public void unlock() { unstampedUnlockWrite(); }
        public Condition newCondition() {
            throw new UnsupportedOperationException();
        }
    }

    final class ReadWriteLockView implements ReadWriteLock {
        public Lock readLock() { return asReadLock(); }
        public Lock writeLock() { return asWriteLock(); }
    }

    // Unlock methods without stamp argument checks for view classes.
    // Needed because view-class lock methods throw away stamps.

    final void unstampedUnlockWrite() {
        WNode h; long s;
        if (((s = state) & WBIT) == 0L)
            throw new IllegalMonitorStateException();
        state = (s += WBIT) == 0L ? ORIGIN : s;
        if ((h = whead) != null && h.status != 0)
            release(h);
    }

    final void unstampedUnlockRead() {
        for (;;) {
            long s, m; WNode h;
            if ((m = (s = state) & ABITS) == 0L || m >= WBIT)
                throw new IllegalMonitorStateException();
            else if (m < RFULL) {
                if (U.compareAndSwapLong(this, STATE, s, s - RUNIT)) {
                    if (m == RUNIT && (h = whead) != null && h.status != 0)
                        release(h);
                    break;
                }
            }
            else if (tryDecReaderOverflow(s) != 0L)
                break;
        }
    }

    private void readObject(java.io.ObjectInputStream s)
        throws java.io.IOException, ClassNotFoundException {
        s.defaultReadObject();
        state = ORIGIN; // reset to unlocked state
    }

    // internals

    /**
     * Tries to increment readerOverflow by first setting state
     * access bits value to RBITS, indicating hold of spinlock,
     * then updating, then releasing.
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryIncReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        // 126个读锁满了之后(s=101111110)，overflow ++;
        if ((s & ABITS) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) { // state:101111111
                ++readerOverflow;
                state = s;
                return s;
            }
        }
        else if ((LockSupport.nextSecondarySeed() &
                  OVERFLOW_YIELD_RATE) == 0)
            // 让出线程
            Thread.yield();
        return 0L;
    }

    /**
     * Tries to decrement readerOverflow.
     *
     * @param s a reader overflow stamp: (s & ABITS) >= RFULL
     * @return new stamp on success, else zero
     */
    private long tryDecReaderOverflow(long s) {
        // assert (s & ABITS) >= RFULL;
        if ((s & ABITS) == RFULL) {
            if (U.compareAndSwapLong(this, STATE, s, s | RBITS)) {
                int r; long next;
                if ((r = readerOverflow) > 0) {
                    readerOverflow = r - 1;
                    next = s;
                }
                else
                    next = s - RUNIT;
                 state = next;
                 return next;
            }
        }
        else if ((LockSupport.nextSecondarySeed() &
                  OVERFLOW_YIELD_RATE) == 0)
            Thread.yield();
        return 0L;
    }

    /**
     * Wakes up the successor of h (normally whead). This is normally
     * just h.next, but may require traversal from wtail if next
     * pointers are lagging. This may fail to wake up an acquiring
     * thread when one or more have been cancelled, but the cancel
     * methods themselves provide extra safeguards to ensure liveness.
     */
    private void release(WNode h) {
        if (h != null) {
            WNode q; Thread w;
            U.compareAndSwapInt(h, WSTATUS, WAITING, 0);
            if ((q = h.next) == null || q.status == CANCELLED) {
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (q != null && (w = q.thread) != null)
                U.unpark(w);
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible true if should check interrupts and if so
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return next state, or INTERRUPTED
     */
    private long acquireWrite(boolean interruptible, long deadline) {
        WNode node = null, p;
        for (int spins = -1;;) { // spin while enqueuing
            long m, s, ns;
            // cas获取锁，返回新的state
            if ((m = (s = state) & ABITS) == 0L) {
                if (U.compareAndSwapLong(this, STATE, s, ns = s + WBIT))
                    return ns;
            }
            else if (spins < 0)
                // 如果写锁被占用且写队列头尾相同（没有线程竞争），入队列前自旋64次，否则直接入队列
                spins = (m == WBIT && wtail == whead) ? SPINS : 0;
            else if (spins > 0) {
                // 获取锁失败，自旋递减
                if (LockSupport.nextSecondarySeed() >= 0)
                    --spins;
            }
            // 自旋完仍然没有获取到锁，开始入队列
            else if ((p = wtail) == null) { // initialize queue
                // 队列初始化，头尾相同节点
                WNode hd = new WNode(WMODE, null);
                if (U.compareAndSwapObject(this, WHEAD, null, hd))
                    wtail = hd;
            }
            else if (node == null)
                node = new WNode(WMODE, p);
            else if (node.prev != p)
                node.prev = p;
            else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                // cas入队尾成功
                p.next = node;
                break;
            }
        }

        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            // 如果node处于头部节点，那么spin获取锁
            if ((h = whead) == p) {
                if (spins < 0)
                    spins = HEAD_SPINS;
                else if (spins < MAX_HEAD_SPINS)
                    // 自旋次数翻倍，直到最大值
                    spins <<= 1;
                for (int k = spins;;) { // spin at head
                    // 自旋获取写锁，
                    long s, ns;
                    if (((s = state) & ABITS) == 0L) {
                        if (U.compareAndSwapLong(this, STATE, s,
                                                 ns = s + WBIT)) {
                            whead = node;
                            node.prev = null;
                            return ns;
                        }
                    }
                    else if (LockSupport.nextSecondarySeed() >= 0 &&
                             --k <= 0)
                        break;
                }
            }
            else if (h != null) { // help release stale waiters
                WNode c; Thread w;
                // 如果是头节点是读节点，unpark它的linked reader node
                while ((c = h.cowait) != null) {
                    if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                        (w = c.thread) != null)
                        U.unpark(w);
                }
            }
            if (whead == h) {
                // 当前节点的前置节点发生变化时，更新p节点
                if ((np = node.prev) != p) {
                    if (np != null)
                        (p = np).next = node;   // stale
                }
                else if ((ps = p.status) == 0)
                    // 获取不到锁，将前置节点的状态改为waiting,当前节点park时是由前置节点唤醒的
                    //第一次获取锁失败后将状态改为waiting，第二次循环还获取不到就将当前node park
                    U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
                else if (ps == CANCELLED) {
                    // 前置节点是取消状态时，要往前找节点
                    if ((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    long time; // 0 argument to park means no timeout
                    if (deadline == 0L)
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        // 超时取消
                        return cancelWaiter(node, node, false);
                    // park当前node
                    Thread wt = Thread.currentThread();
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if (p.status < 0 && (p != h || (state & ABITS) != 0L) &&
                        whead == h && node.prev == p)
                        U.park(false, time);  // emulate LockSupport.park
                    node.thread = null;
                    U.putObject(wt, PARKBLOCKER, null);
                    if (interruptible && Thread.interrupted())
                        return cancelWaiter(node, node, true);
                }
            }
        }
    }

    /**
     * See above for explanation.
     *
     * @param interruptible true if should check interrupts and if so
     * return INTERRUPTED
     * @param deadline if nonzero, the System.nanoTime value to timeout
     * at (and return zero)
     * @return next state, or INTERRUPTED
     */
    private long acquireRead(boolean interruptible, long deadline) {
        WNode node = null, p;
        for (int spins = -1;;) {
            WNode h;
            // 不存在竞争
            if ((h = whead) == (p = wtail)) {
                for (long m, s, ns;;) {
                    // 只有读锁且未饱和，则cas+1，否则，如果没有写锁，那么尝试放到溢出读锁中
                    if ((m = (s = state) & ABITS) < RFULL ?
                        U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) :
                        (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L))
                        return ns;
                    else if (m >= WBIT) {
                        // 如果有写锁，又不存在竞争，则开始spin
                        if (spins > 0) {
                            if (LockSupport.nextSecondarySeed() >= 0)
                                --spins;
                        }
                        else {
                            if (spins == 0) {
                                WNode nh = whead, np = wtail;
                                // 一轮spin结束后，如果头尾节点还是没变/头尾节点变化了，但是头尾已经不再是同一个节点了，跳出循环去排队
                                if ((nh == h && np == p) || (h = nh) != (p = np))
                                    break;
                            }
                            spins = SPINS;
                        }
                    }
                }
            }
            // 头尾节点不等，则尝试入队相关操作
            if (p == null) { // initialize queue
                WNode hd = new WNode(WMODE, null);
                if (U.compareAndSwapObject(this, WHEAD, null, hd))
                    wtail = hd;
            }
            else if (node == null)
                // 创建一个读节点
                node = new WNode(RMODE, p);
            else if (h == p || p.mode != RMODE) { // 如果pre节点是头节点/p节点是写节点
                // 将node挂在尾节点
                if (node.prev != p)
                    node.prev = p;
                // 入队列成功后，跳出当前循环。
                else if (U.compareAndSwapObject(this, WTAIL, p, node)) {
                    p.next = node;
                    break;
                }
            }
            else if (!U.compareAndSwapObject(p, WCOWAIT,
                                             node.cowait = p.cowait, node)) //如果前节点是读节点，要挂在co-waiter上（头插法）
                node.cowait = null;
            else { // 挂在co-waiter上成功后
                for (;;) {
                    WNode pp, c; Thread w;
                    // 帮忙唤醒co-waiter
                    if ((h = whead) != null && (c = h.cowait) != null &&
                        U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                        (w = c.thread) != null) // help release
                        U.unpark(w);
                    // 前面只有一个节点或者只有当前节点在排队
                    if (h == (pp = p.prev) || h == p || pp == null) {
                        long m, s, ns;
                        // 如果没有读锁，则一直尝试获取读锁
                        do {
                            if ((m = (s = state) & ABITS) < RFULL ?
                                U.compareAndSwapLong(this, STATE, s,
                                                     ns = s + RUNIT) :
                                (m < WBIT &&
                                 (ns = tryIncReaderOverflow(s)) != 0L))
                                return ns;
                        } while (m < WBIT);
                    }
                    // 头节点和p的前节点不变的情况下，执行if里面的逻辑
                    if (whead == h && p.prev == pp) {
                        long time;
                        //  如果pp == null || h == p，说明前面的节点已经处理完了或取消了，则不需要阻塞跳出当前循环，执行第一层循环逻辑
                        // todo 直接将node置成null就好了嘛，前面将节点放到了p的co-waiter上了，没有挂上线程，看起来不会有啥问题
                        if (pp == null || h == p || p.status > 0) {
                            node = null; // throw away
                            break;
                        }
                        if (deadline == 0L)
                            time = 0L;
                        // 超时取消
                        else if ((time = deadline - System.nanoTime()) <= 0L)
                            return cancelWaiter(node, p, false);

                        Thread wt = Thread.currentThread();
                        U.putObject(wt, PARKBLOCKER, this);
                        node.thread = wt;
                        // park thread p的前节点不是头节点，或者写锁存且h还是whead节点&&pp还是p的前节点
                        if ((h != pp || (state & ABITS) == WBIT) &&
                            whead == h && p.prev == pp)
                            U.park(false, time);
                        node.thread = null;
                        U.putObject(wt, PARKBLOCKER, null);
                        // 中断取消
                        if (interruptible && Thread.interrupted())
                            return cancelWaiter(node, p, true);
                    }
                }
            }
        }

        for (int spins = -1;;) {
            WNode h, np, pp; int ps;
            // 处于头节点时，head spin获取锁
            if ((h = whead) == p) {
                if (spins < 0)
                    spins = HEAD_SPINS;
                else if (spins < MAX_HEAD_SPINS)
                    spins <<= 1;
                for (int k = spins;;) { // spin at head
                    long m, s, ns;
                    if ((m = (s = state) & ABITS) < RFULL ?
                        U.compareAndSwapLong(this, STATE, s, ns = s + RUNIT) :
                        (m < WBIT && (ns = tryIncReaderOverflow(s)) != 0L)) {
                        WNode c; Thread w;
                        whead = node;
                        node.prev = null;
                        while ((c = node.cowait) != null) {
                            if (U.compareAndSwapObject(node, WCOWAIT,
                                                       c, c.cowait) &&
                                (w = c.thread) != null)
                                U.unpark(w);
                        }
                        return ns;
                    }
                    else if (m >= WBIT &&
                             LockSupport.nextSecondarySeed() >= 0 && --k <= 0)
                        break;
                }
            }
            else if (h != null) { // help unpark threads
                WNode c; Thread w;
                while ((c = h.cowait) != null) {
                    if (U.compareAndSwapObject(h, WCOWAIT, c, c.cowait) &&
                        (w = c.thread) != null)
                        U.unpark(w);
                }
            }
            if (whead == h) {
                // 将p节点更新为node.prev
                if ((np = node.prev) != p) {
                    if (np != null)
                        (p = np).next = node;   // stale
                }
                else if ((ps = p.status) == 0)
                    // 更新waiting
                    U.compareAndSwapInt(p, WSTATUS, 0, WAITING);
                else if (ps == CANCELLED) {
                    // p节点取消后往前找节点
                    if ((pp = p.prev) != null) {
                        node.prev = pp;
                        pp.next = node;
                    }
                }
                else {
                    // park thread
                    long time;
                    if (deadline == 0L)
                        time = 0L;
                    else if ((time = deadline - System.nanoTime()) <= 0L)
                        return cancelWaiter(node, node, false);
                    Thread wt = Thread.currentThread();
                    U.putObject(wt, PARKBLOCKER, this);
                    node.thread = wt;
                    if (p.status < 0 &&
                        (p != h || (state & ABITS) == WBIT) &&
                        whead == h && node.prev == p)
                        U.park(false, time);
                    node.thread = null;
                    U.putObject(wt, PARKBLOCKER, null);
                    if (interruptible && Thread.interrupted())
                        return cancelWaiter(node, node, true);
                }
            }
        }
    }

    /**
     * If node non-null, forces cancel status and unsplices it from
     * queue if possible and wakes up any cowaiters (of the node, or
     * group, as applicable), and in any case helps release current
     * first waiter if lock is free. (Calling with null arguments
     * serves as a conditional form of release, which is not currently
     * needed but may be needed under possible future cancellation
     * policies). This is a variant of cancellation methods in
     * AbstractQueuedSynchronizer (see its detailed explanation in AQS
     * internal documentation).
     *
     * @param node if nonnull, the waiter
     * @param group either node or the group node is cowaiting with
     * @param interrupted if already interrupted
     * @return INTERRUPTED if interrupted or Thread.interrupted, else zero
     */
    private long cancelWaiter(WNode node, WNode group, boolean interrupted) {
        if (node != null && group != null) {
            Thread w;
            node.status = CANCELLED;
            // unsplice cancelled nodes from group
            for (WNode p = group, q; (q = p.cowait) != null;) {
                if (q.status == CANCELLED) {
                    U.compareAndSwapObject(p, WCOWAIT, q, q.cowait);
                    p = group; // restart
                }
                else
                    p = q;
            }
            if (group == node) {
                for (WNode r = group.cowait; r != null; r = r.cowait) {
                    // 唤醒co-waiter
                    if ((w = r.thread) != null)
                        U.unpark(w);       // wake up uncancelled co-waiters
                }
                for (WNode pred = node.prev; pred != null; ) { // unsplice
                    WNode succ, pp;        // find valid successor
                    while ((succ = node.next) == null ||
                           succ.status == CANCELLED) {
                        WNode q = null;    // find successor the slow way
                        // 找到node的下一个有效节点
                        for (WNode t = wtail; t != null && t != node; t = t.prev)
                            if (t.status != CANCELLED)
                                q = t;     // don't link if succ cancelled
                        // succ == q是说succ == null
                        if (succ == q ||   // ensure accurate successor
                            U.compareAndSwapObject(node, WNEXT,
                                                   succ, succ = q) // 通过cas，将node的下一个节点设置成有效的节点
                        ) {
                            // 如果是node是尾节点，那么将wtail指向pred
                            if (succ == null && node == wtail)
                                U.compareAndSwapObject(this, WTAIL, node, pred);
                            break;
                        }
                    }
                    // 将pred节点的next指向succ
                    if (pred.next == node) // unsplice pred link
                        U.compareAndSwapObject(pred, WNEXT, node, succ);
                    if (succ != null && (w = succ.thread) != null) {
                        succ.thread = null;
                        U.unpark(w);       // wake up succ to observe new pred
                    }
                    // pre节点是有效的就break，否则继续往前挂接
                    if (pred.status != CANCELLED || (pp = pred.prev) == null)
                        break;
                    node.prev = pp;        // repeat if new pred wrong/cancelled
                    U.compareAndSwapObject(pp, WNEXT, pred, succ);
                    pred = pp;
                }
            }
        }
        WNode h; // Possibly release first waiter
        while ((h = whead) != null) {
            long s; WNode q; // similar to release() but check eligibility
            if ((q = h.next) == null || q.status == CANCELLED) {
                // 从尾往前，找到离whead最近的有效节点
                for (WNode t = wtail; t != null && t != h; t = t.prev)
                    if (t.status <= 0)
                        q = t;
            }
            if (h == whead) {
                // 没有写锁且是读节点，需要release(h)来竞争锁
                if (q != null && h.status == 0 &&
                    ((s = state) & ABITS) != WBIT && // waiter is eligible
                    (s == 0L || q.mode == RMODE))
                    release(h);
                break;
            }
        }
        return (interrupted || Thread.interrupted()) ? INTERRUPTED : 0L;
    }

    // Unsafe mechanics
    private static final sun.misc.Unsafe U;
    private static final long STATE;
    private static final long WHEAD;
    private static final long WTAIL;
    private static final long WNEXT;
    private static final long WSTATUS;
    private static final long WCOWAIT;
    private static final long PARKBLOCKER;

    static {
        try {
            U = sun.misc.Unsafe.getUnsafe();
            Class<?> k = StampedLock.class;
            Class<?> wk = WNode.class;
            STATE = U.objectFieldOffset
                (k.getDeclaredField("state"));
            WHEAD = U.objectFieldOffset
                (k.getDeclaredField("whead"));
            WTAIL = U.objectFieldOffset
                (k.getDeclaredField("wtail"));
            WSTATUS = U.objectFieldOffset
                (wk.getDeclaredField("status"));
            WNEXT = U.objectFieldOffset
                (wk.getDeclaredField("next"));
            WCOWAIT = U.objectFieldOffset
                (wk.getDeclaredField("cowait"));
            Class<?> tk = Thread.class;
            PARKBLOCKER = U.objectFieldOffset
                (tk.getDeclaredField("parkBlocker"));

        } catch (Exception e) {
            throw new Error(e);
        }
    }
}
