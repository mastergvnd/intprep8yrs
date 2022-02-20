package com.hyperion.planning.olap.japi;

import java.util.*;

import com.hyperion.planning.odl.HspODLLogger;
import com.hyperion.planning.odl.HspODLMsgs;
import com.hyperion.planning.HspRuntimeException;
import com.hyperion.planning.odl.HspLogComponent;

import java.util.Properties;


/**
 *
 */
public abstract class HspPoolNew{
    private final static HspODLLogger logger = HspODLLogger.getLogger(HspLogComponent.PLANNING);
    private Vector pool = new Vector();
    private Vector createdObjects = new Vector();
    private Vector waitingList = new Vector();
    private int numInterrupted = 0;

    private int maxWait = 30000;
    private int minSize = 1;
    private int maxSize = 1;
    private boolean started = false;

    abstract protected boolean startPool();

    abstract protected void stopPool();

    abstract protected void createObject();

    protected synchronized void deleteObject(Object o) {
        pool.remove(o);
        createdObjects.remove(o);
    }

    public HspPoolNew(int min, int max, int wait) {
        if (min < 0 && max >= 0) {
            this.minSize = max;
            this.maxSize = max;
        } else if (min >= 0 && max < 0) {
            this.minSize = min;
            this.maxSize = min;
        } else if (min >= 0 && max >= 0) {
            if (min > max) {
                this.maxSize = min;
                this.minSize = min;
            } else {
                this.minSize = min;
                this.maxSize = max;
            }
        } else {
            logger.finer("HspPool minSize and maxSize cannot be smaller than 0");
        }

        if (wait < 0) {
            //throw new IllegalArgumentException("Max Waiting time can not be shorter than 0 second.");
            logger.finer("Max Waiting time can not be shorter than 0 second.");
        } else {
            this.maxWait = wait;
        }

        if (pool == null)
            throw new RuntimeException("Object pool does not exist");
        if (waitingList == null)
            throw new RuntimeException("Waiting list does not exist");
        ;
    }

    private class ObjectCreatorThread extends Thread {
        public void run() {
            createObjects();
        }
    }

    protected synchronized void init() {
        if (!started) {
            started = startPool();
            (new ObjectCreatorThread()).start();
        }
    }

    /**
     * Create objects with number of minSize, add them into pool.
     */
    //Do not synchronize this.
    private void createObjects() {
        synchronized (this) {
            if (!started)
                return;
        }
        int maxTries = minSize * 2;
        for (int tries = 0; tries < maxTries; tries++) {
            synchronized (this) {
                // Create an object if more are needed and the pool is started
                if (createdObjects.size() >= minSize || !started)
                    break;
                createObject();
            }
        }
    }

    /**
     * Add an object into pool, notify waiting list
     */
    protected synchronized void addObject(Object o) {
        if (o == null) {
            logger.finer("Object is null.");
            return;
        }

        if (createdObjects.size() >= maxSize) {
            logger.finer("numCreated >= maxSize");
            return;
        }

        pool.addElement(o);
        createdObjects.addElement(o);
        notifyPool();
    }

    /**
     * Get an availible object/connection from pool.
     */
    public synchronized Object getObject() throws Exception {
        init();
        if (!started)
            throw new RuntimeException("Unable to start pool");
        createAvailiableObject();
        if (createdObjects.size() == 0)
            throw new Exception("No object were successfully created. This can be caused by any of the following: The OLAP Server is not running, The DBMS is not running, the DBMS is running on a different machine that the one specified, the name and password provided were incorrect.");


        //If the waiting list is empty, and noone was told there was an object for them
        //and the pool is not empty, then we can grab a pooled object and return.
        //Otherwise, we need to add ourselves to the waiting list and wait.  This would be
        //Better with a linked list or que, which are available in jdk 1.2, not 1.1.8
        if ((numInterrupted > 0) || (waitingList.size() > 0) || (pool.size() <= 0)) {
            try {
                waitingList.addElement(Thread.currentThread());
                this.wait(maxWait);
                //If we get here w/o exception, we times out and should remove ourself
                //from the list and return null
                waitingList.removeElement(Thread.currentThread());
                return null;
            } catch (InterruptedException e) { //This means that we are ok to continue as we were interupted.
                //But we need to decrement the numInterrupted value so that other threads
                //know that we are no longer waiting to catch the interrupted exception
                numInterrupted--;
            }
            //If still no elements, return null
            if (pool.size() <= 0)
                return null;
        }
        //Otherwise, we remove last element and pass back
        int lastElement = pool.size() - 1;
        Object o = pool.elementAt(lastElement);
        pool.removeElementAt(lastElement);
        //		HspLogger.trace(Thread.currentThread().getName()+": Removed Element");
        return o;
    }

    public boolean threadsAreWorking() {
        return pool.size() < createdObjects.size();
    }

    /**
     * Add object to pool and notift waiting list for grabbing.
     */
    public synchronized void releaseObject(Object o) {
        if (o == null) {
            logger.finer("Attempted to release a null connection");
            return;
        } else if (isValidObject(o)) {
            pool.addElement(o);
            //			HspLogger.trace(Thread.currentThread().getName()+": Added Element");
            notifyPool();
        } else
            deleteObject(o);
    }
    //Makes sure that the object being release actually exists in the createdObjects
    //List.  If not, then this object is not valid for this pool and is ignored.
    //This happens when users get a connection, then the pool is reset, and then
    //they try to release this connection.  The connection they release is from a
    //previous state of the pool, is not valid for the new state, and thus should be ignore.
    //If an exception was thrown, mass problems would appear when that user returned his
    //connection, so thats why we ignore it.

    private synchronized boolean isValidObject(Object o) {
        if (o == null)
            return false;
        for (int loop1 = 0; loop1 < createdObjects.size(); loop1++) {
            if (o == createdObjects.elementAt(loop1))
                return true;
        }
        return false;
    }

    /**
     * Clear all objects/connections, set started flag to false.
     */
    public synchronized void resetPool() {
        try {
            this.stopPool();
        } finally {
            started = false;
        }
        Iterator it = pool.iterator();
        while (it.hasNext()) {
            deleteObject(it.next());
        }
        createdObjects.removeAllElements();
        pool.removeAllElements();
    }

    /**
     * If this is no availiable object/connection, and created object number is less than max,
     * then create one object.
     */
    private synchronized void createAvailiableObject() {
        if (pool.size() <= 0 && createdObjects.size() < maxSize) {
            logger.finer("Need to create an Object. pool size = {0} creatredObjs = {1}", pool.size(), createdObjects.size());

            createObject();
            notifyPool();
        }
    }

    private synchronized void notifyPool() {
        if (waitingList.size() > 0) {
            Thread t = (Thread)waitingList.elementAt(0);
            //In order to let the next user be notified, we remove the thread from the list.
            //If this is done in the release connection method, there is a possiblity that
            //Another use will come before we get interrupted, and then see that the list is empty
            //And take the connection that was for us.
            waitingList.removeElementAt(0);
            //So, we must add one to numNotified, and they will subtract one later
            if (t != null) {
                numInterrupted++;
                t.interrupt();
            }
        }
    }

    /**
     * Get current availiable object number in pool.
     */
    public synchronized int getPoolSize() {
        return pool.size();
    }

    /**
     * Get created Object number.
     */
    public synchronized int getNumCreated() {
        return createdObjects.size();
    }

    /**
     * Get minimum pool size.
     */
    public synchronized int getMinSize() {
        return this.minSize;
    }

    /**
     * Get max pool size.
     */
    public synchronized int getMaxSize() {
        return this.maxSize;
    }

    /**
     * Get max Wait time
     */
    public int getMaxWait() {
        return maxWait;
    }
}
