package net.sf.freecol.client;

import java.util.concurrent.LinkedBlockingQueue;

public final class Worker extends Thread {
	public static final String COPYRIGHT = "Copyright (C) 2003-2007 The FreeCol Team";
	public static final String LICENSE = "http://www.gnu.org/licenses/gpl.html";
	public static final String REVISION = "$Revision$";

	private final LinkedBlockingQueue<Runnable> jobList;
	private volatile boolean stopRunning;

	public Worker() {
		super("Worker");
		jobList = new LinkedBlockingQueue<Runnable>();
		stopRunning = false;
	}

	public void run() {
		while (!stopRunning) {
			try {
				// run the next waiting job
				Runnable job = jobList.take();
				try {
					job.run();
				} catch (Exception e) {
					System.err.println("a job produced an error:");
					e.printStackTrace();
				}
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
	}

	public void schedule(Runnable job) {
		jobList.add(job);
	}

	public void askToStop() {
		stopRunning = true;
		this.interrupt();
	}
}
