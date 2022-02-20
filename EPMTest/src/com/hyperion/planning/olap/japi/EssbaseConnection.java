package com.hyperion.planning.olap.japi;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssBaseObject;
import com.essbase.api.base.IEssExtendedObject;
import com.essbase.api.base.IEssIterator;
import com.essbase.api.dataquery.IEssCubeView;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapApplication;
import com.essbase.api.datasource.IEssOlapFileObject;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.domain.IEssDomain;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.metadata.IEssMemberSelection;
import com.essbase.api.session.IEssbase;

import com.hyperion.planning.odl.HspODLLogger;

import java.io.File;

import java.io.FileOutputStream;
import java.io.IOException;

import java.io.OutputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.Validate;


/**
 * This class represents a connection to Essbase for a specific user.
 */
public class EssbaseConnection {
    
    private static final HspODLLogger logger = HspODLLogger.getLogger();
    private final String userName;
    private final String password;
    private final boolean passwordIsToken;
    private final String userNameAs;
    private final String olapServerName;
    private final String appName;
    private final String cubeName;
    
    private IEssbase ess = null;
    private IEssOlapServer olapServer = null;
    private IEssDomain dom = null;

    /**
     * Constructs a connection to Essbase with the specified parameters.
     *
     * @param userName The user name. Can be null if password is cssToken and the passwordIsToken flag is true.
     * @param password The user password. Cannot be null. If the passwordIsToken flag is true, this represents the cssToken string
     * @param passwordIsToken A boolean indicating whether the password is cssToken string.
     * @param userNameAs The user name you want to impersonate. If null, no impersonation occurs.
     * @param olapServerName The host name where the analytic server is running.
     * @param appName The name of the Essbase application this connection is bound to
     * @param cubeName The name of the Essbase cube this connection is bound to
     */
    public EssbaseConnection(String userName, String password, boolean passwordIsToken, String userNameAs, String olapServerName, String appName, String cubeName) {
        super();
        this.userName = userName;
        this.password = password;
        this.passwordIsToken = passwordIsToken;
        this.userNameAs = userNameAs;
        this.olapServerName = olapServerName;
        this.appName = appName;
        this.cubeName = cubeName;
    }
    
    public EssbaseConnection(String userName, String password, String olapServerName, String appName, String cubeName, long storageType) {
        super();
        this.userName = userName;
        this.password = password;
        this.passwordIsToken = false;
        this.userNameAs = null;
        this.olapServerName = olapServerName;
        this.appName = appName;
        this.cubeName = cubeName;
    }
    
    public EssbaseConnection(String userName, String password, String olapServerName, String appName, boolean isDefaultStorage) {
        super();
        this.userName = userName;
        this.password = password;
        this.passwordIsToken = false;
        this.userNameAs = null;
        this.olapServerName = olapServerName;
        this.appName = appName;
        this.cubeName = null;
    }

    /**
     * Returns the user name that this connection is for, or null if this is
     * a system user.
     * @return the user name or null if this is a system connection
     */
    public String getEffectiveUserName() {
        return userNameAs;
    }

    /**
     * Retruns true if connected, otherwise false.
     * @return true if connected, otherwise false
     */
    public boolean isConnected() {
        try {
            return olapServer != null && olapServer.isConnected();
        } catch (EssException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * Connects to Essbase.  If this connecton is already connected, this method
     * is a no-op.  In general, this method should only be called by the
     * framework classes and should not be called by application code.
     * @throws RuntimeException if connect fails.
     */
    public void connect() {
        if (!isConnected()) {
            try {
                ess = IEssbase.Home.create(IEssbase.JAPI_VERSION);
    
                // Sign On to the Provider
                dom = ess.signOn(userName, password, passwordIsToken, userNameAs, "embedded");
    
                // Open connection with OLAP server and get the cube.
                olapServer = dom.getOlapServer(olapServerName);
                olapServer.connect();
    
            } catch (EssException e) {
                // Nativecode(1051012) = User not found.
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * Disconnects from Essbase.  In general, this method should only be called
     * by the framework classes and should not be called application code.
     * Application code should instead return the connection to the
     * connection pool when they are done with it.
     */
    public void disconnect() {
        disconnectOlapServer();
        signOffEssbase();
    }
    
    private void disconnectOlapServer() {
        try {
            if (olapServer != null && olapServer.isConnected() == true) {
                olapServer.disconnect();
                olapServer = null;
            }
        } catch (EssException e) {
            logger.warn("Unable to close Essbase server connection.", e);
        }
    }
    
    private void signOffEssbase() {
        try {
            if (ess != null && ess.isSignedOn() == true) {
                ess.signOff();
                ess = null;
            }
        } catch (EssException e) {
            logger.warn("Unable to sign off Essbase server connection.", e);
        }

    }

    /**
     * Resets a connection so that all state is the same as if the connection
     * was disconnected.  The main purpose of this method is to ensure that
     * a reset connection acts the same as a brand new connection.
     */
    public void reset() {
        // TODO: If the olap server is not disconnected, then deploys fail when they get a previously used connection from the pool. Is there a more performant way to do this? Look at this when implememting security filter refresh.
        // disconnectOlapServer();
        disconnect();
    }

    /**
     * Verifies that the essbase connection is connected.
     * 
     * @throws IllegalStateException if the connection is not connected.
     */
    public void verifyConnected() {
        if (!isConnected())
            throw new IllegalStateException("Connection is disconnected.");
    }

    /**
     * Returns the name of the Essbase application for this connection.
     * @return the name of the Essbase application for this connection
     */
    public String getAppName() {
        return appName;
    }

    /**
     * Returns the name of the Essbase cube for this connection.
     * @return the name of the Essbase cube for this connection
     */
    public String getCubeName() {
        return cubeName;
    }

    /**
     * Returns the domain that this connection is connected to.
     * @return the domain that this connection is connected to.
     */
    public IEssDomain getDomain() {
        return dom;
    }

    /**
     * Returns the olap server that this connection is connected to.
     * @return the olap server that this connection is connected to.
     */
    public IEssOlapServer getOlapServer() {
        return olapServer;
    }

    /**
     * Returns the application object associated with this connection or null if
     * it does not exist.
     * @return the application object associated with this connection or null if
     *         it does not exist
     */
    public IEssOlapApplication getApp() {
        return getApp(appName);
    }
    
    /**
     * Returns the application object or null if it is not found.
     * @param appName the name of the application.
     * @return the application object or null if it is not found
     */
    public IEssOlapApplication getApp(String appName) {
        verifyConnected();
        IEssOlapApplication app = null;
        if (appName != null) {
            // Lookup the application
            try {
                olapServer.clearActive();
                IEssIterator appIterator = olapServer.getApplications();
                app = EssbaseConnection.<IEssOlapApplication>getNamedObject(appIterator.getAll(), appName);
                if (app != null)
                    app = olapServer.getApplication(app.getName());
            } catch (EssException e) {
                throw new RuntimeException(e);
            }
        }
        return app;
    }
    
    /**
     * Returns the cube object associated with this connection or null if
     * it does not exist.
     * @return the cube object associated with this connection or null if
     *         it does not exist
     */
    public IEssCube getCube() {
        IEssOlapApplication app = getApp(appName);
        return app == null ? null : getCube(app, cubeName);
    }

    /**
     * Returns the cube object or null if it or the parent app is not found.
     * @param appName the name of the application that contains the cube
     * @param cubeName the name of the cube
     * @return the cube object or null if it is not found
     */
    public IEssCube getCube(String appName, String cubeName) {
        IEssOlapApplication app = getApp(appName);
        return app == null ? null : getCube(app, cubeName);
    }
    
    /**
     * Returns the cube object or null if it is not found.
     * @param app the application that contains the cube.
     * @param cubeName the name of the cube.
     * @return the cube object or null if it is not found
     */
    public IEssCube getCube(IEssOlapApplication app, String cubeName) {
        verifyConnected();
        IEssCube cube = null;
        if (app != null && cubeName != null) {
            // Lookup the application
            try {
                IEssIterator cubeIterator = app.getCubes();
                cube = EssbaseConnection.<IEssCube>getNamedObject(cubeIterator.getAll(), cubeName);
                if (cube != null)
                    cube = app.getCube(cube.getName());
            } catch (EssException e) {
                throw new RuntimeException(e);
            }
        }
        return cube;
    }

    /**
     * Disconnects all users that are using the specified application and cube.
     * If the application is null, all users are disconnected.
     * If the application is not null and the cube is null, then all connections
     * to the application are disconnected.
     * If the application and the cube are not null, then all connections to
     * the specified cube in the specified applicaiton are disconnected.
     * 
     * @param applicationName the name of the application
     * @param cubeName the name of the cube
     * @throws EssException
     */
    public void disconnectUsers(String applicationName, String cubeName) throws EssException {
        verifyConnected();
        IEssOlapServer olapServer = getOlapServer();        
        IEssIterator connections = olapServer.getConnections();
        for (int i = 0; i < connections.getCount(); i++) {
            IEssOlapServer.IEssOlapConnectionInfo connection = (IEssOlapServer.IEssOlapConnectionInfo)connections.getAt(i);
            // If the connection matches the filter, call loggoffUser.
            if (connectionMatchesFilter(connection, applicationName, cubeName))
                connection.logoffUser();
        }
    }

    private boolean connectionMatchesFilter(IEssOlapServer.IEssOlapConnectionInfo connection, String applicationName, String cubeName) throws EssException {
        boolean matches;
        // If applicationName is null, all connections are considered a match
        if (applicationName == null)
            matches = true;
        else if (applicationName.equalsIgnoreCase(connection.getConnectedApplicationName())) {
            // If cubeName is null, all connections that match the appName
            // are considered a match.  If it is non null, then the connection
            // is considered a match if the appName and cubeName match.
            matches = (cubeName == null || cubeName.equalsIgnoreCase(connection.getConnectedCubeName()));
        } else
            matches = false;
        
        return matches;
    }

    /**
     * Unlocks all objects of the type specified in the given cube.
     * To unlock all objects, use IEssOlapFileObject.TYPE_ALL.
     * 
     * @param essCube the essbase cube to unlock the objects in.
     * @param objType an object type from: IEssOlapFileObject.TYPE_*.
     * @throws EssException
     * @see IEssOlapFileObject
     */
    public static void unlockOlapObjects(IEssCube essCube, int objType) throws EssException {
        Validate.notNull(essCube, "Essbase cube is null.");
        IEssIterator objectIterator = essCube.getOlapFileObjects(objType);
        for (int i = 0; i < objectIterator.getCount(); i++) {
            IEssOlapFileObject fileObject = (IEssOlapFileObject) objectIterator.getAt(i);
            if (fileObject.isLocked())
                essCube.unlockOlapFileObject(fileObject.getType(), fileObject.getName());
        }
    }
    /**
     * Gets an object by name from a list of base objects using a case
     * insensitive compare.  The object must extend {@code IEssExtendedObject}.
     * 
     * @param <T> the type of the objects
     * @param objects an array of objects
     * @param name the name of the object to find
     * @return the object or null if it is not found
     * @throws RuntimeException if an error occurs
     */
    public static <T extends IEssExtendedObject> T getNamedObject(IEssBaseObject[] objects, String name) {
        try {
            if (objects != null) {
                for (IEssBaseObject object : objects) {
                    T extendedObject = (T) object;
                    if (extendedObject != null && name.equalsIgnoreCase(extendedObject.getName()))
                        return extendedObject;
                }
            }
        } catch (EssException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

    /**
     * Deletes the specified object on the server or returns false if the
     * object is not found.
     * 
     * @param essCube The essbase cube to delete the object from.
     * @param objType The object type. Value from IEssOlapFileObject.TYPE_*.
     * @param objName The object name.
     * @return true if 1 or more objects are deleted, otherwise false.
     * @throws EssException
     */
    public static boolean deleteServerObject(IEssCube essCube, int objType, String objName) throws EssException {
        Validate.notNull(essCube, "Essbase cube is null.");
        boolean deleted = false;
        IEssIterator files = essCube.getOlapFileObjects(objType);
        if (files != null) {
            IEssBaseObject[] objects = files.getAll();
            if (objects != null) {
                for (IEssBaseObject essobj : objects) {
                    if (((IEssOlapFileObject)essobj).getName().equals(objName)) {
                        essCube.deleteOlapFileObject(objType, objName);
                        deleted = true;
                    }
                }
            }
        }
        return deleted;
    }

    /**
     * Copies an object from a client file to the olap server, and optionally
     * unlocks the server object to allow updates by others. If the object
     * already exists, it is replaced by the new file object. Example: <p>
     *
     * {@code copyOlapFileObjectToServer(IEssOlapFileObject.TYPE_TEXT, "Test", "C:/temp/test.txt", false);}
     *
     * <p>In case of copying a IEssOlapFileObject.TYPE_OUTLINE type object, the
     * outline file will be copied as a ".otn" file on the server representing
     * it as an intermediate Outline file. If this object name is same as the
     * existing cube (or outline) name, then, performing a restructure operation
     * on this cube will replace the .otn file as the outline file.
     *
     * @param essCube The essbase cube to copy the file to.
     * @param objType The object type. Value from IEssOlapFileObject.TYPE_*.
     * @param objName The object name.
     * @param clientFileName The full path of source file in the client.
     * @param unlock Flag to control object locking. If true, the server object is unlocked.
     * @throws EssException
     */
    public static void copyFileAndOverwrite(IEssCube essCube, int objType, String objName, String clientFileName, boolean unlock) throws EssException {
        deleteServerObject(essCube, objType, objName);
        essCube.copyOlapFileObjectToServer(objType, objName, clientFileName, unlock);
    }
    
    /**
     * Copies an object in the form of byte sequence to the olap server, and
     * optionally unlocks the server object to allow updates by others. 
     * <p>
     * This version no longer delegates to the byte[] version of the Essbase
     * Java API.  Instead, this version creates a temporary file, copies
     * the content into this file and then subcalls
     * {@link #copyFileAndOverwrite(IEssCube essCube, int objType, String objName, String clientFileName, boolean unlock)}.
     * <p>
     * The temporary file is then deleted once the operation is complete.
     * <p>
     * This change was made due to the fact that the default data directory in
     * the Essbase Java API is a shared read only file system when deployed
     * on the Oracle Public Cloud.
     *
     * @param essCube The essbase cube to copy the file to.
     * @param objType The object type. Value from IEssOlapFileObject.TYPE_*.
     * @param objName The object name.
     * @param content The byte sequence of the object in the client.
     * @param unlock Flag to control object locking. If true, the server object is unlocked.
     * @throws EssException
     * 
     * @see #copyFileAndOverwrite(IEssCube essCube, int objType, String objName, String clientFileName, boolean unlock)
     */
    public static void copyFileAndOverwrite(IEssCube essCube, int objType, String objName, byte[] content, boolean unlock) throws EssException {
        File rulesFile = null;
        try {
            rulesFile = File.createTempFile("import_rule_file_", ".rul");
            OutputStream out = new FileOutputStream(rulesFile);
            try {
                out.write(content);
                out.flush();
            } finally {
                IOUtils.closeQuietly(out);
            }
            copyFileAndOverwrite(essCube, objType, objName, rulesFile.getCanonicalPath(), unlock);
        } catch (IOException e) {
            throw new RuntimeException("Error writing the import data rules file to a temporary file.", e);
        } finally {
            if (rulesFile != null)
                rulesFile.delete();
        }
    }

    /**
     * This convenience method can be used to shield the caller from cumbersome
     * null checks when dealing with methods that return IEssIterator objects.
     * <p>
     * For example, to get a list of all members in a member selection, or
     * an empty non null list if there are no members, use the following code:
     * <p>
     * {@code IEssBaseObject[] essMemberObjects = EssbaseConnection.getObjects(essMemberSelection.getMembers());}
     * 
     * @param essIterator the iterator to retrieve the object from, or null
     * @return the list of objects in the iterator, or an empty list if the
     *         iterator is null or if the array it returns is null.
     * @throws EssException
     */
    public static IEssBaseObject[] getObjects(IEssIterator essIterator) throws EssException {
        IEssBaseObject[] objects = essIterator == null ? null : essIterator.getAll();
        return objects == null ? new IEssBaseObject[0] : objects;        
    }
    
    /**
     * Unconditionally close an IEssMemberSelection.
     * <p>
     * Equivalent to memberSelection.close(), except any exceptions will be
     * ignored.  This is typically used in finally blocks.
     * 
     * @param memberSelection the memberSelection to close, may be null or
     *        already closed
     */
    public static void closeQuietly(IEssMemberSelection memberSelection) {
        if (memberSelection != null) {
            try {
                memberSelection.close();
            } catch (EssException e) {
                logger.warn("Unable to close memberSelection.", e);
            }
        }
    }
    
    /**
     * Unconditionally close an IEssCubeView.
     * <p>
     * Equivalent to cubeView.close(), except any exceptions will be
     * ignored.  This is typically used in finally blocks.
     * 
     * @param cubeView the cubeView to close, may be null or
     *        already closed
     */
    public static void closeQuietly(IEssCubeView cubeView) {
        if (cubeView != null) {
            try {
                cubeView.close();
            } catch (EssException e) {
                logger.warn("Unable to close cubeView.", e);
            }
        }
    }
    
    /**
     * Unconditionally close an IEssCubeOutline.
     * <p>
     * Equivalent to otl.close(), except any exceptions will be
     * ignored.  This is typically used in finally blocks.
     * 
     * @param otl the cubeOutline to close, may be null or
     *        already closed
     */
    public static void closeQuietly(IEssCubeOutline otl) {
        if (otl != null) {
            try {
                if (otl.isOpen())
                    otl.close();
            } catch (EssException e) {
                logger.warn("Unable to close cube outline.", e);
            }
        }
    }

    /**
     * Converts the EssException to a UserException if it is a recognized
     * generic exception.  If not, then the original EssbaseException is
     * returned.
     * 
     * @param e the EssException to be converted
     * @return a UserException or EssException depending on the input value
     */
    public static Exception convertKnownException(EssException e) {
        if (e != null) {
            // Cannot open cube outline. Essbase Error(1053010): Object [?] is already locked by user [?]
            if (e.getNativeCode() == 1053010)
                // TODO: Localize The deployed model is locked by another user.
                return new RuntimeException("The cube is locked by another user.", e);
            // Cannot begin incremental build. Essbase Error(1013132): Cannot build dimensions. There are other active users on database [?]
            else if (e.getNativeCode() == 1013132)
                // TODO: Localize The deployed model is in use by another user.
                return new RuntimeException(" The cube is in use by another user.", e);
        }
        return e;
    }

    /**
     * Gets the API version of JAPI runtime that is used for connecting to Essbase 
     * specified by this connection's host and credendtials.
     * 
     * @return the JAPI version as a string.
     * @throws EssException the exception thrown by the underlying Essbase server.
     */
    protected String getEssbaseServerVersion() throws EssException {
        connect();
        return olapServer.getOlapServerVersion();
    }
}
