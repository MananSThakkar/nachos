package nachos.threads;

import java.util.ArrayList;
import java.util.LinkedList;

import nachos.machine.*;

/**
 * Uses the hardware timer to provide preemption, and to allow threads to sleep
 * until a certain time.
 */
public class Alarm {
    /**
     * Allocate a new Alarm. Set the machine's timer interrupt handler to this
     * alarm's callback.
     *
     * <p><b>Note</b>: Nachos will not function correctly with more than one
     * alarm.
     */
    public Alarm() {
	Machine.timer().setInterruptHandler(new Runnable() {
		public void run() { timerInterrupt(); }
	    });
    }

    /**
     * The timer interrupt handler. This is called by the machine's timer
     * periodically (approximately every 500 clock ticks). Causes the current
     * thread to yield, forcing a context switch if there is another thread
     * that should be run.
     */
    public void timerInterrupt() {
    	boolean intStatus = Machine.interrupt().disable();
    	long currTime = Machine.timer().getTime();
    	int i = 0;  
    	while(i < list.size()) { // iterate through the list using i until i is less then size of timer list.
    		if(list.get(i).getTime() < currTime) {   // if i < size of list get the position and the time it took to get there and compare it with current time.
    			KThread thread = list.get(i).getKThread();  // if it is < then current time get the KThread at that position
    			thread.ready();  // and mark it ready.
    			list.remove(i);
    			i = 0;  // set i value to zero again so a new thread can be easily mapped from zero to current time.
    		}
    	i++;
    	}
    	KThread.yield();
    	Machine.interrupt().restore(intStatus);
    }

    /**
     * Put the current thread to sleep for at least <i>x</i> ticks,
     * waking it up in the timer interrupt handler. The thread must be
     * woken up (placed in the scheduler ready set) during the first timer
     * interrupt where
     *
     * <p><blockquote>
     * (current time) >= (WaitUntil called time)+(x)
     * </blockquote>
     *
     * @param	x	the minimum number of clock ticks to wait.
     *
     * @see	nachos.machine.Timer#getTime()
     */
    public void waitUntil(long x) {
	// for now, cheat just to get something working (busy waiting is bad)
    	boolean intStatus = Machine.interrupt().disable();  //disable the interrupt
    	long wakeTime = Machine.timer().getTime() + x; 
    	list.add(new timer(KThread.currentThread(), wakeTime));  //use the timer list  and add wake time of the current thread. 
    	KThread.sleep();  // put KThread to sleep until tone thread has finished its job.
    	Machine.interrupt().restore(intStatus); // enable the interrupt.
	
    }
    
    private ArrayList<timer> list = new ArrayList<timer>(); //create a list using Array list with timer instances.

    private class timer{  // timer class returns the current thread and wake time.
    	private long time = 0;
    	private KThread Thread = null;
		public timer(KThread currentThread, long wakeTime) {
			this.Thread = currentThread;
			this.time = wakeTime;
		}
		public long getTime() {
			return time;
		}
		public void setTime(long time) {
			this.time = time;
		}
		public KThread getKThread() {
			return Thread;
		}
		public void setKThread(KThread thread) {
			Thread = thread;
		}
    }
    

    public static void alarmTest1() {
	int durations[] = {1000, 10*1000, 100*1000};
	long t0, t1;

	for (int d : durations) {
	    t0 = Machine.timer().getTime();
	    ThreadedKernel.alarm.waitUntil (d);
	    t1 = Machine.timer().getTime();
	    System.out.println ("alarmTest1: waited for " + (t1 - t0) + " ticks");
	}
   }

   // Implement more test methods here ...

   // Invoke Alarm.selfTest() from ThreadedKernel.selfTest()
   public static void selfTest() {
	alarmTest1();

	// Invoke your other test methods here ...
   }

}
