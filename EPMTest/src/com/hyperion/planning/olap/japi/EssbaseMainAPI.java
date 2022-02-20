package com.hyperion.planning.olap.japi;


import static com.hyperion.planning.olap.japi.EssbaseAPIHelper.doExceptionTranslation;
import static com.hyperion.planning.olap.japi.EssbaseAPIHelper.validateArguments;
import static com.hyperion.planning.olap.japi.EssbaseAPIHelper.validateEssbaseConnection;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssBaseObject;
import com.essbase.api.base.IEssIterator;
import com.essbase.api.datasource.EssBuildDimDataLoadState;
import com.essbase.api.datasource.EssCalcClientParams;
import com.essbase.api.datasource.EssCube;
import com.essbase.api.datasource.EssOlapApplication;
import com.essbase.api.datasource.EssSecurityFilter;
import com.essbase.api.datasource.IEssCalcList;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssCube.EEssCubeAccess;
import com.essbase.api.datasource.IEssCube.IEssSecurityFilter;
import com.essbase.api.datasource.IEssMaxlResultSet;
import com.essbase.api.datasource.IEssMaxlSession;
import com.essbase.api.datasource.IEssOlapApplication;
import com.essbase.api.datasource.IEssOlapFileObject;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.datasource.IEssOlapUser;
import com.essbase.api.datasource.IEssPerformAllocation;
import com.essbase.api.datasource.IEssPerformCustomCalc;
import com.essbase.api.domain.IEssPrivilege;
import com.essbase.api.session.IEssbase;
import com.hyperion.planning.HspUtils;
import com.hyperion.planning.odl.HspLogComponent;
import com.hyperion.planning.odl.HspODLLogger;
import com.hyperion.planning.olap.EssbaseException;
import com.hyperion.planning.olap.HspEssApplication;
import com.hyperion.planning.olap.HspEssCube;
import com.hyperion.planning.olap.HspEssDBStats;
import com.hyperion.planning.olap.HspEssVariable;
import com.hyperion.planning.olap.HspMDXDataQueryBuilder;
import com.hyperion.planning.olap.HspMbrError;
import com.hyperion.planning.utils.HspEssAppStats;


public final class EssbaseMainAPI {

    private static final HspODLLogger logger = HspODLLogger.getLogger(HspLogComponent.OLAP);

    private static Boolean containsSMStatsMethod = null;
    private static Method handleSMStatsMethod = null;
    private static Method handleSMStatKeyWordMethod = null;
    private static Method handleSMStatValueMethod = null;

    private EssbaseMainAPI() {
    }

    public static void clearActive(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                olapServer.clearActive();
            }
        } catch (Exception e) {
            logger.warn("Exception in clearActive of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void setActive(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                olapServer.setActive(application, cube);
            }
        } catch (Exception e) {
            logger.warn("Exception in setActive of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static String getEssbaseServerVersion(final EssbaseConnection connection) throws EssbaseException {
        logger.entering(connection);
        validateArguments(connection);
        String serverVersion = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                serverVersion = olapServer.getOlapServerVersion();
            }
        } catch (Exception e) {
            logger.warn("Exception in getEssbaseServerVersion of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return serverVersion;
    }

    public static String getAPIVersion() {
        return IEssbase.JAPI_VERSION;
    }

    public static IEssOlapApplication createApplication(final EssbaseConnection connection, final String application, final boolean bUnicode) throws EssbaseException {
        logger.entering(connection, application, bUnicode);
        validateArguments(connection, application);
        IEssOlapApplication createdApp = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                createdApp = olapServer.createApplication(application, (bUnicode ? APP_TYPE_UTF8 : APP_TYPE_NATIVE));
            }
        } catch (Exception e) {
            logger.warn("Exception in createApplication of HspEssbaseMainJAPIImpl for application " + application + " and bUnicode " + bUnicode + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return createdApp;
    }

    public static IEssOlapApplication createStorageTypeApplicationEx(final EssbaseConnection connection, final String application, final int storageType, final boolean bUnicode) throws EssbaseException {
        logger.entering(connection, application, storageType, bUnicode);
        validateArguments(connection, application);
        IEssOlapApplication createdApp = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                createdApp = olapServer.createApplication(application, ((storageType == ASO_STORAGE_TYPE) ? ASO_STORAGE_TYPE_SHORT : DEF_STORGAE_TYPE), (bUnicode ? APP_TYPE_UTF8 : APP_TYPE_NATIVE));
            }
        } catch (Exception e) {
            logger.warn("Exception in createApplication of HspEssbaseMainJAPIImpl for application " + application + " storageType " + storageType + " and bUnicode " + bUnicode + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return createdApp;
    }

    public static void deleteApplication(final EssbaseConnection connection, final String application) throws EssbaseException {
        logger.entering(connection, application);
        validateArguments(connection, application);
        IEssOlapApplication existingApp = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    existingApp.delete();
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in deleteApplication of HspEssbaseMainJAPIImpl for application " + application + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void renameApplication(final EssbaseConnection connection, final String oldApplicationName, final String newApplicationName) throws EssbaseException {
        logger.entering(connection, oldApplicationName, newApplicationName);
        validateArguments(connection, oldApplicationName);
        validateArguments(newApplicationName, NEW_APP_NAME_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(oldApplicationName);
                if (null != existingApp) {
                    existingApp.rename(newApplicationName);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in renameApplication of HspEssbaseMainJAPIImpl for existing application " + oldApplicationName + " to new name " + newApplicationName + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void lockObject(final EssbaseConnection connection, final String application, final String cube, final String objName, final int objType) throws EssbaseException {
        logger.entering(connection, application, cube, objName, objType);
        validateArguments(connection, application, cube);
        validateArguments(objName, OBJ_NAME_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.lockOlapFileObject(objType, objName);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in lockObject of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " objName " + objName + " and objType " + objType + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void unlockObject(final EssbaseConnection connection, final String application, final String cube, final String objName, final int objType) throws EssbaseException {
        logger.entering(connection, application, cube, objName, objType);
        validateArguments(connection, application, cube);
        validateArguments(objName, OBJ_NAME_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.unlockOlapFileObject(objType, objName);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in unlockObject of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " objName " + objName + " and objType " + objType + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void unloadDatabase(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.stop();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in unloadDatabase of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static IEssCube createDatabaseEx(final EssbaseConnection connection, final String application, final String cube, final int dbType, final boolean nonUniqueNames) throws EssbaseException {
        logger.entering(connection, application, cube, dbType, nonUniqueNames);
        validateArguments(connection, application, cube);
        IEssCube createdCube = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    createdCube = existingApp.createCube(cube, IEssCube.EEssCubeType.sm_fromInt(dbType), nonUniqueNames);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in createDatabase of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " of dbType " + dbType + " with nonUniqueNames as " + nonUniqueNames + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return createdCube;
    }

    public static void deleteDatabase(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.delete();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in deleteDatabase of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void renameDatabase(final EssbaseConnection connection, final String application, final String oldNameOfCube, final String newNameOfCube) throws EssbaseException {
        logger.entering(connection, application, oldNameOfCube);
        validateArguments(connection, application, oldNameOfCube);
        validateArguments(newNameOfCube, NEW_NAME_CUBE_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(oldNameOfCube);
                    if (null != existingCube) {
                        existingCube.rename(newNameOfCube);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in renameDatabase of HspEssbaseMainJAPIImpl for application " + application + " with old cube name " + oldNameOfCube + " and new cube name" + newNameOfCube + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static String getAppType(final EssbaseConnection connection, final String application) throws EssbaseException {
        logger.entering(connection, application);
        validateArguments(connection, application);
        IEssOlapApplication existingApp = null;
        String appType = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    appType = existingApp.getType();
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getAppType of HspEssbaseMainJAPIImpl for application " + application + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return appType;
    }

    public static int getAppStorageInfo(final EssbaseConnection connection, final String application) throws EssbaseException {
        logger.entering(connection, application);
        validateArguments(connection, application);
        IEssOlapApplication existingApp = null;
        int storageType = -1;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    storageType = existingApp.getDataStorageType().intValue();
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getAppStorageInfo of HspEssbaseMainJAPIImpl for application " + application + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return storageType;
    }

    public static String getServerLocaleString(final EssbaseConnection connection) throws EssbaseException {
        logger.entering(connection);
        validateArguments(connection);
        String serverLocale = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                serverLocale = olapServer.getLocale();
            }
        } catch (Exception e) {
            logger.warn("Exception in getServerLocaleString of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return serverLocale;
    }

    public static IEssOlapFileObject getOlapFileObjectDetails(final EssbaseConnection connection, final String application, final String cube, final int objType, final String objName) throws EssbaseException {
        logger.entering(connection, application, cube, objType, objName);
        validateArguments(connection, application);
        validateArguments(objName, OBJ_NAME_NULL);
        IEssOlapFileObject fileObj = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                fileObj = olapServer.getOlapFileObject(application, cube, objType, objName);
            }
        } catch (Exception e) {
            logger.warn("Exception in getOlapFileObject of HspEssbaseMainJAPIImpl for application " + application + " with cube name " + cube + "  objType " + objType + " and objName " + objName, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return fileObj;
    }

    public static IEssBaseObject[] getOlapFileObjects(final EssbaseConnection connection, final String application, final String cube, final int objType) throws EssbaseException {
        logger.entering(connection, application, cube, objType);
        validateArguments(connection, application);
        IEssBaseObject[] returnVal = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                IEssIterator itr = olapServer.getOlapFileObjects(application, cube, objType);
                if(null != itr && itr.getCount() > 0){ 
                  returnVal = itr.getAll();
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getOlapFileObjects of HspEssbaseMainJAPIImpl for application " + application + " with cube name " + cube + " and  objType " + objType, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return returnVal;
    }

    public static boolean doesOlapFileObjectExist(final EssbaseConnection connection, final String application, final String cube, final int objType, final String objectName) throws EssbaseException {
        logger.entering(connection, application, cube, objType);
        validateArguments(connection, application);
        boolean returnVal = false;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                IEssIterator itr = olapServer.getOlapFileObjects(application, cube, objType);
                if(null != itr && itr.getCount() > 0){ 
                  IEssBaseObject[] allObjects = itr.getAll();
                  for(IEssBaseObject anObject : allObjects){
                      if( ((IEssOlapFileObject) anObject).getName().equalsIgnoreCase(objectName)){
                          returnVal = true;
                          break;
                      }
                  }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in doesOlapFileObjectExist of HspEssbaseMainJAPIImpl for application " + application + " with cube name " + cube + "  objType " + objType + " , and object name " + objectName, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return returnVal;
    }

    public static void getObject(final EssbaseConnection connection, final String application, final String cube, final String objName, final int objType, final String clientFileName) throws EssbaseException {
        getObject(connection, application, cube, objName, objType, clientFileName, false);
    }

    public static void getObject(final EssbaseConnection connection, final String application, final String cube, final String objName, final int objType, final String clientFileName, final boolean lock) throws EssbaseException {
        logger.entering(connection, application, cube, objType, objName, clientFileName, lock);
        validateArguments(connection, application);
        validateArguments(objName, OBJ_NAME_NULL);
        validateArguments(clientFileName, CLIENT_FILE_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                olapServer.copyOlapFileObjectFromServer(application, cube, objType, objName, clientFileName, lock);
            }
        } catch (Exception e) {
            logger.warn("Exception in getObject of HspEssbaseMainJAPIImpl for application " + application + " with cube name " + cube + "  objType " + objType + " objName " + objName + " clientFileName " + clientFileName + " and lock " + lock, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void deleteObject(final EssbaseConnection connection, final String application, final String cube, final String objName, final int objType) throws EssbaseException {
        logger.entering(connection, application, cube, objType, objName);
        validateArguments(connection, application);
        validateArguments(objName, OBJ_NAME_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                olapServer.deleteOlapFileObject(application, cube, objType, objName);
            }
        } catch (Exception e) {
            logger.warn("Exception in deleteOlapFileObject of HspEssbaseMainJAPIImpl for application " + application + " with cube name " + cube + "  objType " + objType + " and objName " + objName, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void putObject(final EssbaseConnection connection, final String application, final String cube, final String objName, final int objType, final String clientFileName, final boolean unlock) throws EssbaseException {
        logger.entering(connection, application, cube, objType, objName, clientFileName, unlock);
        validateArguments(connection, application);
        validateArguments(objName, OBJ_NAME_NULL);
        validateArguments(clientFileName, CLIENT_FILE_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                olapServer.copyOlapFileObjectToServer(application, cube, objType, objName, clientFileName, unlock);
            }
        } catch (Exception e) {
            logger.warn("Exception in putObject of HspEssbaseMainJAPIImpl for application " + application + " with cube name " + cube + "  objType " + objType + " objName " + objName + " clientFileName " + clientFileName + " and unlock " + unlock, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void createObject(final EssbaseConnection connection, final String application, final String cube, final String objName, final int objType) throws EssbaseException {
        logger.entering(connection, application, cube, objType, objName);
        validateArguments(connection, application);
        validateArguments(objName, OBJ_NAME_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                olapServer.createOlapFileObject(application, cube, objType, objName);
            }
        } catch (Exception e) {
            logger.warn("Exception in createObject of HspEssbaseMainJAPIImpl for application " + application + " with cube name " + cube + "  objType " + objType + " and objName " + objName, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static String[][] listVariables(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection);
        String[][] variables = null;
        IEssOlapServer olapServer = null;
        IEssOlapApplication existingApp = null;
        try {
            validateEssbaseConnection(connection);
            olapServer = connection.getOlapServer();
            if (HspUtils.isNullOrEmpty(application)) {
                if (null != olapServer) {
                    variables = olapServer.getSubstitutionVariables();
                }
            } else if (HspUtils.isNotNullOrEmpty(application) && HspUtils.isNullOrEmpty(cube)) {
                if (null != olapServer) {
                    existingApp = olapServer.getApplication(application);
                    if (null != existingApp) {
                        variables = existingApp.getSubstitutionVariables();
                    }
                }
            } else if (HspUtils.isNotNullOrEmpty(application) && HspUtils.isNotNullOrEmpty(cube)) {
                if (null != olapServer) {
                    existingApp = olapServer.getApplication(application);
                    if (null != existingApp) {
                        final IEssCube existingCube = existingApp.getCube(cube);
                        if (null != existingCube) {
                            variables = existingCube.getSubstitutionVariables();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in listVariables of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return variables;
    }

    public static HspEssVariable[] listVariables2(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        HspEssVariable[] variables = null;
        final String[][] variablesArray = listVariables(connection, application, cube);
        if (null != variablesArray) {
            variables = new HspEssVariable[variablesArray.length];
            for (int i = 0; i < variablesArray.length; i++) {
                variables[i] = new HspEssVariable(variablesArray[i][SERVER_IDX], variablesArray[i][APP_IDX], variablesArray[i][DB_IDX], variablesArray[i][VAR_IDX], variablesArray[i][VAR_VAL_IDX]);
            }
        }
        return variables;
    }

    public static String getVariable(final EssbaseConnection connection, final String application, final String cube, final String variableName) throws EssbaseException {
        logger.entering(connection, application, cube, variableName);
        validateArguments(connection);
        validateArguments(variableName, VA_NAME_NULL);
        String value = null;
        IEssOlapServer olapServer = null;
        IEssOlapApplication existingApp = null;
        try {
            validateEssbaseConnection(connection);
            olapServer = connection.getOlapServer();
            if (HspUtils.isNotNullOrEmpty(application) && HspUtils.isNotNullOrEmpty(cube)) {
                if (null != olapServer) {
                    existingApp = olapServer.getApplication(application);
                    if (null != existingApp) {
                        final IEssCube existingCube = existingApp.getCube(cube);
                        if (null != existingCube) {
                            value = existingCube.getSubstitutionVariableValue(variableName);
                        }
                    }
                }
            } else if (HspUtils.isNotNullOrEmpty(application) && HspUtils.isNullOrEmpty(cube)) {
                if (null != olapServer) {
                    existingApp = olapServer.getApplication(application);
                    if (null != existingApp) {
                        value = existingApp.getSubstitutionVariableValue(variableName);
                    }
                }
            } else if (HspUtils.isNullOrEmpty(application) && HspUtils.isNullOrEmpty(cube)) {
                if (null != olapServer) {
                    value = olapServer.getSubstitutionVariableValue(variableName);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getVariable of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " and variable name " + variableName, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return value;
    }

    public static HspEssVariable getVariable2(final EssbaseConnection connection, final String application, final String cube, final String variableName) throws EssbaseException {
        HspEssVariable variable = null;
        try {
            final String value = getVariable(connection, application, cube, variableName);
            if (null != value) {
                variable = new HspEssVariable(connection.getOlapServer().getName(), application, cube, variableName, value);
            }
        } catch (Exception e) {
            logger.warn("Exception in getVariable2 of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " and variable name " + variableName, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return variable;
    }

    public static void deleteVariable(final EssbaseConnection connection, final String application, final String cube, final String variableName) throws EssbaseException {
        logger.entering(connection, application, cube, variableName);
        validateArguments(connection);
        validateArguments(variableName, VA_NAME_NULL);
        IEssOlapServer olapServer = null;
        IEssOlapApplication existingApp = null;
        try {
            validateEssbaseConnection(connection);
            olapServer = connection.getOlapServer();
            if (HspUtils.isNotNullOrEmpty(application) && HspUtils.isNotNullOrEmpty(cube)) {
                if (null != olapServer) {
                    existingApp = olapServer.getApplication(application);
                    if (null != existingApp) {
                        final IEssCube existingCube = existingApp.getCube(cube);
                        if (null != existingCube) {
                            existingCube.deleteSubstitutionVariable(variableName);
                        }
                    }
                }
            } else if (HspUtils.isNotNullOrEmpty(application) && HspUtils.isNullOrEmpty(cube)) {
                if (null != olapServer) {
                    existingApp = olapServer.getApplication(application);
                    if (null != existingApp) {
                        existingApp.deleteSubstitutionVariable(variableName);
                    }
                }
            } else if (HspUtils.isNullOrEmpty(application) && HspUtils.isNullOrEmpty(cube)) {
                if (null != olapServer) {
                    olapServer.deleteSubstitutionVariable(variableName);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in deleteVariable of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " and variable name " + variableName, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void setVariable(final EssbaseConnection connection, final String application, final String cube, final String variableName, final String variableValue) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection);
        validateArguments(variableName, VA_NAME_NULL);
        IEssOlapServer olapServer = null;
        IEssOlapApplication existingApp = null;
        try {
            validateEssbaseConnection(connection);
            olapServer = connection.getOlapServer();
            if (HspUtils.isNotNullOrEmpty(application) && HspUtils.isNotNullOrEmpty(cube)) {
                if (null != olapServer) {
                    existingApp = olapServer.getApplication(application);
                    if (null != existingApp) {
                        final IEssCube existingCube = existingApp.getCube(cube);
                        if (null != existingCube) {
                            existingCube.createSubstitutionVariable(variableName, variableValue);
                        }
                    }
                }
            } else if (HspUtils.isNotNullOrEmpty(application) && HspUtils.isNullOrEmpty(cube)) {
                if (null != olapServer) {
                    existingApp = olapServer.getApplication(application);
                    if (null != existingApp) {
                        existingApp.createSubstitutionVariable(variableName, variableValue);
                    }
                }
            } else if (HspUtils.isNullOrEmpty(application) && HspUtils.isNullOrEmpty(cube)) {
                if (null != olapServer) {
                    olapServer.createSubstitutionVariable(variableName, variableValue);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setVariable of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " variable name " + variableName + " and variable alue " + variableValue, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static boolean isDBinArchiveMode(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        boolean isInArchiveMode = false;
        try {
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssMaxlSession maxlSession = olapServer.openMaxlSession(PLANNING_JAPI_SESSION_ID);
                final boolean executed = maxlSession.execute(DISPLAY_DB_MAXL_PREFIX + COMMA + application + JOINING_STR + cube + COMMA);
                if (executed) {
                    final IEssMaxlResultSet resultSet = maxlSession.getResultSet();
                    resultSet.next();
                    isInArchiveMode = resultSet.getValue(READ_ONLY_COLUMN_IDX).getBoolean();
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in isDBinArchiveMode of HspEssbaseMainJAPIImpl for application " + application + " and cube " + cube + " ");
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return isInArchiveMode;
    }

    public static String[][] getLocationAliasListForCube(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        String[][] allLocAliases = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        allLocAliases = existingCube.getLocationAliases();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getLocationAliasListForCube of HspEssbaseMainJAPIImpl for application " + application + " and cube " + cube + " ");
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return allLocAliases;
    }

    public static void createLocationAlias(final EssbaseConnection connection, final String sourceApplication, final String sourceCube, final String aliasName, final String hostName, final String targetApplication, final String targetCube, final String userName, final String password) throws EssbaseException {
        logger.entering(connection, sourceApplication, sourceCube, aliasName, hostName, targetApplication, targetCube, userName);
        validateArguments(connection, sourceApplication, sourceCube);
        validateArguments(targetApplication, TARGET_APP_NULL);
        validateArguments(targetCube, TARGET_CUBE_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(sourceApplication);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(sourceCube);
                    if (null != existingCube) {
                        existingCube.createLocationAlias(aliasName, hostName, targetApplication, targetCube, userName, password);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in createLocationAlias of HspEssbaseMainJAPIImpl for source application " + sourceApplication + " and source cube " + targetCube + " with name " + aliasName + " on host " + hostName + " to target application " + targetApplication + " and target cube " + targetCube + " with user name " + userName);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static HspEssApplication[] listApplicationsEx(final EssbaseConnection connection) throws EssbaseException {
        logger.entering(connection);
        validateArguments(connection);
        HspEssApplication[] allApps = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssIterator appsItr = olapServer.getApplications();
                if (null != appsItr) {
                    final IEssBaseObject[] allBaseObjects = appsItr.getAll();
                    allApps = new HspEssApplication[allBaseObjects.length];
                    int i = 0;
                    for (final IEssBaseObject aBaseObject : allBaseObjects) {
                        final IEssOlapApplication olapApp = (IEssOlapApplication)aBaseObject;
                        final HspEssApplication anEssApp = new HspEssApplication();
                        anEssApp.setAppName(olapApp.getName());
                        anEssApp.setAppStorageType(olapApp.getDataStorageType().intValue());
                        anEssApp.setAppType(olapApp.getType().equals(APP_TYPE_UTF8) ? HspEssApplication.ESS_APP_UNICODE : HspEssApplication.ESS_APP_NONUNICODE);
                        allApps[i++] = anEssApp;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in listApplicationsEx of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return allApps;
    }

    public static HspEssCube[] listDatabasesEx(final EssbaseConnection connection, final String application) throws EssbaseException {
        logger.entering(connection, application);
        validateArguments(connection, application);
        HspEssCube[] allDBs = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssIterator dbsItr = existingApp.getCubes();
                    if (null != dbsItr) {
                        final IEssBaseObject[] allBaseObjects = dbsItr.getAll();
                        allDBs = new HspEssCube[allBaseObjects.length];
                        int i = 0;
                        for (final IEssBaseObject aBaseObject : allBaseObjects) {
                            final IEssCube aCube = (IEssCube)aBaseObject;
                            final HspEssCube anEssCube = new HspEssCube(aCube.getName());
                            allDBs[i++] = anEssCube;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in listDatabasesEx of HspEssbaseMainJAPIImpl for application " + application, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return allDBs;
    }

    public static void validateDB(final EssbaseConnection connection, final String application, final String cube, final String errorFileName) throws EssbaseException {
        logger.entering(connection, application, cube, errorFileName);
        validateArguments(connection, application, cube);
        validateArguments(errorFileName, ERR_FILE_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.validateCube(errorFileName);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in validateDB of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " and error file name " + errorFileName);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void getSMStats(final EssbaseConnection connection, final String application, final String cube, HspEssAppStats appStats) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final EssCube cubeImpl = (EssCube)existingCube;
                        if (null == containsSMStatsMethod) {
                            try {
                                handleSMStatsMethod = EssCube.class.getMethod(GET_SM_STATS, new Class[0]);
                                containsSMStatsMethod = true;
                            } catch (Exception e) {
                                containsSMStatsMethod = false;
                            }
                        }
                        if (containsSMStatsMethod) {
                            final Object result = handleSMStatsMethod.invoke(cubeImpl, new Object[0]);
                            if (null != result) {
                                final Object[] allStats = (Object[])result;
                                if (null == handleSMStatKeyWordMethod) {
                                    try {
                                        handleSMStatKeyWordMethod = allStats[0].getClass().getMethod(GET_KEY_WORD, new Class[0]);
                                    } catch (Exception e) {
                                        logger.warn("Ignoring this exception", e);
                                    }
                                }
                                if (null == handleSMStatValueMethod) {
                                    try {
                                        handleSMStatValueMethod = allStats[0].getClass().getMethod(GETD_VALUE, new Class[0]);
                                    } catch (Exception e) {
                                        logger.warn("Ignoring this exception", e);
                                    }
                                }

                                if ((null != handleSMStatKeyWordMethod) && (null != handleSMStatValueMethod)) {
                                    if (null == appStats) {
                                        appStats = new HspEssAppStats();
                                    }
                                    for (final Object aStat : allStats) {
                                        String keyWordResult = null;
                                        try {
                                            keyWordResult = (String)handleSMStatKeyWordMethod.invoke(aStat, new Object[0]);
                                        } catch (Exception e) {
                                            logger.warn("Ignoring this exception", e);
                                        }

                                        Double valueResult = null;
                                        try {
                                            valueResult = (Double)handleSMStatValueMethod.invoke(aStat, new Object[0]);
                                        } catch (Exception e) {
                                            logger.warn("Ignoring this exception", e);
                                        }

                                        if ((null != keyWordResult) && (null != valueResult)) {
                                            appStats.createNewValuePair();
                                            appStats.setName(keyWordResult);
                                            appStats.setValue(valueResult);
                                            appStats.addNewValuePair();
                                        }
                                    }
                                }

                            }

                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getSMStats of HspEssbaseMainJAPIImpl for application " + application + " and cube " + cube + " ");
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static HspEssDBStats getDBStats(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        HspEssDBStats dbStats = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                double clusterRatio = 1.0;
                try {
                    final IEssMaxlSession maxlSession = olapServer.openMaxlSession(PLANNING_JAPI_SESSION_ID);
                    final boolean executed = maxlSession.execute(QUERY_DB_MAXL_PREFIX + COMMA + application + JOINING_STR + cube + COMMA + DB_STATS_DATA_BLKS_SUFFIX);
                    if (executed) {
                        final IEssMaxlResultSet resultSet = maxlSession.getResultSet();
                        resultSet.next();
                        clusterRatio = resultSet.getValue(CLUSTER_RATIO_COLUMN_IDX).getDouble();
                    }
                } catch (Exception e) {
                    logger.warn("Failed to read the cluster ration from MaxL for application " + application + " and cube " + cube + " . Defaulting it to 1.", e);
                }

                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        dbStats = new HspEssDBStats(OBSOLETE_INDEX_TYPE, existingCube.getCountDimensions(), existingCube.getDeclaredBlockSize(), existingCube.getActualBlockSize(), existingCube.getDeclaredMaxBlocks(), existingCube.getActualMaxBlocks(), existingCube.getNonMissingLeafBlocks(), existingCube.getNonMissingNonLeafBlocks(), OBSOLETE_NON_MISSING_BLOCKS, existingCube.getTotMemPagedInBlocks(), existingCube.getInMemCompBlocks(), existingCube.getTotalBlocks(), existingCube.getTotMemPagedInBlocks(), existingCube.getTotMemBlocks(), existingCube.getTotMemIndex(), existingCube.getTotMemInMemCompBlocks(), existingCube.getBlockDensity(), existingCube.getSparseDensity(), existingCube.getCompressionRatio(), clusterRatio);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getDBStats of HspEssbaseMainJAPIImpl for application " + application + " and cube " + cube + " ");
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return dbStats;
    }

    public static void setApplicationId(final EssbaseConnection connection, final String application, final String appId) throws EssbaseException {
        logger.entering(connection, application, appId);
        validateArguments(connection, application);
        validateArguments(appId, APP_ID_NULL);
        IEssOlapApplication existingApp = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    ((EssOlapApplication)existingApp).setApplicationId(appId);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setApplicationId of HspEssbaseMainJAPIImpl for application " + application + " and appId " + appId + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static String getApplicationId(final EssbaseConnection connection, final String application) throws EssbaseException {
        logger.entering(connection, application);
        validateArguments(connection, application);
        IEssOlapApplication existingApp = null;
        String appId = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    appId = ((EssOlapApplication)existingApp).getApplicationId();
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getApplicationId of HspEssbaseMainJAPIImpl for application " + application + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return appId;
    }

    public static int getApplicationFrontEndAppType(final EssbaseConnection connection, final String application) throws EssbaseException {
        logger.entering(connection, application);
        validateArguments(connection, application);
        IEssOlapApplication existingApp = null;
        int appFrontEndType = -1;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    appFrontEndType = ((EssOlapApplication)existingApp).getApplicationFrontEndAppType();
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getApplicationFrontEndAppType of HspEssbaseMainJAPIImpl for application " + application + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return appFrontEndType;
    }

    public static void setApplicationFrontEndAppType(final EssbaseConnection connection, final String application, final short frontEndAppType) throws EssbaseException {
        logger.entering(connection, application);
        validateArguments(connection, application);
        IEssOlapApplication existingApp = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    ((EssOlapApplication)existingApp).setApplicationFrontEndAppType(frontEndAppType);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setApplicationFrontEndAppType of HspEssbaseMainJAPIImpl for application " + application + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static boolean validateHCtx(final EssbaseConnection connection) throws EssbaseException {
        logger.entering(connection);
        validateArguments(connection);
        boolean isConnected = false;
        try {
            isConnected = connection.isConnected();
        } catch (Exception e) {
            logger.warn("Exception in validateHCtx of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return isConnected;
    }

    public static boolean isConnected(final EssbaseConnection connection) throws EssbaseException {
        logger.entering(connection);
        validateArguments(connection);
        boolean isConnected = false;
        try {
            isConnected = connection.isConnected();
        } catch (Exception e) {
            logger.warn("Exception in isConnected of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return isConnected;
    }

    public static String[] getCellDrillThruReports(final EssbaseConnection connection, final String application, final String cube, final String[] pMembers) throws EssbaseException {
        logger.entering(connection, application, cube, pMembers);
        validateArguments(connection, application, cube);
        validateArguments(pMembers, MEMBERS_NULL);
        String[] cellDrillThruReports = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        cellDrillThruReports = existingCube.getCellDrillThroughReports(pMembers);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getCellDrillThruReports of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " and members " + Arrays.deepToString(pMembers) + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return cellDrillThruReports;
    }

    public static void clearData(final EssbaseConnection connection, final String application, final String cube, final String region, final boolean logical) throws EssbaseException {
        logger.entering(connection, application, cube, region, logical);
        validateArguments(connection, application, cube);
        validateArguments(region, REGION_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.clearPartialData(region, !logical);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in clearData of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " and region " + region + " and logical flag as " + logical + " .", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static int getSecurityMode(final EssbaseConnection connection) throws EssbaseException {
        logger.entering(connection);
        validateArguments(connection);
        int securityMode = -1;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                securityMode = olapServer.getOlapSecurityMode();
            }
        } catch (Exception e) {
            logger.warn("Exception in getSecurityMode of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return securityMode;
    }

    public static boolean verifyFormula(final EssbaseConnection connection, final String application, final String cube, final String sFormula) throws EssbaseException {
        logger.entering(connection, application, cube, sFormula);
        validateArguments(connection, application, cube);
        validateArguments(sFormula, FORMULA_NULL);
        boolean valid = false;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.setActive();
                        valid = existingCube.verifyFormula(sFormula);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in verifyFormula of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " and formula " + sFormula + " .", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return valid;
    }

    private static void sendString(final EssbaseConnection connection, final String application, final String cube, final String query) throws EssbaseException {
        logger.entering(connection, application, cube, query);
        validateArguments(connection, application, cube);
        validateArguments(query, QUERY_STR_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.sendString(query);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in sendString of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " and query " + query + " .", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }
    
    public static void sendString(final EssbaseConnection connection, IEssCube existingCube, final String query) throws EssbaseException {
        logger.entering(connection, existingCube, query);
        validateArguments(connection);
        validateArguments(existingCube, IESSCUBE_NULL);
        validateArguments(query, QUERY_STR_NULL);
        try {
            validateEssbaseConnection(connection);
            existingCube.sendString(query);
        } catch (Exception e) {
            logger.warn("Exception in sendString of HspEssbaseMainJAPIImpl for query " +query + " .", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void sendString(final EssbaseConnection connection, final String application, final String cube, final String query, final boolean bFirst) throws EssbaseException {
        logger.entering(connection, application, cube, query);
        validateArguments(connection, application, cube);
        validateArguments(query, QUERY_STR_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        if (existingApp.getType().equalsIgnoreCase(APP_TYPE_UTF8) && bFirst) {
                            existingCube.sendString(UNICODE_BYTE_ORDER_MARK);
                        }
                        existingCube.sendString(query);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in sendString of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " query " + query + " and bFirst " + bFirst, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void logout(final EssbaseConnection connection) throws EssbaseException {
        logger.entering(connection);
        validateArguments(connection);
        try {
            if (connection.isConnected()) {
                connection.disconnect();
            }
        } catch (Exception e) {
            logger.warn("Exception in logout of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void disconnect(final EssbaseConnection connection) throws EssbaseException {
        logger.entering(connection);
        validateArguments(connection);
        try {
            if (connection.isConnected()) {
                connection.disconnect();
            }
        } catch (Exception e) {
            logger.warn("Exception in disconnect of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static boolean disconnected(final EssbaseConnection connection) throws EssbaseException {
        logger.entering(connection);
        validateArguments(connection);
        boolean isConnected = false;
        try {
            isConnected = connection.isConnected();
        } catch (Exception e) {
            logger.warn("Exception in disconnected of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return isConnected;
    }

    public static void calculate(final EssbaseConnection connection, final String application, final String cube, final boolean bCalc, final String calc) throws EssbaseException {
        logger.entering(connection, application, cube, bCalc, calc);
        validateArguments(connection, application, cube);
        validateArguments(calc, CALC_SCRIPT_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.setActive();
                        existingCube.calculate(calc, !bCalc);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in calculate of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " calc " + calc + " and bCalc " + bCalc, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void calculate(final EssbaseConnection connection, final String application, final String cube, final String calcFileName) throws EssbaseException {
        calculate(connection, application, cube, calcFileName, false);
    }

    public static void calculate(final EssbaseConnection connection, final String application, final String cube, final String calcFileName, final boolean bCalc) throws EssbaseException {
        logger.entering(connection, application, cube, calcFileName, bCalc);
        validateArguments(connection, application, cube);
        validateArguments(calcFileName, CALC_SCRIPT_FILE_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.setActive();
                        existingCube.calculate(!bCalc, calcFileName);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in calculate of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " calcFileName " + calcFileName + " and bCalc " + bCalc, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static int getProcessState(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        int processState = -1;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final EssBuildDimDataLoadState processStateEnum = existingCube.getAsyncProcessState();
                        if (null != processStateEnum) {
                            processState = existingCube.getAsyncProcessState().getProcessState();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getProcessState of HspEssbaseMainJAPIImpl for application " + application + " and cube " + cube + " ");
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return processState;
    }


    public static IEssOlapUser createUser(final EssbaseConnection connection, final String userName, final String password) throws EssbaseException {
        logger.entering(connection, userName);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        validateArguments(password, USER_PWD_NULL);
        IEssOlapUser createdUser = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                createdUser = olapServer.createOlapUser(userName, password);
            }
        } catch (Exception e) {
            logger.warn("Exception in createUser of HspEssbaseMainJAPIImpl with userName " + userName + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return createdUser;
    }

    public static IEssOlapUser createUserWithType(final EssbaseConnection connection, final String userName, final String password, final short userType) throws EssbaseException {
        logger.entering(connection, userName, userType);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        validateArguments(password, USER_PWD_NULL);
        IEssOlapUser createdUser = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                createdUser = olapServer.createOlapUserWithType(userName, password, userType);
            }
        } catch (Exception e) {
            logger.warn("Exception in createUserWithType of HspEssbaseMainJAPIImpl with userName " + userName + " and user type " + userType + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return createdUser;
    }

    public static IEssOlapUser createExtUser(final EssbaseConnection connection, final String userName, final String password, final String provider, final String parametes) throws EssbaseException {
        logger.entering(connection, userName, provider, parametes);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        validateArguments(password, USER_PWD_NULL);
        validateArguments(provider, PROVIDER_NULL);
        validateArguments(parametes, CONN_PARAM_NULL);
        IEssOlapUser createdUser = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                createdUser = olapServer.createOlapExtUser(userName, password, provider, parametes);
            }
        } catch (Exception e) {
            logger.warn("Exception in createExtUser of HspEssbaseMainJAPIImpl with userName " + userName + " provider " + provider + " and parameters " + parametes, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return createdUser;
    }

    public static IEssOlapUser createExtUserWithType(final EssbaseConnection connection, final String userName, final String password, final String provider, final String parametes, final short userType) throws EssbaseException {
        logger.entering(connection, userName, provider, parametes, userType);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        validateArguments(password, USER_PWD_NULL);
        validateArguments(provider, PROVIDER_NULL);
        validateArguments(parametes, CONN_PARAM_NULL);
        IEssOlapUser createdUser = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                createdUser = olapServer.createOlapExtUserWithType(userName, password, provider, parametes, userType);
            }
        } catch (Exception e) {
            logger.warn("Exception in createExtUserWithType of HspEssbaseMainJAPIImpl with userName " + userName + " provider " + provider + " parameters " + parametes + " and userType " + userType, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return createdUser;
    }


    public static void setPassword(final EssbaseConnection connection, final String userName, final String newPassword) throws EssbaseException {
        logger.entering(connection, userName);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        validateArguments(newPassword, USER_PWD_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapUser existingUser = olapServer.getOlapUser(userName);
                if (null != existingUser) {
                    existingUser.changePassword(newPassword);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setPassword of HspEssbaseMainJAPIImpl with userName " + userName + " ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void setUserAppAccess(final EssbaseConnection connection, final String userName, final String application, final int access) throws EssbaseException {
        logger.entering(connection, userName);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        validateArguments(application, APP_NAME);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapUser existingUser = olapServer.getOlapUser(userName);
                if (null != existingUser) {
                    final IEssOlapApplication existingApp = olapServer.getApplication(application);
                    if (null != existingApp) {
                        existingApp.setUserOrGroupAccess(userName, IEssOlapApplication.EEssOlapApplicationAccess.sm_fromInt(access));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setUserAppAccess of HspEssbaseMainJAPIImpl with userName " + userName + " application " + application + " and access " + access, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void setUserType(final EssbaseConnection connection, final String userName, final int type) throws EssbaseException {
        logger.entering(connection, userName);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapUser existingUser = olapServer.getOlapUser(userName);
                if (null != existingUser) {
                    existingUser.setUserType((short)type, ESS_USERTYPE_CMD_ADD);
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setUserType of HspEssbaseMainJAPIImpl with userName " + userName + " and type " + type, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void getCalcObject(final EssbaseConnection connection, final String application, final String cube, final String calcScriptName, final String clientFileName, final boolean lock) throws EssbaseException {
        logger.entering(connection, application, cube, calcScriptName, clientFileName, lock);
        validateArguments(connection, application, cube);
        validateArguments(calcScriptName, CALC_SCRIPT_NAM);
        validateArguments(clientFileName, CLIENT_FILE_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                olapServer.copyOlapFileObjectFromServer(application, cube, IEssOlapFileObject.TYPE_CALCSCRIPT, calcScriptName, clientFileName, lock);
            }
        } catch (Exception e) {
            logger.warn("Exception in getCalcObject of HspEssbaseMainJAPIImpl for application " + application + " with cube name " + cube + " calcScriptName " + calcScriptName + " clientFileName " + clientFileName + " and lock " + lock, e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }


//    private static void beginDataLoad(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final int rulesObjectType, final boolean store, final boolean unlock, final boolean abortOnError) throws EssbaseException {
//        logger.entering(connection, application, cube, rulesFileName, rulesObjectType, store, unlock, abortOnError);
//        validateArguments(connection, application, cube);
//        validateArguments(rulesFileName, RULE_FILE_NAME);
//        try {
//            validateEssbaseConnection(connection);
//            final IEssOlapServer olapServer = connection.getOlapServer();
//            if (null != olapServer) {
//                final IEssOlapApplication existingApp = olapServer.getApplication(application);
//                if (null != existingApp) {
//                    final IEssCube existingCube = existingApp.getCube(cube);
//                    if (null != existingCube) {
//                        existingCube.beginDataload(store, unlock, abortOnError, rulesFileName, rulesObjectType);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.warn("Exception in beginDataLoad of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " and rules file name " + rulesFileName);
//            doExceptionTranslation(e);
//        } finally {
//            logger.exiting();
//        }
//    }

    public static IEssCube beginDataLoad(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final int rulesObjectType, final boolean store, final boolean unlock, final boolean abortOnError) throws EssbaseException {
        logger.entering(connection, application, cube, rulesFileName, rulesObjectType, store, unlock, abortOnError);
        validateArguments(connection, application, cube);
        validateArguments(rulesFileName, RULE_FILE_NAME);
        IEssCube existingCube = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.beginDataload(store, unlock, abortOnError, rulesFileName, rulesObjectType);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in beginDataLoad of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " and rules file name " + rulesFileName);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return existingCube;
    }

    public static HspMbrError[] dataLoad(final EssbaseConnection connection, final String application, final String cube, final String query, final String rulesFileName, final int rulesObjectType, final boolean store, final boolean unlock, final boolean abortOnError) throws EssbaseException {
        logger.entering(connection, application, cube, rulesFileName, rulesObjectType, store, unlock, abortOnError);
        validateArguments(connection, application, cube);
        validateArguments(rulesFileName, RULE_FILE_NAME);
        HspMbrError[] mbrErrors = null;
        try {
             IEssCube existingCube = beginDataLoad(connection, application, cube, rulesFileName, rulesObjectType, store, unlock, abortOnError);
             sendString(connection, existingCube, query);
             mbrErrors = endDataload(connection, existingCube);
        }catch (Exception e) {
            logger.warn("Exception in dataLoad of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , query " + query + " and rules file name " + rulesFileName);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return mbrErrors;
    }

//    public static void beginDataLoadASO(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final int rulesObjectType, final boolean store, final boolean unlock, final boolean abortOnError, final int bufferId) throws EssbaseException {
//        logger.entering(connection, application, cube, rulesFileName, rulesObjectType, store, unlock, abortOnError, bufferId);
//        validateArguments(connection, application, cube);
//        validateArguments(rulesFileName, RULE_FILE_NAME);
//        try {
//            validateEssbaseConnection(connection);
//            final IEssOlapServer olapServer = connection.getOlapServer();
//            if (null != olapServer) {
//                final IEssOlapApplication existingApp = olapServer.getApplication(application);
//                if (null != existingApp) {
//                    final IEssCube existingCube = existingApp.getCube(cube);
//                    if (null != existingCube) {
//                        existingCube.beginDataload(store, unlock, abortOnError, rulesFileName, rulesObjectType, bufferId);
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.warn("Exception in beginDataLoadASO of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " rules file name " + rulesFileName + " , and buffer id " + bufferId);
//            doExceptionTranslation(e);
//        } finally {
//            logger.exiting();
//        }
//    }
    
        public static IEssCube beginDataLoadASO(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final int rulesObjectType, final boolean store, final boolean unlock, final boolean abortOnError, final int bufferId) throws EssbaseException {
        logger.entering(connection, application, cube, rulesFileName, rulesObjectType, store, unlock, abortOnError, bufferId);
        validateArguments(connection, application, cube);
        validateArguments(rulesFileName, RULE_FILE_NAME);
        IEssCube existingCube = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final int cubeType = existingCube.getCubeType().intValue();
                        if (cubeType == ASO_CUBE_TYPE) {
                            existingCube.beginDataload(store, unlock, abortOnError, rulesFileName, rulesObjectType, bufferId);
                        }else{
                            logger.warn("Data load can only be done for ASO type cube with this API, in beginDataLoadASO of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , rules file name " + rulesFileName + " , rules object type " + rulesObjectType);
                            existingCube = null;
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in beginDataLoadASO of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " rules file name " + rulesFileName + " , and buffer id " + bufferId);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return existingCube;
    }

    public static HspMbrError[] beginDataload(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final int rulesObjectType, final String dataFileName, final int dataFileType, final boolean abortOnError, final int bufferId) throws EssbaseException {
        logger.entering(connection, application, cube, rulesFileName, rulesObjectType, dataFileName, dataFileType, abortOnError, bufferId);
        validateArguments(connection, application, cube);
        validateArguments(rulesFileName, RULE_FILE_NAME);
        validateArguments(dataFileName, DATA_FILE_NAME);
        HspMbrError[] mbrErrors = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final String[][] dataLoadErrors = existingCube.beginDataload(rulesFileName, rulesObjectType, dataFileName, dataFileType, abortOnError, bufferId);
                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in beginDataLoad of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " rules file name " + rulesFileName + " , dataFileName " + dataFileName + " , and buffer id " + bufferId);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return mbrErrors;
    }

//    public static HspMbrError[] beginDataload(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final int rulesObjectType, final String dataFileName, final int dataFileType, final String userName, final String password, final boolean abortOnError, final int bufferId) throws EssbaseException {
//        logger.entering(connection, application, cube, rulesFileName, rulesObjectType, dataFileName, dataFileType, userName, abortOnError, bufferId);
//        validateArguments(connection, application, cube);
//        validateArguments(rulesFileName, RULE_FILE_NAME);
//        validateArguments(dataFileName, DATA_FILE_NAME);
//        validateArguments(userName, USER_NAME_NULL);
//        validateArguments(password, USER_PWD_NULL);
//
//        HspMbrError[] mbrErrors = null;
//        try {
//            validateEssbaseConnection(connection);
//            final IEssOlapServer olapServer = connection.getOlapServer();
//            if (null != olapServer) {
//                final IEssOlapApplication existingApp = olapServer.getApplication(application);
//                if (null != existingApp) {
//                    final IEssCube existingCube = existingApp.getCube(cube);
//                    if (null != existingCube) {
//                        final String[][] dataLoadErrors = existingCube.beginDataload(rulesFileName, rulesObjectType, dataFileName, dataFileType, userName, password, abortOnError, bufferId);
//                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
//                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
//                        }
//                    }
//                }
//            }
//        } catch (Exception e) {
//            logger.warn("Exception in beginDataLoad of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " rules file name " + rulesFileName + " , dataFileName " + dataFileName + " , and buffer id " + bufferId);
//            doExceptionTranslation(e);
//        } finally {
//            logger.exiting();
//        }
//        return mbrErrors;
//    }

    private static HspMbrError[] endDataload(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        HspMbrError[] mbrErrors = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final String[][] dataLoadErrors = existingCube.endDataload();
                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in endDataload of HspEssbaseMainJAPIImpl for application " + application + " and cube " + cube);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return mbrErrors;
    }
    
    public static HspMbrError[] endDataload(final EssbaseConnection connection, final IEssCube existingCube) throws EssbaseException {
        logger.entering(connection, existingCube);
        validateArguments(connection);
        validateArguments(existingCube, IESSCUBE_NULL);
        HspMbrError[] mbrErrors = null;
        try {
            validateEssbaseConnection(connection);
            final String[][] dataLoadErrors = existingCube.endDataload();
            if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
            }
        } catch (Exception e) {
            logger.warn("Exception in endDataload of HspEssbaseMainJAPIImpl");
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return mbrErrors;
    }

    public static void loadBufferTerm(final EssbaseConnection connection, final String application, final String cube, final long bufferId, final long commitType, final long actionType, final long options) throws EssbaseException {
        logger.entering(connection, application, cube, bufferId, commitType, actionType, options);
        validateArguments(connection, application, cube);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.loadBufferTerm(new long[] { bufferId }, commitType, actionType, options);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in loadBufferTerm of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , and buffer id " + bufferId);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void loadBufferInit(final EssbaseConnection connection, final String application, final String cube, final long bufferId, final long duplctAggrMethod, final long options, final long size) throws EssbaseException {
        logger.entering(connection, application, cube, bufferId, duplctAggrMethod, options, size);
        validateArguments(connection, application, cube);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.loadBufferInit(bufferId, duplctAggrMethod, options, size);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in loadBufferInit of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , and buffer id " + bufferId);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void loadBufferInitASO(final EssbaseConnection connection, final String application, final String cube, final long bufferId, final long duplctAggrMethod, final long options, final long size) throws EssbaseException {
        loadBufferInit(connection, application, cube, bufferId, duplctAggrMethod, options, size);
    }

    public static IEssPerformCustomCalc getPerformCustomCalcInstance(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        IEssPerformCustomCalc customCalc = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        customCalc = existingCube.getPerformCustomCalcInstance();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getPerformCustomCalcInstance of HspEssbaseMainJAPIImpl for application " + application + " and cube " + cube);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return customCalc;
    }

    public static boolean performCustomCalc(final IEssPerformCustomCalc customCalc, final boolean verifyOnly, final List outErrorsAndWarnings) throws EssbaseException {
        logger.entering(customCalc, verifyOnly);
        validateArguments(customCalc, CUSTOM_CALC);
        validateArguments(outErrorsAndWarnings, ERR_WARN_LIST);
        boolean returnVal = false;
        try {
            returnVal = customCalc.performCustomCalc(verifyOnly, outErrorsAndWarnings);
        } catch (Exception e) {
            logger.warn("Exception in performCustomCalc of HspEssbaseMainJAPIImpl");
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return returnVal;
    }

    public static IEssPerformAllocation getPerformAllocationInstance(final EssbaseConnection connection, final String application, final String cube) throws EssbaseException {
        logger.entering(connection, application, cube);
        validateArguments(connection, application, cube);
        IEssPerformAllocation performAlloc = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        performAlloc = existingCube.getPerformAllocationInstance();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getPerformAllocationInstance of HspEssbaseMainJAPIImpl for application " + application + " and cube " + cube);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return performAlloc;
    }

    public static boolean performAllocationASO(final IEssPerformAllocation performAlloc, final boolean verifyOnly, final List outErrorsAndWarnings) throws EssbaseException {
        logger.entering(performAlloc, verifyOnly);
        validateArguments(performAlloc, PERFORM_ALLOC);
        validateArguments(outErrorsAndWarnings, ERR_WARN_LIST);
        boolean returnVal = false;
        try {
            returnVal = performAlloc.performAllocation(verifyOnly, outErrorsAndWarnings);
        } catch (Exception e) {
            logger.warn("Exception in performAllocationASO of HspEssbaseMainJAPIImpl");
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return returnVal;
    }
    
    public static boolean performAllocationASO22(final IEssPerformAllocation performAlloc, final boolean verifyOnly, final List outErrorsAndWarnings) throws EssbaseException {
        logger.entering(performAlloc, verifyOnly);
        validateArguments(performAlloc, PERFORM_ALLOC);
        validateArguments(outErrorsAndWarnings, ERR_WARN_LIST);
        boolean returnVal = false;
        try {
            returnVal = performAlloc.performAllocation(verifyOnly, outErrorsAndWarnings);
        } catch (Exception e) {
            logger.warn("Exception in performAllocationASO of HspEssbaseMainJAPIImpl : "+e.getMessage());
        } finally {
            logger.exiting();
        }
        return returnVal;
    }

    public static String[] getCalcList(final EssbaseConnection connection, final String application, final String cube, final String userName) throws EssbaseException {
        logger.entering(connection, application, cube, userName);
        validateArguments(connection, application, cube);
        validateArguments(userName, USER_NAME_NULL);
        String[] calcList = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final IEssCalcList essCalcList = existingApp.getCalcListEx(userName, existingCube);
                        if (null != essCalcList) {
                            if (essCalcList.getAllCalcs()) {
                                IEssIterator fileObjsItr = olapServer.getOlapFileObjects(application, cube, IEssOlapFileObject.TYPE_CALCSCRIPT);
                                if (null != fileObjsItr) {
                                    calcList = new String[fileObjsItr.getCount()];
                                    IEssOlapFileObject olapFileObj = null;
                                    for (int i = 0; i < fileObjsItr.getCount(); i++) {
                                        olapFileObj = (IEssOlapFileObject)fileObjsItr.getAt(i);
                                        calcList[i] = olapFileObj.getName();
                                    }
                                }
                            } else {
                                calcList = essCalcList.getCalcList();
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getCalcList of HspEssbaseMainJAPIImpl for application " + application + "  cube " + cube + " for user " + userName);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return calcList;
    }

    public static HspMbrError[] importData(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final int rulesObjectType, final String dataFileName, final int dataFileType, final boolean abortOnError) throws EssbaseException {
        logger.entering(connection, application, cube, rulesFileName, rulesObjectType, dataFileName, dataFileType, abortOnError);
        validateArguments(connection, application, cube);
        validateArguments(rulesFileName, RULE_FILE_NAME);
        validateArguments(dataFileName, DATA_FILE_NAME);
        HspMbrError[] mbrErrors = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final String[][] dataLoadErrors = existingCube.loadData(rulesObjectType, rulesFileName, dataFileType, dataFileName, abortOnError);
                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in importData of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " rules file name " + rulesFileName + " , dataFileName " + dataFileName + " , and abort on error " + abortOnError);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return mbrErrors;
    }

    public static void syncUsersAndGroupsWithApp(final EssbaseConnection connection, final String application, final String userName) throws EssbaseException {
        logger.entering(connection, application, userName);
        validateArguments(connection, application);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer && (olapServer.getOlapSecurityMode() == ESS_CAS_SECURITY)) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    if (HspUtils.isNullOrEmpty(userName)) {
                        olapServer.syncUsersAndGroupsWithApp(existingApp);
                    } else {
                        final IEssOlapUser existingUser = olapServer.getOlapUser(userName);
                        if (null != existingUser) {
                            existingUser.syncUserWithApp(existingApp);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in syncUsersAndGroupsWithApp of HspEssbaseMainJAPIImpl for application " + application + "  and for user " + userName);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static boolean deleteUser(final EssbaseConnection connection, final String userName) throws EssbaseException {
        logger.entering(connection, userName);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        boolean returnVal = false;
        boolean shouldDelete = true;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapUser existingUser = olapServer.getOlapUser(userName);
                if (null != existingUser) {
                    if (existingUser.getMaxAccess() != IEssPrivilege.ESS_ACCESS_SUPER) {
                        final IEssIterator appsItr = olapServer.getApplications();
                        if (null != appsItr) {
                            final IEssBaseObject[] allBaseObjects = appsItr.getAll();
                            for (final IEssBaseObject aBaseObject : allBaseObjects) {
                                final IEssOlapApplication olapApp = (IEssOlapApplication)aBaseObject;
                                if (null != olapApp) {
                                    final IEssOlapApplication.EEssOlapApplicationAccess access = olapApp.getUserOrGroupAccess(userName);
                                    if (null != access && !access.equals(IEssOlapApplication.EEssOlapApplicationAccess.NONE)) {
                                        shouldDelete = false;
                                        break;
                                    }
                                }
                            }
                            if (shouldDelete) {
                                existingUser.deleteUser();
                                returnVal = true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in deleteUser of HspEssbaseMainJAPIImpl for user " + userName);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return returnVal;
    }

    public static void assignFilterToUser(final EssbaseConnection connection, final String application, final String cube, final String userName, final String filterName) throws EssbaseException {
        logger.entering(connection, application, cube, userName, filterName);
        validateArguments(connection, application, cube);
        validateArguments(userName, USER_NAME_NULL);
        validateArguments(filterName, FILTER_NAME_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final IEssCube.IEssSecurityFilter filter = existingCube.getSecurityFilter(filterName);
                        if (null != filter) {
                            final IEssOlapUser existingUser = olapServer.getOlapUser(userName);
                            if (null != existingUser) {
                                final String[] allAccess = filter.getSecurityFilterList();
                                if (HspUtils.isNullOrEmpty(allAccess)) {
                                    existingCube.setSecurityFilterList(filter, new String[] { userName });
                                } else {
                                	List<String> allAccessList = new ArrayList<String>();
                                	allAccessList.addAll(Arrays.asList(allAccess));
                                    if (!allAccessList.contains(userName)) {
                                        allAccessList.add(userName);
                                        System.out.println(allAccessList.toArray(new String[0]));
                                        existingCube.setSecurityFilterList(filter, allAccessList.toArray(new String[0]));
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in assignFilterToUser of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + ", filter " + filterName + " for user " + userName);
            System.out.println(e.getMessage());
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void verifyFilterRow(final EssbaseConnection connection, final String application, final String cube, final String filterName, final String filterRow) throws EssbaseException {
        logger.entering(connection, application, cube, filterName, filterRow);
        validateArguments(connection, application, cube);
        validateArguments(filterName, FILTER_NAME_NULL);
        validateArguments(filterRow, FILTER_ROW_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final IEssCube.IEssSecurityFilter filter = existingCube.getSecurityFilter(filterName);
                        if (null != filter) {
                            ((EssSecurityFilter)filter).verifyFilterRow(filterRow);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in verifyFilterRow of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + ", filter " + filterName + " for filter row " + filterRow);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }
    
    public static void verifyFilterRow(final EssbaseConnection connection, IEssSecurityFilter filter, final String filterRow) throws EssbaseException {
        logger.entering(connection, filterRow);
        validateArguments(connection);
        validateArguments(filter, FILTER_NULL);
        validateArguments(filterRow, FILTER_ROW_NULL);
        try {
			validateEssbaseConnection(connection);
			((EssSecurityFilter)filter).verifyFilterRow(filterRow);
        } catch (Exception e) {
            logger.warn("Exception in verifyFilterRow of HspEssbaseMainJAPIImpl for filter row " + filterRow);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

/*    public static void setFilterEx(final EssbaseConnection connection, final String application, final String cube, final String filterName, final boolean active, final int access) throws EssbaseException {
        logger.entering(connection, application, cube, filterName, active, access);
        validateArguments(connection, application, cube);
        validateArguments(filterName, FILTER_NAME_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final IEssCube.EEssCubeAccess cubeAccess = IEssCube.EEssCubeAccess.sm_fromInt(access);
                        existingCube.createSecurityFilter(filterName);
                        existingCube.setSecurityFilter(filterName, active, cubeAccess);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setFilterEx of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + ", filter " + filterName + " with value of active as " + active + " and access " + access);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }*/

    public static IEssSecurityFilter setFilterEx(final EssbaseConnection connection, final String application, final String cube, final String filterName, final boolean active, final int access) throws EssbaseException {
        logger.entering(connection, application, cube, filterName, active, access);
        validateArguments(connection, application, cube);
        validateArguments(filterName, FILTER_NAME_NULL);
        IEssSecurityFilter securityFilter = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final IEssCube.EEssCubeAccess cubeAccess = IEssCube.EEssCubeAccess.sm_fromInt(access);
                        securityFilter = existingCube.setSecurityFilter(filterName, active, cubeAccess);
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setFilterEx of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + ", filter " + filterName + " with value of active as " + active + " and access " + access);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return securityFilter;
    }
    
    public static void setFilterRow(final EssbaseConnection connection, final String application, final String cube, final String filterName, final String filterRow, final short access) throws EssbaseException {
        logger.entering(connection, application, cube, filterName, filterRow, access);
        validateArguments(connection, application, cube);
        validateArguments(filterName, FILTER_NAME_NULL);
        validateArguments(filterRow, FILTER_ROW_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final IEssCube.IEssSecurityFilter filter = existingCube.getSecurityFilter(filterName);
                        if (null != filter) {
                            filter.setFilterRow(filterRow, access);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setFilterRow of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + ", filter " + filterName + " for filter row " + filterRow + " and access " + access);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }
    
    public static void setFilterRow(final EssbaseConnection connection, final IEssSecurityFilter filter, final String filterRow, final short access) throws EssbaseException {
        logger.entering(connection, filterRow, access);
        validateArguments(connection);
        validateArguments(filter, FILTER_NULL);
        validateArguments(filterRow, FILTER_ROW_NULL);
        try {
            validateEssbaseConnection(connection);
            filter.setFilterRow(filterRow, access);
        } catch (Exception e) {
            logger.warn("Exception in setFilterRow of HspEssbaseMainJAPIImpl for filter row " + filterRow + " and access " + access);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

/*    public static void verifyFilter(final EssbaseConnection connection, final String application, final String cube, final String filterName, final String[] filterRows) throws EssbaseException {
        logger.entering(connection, application, cube, filterName, filterRows);
        validateArguments(connection, application, cube);
        validateArguments(filterName, FILTER_NAME_NULL);
        validateArguments(filterRows, FILTER_ROWS_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final IEssCube.IEssSecurityFilter filter = existingCube.getSecurityFilter(filterName);
                        if (null != filter) {
                        	List<String> filterRowsList = new ArrayList<String>();
                        	filterRowsList.addAll(Arrays.asList(filterRows));
                            //List<String> filterRowsList = Arrays.asList(filterRows);
                            final String last = filterRowsList.get(filterRowsList.size()-1);
                            if (!HspUtils.isNullOrEmpty(last)) {
                                filterRowsList.add(null);
                            }
                            filter.verifyFilter(filterRowsList.toArray(new String[0]));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in verifyFilter of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + ", filter " + filterName);
            System.out.println(e.getMessage());
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }*/
    
    public static IEssSecurityFilter verifyFilter(final EssbaseConnection connection, final String application, final String cube, final String filterName, final String[] filterRows) throws EssbaseException {
        logger.entering(connection, application, cube, filterName, filterRows);
        validateArguments(connection, application, cube);
        validateArguments(filterName, FILTER_NAME_NULL);
        validateArguments(filterRows, FILTER_ROWS_NULL);
        IEssSecurityFilter filter = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                    	filter = existingCube.getSecurityFilter(filterName);
                        if (null != filter) {
                        	List<String> filterRowsList = new ArrayList<String>();
                        	filterRowsList.addAll(Arrays.asList(filterRows));
                            final String last = filterRowsList.get(filterRowsList.size()-1);
                            if (!HspUtils.isNullOrEmpty(last)) {
                                filterRowsList.add(null);
                            }
                            filter.verifyFilter(filterRowsList.toArray(new String[0]));
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in verifyFilter of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + ", filter " + filterName);
            System.out.println(e.getMessage());
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return filter;
    }

    public static short getUserType(final EssbaseConnection connection, final String userName) throws EssbaseException {
        logger.entering(connection, userName);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        short returnVal = 0;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapUser existingUser = olapServer.getOlapUser(userName);
                if (null != existingUser) {
                    returnVal = existingUser.getUserType();
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in getUserType of HspEssbaseMainJAPIImpl for user " + userName);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return returnVal;
    }

    public static boolean setUserAccess(final EssbaseConnection connection, final String userName, final boolean changePassword) throws EssbaseException {
        logger.entering(connection, userName);
        validateArguments(connection);
        validateArguments(userName, USER_NAME_NULL);
        boolean setUser = false;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapUser existingUser = olapServer.getOlapUser(userName);
                if (null != existingUser) {
                    if (existingUser.getMaxAccess() != IEssPrivilege.ESS_ACCESS_SUPER) {
                        if (existingUser.getAccess() != IEssPrivilege.ESS_ACCESS_NONE) {
                            existingUser.setAccess(IEssPrivilege.ESS_ACCESS_NONE);
                            setUser = true;
                        }
                        if (changePassword && !existingUser.isPasswordChangeNeededNow()) {
                            setUser = true;
                            existingUser.setPasswordChangeNeededNow(true);
                        }
                        if (setUser) {
                            existingUser.updatePropertyValues();
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in setUserAccess of HspEssbaseMainJAPIImpl for user " + userName + " and change password " + changePassword);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return setUser;
    }

    public static void updateCell(final EssbaseConnection connection, final String application, final String cube, final String[] queries) throws EssbaseException {
        logger.entering(connection, application, cube, queries);
        validateArguments(connection, application, cube);
        validateArguments(queries, QUERIES_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        olapServer.setActive(application, cube);
                        existingCube.beginUpdate(true, false);
                        for (final String aQuery : queries) {
                            existingCube.sendString(aQuery);
                        }
                        existingCube.endUpdate();
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in updateCell of HspEssbaseMainJAPIImpl for application " + application + " cube " + cube + " .", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
    }

    public static void processMDXRowQuery(final EssbaseConnection essbaseConnection, final HspMDXDataQueryBuilder dataQueryBuilder) throws EssbaseException {
        MDXRowQueryProcessor.processMDXRowQuery(essbaseConnection, dataQueryBuilder);
    }

    public static HspMbrError[] importASO(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final String dataFileName, final boolean abortOnError, final int bufferId, long commitType, long actionType, long options) throws EssbaseException {
        logger.entering(connection, application, cube, rulesFileName, dataFileName, abortOnError, bufferId);
        validateArguments(connection, application, cube);
        validateArguments(rulesFileName, RULE_FILE_NAME);
        validateArguments(dataFileName, DATA_FILE_NAME);
        HspMbrError[] mbrErrors = null;
        IEssCube existingCube = null;
        boolean isBufferCleared = false;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.loadBufferInit(bufferId, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_DUPLICATES_ADD, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_IGNORE_MISSING_VALUES, 100);
                        final String[][] dataLoadErrors = existingCube.beginDataload(rulesFileName, IEssOlapFileObject.TYPE_TEXT, dataFileName, IEssOlapFileObject.TYPE_TEXT, abortOnError, bufferId);
                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
                        }
                        if(null != mbrErrors){
                            existingCube.loadBufferTerm(new long[] { bufferId }, commitType, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_ABORT, options);
                        }else{
                            existingCube.loadBufferTerm(new long[] { bufferId }, commitType, actionType, options);
                        }
                        isBufferCleared = true;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in importASO of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " rules file name " + rulesFileName + " , dataFileName " + dataFileName + " , and buffer id " + bufferId);
            doExceptionTranslation(e);
        } finally {
        	try {
			if (!isBufferCleared && existingCube != null) {
				if (null != mbrErrors) {
						existingCube.loadBufferTerm(new long[] { 6 }, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA,	IEssCube.ESS_ASO_DATA_LOAD_BUFFER_ABORT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE);
				} else {
					existingCube.loadBufferTerm(new long[] { 6 }, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA,	IEssCube.ESS_ASO_DATA_LOAD_BUFFER_COMMIT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE);
				}
			}
        	} catch (EssException e) {
				e.printStackTrace();
			}
            logger.exiting();
        }
        return mbrErrors;
    }

    public static HspMbrError[] importASO(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final int rulesObjectType, final String dataFileName, final int dataFileType, final boolean abortOnError, final int bufferId, long commitType, long actionType, long options) throws EssbaseException {
        logger.entering(connection, application, cube, rulesFileName, rulesObjectType, dataFileName, dataFileType, abortOnError, bufferId);
        validateArguments(connection, application, cube);
        validateArguments(rulesFileName, RULE_FILE_NAME);
        validateArguments(dataFileName, DATA_FILE_NAME);
        HspMbrError[] mbrErrors = null;
        IEssCube existingCube = null;
        boolean isBufferCleared = false;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.loadBufferInit(bufferId, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_DUPLICATES_ADD, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_IGNORE_MISSING_VALUES, 100);

                        final String[][] dataLoadErrors = existingCube.beginDataload(rulesFileName, rulesObjectType, dataFileName, dataFileType, abortOnError, bufferId);
                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
                        }
                        if(null != mbrErrors){
                            existingCube.loadBufferTerm(new long[] { bufferId }, commitType, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_ABORT, options);
                        }else{
                            existingCube.loadBufferTerm(new long[] { bufferId }, commitType, actionType, options);
                        }
                        isBufferCleared = true;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in importASO of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " rules file name " + rulesFileName + " , dataFileName " + dataFileName + " , and buffer id " + bufferId);
            doExceptionTranslation(e);
        } finally {
        	try {
    			if (!isBufferCleared && existingCube != null) {
    				if (null != mbrErrors) {
    						existingCube.loadBufferTerm(new long[] { 6 }, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA,	IEssCube.ESS_ASO_DATA_LOAD_BUFFER_ABORT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE);
    				} else {
    					existingCube.loadBufferTerm(new long[] { 6 }, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA,	IEssCube.ESS_ASO_DATA_LOAD_BUFFER_COMMIT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE);
    				}
    			}
            } catch (EssException e) {
    				e.printStackTrace();
    		}
            logger.exiting();
        }
        return mbrErrors;
    }

    public static void calcWithName(final EssbaseConnection connection, final String application, final String cube, final boolean syntaxCheckOnly, final String calcName, final String calcScript) throws EssbaseException{
        logger.entering(connection, application, cube, syntaxCheckOnly, calcName, calcScript);
        validateArguments(connection, application, cube);
        validateArguments(calcScript, CALC_SCRIPT_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final EssCube existingCube = (EssCube) existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.calculateWithClientParams(calcScript,syntaxCheckOnly,new EssCalcClientParams(calcName));
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in calcWithName of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , " + " syntaxCheckOnly " + syntaxCheckOnly + " , and calcName " + (HspUtils.isNotNullOrEmpty(calcName) ? calcName : "NA"));
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }        
    }
    
    private static HspMbrError[] convertDataLoadErrToMbrErr(final String[][] dataLoadErrors) {
        HspMbrError[] mbrErrors = null;
        mbrErrors = new HspMbrError[dataLoadErrors.length];
        for (int i = 0; dataLoadErrors != null && i < dataLoadErrors.length; i++) {
            int error = HspMbrError.ESS_MBRERR_UNKNOWN;
            try {
                error = Integer.valueOf(dataLoadErrors[i][0]);
            } catch (NumberFormatException nfe) {
                logger.warn("Exception in converting Essbase error number to int. Default value " + HspMbrError.ESS_MBRERR_UNKNOWN + " will be used.");
            }
            mbrErrors[i] = new HspMbrError(error, dataLoadErrors[i][1], dataLoadErrors[i][2]);
        }
        return mbrErrors;
    }
    
    public static EssbaseConnection login(String server, String user, String password, String[][] apps) throws EssbaseException {
        logger.entering(server, user, apps);
        validateArguments(server, SERVER_NULL);
        validateArguments(user, USER_NAME_NULL);
        validateArguments(password, USER_PWD_NULL);
        EssbaseConnection connection = null;
        try {
            connection = new EssbaseConnection(user, password, false, null, server, null, null);
            connection.connect();
            validateEssbaseConnection(connection);
            if(apps != null){
                final IEssOlapServer olapServer = connection.getOlapServer();
                if (null != olapServer) {
                    final IEssIterator appsItr = olapServer.getApplications();
                    if (null != appsItr && appsItr.getCount() > 0) {
                        final IEssBaseObject[] allBaseObjects = appsItr.getAll();
                        int i = 0;
                        int j = 0;
                        for (final IEssBaseObject aBaseObject : allBaseObjects) {
                            final IEssOlapApplication olapApp = (IEssOlapApplication)aBaseObject;
                            System.out.println("App name  : "+olapApp.getName()+olapApp.getCountCubes());
                            apps[0][i++] = olapApp.getName();
                            final IEssIterator cubesItr = olapApp.getCubes();
                            if (null != cubesItr && cubesItr.getCount() > 0) {
                                final IEssBaseObject[] cubesBaseObjects = cubesItr.getAll();
                                for (final IEssBaseObject cubeBaseObject : cubesBaseObjects) {
                                    final IEssCube cube = (IEssCube)cubeBaseObject;
                                    apps[1][j++] = cube.getName();
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in login of HspEssbaseMainJAPIImpl ", e);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return connection;
    }
    
    public static HspMbrError[] updateFileEx(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final String dataFileName, final int dataFileType, final boolean abortOnError) throws EssbaseException {
        logger.entering(connection, application, cube, dataFileName, abortOnError);
        validateArguments(connection, application, cube);
        validateArguments(dataFileName, DATA_FILE_NAME);
        HspMbrError[] mbrErrors = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        final String[][] dataLoadErrors = existingCube.loadData(IEssOlapFileObject.TYPE_RULES, rulesFileName, dataFileType, dataFileName, abortOnError);
                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in updateFileEx of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , rulesFileName " + rulesFileName + " , dataFileName " + dataFileName + " and dataFileType " + dataFileType);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return mbrErrors;
    }
    
    public static HspMbrError[] updateFileEx(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final String dataFileName, final boolean store, boolean unlock, final boolean abortOnError) throws EssbaseException {
        logger.entering(connection, application, cube, dataFileName, store, unlock);
        validateArguments(connection, application, cube);
        validateArguments(dataFileName, DATA_FILE_NAME);
        HspMbrError[] mbrErrors = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                    	existingCube.beginDataload(store, unlock, abortOnError, rulesFileName, IEssOlapFileObject.TYPE_RULES);
                    	File file = new File(dataFileName);
                        FileReader fileReader = new FileReader(file);
                        BufferedReader bufferReader = new BufferedReader(fileReader);  
                        String line;
                        StringBuilder fileData = new StringBuilder();
                        while ((line = bufferReader.readLine()) != null){
                             fileData.append(line).append(System.lineSeparator());
                        }
                        fileReader.close();
                        bufferReader.close();
                        existingCube.sendString(fileData.toString());
                        final String[][] dataLoadErrors = existingCube.endDataload();
                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
                        }
                    }
                }
            }
        } catch (Exception e) {
        	logger.warn("Exception in updateFileEx of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , rulesFileName " + rulesFileName + " , and dataFileName " + dataFileName);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return mbrErrors;
    }

    public static HspMbrError[] updateFileASOEx2(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final String dataFileName, final int dataFileType, final boolean store, final boolean unlock, final int bufferId, long commitType, long actionType, long options, long size) throws EssbaseException {

        logger.entering(connection, application, cube, dataFileName, store, bufferId);
        validateArguments(connection, application, cube);
        validateArguments(dataFileName, DATA_FILE_NAME);
        HspMbrError[] mbrErrors = null;
        boolean isBufferCleared = false;
        IEssCube existingCube = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.loadBufferInit(bufferId, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_DUPLICATES_ADD, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_IGNORE_MISSING_VALUES, 100);
                        final String[][] dataLoadErrors = existingCube.beginDataload(rulesFileName, IEssOlapFileObject.TYPE_RULES, dataFileName, dataFileType, false, bufferId);
                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
                        }
                        if(null != mbrErrors){
                            existingCube.loadBufferTerm(new long[] { bufferId }, commitType, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_ABORT, options);
                        }else{
                            existingCube.loadBufferTerm(new long[] { bufferId }, commitType, actionType, options);
                        }
                        isBufferCleared = true;
                    }
                }
            }
        } catch (Exception e) {
            if(!isBufferCleared){
            	try{
	            	if(null != mbrErrors){
	                    existingCube.loadBufferTerm(new long[] { bufferId }, commitType, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_ABORT, options);
	                }else{
	                    existingCube.loadBufferTerm(new long[] { bufferId }, commitType, actionType, options);
	                }
            	}catch(Exception bufferClearEx){
            		logger.warn("Exception in updateFileASOEx2 of HspEssbaseMainJAPIImpl during buffer clearance for application " + application + " , cube " + cube + " , rulesFileName " + rulesFileName +" , dataFileName " + dataFileName + " , and buffer id " + bufferId);
                    doExceptionTranslation(bufferClearEx);
            	}
            }
        	logger.warn("Exception in updateFileASOEx2 of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , rulesFileName " + rulesFileName +" , dataFileName " + dataFileName + " , and buffer id " + bufferId);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return mbrErrors;
    }
    
    public static HspMbrError[] updateFileASOEx2(final EssbaseConnection connection, final String application, final String cube, final String rulesFileName, final String dataFileName, final int dataFileType, final boolean store, final boolean unlock, final boolean abortOnError, final int bufferId, long commitType, long actionType, long options, long size) throws EssbaseException {

        logger.entering(connection, application, cube, dataFileName, store, bufferId);
        validateArguments(connection, application, cube);
        validateArguments(dataFileName, DATA_FILE_NAME);
        HspMbrError[] mbrErrors = null;
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer olapServer = connection.getOlapServer();
            if (null != olapServer) {
                final IEssOlapApplication existingApp = olapServer.getApplication(application);
                if (null != existingApp) {
                    final IEssCube existingCube = existingApp.getCube(cube);
                    if (null != existingCube) {
                        existingCube.loadBufferInit(bufferId, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_DUPLICATES_ADD, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_IGNORE_MISSING_VALUES, 100);
                        final String[][] dataLoadErrors = existingCube.beginDataload(rulesFileName, IEssOlapFileObject.TYPE_RULES, dataFileName, dataFileType, abortOnError, bufferId);
                        if ((null != dataLoadErrors) && (dataLoadErrors.length > 0)) {
                            mbrErrors = convertDataLoadErrToMbrErr(dataLoadErrors);
                        }
                        if(null != mbrErrors){
                            existingCube.loadBufferTerm(new long[] { bufferId }, commitType, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_ABORT, options);
                        }else{
                            existingCube.loadBufferTerm(new long[] { bufferId }, commitType, actionType, options);
                        }
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Exception in updateFileASOEx2 of HspEssbaseMainJAPIImpl for application " + application + " , cube " + cube + " , rulesFileName " + rulesFileName +" , dataFileName " + dataFileName + " , and buffer id " + bufferId);
            doExceptionTranslation(e);
        } finally {
            logger.exiting();
        }
        return mbrErrors;
    }
    
    public static boolean setUserDbAccess(final EssbaseConnection connection, final String user, final String application, int access, boolean calcAccess) throws EssException {
        logger.entering(connection, user, application, access, calcAccess);
        validateArguments(connection, application);
        validateArguments(user, USER_NAME_NULL);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapServer server = connection.getOlapServer();
            if(server != null) {
                final IEssOlapUser userObject = server.getOlapUser(user);
                if(userObject != null){
                	//final long maxAccess = userObject.getMaxAccess();
                	long maxAccess = 3; 
                    if(maxAccess != IEssOlapUser.EEssAccess.ESS_ACCESS_SUPER_INT_VALUE){
                        final IEssOlapApplication olapApp = connection.getApp(application);
                        if(olapApp != null){
                            final IEssIterator cubesItr = olapApp.getCubes();
                            if (null != cubesItr && cubesItr.getCount() > 0) {
                                final IEssBaseObject[] cubesBaseObjects = cubesItr.getAll();
                                short mode = server.getOlapSecurityMode();
                                mode = 3;
                                for (final IEssBaseObject cubeBaseObject : cubesBaseObjects) {
                                    final IEssCube cube = (IEssCube)cubeBaseObject;
                                    final EEssCubeAccess cubeAccess = cube.getCubeAccess();
                                    if(cubeAccess.intValue() != access && mode != ESS_SECURITY_MODE_CSA){//check for mode
                                    	cube.setCubeAccess(EEssCubeAccess.sm_fromInt(access));
                                    	final IEssIterator filterItr = cube.getSecurityFilters();
                                    	if (null != filterItr && filterItr.getCount() > 0) {
                                    		final IEssBaseObject[] filterObjects = filterItr.getAll();
                                    		for (final IEssBaseObject filterObject : filterObjects) {
                                    			final IEssSecurityFilter filter = (IEssSecurityFilter)filterObject;
                                    			List<String> userNames =  new ArrayList<String>(Arrays.asList(filter.getSecurityFilterList()));
                                    			System.out.println(Arrays.toString(userNames.toArray()));
                                    			if(userNames.contains(user+"@Native Directory")){
                                    				userNames.remove(user+"@Native Directory");
                                    				cube.setSecurityFilterList(filter, userNames.toArray(new String[0]));
//                                    				final IEssMaxlSession maxlSession = server.openMaxlSession(PLANNING_JAPI_SESSION_ID);
//                                    				System.out.println(ALTER_USER + user + REVOKE_FILTER + filter.getName());
//                                    	            final boolean executed = maxlSession.execute(ALTER_USER + user + SPACE + REVOKE_FILTER + filter.getName());
                                    	            userNames =  new ArrayList<String>(Arrays.asList(filter.getSecurityFilterList()));
                                        			System.out.println(Arrays.toString(userNames.toArray()));
                                    			}
                                    		}
                                    	}
                                    	if((access & IEssPrivilege.ESS_PRIV_CALC) > 0){//replace 1 with ESS_PRIV_CALC
                                    		olapApp.setCalcList(user, cube, calcAccess, null);
                                    	}
                                    }
                                }
                            }
                        }
                    }else{
                    	return false;
                    }
                }
            }
        }catch(Exception e){
            
        }
        return true;
    }
    
    private static final short ASO_STORAGE_TYPE_SHORT = 4;
    private static final short DEF_STORGAE_TYPE = 0;
    private static final short ESS_USERTYPE_CMD_ADD = 0;
    private static final short ESS_CAS_SECURITY = 2;

    private static final int APP_IDX = 2;
    private static final int ASO_STORAGE_TYPE = 4;
    private static final int ASO_CUBE_TYPE = 2;
    private static final int CLUSTER_RATIO_COLUMN_IDX = 13;
    private static final int DB_IDX = 3;
    private static final int ESS_SECURITY_MODE_CSA = 2;
    private static final int OBSOLETE_INDEX_TYPE = 1; // This is obsolete as per the documentation and 1 is the fixed value returned in EAS Console.
    private static final int READ_ONLY_COLUMN_IDX = 31;
    private static final int SERVER_IDX = 4;
    private static final int VAR_IDX = 0;
    private static final int VAR_VAL_IDX = 1;

    private static final double OBSOLETE_NON_MISSING_BLOCKS = 0.0; //This is obsolete as per the documentation and is always 0.0;

    private static final String ALTER_USER = "alter user ";
    private static final String APP_ID_NULL = "Application Id.";
    private static final String APP_NAME = "Application Name ";
    private static final String APP_TYPE_NATIVE = "native";
    private static final String APP_TYPE_UTF8 = "utf8";
    private static final String CALC_SCRIPT_FILE_NULL = "Calculation script file name ";
    private static final String CALC_SCRIPT_NAM = "Calc Script Name";
    private static final String CALC_SCRIPT_NULL = "Calculation script ";
    private static final String CLIENT_FILE_NULL = "Client file name .";
    private static final String COMMA = "'";
    private static final String CONN_PARAM_NULL = "Connection parameter ";
    private static final String CUSTOM_CALC = "Custom Calc Obj";
    private static final String DATA_FILE_NAME = "DATA File Name";
    private static final String DB_STATS_DATA_BLKS_SUFFIX = " get dbstats data_block";
    private static final String DISPLAY_DB_MAXL_PREFIX = "display database ";
    private static final String ERR_FILE_NULL = "Error file name ";
    private static final String ERR_WARN_LIST = "Error & Warning list";
    private static final String FILTER_NULL = "Filter";
    private static final String FILTER_NAME_NULL = "Filter name ";
    private static final String FILTER_ROW_NULL = "Filter row ";
    private static final String FILTER_ROWS_NULL = "Filter rows ";
    private static final String FORMULA_NULL = "Formula ";
    private static final String GET_KEY_WORD = "getKeyWord";
    private static final String GET_SM_STATS = "getSMStats";
    private static final String GETD_VALUE = "getdValue";
    private static final String IESSCUBE_NULL = "IEssCube ";
    private static final String JOINING_STR = "'.'";
    private static final String MEMBERS_NULL = "Members ";
    private static final String NEW_APP_NAME_NULL = "New application name ";
    private static final String NEW_NAME_CUBE_NULL = "New name of cube ";
    private static final String QUERY_STR_NULL = "Query string ";
    private static final String QUERIES_NULL = "Queries ";
    private static final String OBJ_NAME_NULL = "Object name ";
    private static final String PERFORM_ALLOC = "Perform Alloc Obj";
    private static final String PLANNING_JAPI_SESSION_ID = "PLANNINGJAPISESSION";
    private static final String PROVIDER_NULL = "Provider ";
    private static final String QUERY_DB_MAXL_PREFIX = "query database ";
    private static final String REGION_NULL = "Region value ";
    private static final String RULE_FILE_NAME = "Rule File Name";
    private static final String REVOKE_FILTER = "revoke filter ";
    private static final String SERVER_NULL = "Server ";
    private static final String SPACE = " ";
    private static final String TARGET_APP_NULL = "Target application ";
    private static final String TARGET_CUBE_NULL = "Target cube ";
    private static final String VA_NAME_NULL = "Variable name ";
    private static final String USER_NAME_NULL = "User name ";
    private static final String USER_PWD_NULL = "User password ";
    private static final String UNICODE_BYTE_ORDER_MARK = "\uFEFF\n\r";
}