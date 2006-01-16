
package net.sf.freecol.client;


import java.util.ArrayList;
import java.util.List;


public final class Worker implements Runnable
{
    private final  List     jobList;
    private        boolean  shouldRun;
    private final  Object   lock;


    // ----------------------------------------------------------- constructors

    public Worker() {

        jobList = new ArrayList();
        shouldRun = true;
        lock = new Object();
    }


    // ------------------------------------------------------------ API methods

    public void schedule( Runnable job ) {

        synchronized ( lock ) {

            jobList.add( job );
        }
    }


    public void askToStop() {

        synchronized ( lock ) {

            shouldRun = false;
        }
    }


    public void run() {

        while ( shouldRun() ) {

            try {
                // run any waiting jobs
                Runnable  job;
                while ( (job = nextJob()) != null ) {

                    job.run();
                }

                // sleep a bit so as not to redline the CPU
                Thread.sleep( 1 );
            }
            catch ( InterruptedException e ) {

                e.printStackTrace();
            }
        }
    }


    // -------------------------------------------------------- support methods

    private boolean shouldRun() {

        synchronized ( lock ) {

            return shouldRun;
        }
    }


    private Runnable nextJob() {

        synchronized ( lock ) {

            if ( jobList.isEmpty() ) {

                return null;
            }

            return (Runnable) jobList.remove( 0 );
        }
    }

}
