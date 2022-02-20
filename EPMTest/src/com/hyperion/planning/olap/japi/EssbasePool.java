package com.hyperion.planning.olap.japi;

import com.hyperion.planning.odl.HspODLLogger;
import com.hyperion.planning.odl.HspODLMsgs;
import com.hyperion.planning.olap.EssbaseException;
import com.hyperion.planning.HspRuntimeException;
import com.hyperion.planning.odl.HspLogComponent;
import java.util.Properties;
import java.util.*;

/**
 * EssbasePool is a concrete class for creating Essbase Connection pool
 * Based on HspPool platform.
 */
public final class EssbasePool extends com.hyperion.planning.HspPool
{
    private final static HspODLLogger logger = HspODLLogger.getLogger(HspLogComponent.OLAP);
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

    private final class EssbaseGridPool extends HspPoolNew
    {
        private boolean valid = false;

        private String dbName;
        private long storageType;

        /**
         * EssbasePool will initialize HspPool and Essabase parameters. Then create
         * Essabase connections and add then into pool vector.
         */
        EssbaseGridPool(String dbName, long storageType)
        {
            super(EssbasePool.this.getMinSize(), EssbasePool.this.getMaxSize(), EssbasePool.this.getMaxWait());
            if (dbName == null) throw new IllegalArgumentException("Essbase DB cannot be null");
            this.dbName  = dbName;
            this.storageType=storageType;
            init();
        }
        EssbaseConnection getConnection() throws Exception
        {
            EssbaseConnection connection = null;
            try {
                connection = (EssbaseConnection) getObject();
            }
            catch (EssbaseCubeNotCreatedException ce)
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
                //throw new NoAvailableOlapConnectionsException();
            	throw new Exception("Olap Connection is not available");
            return connection;
        }

        /**
         * Initialize Essbase and get the handle.
         */
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

        /**
         * Shutdown Essbase.
         */
        protected synchronized void stopPool()
        {	if (!valid)
                return;
            this.shutDown();
            valid = false;
        }

        /**
         * Create Essbase connection and add it into pool vector.
         */
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
                    final EssbaseConnection o = new EssbaseConnection(server, userName, password, appName, dbName,storageType);
                    addObject(o);
                } 
/*                catch (EssbaseException e) {
                    valid = false;
                   // long result = e.getResultCode();
                    //if (result==ESSBASE_IS_NOT_RUNNING)
                    //{
                    //	this.deleteAllObjects();
                    //}
                    e.printStackTrace(System.err);
                    logger.error(HspODLMsgs.ERR_UNABLE_TO_CONNECT, e, dbName);
                } */
                catch (Exception e2) {
                      valid = false;
                      logger.finer(e2);
                }
            }
        }

        /**
         * Close Essbase connection. If it is the last connection, shutdown Essbase.
         */
        protected synchronized void deleteObject(Object o)
        {
            if (getPoolSize() == 0)
            {
                shutDown();
            }
            else
            {
                EssbaseConnection ess = (EssbaseConnection) o;
                ess.disconnect();
            }
        }

        /**
         * This modifies getObject on HspPool to check for a created cube if getObject fails,
         * and replaces the error with an EssbaseCubeNotCreatedException if the cube isnt created.
         */
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
                    throw new EssbaseCubeNotCreatedException();
                else //throw original exception
                    throw e;
            }
            return retVal;
        }
        /**
         * Shutdown Essbase.
         */
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
        /**
         * This method checks if the Cube Create has been run for this application.
         */
        
/*        private boolean isCubeCreated() throws EssbaseException
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
        } */
        
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
    private Hashtable<Integer,EssbaseConnection> jobIdToEssConnTbl = new Hashtable<Integer,EssbaseConnection>();

  public EssbasePool(int min, int max, int wait, String server, String userName, String password, String appName){
      this(min, max, wait, server, userName, password, appName, 2); // let's this be default
  }
    public EssbasePool(int min, int max, int wait, String server, String userName, String password, String appName, long storageType)
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
    public EssbaseConnection getOutlConnection(String dbName) throws Exception{
        synchronized (syncObject) {
            syncObject.setOutlineThreadWaitOrWorks(true);
            while (true) {
                if (threadsAreWorking() || ((EssbaseGridPool)getPool(dbName)).threadsAreWorking()) {
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
    public boolean essMAXLSessionCreate( EssbaseConnection connection) throws EssbaseException {
        logger.entering();
        logger.fine("MAXL creating MAXL session for connection." );
        //return connection.essMAXLSessionCreate(server, userName, password);
        return false;
    }
    public void releaseOutlConnection(EssbaseConnection connection) {
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

    public EssbaseConnection getConnection() throws Exception {
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
    public EssbaseConnection getConnectionForJobId(Integer jobId) throws Exception {
       EssbaseConnection connection = getConnection();
       if(connection!=null && jobId!=null && jobId.intValue()!=-1){
          logger.finer("associating connection {0} with jobId {1} ", connection, jobId);
          jobIdToEssConnTbl.put(jobId,connection);
       }
       return connection;
    }
    public void releaseConnectionForJobId(EssbaseConnection connection, Integer jobId)  {
       if(connection!=null && jobId!=null && jobId.intValue()!=-1){
          logger.finer("disassociating connection {0} with jobId {1} ", connection, jobId);
          jobIdToEssConnTbl.remove(jobId);
          //connection.setPendingKillRequest(false);
       }
       releaseConnectionTwo(connection);
    }
    public EssbaseConnection getConnectionInUseForJobId(int jobId){
        return jobIdToEssConnTbl.get(jobId);
    }
    public void releaseConnectionTwo(EssbaseConnection connection) {
        logger.entering();
        logger.finer("Thread  " + Thread.currentThread().getName() + " releasing connection " + connection);
        releaseConnectionInternal(connection);
        synchronized (syncObject) {
            syncObject.notifyAll();
        }
        logger.exiting();
    }
     public EssbaseConnection getConnectionInternal() throws Exception
    {
        logger.entering();
        EssbaseConnection connection = null;
        try {
            connection = (EssbaseConnection) getObject();
        }
        catch (EssbaseCubeNotCreatedException ce)
            {
                logger.error(HspODLMsgs.ERR_OLAP_CONN_NOT_GETTING,ce );
                throw ce;
            }
            catch (Exception e)
            {
                logger.error(HspODLMsgs.ERR_OLAP_CONN_NOT_GETTING,e);
            }
        if (connection == null)
            //throw new NoAvailableOlapConnectionsException();
        	throw new Exception("No OLAP connection available.");
        logger.finer("Thread  " + Thread.currentThread().getName() + " acquired connection " + connection);
        logger.exiting();
        return connection;
    }
    public EssbaseConnection getConnection(String dbName) throws Exception {
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
    public void releaseConnectionOne(EssbaseConnection connection)
    {
        logger.entering();
        if (!connection.isConnected())
        {
            try
            {
                //connection.connect(server, userName, password, connection.getAppName(), connection.getCubeName());
            	connection.connect();
            } finally{
            	
            }
/*            catch(EssbaseException e)
            {
                if (logger.isLogFineEnabled())
                {
                    logger.fine("Unable to restore connection to Essbase. Recycling pool");
                    logger.fine(e);
                }
                getPool(connection.getCubeName()).resetPool();
            }*/
        }
        getPool(connection.getCubeName()).releaseObject(connection);
        synchronized (syncObject)
        {
            syncObject.notifyAll();
        }
        logger.exiting();
    }
    protected void releaseConnectionInternal(EssbaseConnection connection)
    {
        logger.finer("Thread  " + Thread.currentThread().getName() + " released connection " + connection);
        if (!connection.isConnected())
        {
            resetPool();
            try
            {
               // connection.connect(server, userName, password, appName, storageType==EssbaseConnection.ESS_APP_DEFAULT_STORAGE?false:true);
            	connection.connect();
            }finally{
            	
            }
            /*catch (EssbaseException e)
            {
                logger.fine(e);
                deleteObject(connection);
                return;
            }*/
        }
        super.releaseObject(connection);
    }
    private final synchronized EssbaseGridPool getPool(String dbName)
    {
        EssbaseGridPool hspEssbaseGridPool;
        hspEssbaseGridPool = (EssbaseGridPool)gridPool.get(dbName);
        if (hspEssbaseGridPool == null)
        {
            hspEssbaseGridPool = new EssbaseGridPool(dbName,storageType);
            if (hspEssbaseGridPool != null)
                gridPool.put(dbName, hspEssbaseGridPool);
        }
        return hspEssbaseGridPool;
    }
    /**
     * Initialize Essbase and get the handle.
     */
    protected synchronized boolean startPool()
    {	if (valid)
            return valid;
        try {
//			handle = HspEssbaseGridAPI.EssGInit();	//Will throw an HspOlapException if it fails
//			hMainAPIInstance=HspEssbaseMainAPI.EssInit(); // B.S. we need it too
            valid = true;
        } catch (Exception e) {
            logger.fine("Can not Validate OLAP: ",e);
        }
        return valid;
    }

    /**
     * Shutdown Essbase.
     */
    protected synchronized void stopPool()
    {	if (!valid)
            return;
        this.shutDown();
        valid = false;
    }
    /**
     * Create Essbase connection and add it into pool vector.
     */
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
                //final EssbaseConnection o = new EssbaseConnection(server, userName, password, appName, storageType==EssbaseConnection.ESS_APP_DEFAULT_STORAGE?false:true);
            	final EssbaseConnection o = new EssbaseConnection(server, userName, password, appName, true);
                addObject(o);
            } 
/*            catch (EssbaseException e) {
                valid = false;
                //long result = e.getResultCode();
                //if (result==ESSBASE_IS_NOT_RUNNING)
                //{
                //	this.deleteAllObjects();
                //}
                logger.error(HspODLMsgs.ERR_OLAP_CONN_NOT_GETTING,e );
            }*/
            catch (Exception e2) {
                  valid = false;
                  logger.fine(e2);
            }
        }
        logger.exiting();
    }

    /**
     * Close Essbase connection. If it is the last connection, shutdown Essbase.
     */
    protected synchronized void deleteObject(Object o)
    {
        if (getPoolSize() == 0)
        {
            shutDown();
        }
        else
        {
            EssbaseConnection ess = (EssbaseConnection) o;
            ess.disconnect();
        }
    }

    /**
     * This modifies getObject on HspPool to check for a created cube if getObject fails,
     * and replaces the error with an EssbaseCubeNotCreatedException if the cube isnt created.
     */
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
                throw new EssbaseCubeNotCreatedException();
            else //throw original exception
                throw e;
        }
        return retVal;
    }
    /**
     * Shutdown Essbase.
     */
    private synchronized void shutDown()
    {	if (valid)
        {	valid = false;
            try
            {
                Collection col = gridPool.values();
                if (col != null)
                {
                    Iterator it = col.iterator();
                    EssbaseGridPool hspEssbaseGridPool;
                    while (it.hasNext())
                    {
                        hspEssbaseGridPool = (EssbaseGridPool)it.next();
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
    /**
     * This method checks if the Cube Create has been run for this application.
     */
    /*
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

    } */
    /*
    public String getEssDefaultDB()
    {
        return essDefaultDB;
    }
    */
    public void finalize()
    {	try
        {	shutDown();
        } catch (Exception e) {
            logger.error(HspODLMsgs.ERR_OLAP_CONNECTION_SHUTDOWN, e);
            }
    }

}