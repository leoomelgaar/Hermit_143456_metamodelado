package org.semanticweb.HermiT.tableau;

import java.io.Serializable;
import org.semanticweb.owlapi.reasoner.ReasonerInterruptedException;
import org.semanticweb.owlapi.reasoner.TimeOutException;

public final class InterruptFlag
implements Serializable {
    private static final long serialVersionUID = -6983680374511847003L;
    private final InterruptTimer m_interruptTimer;
    private volatile InterruptType m_interruptType;

    public InterruptFlag(long individualTaskTimeout) {
        this.m_interruptTimer = individualTaskTimeout > 0L ? new InterruptTimer(individualTaskTimeout) : null;
    }

    public void checkInterrupt() {
        InterruptType interruptType = this.m_interruptType;
        if (interruptType != null) {
            if (interruptType == InterruptType.TIMEOUT) {
                throw new TimeOutException();
            }
            throw new ReasonerInterruptedException();
        }
    }

    public void interrupt() {
        this.m_interruptType = InterruptType.INTERRUPTED;
    }

    public void startTask() {
        this.m_interruptType = null;
        if (this.m_interruptTimer != null) {
            this.m_interruptTimer.startTiming();
        }
    }

    public void endTask() {
        if (this.m_interruptTimer != null) {
            this.m_interruptTimer.stopTiming();
        }
        this.m_interruptType = null;
    }

    public void dispose() {
        if (this.m_interruptTimer != null) {
            this.m_interruptTimer.dispose();
        }
    }

    protected class InterruptTimer
    extends Thread {
        protected final long m_timeout;
        protected TimerState m_timerState;

        public InterruptTimer(long timeout) {
            super("HermiT Interrupt Current Task Thread");
            this.setDaemon(true);
            this.m_timeout = timeout;
            this.start();
        }

        @Override
        public synchronized void run() {
            while (this.m_timerState != TimerState.DISPOSED) {
                this.m_timerState = TimerState.WAIT_FOR_TASK;
                this.notifyAll();
                while (this.m_timerState == TimerState.WAIT_FOR_TASK) {
                    try {
                        this.wait();
                    }
                    catch (InterruptedException stopped) {
                        this.m_timerState = TimerState.DISPOSED;
                    }
                }
                if (this.m_timerState != TimerState.TIMING) continue;
                try {
                    this.wait(this.m_timeout);
                    if (this.m_timerState != TimerState.TIMING) continue;
                    InterruptFlag.this.m_interruptType = InterruptType.TIMEOUT;
                }
                catch (InterruptedException stopped) {
                    this.m_timerState = TimerState.DISPOSED;
                }
            }
        }

        public synchronized void startTiming() {
            while (this.m_timerState != TimerState.WAIT_FOR_TASK && this.m_timerState != TimerState.DISPOSED) {
                try {
                    this.wait();
                }
                catch (InterruptedException interruptedException) {}
            }
            if (this.m_timerState == TimerState.WAIT_FOR_TASK) {
                this.m_timerState = TimerState.TIMING;
                this.notifyAll();
            }
        }

        public synchronized void stopTiming() {
            if (this.m_timerState == TimerState.TIMING) {
                this.m_timerState = TimerState.TIMING_STOPPED;
                this.notifyAll();
                while (this.m_timerState != TimerState.WAIT_FOR_TASK && this.m_timerState != TimerState.DISPOSED) {
                    try {
                        this.wait();
                    }
                    catch (InterruptedException stopped) {
                        return;
                    }
                }
            }
        }

        public synchronized void dispose() {
            this.m_timerState = TimerState.DISPOSED;
            this.notifyAll();
            try {
                this.join();
            }
            catch (InterruptedException interruptedException) {
                // empty catch block
            }
        }
    }

    protected enum TimerState {
        WAIT_FOR_TASK,
        TIMING,
        TIMING_STOPPED,
        DISPOSED

    }

    protected enum InterruptType {
        INTERRUPTED,
        TIMEOUT

    }

}

