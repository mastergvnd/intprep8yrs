package com.hyperion.planning.olap.japi;

import com.hyperion.planning.odl.HspODLLogger;
import com.hyperion.planning.odl.HspODLMsgs;
import com.hyperion.planning.HspRuntimeException;
import com.hyperion.planning.odl.HspLogComponent;
import java.util.Properties;
import java.util.*;

/**
 * HspEssbasePool is a concrete class for creating Essbase Connection pool
 * Based on HspPool platform.
 */
public final class HspEssbasePool extends com.hyperion.planning.HspPool
{
 /*   private final static HspODLLogger logger = HspODLLogger.getLogger(HspLogComponent.OLAP);
    private final static class SyncObject {

        private int nCount =0;

        public SyncObject() {
        }

       // private boolean bOutlineThreadWaitsOrWorks = false;

        public synchronized boolean outlineThreadWaitOrWorks() {
            return nCount > 0; //bOutlineThreadWaitsOrWorks;
        }

        public  synchronized void setOutlineThreadWaitOrWorks(boolean bValue) {
            if (bValue)
                nCount++;
            else
                nCount--;

             if( nCount < 0 ) throw new IllegalStateException( "ncount is negative");

            if (logger.isLogFineEnabled())
                logger.fine("outline connection lock count is " + nCount);
            // bOutlineThreadWaitsOrWorks = bValue;
        }

    }

    private final class HspEssbaseGridPool extends com.hyperion.planning.HspPool
    {
        private boolean valid = false;

        private String dbName;
        private long storageType;

        *//**
         * HspEssbasePool will initialize HspPool and Essabase parameters. Then create
         * Essabase connections and add then into pool vector.
         *//*
        HspEssbaseGridPool(String dbName, long storageType)
        {
            super(HspEssbasePool.this.getMinSize(), HspEssbasePool.this.getMaxSize(), HspEssbasePool.this.getMaxWait());
            if (dbName == null) throw new IllegalArgumentException("Essbase DB cannot be null");
            this.dbName  = dbName;
            this.storageType=storageType;
            init();
        }
        HspEssGConnection getConnection() throws HspCubeNotCreatedException
        {
            HspEssGConnection connection = null;
            try {
                connection = (HspEssGConnection) getObject();
            }
            catch (HspCubeNotCreatedException ce)
            {
                logger.error(HspODLMsgs.ERR_OLAP_CONNECTION, ce, dbName);
               // logger.throwing(ce);
                throw ce;
            }
            catch (Exception ce)
            {
                logger.error(HspODLMsgs.ERR_OLAP_CONNECTION, ce, dbName);
                //logger.throwing(ce);
            }
            if (connection == null)
                throw new NoAvailableOlapConnectionsException();
            return connection;
        }

        *//**
         * Initialize Essbase and get the handle.
         *//*
        protected synchronized boolean startPool()
        {	if (valid)
                return valid;
            try {
    //			handle = HspEssbaseGridAPI.EssGInit();	//Will throw an HspOlapException if it fails
    //			hMainAPIInstance=HspEssbaseMainAPI.EssInit(); // B.S. we need it too
                valid = true;
            } catch (Exception e) {
               logger.fine(e);
            }
            return valid;
        }

        *//**
         * Shutdown Essbase.
         *//*
        protected synchronized void stopPool()
        {	if (!valid)
                return;
            this.shutDown();
            valid = false;
        }

        *//**
         * Create Essbase connection and add it into pool vector.
         *//*
        public synchronized void createObject()
        {
            if (!valid)
            {
                startPool();
            }
            if (valid)
            {
                try
                {
                    final HspEssGConnection o = new HspEssGConnection(server, userName, password, appName, dbName,storageType);
                    addObject(o);
                } catch (EssbaseException e) {
                    valid = false;
                   // long result = e.getResultCode();
                    //if (result==ESSBASE_IS_NOT_RUNNING)
                    //{
                    //	this.deleteAllObjects();
                    //}
                    e.printStackTrace(System.err);
                    logger.error(HspODLMsgs.ERR_UNABLE_TO_CONNECT, e, dbName);
                } catch (Exception e2) {
                      valid = false;
                      logger.finer(e2);
                }
            }
        }

        *//**
         * Close Essbase connection. If it is the last connection, shutdown Essbase.
         *//*
        protected synchronized void deleteObject(Object o)
        {
            if (getPoolSize() == 0)
            {
                shutDown();
            }
            else
            {
                HspEssGConnection ess = (HspEssGConnection) o;
                ess.disconnect();
            }
        }

        *//**
         * This modifies getObject on HspPool to check for a created cube if getObject fails,
         * and replaces the error with an HspCubeNotCreatedException if the cube isnt created.
         *//*
        public synchronized Object getObject() throws Exception
        {	Object retVal = null;
            try
            {
                retVal = super.getObject();
            } catch (Exception e)
            {	boolean requiresCubeCreate = false;
                try
                {	//If cube isnt created, set requiresCubeCreate
                    //requiresCubeCreate = !isCubeCreated();
                }
                catch (Exception ignore)
                {
                    //We ignore this, but we set requiresCubeCreate to false, as we dont know if
                    //it needs a cube create or not, and then buble up the original exception
                    requiresCubeCreate = false;
                }
                //Throw message in english and let the client remap this to the correclt locale.
                if (requiresCubeCreate)
                    throw new HspCubeNotCreatedException();
                else //throw original exception
                    throw e;
            }
            return retVal;
        }
        *//**
         * Shutdown Essbase.
         *//*
        private synchronized void shutDown()
        {	if (valid)
            {	valid = false;
                try
                {
                    resetPool();
                }  catch (Exception e)
                {
                    logger.error(HspODLMsgs.ERR_OLAP_CONNECTION_CLOSE, e, dbName);
                }
            }
        }
        *//**
         * This method checks if the Cube Create has been run for this application.
         *//*
        
        private boolean isCubeCreated() throws EssbaseException
        {
            boolean foundMatch = false;

            String[][] pApps = new String[2][];
            int handle = HspEssbaseMainAPI.EssInit();
            int hCtx = HspEssbaseMainAPI.EssLogin(handle, essServer, essUserName, essPassword, pApps);
            HspEssbaseMainAPI.EssLogout(hCtx);
            HspEssbaseMainAPI.EssTerm(handle);
            String[] apps = pApps[0];
            String[] dbs = pApps[1];

            if ((apps != null) && (dbs != null))
            {
                for (int loop1 = 0;loop1< apps.length;loop1++)
                {
                    if ((essDefaultApp.equalsIgnoreCase(apps[loop1]))&&(dbs[loop1]!=null))
                    {	foundMatch = true;
                        break;
                    }
                }
            }
            return foundMatch;

        } 
        
        public String getEssDefaultDB()
        {
            return essDefaultDB;
        }
        
        public void finalize()
        {	try
            {	shutDown();
            } catch (Exception e) {
                    logger.error(HspODLMsgs.ERR_OLAP_CONNECTION_SHUTDOWN, e);
                    }
        }
    }

    private final Map gridPool = new HashMap();
    private String server;
    private String userName;
    private String password = null;
    private String appName;
    private boolean valid = false;
    private SyncObject syncObject = new SyncObject();
    private final long storageType;
    private String ssoToken;
    private Hashtable<Integer,HspEssConnection> jobIdToEssConnTbl = new Hashtable<Integer,HspEssConnection>();

  public HspEssbasePool(int min, int max, int wait, String server, String userName, String password, String appName){
      this(min, max, wait, server, userName, password, appName, HspEssConnection.ESS_APP_DEFAULT_STORAGE); // let's this be default
  }
    public HspEssbasePool(int min, int max, int wait, String server, String userName, String password, String appName, long storageType)
    {
        super(min, max, wait);

        if (server == null) throw new IllegalArgumentException("Essbase server cannot be null");
        this.server = server;
        if (userName == null) throw new IllegalArgumentException("Essbase user cannot be null");
        this.userName = userName;
        if (password != null)
        {
            this.password = password;
        }
        if (appName == null) throw new IllegalArgumentException("Application Name cannot be null");
        this.appName = appName;
        this.storageType=storageType;
        init();
    }
    public String getServer()
    {
        return server;
    }
    public String getUserName()
    {
        return userName;
    }
    public String getPassword()
    {
        return password;
    }
    public String getAppName()
    {
        return appName;
    }
    public HspEssConnection getOutlConnection(String dbName) throws HspCubeNotCreatedException {
        synchronized (syncObject) {
            syncObject.setOutlineThreadWaitOrWorks(true);
            while (true) {
                if (threadsAreWorking() || ((HspEssbaseGridPool)getPool(dbName)).threadsAreWorking()) {
                    try {
                        if (logger.isLogFineEnabled())
                            logger.fine("OLAP pools are busy, waiting...");
                        syncObject.wait();
                    } catch (InterruptedException e) {
                        if (logger.isLogFineEnabled())
                            logger.fine("OLAP pool notification event");
                    }
                } else {
                    if (logger.isLogFineEnabled())
                        logger.fine("No active task in OLAP pools");
                    return getConnectionInternal();
                }
            }
        }
    }
    public boolean essMAXLSessionCreate( HspEssConnection connection) throws EssbaseException {
        logger.entering();
        logger.fine("MAXL creating MAXL session for connection." );
        return connection.essMAXLSessionCreate(server, userName, password);
    }
    public void releaseOutlConnection(HspEssConnection connection) {
        logger.entering();
        logger.finer("Thread  " + Thread.currentThread().getName() + " releasing connection " + connection);
        if( connection == null) throw new IllegalStateException("Connection is null");

        releaseConnectionInternal(connection);
        synchronized (syncObject) {
            syncObject.setOutlineThreadWaitOrWorks(false);
            syncObject.notifyAll();
        }
        logger.exiting();
    }

    public HspEssConnection getConnection() throws HspCubeNotCreatedException {
          synchronized (syncObject) {
            while (syncObject.outlineThreadWaitOrWorks())
            {
                try {
                    syncObject.wait();
                } catch (InterruptedException e) {
                }
            }
            return getConnectionInternal();
        }
    }
    public HspEssConnection getConnectionForJobId(Integer jobId) throws HspCubeNotCreatedException {
       HspEssConnection connection = getConnection();
       if(connection!=null && jobId!=null && jobId.intValue()!=-1){
          logger.finer("associating connection {0} with jobId {1} ", connection, jobId);
          jobIdToEssConnTbl.put(jobId,connection);
       }
       return connection;
    }
    public void releaseConnectionForJobId(HspEssConnection connection, Integer jobId)  {
       if(connection!=null && jobId!=null && jobId.intValue()!=-1){
          logger.finer("disassociating connection {0} with jobId {1} ", connection, jobId);
          jobIdToEssConnTbl.remove(jobId);
          connection.setPendingKillRequest(false);
       }
       releaseConnection(connection);
    }
    public HspEssConnection getConnectionInUseForJobId(int jobId){
        return jobIdToEssConnTbl.get(jobId);
    }
    public void releaseConnection(HspEssConnection connection) {
        logger.entering();
        logger.finer("Thread  " + Thread.currentThread().getName() + " releasing connection " + connection);
        releaseConnectionInternal(connection);
        synchronized (syncObject) {
            syncObject.notifyAll();
        }
        logger.exiting();
    }
     public HspEssConnection getConnectionInternal() throws HspCubeNotCreatedException
    {
        logger.entering();
        HspEssConnection connection = null;
        try {
            connection = (HspEssConnection) getObject();
        }
        catch (HspCubeNotCreatedException ce)
            {
                logger.error(HspODLMsgs.ERR_OLAP_CONN_NOT_GETTING,ce );
                throw ce;
            }
            catch (Exception e)
            {
                logger.error(HspODLMsgs.ERR_OLAP_CONN_NOT_GETTING,e);
            }
        if (connection == null)
            throw new NoAvailableOlapConnectionsException();
        logger.finer("Thread  " + Thread.currentThread().getName() + " acquired connection " + connection);
        logger.exiting();
        return connection;
    }
    public HspEssGConnection getConnection(String dbName) throws HspOlapException {
        synchronized (syncObject) {
            while (syncObject.outlineThreadWaitOrWorks()) {
                try {
                    syncObject.wait();
                } catch (InterruptedException e) {
                }
            }
            return getPool(dbName).getConnection();
        }
    }
    public void releaseConnection(HspEssGConnection connection)
    {
        logger.entering();
        if (connection.disconnected())
        {
            try
            {
                connection.connect(server, userName, password, connection.getApplicationName(), connection.getDatabaseName());
            }
            catch(EssbaseException e)
            {
                if (logger.isLogFineEnabled())
                {
                    logger.fine("Unable to restore connection to Essbase. Recycling pool");
                    logger.fine(e);
                }
                getPool(connection.getDatabaseName()).resetPool();
            }
        }
        getPool(connection.getDatabaseName()).releaseObject(connection);
        synchronized (syncObject)
        {
            syncObject.notifyAll();
        }
        logger.exiting();
    }
    protected void releaseConnectionInternal(HspEssConnection connection)
    {
        logger.finer("Thread  " + Thread.currentThread().getName() + " released connection " + connection);
        if (connection.disconnected())
        {
            resetPool();
            try
            {
                connection.connect(server, userName, password, appName, storageType==HspEssConnection.ESS_APP_DEFAULT_STORAGE?false:true);
            }
            catch (EssbaseException e)
            {
                logger.fine(e);
                deleteObject(connection);
                return;
            }
        }
        super.releaseObject(connection);
    }
    private final synchronized HspEssbaseGridPool getPool(String dbName)
    {
        HspEssbaseGridPool hspEssbaseGridPool;
        hspEssbaseGridPool = (HspEssbaseGridPool)gridPool.get(dbName);
        if (hspEssbaseGridPool == null)
        {
            hspEssbaseGridPool = new HspEssbaseGridPool(dbName,storageType);
            if (hspEssbaseGridPool != null)
                gridPool.put(dbName, hspEssbaseGridPool);
        }
        return hspEssbaseGridPool;
    }
    *//**
     * Initialize Essbase and get the handle.
     *//*
    protected synchronized boolean startPool()
    {	if (valid)
            return valid;
        try {
			handle = HspEssbaseGridAPI.EssGInit();	//Will throw an HspOlapException if it fails
			hMainAPIInstance=HspEssbaseMainAPI.EssInit(); // B.S. we need it too
            valid = true;
        } catch (Exception e) {
            logger.fine("Can not Validate OLAP: ",e);
        }
        return valid;
    }

    *//**
     * Shutdown Essbase.
     *//*
    protected synchronized void stopPool()
    {	if (!valid)
            return;
        this.shutDown();
        valid = false;
    }
    *//**
     * Create Essbase connection and add it into pool vector.
     *//*
    public synchronized void createObject()
    {
        logger.entering();
        if (!valid)
        {
            startPool();
        }
        if (valid)
        {
            try
            {
                final HspEssConnection o = new HspEssConnection(server, userName, password, appName, storageType==HspEssConnection.ESS_APP_DEFAULT_STORAGE?false:true);
                addObject(o);
            } catch (EssbaseException e) {
                valid = false;
                //long result = e.getResultCode();
                //if (result==ESSBASE_IS_NOT_RUNNING)
                //{
                //	this.deleteAllObjects();
                //}
                logger.error(HspODLMsgs.ERR_OLAP_CONN_NOT_GETTING,e );
            } catch (Exception e2) {
                  valid = false;
                  logger.fine(e2);
            }
        }
        logger.exiting();
    }

    *//**
     * Close Essbase connection. If it is the last connection, shutdown Essbase.
     *//*
    protected synchronized void deleteObject(Object o)
    {
        if (getPoolSize() == 0)
        {
            shutDown();
        }
        else
        {
            HspEssConnection ess = (HspEssConnection) o;
            ess.disconnect();
        }
    }

    *//**
     * This modifies getObject on HspPool to check for a created cube if getObject fails,
     * and replaces the error with an HspCubeNotCreatedException if the cube isnt created.
     *//*
    public synchronized Object getObject() throws Exception
    {	Object retVal = null;
        try
        {
            retVal = super.getObject();
        } catch (Exception e)
        {	boolean requiresCubeCreate = false;
            try
            {	//If cube isnt created, set requiresCubeCreate
                //requiresCubeCreate = !isCubeCreated();
            }
            catch (Exception ignore)
            {
                //We ignore this, but we set requiresCubeCreate to false, as we dont know if
                //it needs a cube create or not, and then buble up the original exception
                requiresCubeCreate = false;
            }
            //Throw message in english and let the client remap this to the correclt locale.
            if (requiresCubeCreate)
                throw new HspCubeNotCreatedException();
            else //throw original exception
                throw e;
        }
        return retVal;
    }
    *//**
     * Shutdown Essbase.
     *//*
    private synchronized void shutDown()
    {	if (valid)
        {	valid = false;
            try
            {
                Collection col = gridPool.values();
                if (col != null)
                {
                    Iterator it = col.iterator();
                    HspEssbaseGridPool hspEssbaseGridPool;
                    while (it.hasNext())
                    {
                        hspEssbaseGridPool = (HspEssbaseGridPool)it.next();
                        hspEssbaseGridPool.shutDown();
                    }
                }
                resetPool();
            } catch (Exception e)
            {
                logger.error(HspODLMsgs.ERR_TERMINATING_ESSBASE,e );
            }
        }
    }
    *//**
     * This method checks if the Cube Create has been run for this application.
     *//*
    
    private boolean isCubeCreated() throws EssbaseException
    {
        boolean foundMatch = false;

        String[][] pApps = new String[2][];
        int handle = HspEssbaseMainAPI.EssInit();
        int hCtx = HspEssbaseMainAPI.EssLogin(handle, essServer, essUserName, essPassword, pApps);
        HspEssbaseMainAPI.EssLogout(hCtx);
        HspEssbaseMainAPI.EssTerm(handle);
        String[] apps = pApps[0];
        String[] dbs = pApps[1];

        if ((apps != null) && (dbs != null))
        {
            for (int loop1 = 0;loop1< apps.length;loop1++)
            {
                if ((essDefaultApp.equalsIgnoreCase(apps[loop1]))&&(dbs[loop1]!=null))
                {	foundMatch = true;
                    break;
                }
            }
        }
        return foundMatch;

    } 
    
    public String getEssDefaultDB()
    {
        return essDefaultDB;
    }
    
    public void finalize()
    {	try
        {	shutDown();
        } catch (Exception e) {
            logger.error(HspODLMsgs.ERR_OLAP_CONNECTION_SHUTDOWN, e);
            }
    }
*/
}