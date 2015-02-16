/**
 *  Copyright (C) 2002-2015   The FreeCol Team
 *
 *  This file is part of FreeCol.
 *
 *  FreeCol is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  FreeCol is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with FreeCol.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.sf.freecol.client;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;

import net.sf.freecol.FreeCol;


/**
 * The Worker Thread executes jobs one after another.  The thread
 * manages a queue where new jobs can be enqueued.  The jobs are
 * processed synchronously by the worker.
 */
public final class Worker extends Thread {

    private final LinkedBlockingQueue<Runnable> jobList;

    private volatile boolean stopRunning;

    private static final Logger logger = Logger.getLogger(Worker.class.getName());
    
    public Worker() {
        super(FreeCol.CLIENT_THREAD+"Worker");
        jobList = new LinkedBlockingQueue<>();
        stopRunning = false;
    }

    @Override
    public void run() {
        while (!stopRunning) {
            try {
                // run the next waiting job
                Runnable job = jobList.take();
                try {
                    job.run();
                } catch (Exception e) {
                    logger.log(Level.SEVERE, "Worker task failed!", e);
                }
            } catch (InterruptedException e) {
                logger.log(Level.INFO, "Worker interrupted, aborting!", e);
            }
        }
    }

    /**
     * Adds a new job to the queue
     * 
     * @param job the job to add to the queue.
     */
    public void schedule(Runnable job) {
        jobList.add(job);
    }

    /**
     * Makes the worker thread stop running.
     */
    public void askToStop() {
        stopRunning = true;
        this.interrupt();
    }
}
