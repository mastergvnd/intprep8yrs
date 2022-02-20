package com.hyperion.planning.op;


import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssMaxlSession;
import com.essbase.api.metadata.IEssDimension;

import com.hyperion.calcmgr.core.StringConstants;
import com.hyperion.calcmgr.provider.essbase.CMMaxlResultSet;
import com.hyperion.calcmgr.provider.essbase.DefaultJAPISession;
import com.hyperion.calcmgr.provider.essbase.IJAPISession;
import com.hyperion.calcmgr.provider.essbase.IMaxlResultSet;
import com.hyperion.planning.HspCSM;
import com.hyperion.planning.HspConstants;
import com.hyperion.planning.HspJS;
import com.hyperion.planning.HspJSHome;
import com.hyperion.planning.HspJSImpl;
import com.hyperion.planning.HspObjectNameComparator;
import com.hyperion.planning.HspRuntimeException;
import com.hyperion.planning.HspUtils;
import com.hyperion.planning.HyperionPlanningBean;
import com.hyperion.planning.db.HspDEDB;
import com.hyperion.planning.jobs.HspOLUJobHelper;
import com.hyperion.planning.odl.HspODLLogger;
import com.hyperion.planning.olap.HspCubeRefreshTask;
import com.hyperion.planning.olap.HspOlapException;
import com.hyperion.planning.olap.japi.EssbaseAttributeDimension;
import com.hyperion.planning.olap.japi.EssbaseConnection;
import com.hyperion.planning.olap.japi.EssbaseDimension;
import com.hyperion.planning.olap.japi.OutlineTypeDetector;
import com.hyperion.planning.sql.HspAttributeDimension;
import com.hyperion.planning.sql.HspCube;
import com.hyperion.planning.sql.HspDataSource;
import com.hyperion.planning.sql.HspDimension;
import com.hyperion.planning.sql.HspJobStatus;
import com.hyperion.planning.sql.HspOutlineLoadStatus;
import com.hyperion.planning.utils.HspJobStatusUtils;
import com.hyperion.planning.utils.HspOutlineLoad;

import java.io.File;
import java.io.InputStream;
import java.io.PrintWriter;

import java.sql.Timestamp;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.Vector;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.output.CloseShieldOutputStream;


public class OutlineToApplicationBuilder {

    private static final HspODLLogger logger = HspODLLogger.getLogger();

    public static void importOutlineFromOtl(String cubeName, HspJSImpl hspJS, HyperionPlanningBean planning) throws Exception {
        HspJobStatus jobStatus = null;
        String jobMessage = HspUtils.getLocalizedMessage(LABEL_PROCESS_OTL_PROCESSING_STARTED, null, new Locale[] { planning.getMyLocale() });
        int runStatus = -1;
        EssbaseApplicationShapeSource essbaseAppShapeSource = null;
        try {
            jobStatus = insertInitJobStatus(cubeName, planning);
            final boolean isAso = isOutlineAso(planning.getHspJSHome(), cubeName, planning.getCurrentApplication());
            if (isAso) {
                //TODO delete the BSO appplication is otl type is ASO
                //TODO create ASO Essbase application with an ASO cube name matching the otl file name.
                try {
                    String otlFilePath = OperationalPlanningHelper.getOtlFileFromInbox(cubeName);
                    essbaseAppShapeSource = EssbaseApplicationShapeSource.createEssbaseAppShapeSourceFromOtlFile(otlFilePath, hspJS, isAso, cubeName);
                } catch (HspOlapException hoe) {
                    String message = "";
                    if (hoe.getCause() != null && HspUtils.isNotNullOrEmpty(hoe.getCause().getMessage())) {
                        message = hoe.getCause().getMessage();
                    }
                    if (HspUtils.isNullOrEmpty(message)) {
                        message = hoe.getMessage();
                    }
                    throw new HspRuntimeException(LABEL_PROCESS_OTL_EXTRACT_FAILED, HspUtils.createProperties(ERROR_MSG, message));
                }
            } else {
                EssbaseConnection connection = connectToEssbaseApp(hspJS, HspConstants.ESSBASE_TEMP_APP_NAME, cubeName);
                essbaseAppShapeSource = new EssbaseApplicationShapeSource(connection);
            }
            createCube(cubeName, planning, isAso);
            jobMessage = jobMessage + prepareDBStats(HspConstants.ESSBASE_TEMP_APP_NAME, cubeName, planning);
            try {
                jobMessage = jobMessage + "\n" +
                        processEssbaseTempAppToCreatePlanningApp(cubeName, hspJS, essbaseAppShapeSource, planning);
                runStatus = HspConstants.JOB_STATUS_JOB_STATUS_COMPLETED;
            } catch (Exception e) {
                runStatus = HspConstants.JOB_STATUS_JOB_STATUS_ERROR;
                jobMessage = jobMessage + "\n" +
                        e.getMessage();
                throw e;
            }

            if ((runStatus == HspConstants.JOB_STATUS_JOB_STATUS_COMPLETED)) {
                try {
                    planning.runCubeRefresh(true, true, false, HspCubeRefreshTask.FILTER_NO_OP);
                    jobMessage = jobMessage + "\n\n" +
                            HspUtils.getLocalizedMessage(LABEL_PROCESS_OTL_PROCESSING_SUCCESS, null, new Locale[] { planning.getMyLocale() });
                } catch (Exception e) {
                    runStatus = HspConstants.JOB_STATUS_JOB_STATUS_ERROR;
                    jobMessage = jobMessage + "\n\n" +
                            HspUtils.getLocalizedMessage("MSG_CUBE_REFRESH_FAILED", HspUtils.createProperties("DATABASE", cubeName, "MESSAGE", e.getMessage()), new Locale[] { planning.getMyLocale() });
                    throw e;
                }
            }
        } catch (Exception e) {
            logger.throwing(e);
            throw new HspRuntimeException(LABEL_PROCESS_OTL_PROCESSING_FAILED);
        } finally {
            if (null != essbaseAppShapeSource) {
                try {
                    deleteEssbaseTempApp(essbaseAppShapeSource);
                } catch (Exception e) {
                }
            }
            if (null != jobStatus) {
                jobStatus.setEndTime(new Timestamp(System.currentTimeMillis()));
                File exportFile = new File(HspOLUJobHelper.getFileNameWithPlanningOutboxLocation("ProcessOutlineLog_" + cubeName + ".txt"));
                try {
                    FileUtils.writeStringToFile(exportFile, jobMessage, "utf-8");
                } catch (Exception ioe) {
                }
                HspJobStatusUtils.updateJobStatus(planning, jobStatus, runStatus, jobMessage, exportFile.getName());
                long jobTime = jobStatus.getEndTime().getTime() - jobStatus.getStartTime().getTime();
                logger.fine("Outline Processing Job Exceution Time : " + jobTime / 1000 + " seconds.");
            }

        }

    }

    private static HspJobStatus insertInitJobStatus(String cubeName, HyperionPlanningBean planning) throws Exception {
        HspJobStatus jobStatus = HspJobStatusUtils.createJobStatus("Process Outline", HspConstants.JOB_STATUS_JOB_TYPE_PROCESS_OUTLINE, planning.getUserID(), HspUtils.createMap("cubeName", cubeName));
        jobStatus.setStartTime(new Timestamp(System.currentTimeMillis()));
        planning.addJobStatus(jobStatus);
        return jobStatus;
    }

    private static String processEssbaseTempAppToCreatePlanningApp(String cubeName, HspJSImpl hspJS, EssbaseApplicationShapeSource esssbaseAppShapeSource, HyperionPlanningBean planning) throws Exception {
        try {
            esssbaseAppShapeSource.convertEssbaseAppToJavaPojo(esssbaseAppShapeSource.getConnection());
            esssbaseAppShapeSource.createOluCsvFiles(cubeName);
            hidePlanningDimensions(planning, esssbaseAppShapeSource, hspJS);
        } catch (Exception e) {
            throw new HspRuntimeException(LABEL_PROCESS_OTL_EXTRACT_FAILED, HspUtils.createProperties(ERROR_MSG, e.getMessage()));
        }
        return pushEssbaseDimsToPlanning(hspJS, planning, esssbaseAppShapeSource);
    }

    private static String pushEssbaseDimsToPlanning(HspJSImpl hspJS, HyperionPlanningBean planning, EssbaseApplicationShapeSource esssbaseAppShapeSource) throws Exception {
        StringBuilder builder = new StringBuilder();
        builder.append("\n").append(HspUtils.getLocalizedMessage(LABEL_PROCESS_OTL_SRC_OTL_PROCESS_LOG, null, new Locale[] { planning.getMyLocale() })).append("\n");
        Map<EssbaseDimension, Throwable> exceptionPerDimensionLoad = new LinkedHashMap<>();
        for (EssbaseDimension mappedDim : esssbaseAppShapeSource.getDimensions()) {
            try {
                HspDimension planningDim = null;
                if (planningDim == null) {
                    if (OperationalPlanningDimensionMapping.getDimType(mappedDim.getDimensionType()) != HspConstants.kDimTypeAttribute) {
                        planningDim = EssbaseToPlanningDimensionTransformer.getHspDimension(mappedDim, planning, hspJS);
                        planning.saveDimension(planningDim);
                        if (!HspUtils.isNullOrEmpty(mappedDim.getAttributeDimensions())) {
                            List<EssbaseAttributeDimension> attributeDimensions = mappedDim.getAttributeDimensions();
                            for (EssbaseAttributeDimension attributeDim : attributeDimensions) {
                                HspAttributeDimension planningAttrDim = EssbaseToPlanningDimensionTransformer.getHspAttributeDimension(attributeDim, planningDim, planning, hspJS);
                                planning.saveDimension(planningAttrDim);
                                try (InputStream is = esssbaseAppShapeSource.createOluStreamFromDimension(attributeDim.getName())) {
                                    HspOutlineLoad outlineLoad = new HspOutlineLoad();
                                    PrintWriter logWriter = new PrintWriter(new CloseShieldOutputStream(System.out), true);
                                    PrintWriter exceptionWriter = new PrintWriter(new CloseShieldOutputStream(System.err), true);
                                    Properties props = HspUtils.createProperties("/DA:" + planningAttrDim.getName() + ":" + planningDim.getName(), "", "/IDU", "", "/DL:comma", "");
                                    HspOutlineLoadStatus status = outlineLoad.outlineLoad(planning, props, is, null, logWriter, exceptionWriter);
                                    builder.append("\n").append(prepareOLUStatusToString(status, planning));
                                    status.rethrowThrowable();
                                }
                            }
                        }
                    }
                } else {
                    planningDim = (HspDimension)planningDim.cloneForUpdate();
                    planningDim.setObjectName(mappedDim.getName());
                    planning.saveDimension(planningDim);
                }

                if (OperationalPlanningDimensionMapping.getDimType(mappedDim.getDimensionType()) != HspConstants.kDimTypeAttribute) {
                    try (InputStream is = esssbaseAppShapeSource.createOluStreamFromDimension(mappedDim.getName())) {
                        HspOutlineLoad outlineLoad = new HspOutlineLoad();
                        PrintWriter logWriter = new PrintWriter(new CloseShieldOutputStream(System.out), true);
                        PrintWriter exceptionWriter = new PrintWriter(new CloseShieldOutputStream(System.err), true);
                        Properties props = new Properties();
                        if (OperationalPlanningDimensionMapping.getObjectType(mappedDim.getDimensionType()) == HspConstants.gObjType_Period) {
                            props = HspUtils.createProperties("/D:" + planningDim.getName(), "", "/PLP", "", "/IDU", "", "/DL:comma", "", "/-N", "");
                        } else {
                            props = HspUtils.createProperties("/D:" + planningDim.getName(), "", "/IDU", "", "/DL:comma", "", "/M", "");
                        }
                        HspOutlineLoadStatus status = outlineLoad.outlineLoad(planning, props, is, null, logWriter, exceptionWriter);
                        builder.append("\n").append(prepareOLUStatusToString(status, planning));
                        status.rethrowThrowable();
                    }
                }
            } catch (Throwable e) {
                exceptionPerDimensionLoad.put(mappedDim, e);
            }
        }
        if (!exceptionPerDimensionLoad.isEmpty()) {
            throw new HspRuntimeException(LABEL_PROCESS_OTL_PROCESSING_ERR_MSG, HspUtils.createProperties(ERROR_MSG, convertToMessageString(exceptionPerDimensionLoad, planning)));
        }
        return builder.toString();
    }

    private static String convertToMessageString(Map<EssbaseDimension, Throwable> exceptionPerDimensionLoad, HyperionPlanningBean planning) {
        StringBuilder message = new StringBuilder("\n\n");
        for (Map.Entry<EssbaseDimension, Throwable> entry : exceptionPerDimensionLoad.entrySet()) {
            message.append(HspUtils.getLocalizedMessage(LABEL_PROCESS_OTL_DIM_ERR_MSG, HspUtils.createProperties(DIM_NAME, entry.getKey().getName(), ERROR_MSG, entry.getValue().getMessage()), new Locale[] { planning.getMyLocale() }));
            message.append("\n");
        }
        return message.toString();
    }

    private static void hidePlanningDimensions(HyperionPlanningBean planning, EssbaseApplicationShapeSource essbaseAppShapeSource, HspJSImpl hspJS) throws Exception {
        final int sessionId = planning.getSessionId();

        for (HspDimension planningDim : getBaseDimensions(hspJS, sessionId)) {
            if (!essbaseAppShapeSource.containsDimension(planningDim)) {
                planningDim = (HspDimension)planningDim.cloneForUpdate();
                if (!planningDim.getName().startsWith("HSP_")) {
                    planningDim.setObjectName("HSP_" + planningDim.getName());
                }
                //planningDim.setEnabled(false);
                planningDim.setUsedIn(0);
                planning.saveDimension(planningDim);
            }
        }
    }

    private static Vector<HspDimension> getBaseDimensions(HspJSImpl hspJS, int sessionId) {
        final HspDEDB hspDEDB = hspJS.getDEDB(sessionId);
        Vector<HspDimension> dims = hspDEDB.getBaseDimensions(sessionId);
        Vector<HspDimension> adminDims = null;

        if (dims != null) {
            adminDims = new Vector<HspDimension>();
            for (int d = 0; d < dims.size(); d++) {
                HspDimension dim = dims.elementAt(d);
                adminDims.addElement(dim);

            }

            try {
                HspCSM.sortVector(adminDims, new HspObjectNameComparator());
            } catch (Exception e) {
            }
        }

        return (adminDims);
    }

    public static void hidePlanningAllDimensions(HyperionPlanningBean planning) throws Exception {
        for (HspDimension planningDim : planning.getBaseDimensions()) {
            planningDim = (HspDimension)planningDim.cloneForUpdate();
            planningDim.setUsedIn(0);
            planning.saveDimension(planningDim);
        }
    }

    private static EssbaseConnection connectToEssbaseApp(HspJSImpl hspJS, String appName, String essbaseCubeName) {
        final String userName = hspJS.getAppProperty(HspJSHome.OLAP_USERNAME);
        final String password = hspJS.getAppProperty(HspJSHome.OLAP_PASSWORD);
        final String server = hspJS.getAppProperty(HspJSHome.OLAP_SERVER);
        // Indicates that Essbase password is not a token.
        final boolean passwordIsToken = false;

        // The Essbase impersonation username is set to null, meaning, do not impersonate.
        final String essbaseUserNameAsIsNull = null;

        EssbaseConnection connection = new EssbaseConnection(userName, password, passwordIsToken, essbaseUserNameAsIsNull, server, appName, essbaseCubeName);
        connection.connect();
        return connection;
    }

    private static void deleteEssbaseTempApp(EssbaseApplicationShapeSource essbaseAppShapeSource) {
        if (!HspUtils.isHPDevMode()) {
            essbaseAppShapeSource.deleteEssbaseTempApp();
        }
    }

    private static boolean isOutlineAso(HspJSHome hspJsHome, String fileName, String currentApplicationName) throws Exception, EssException {
        boolean asoOpPlanFlag = false;
        if (!HspUtils.isNullOrEmpty(fileName)) {
            HspDataSource dataSource = OperationalPlanningHelper.getDataSourceForApp(hspJsHome, currentApplicationName);
            if (dataSource != null) {
                String otlFilePath = OperationalPlanningHelper.getOtlFileFromInbox(fileName);
                asoOpPlanFlag = OutlineTypeDetector.determineOutlineIfAso(otlFilePath, fileName, dataSource);
            } else {
                throw new RuntimeException("MSG_NO_DAtASOURCES");
            }
        }
        return asoOpPlanFlag;
    }

    private static String createValidASOAppName(String applicationName) {
        String essbaseAppPrefix = "A"; // FIXME 18780985
        String essBaseAppName = "";
        if (!HspUtils.isNullOrEmpty(applicationName)) {
            if (applicationName.length() < 8) {
                essBaseAppName = essbaseAppPrefix + applicationName;
            } else {
                if (applicationName.charAt(0) == 'A' || applicationName.charAt(0) == 'a') {
                    essBaseAppName = 'B' + applicationName.substring(1);
                } else {
                    essBaseAppName = essbaseAppPrefix + applicationName.substring(1);
                }
            }
        }
        return essBaseAppName;
    }

    private static void createCube(String cubeName, HyperionPlanningBean planning, boolean isAso) throws Exception {
        HspCube cube = new HspCube();
        cube.setCubeName(cubeName);
        cube.setLocationAlias(cubeName);
        cube.setObjectName(cubeName);
        cube.setPlanTypeName(cubeName);
        if (isAso) {
            String essBaseAppName = createValidASOAppName(planning.getCurrentApplication());
            cube.setAppName(essBaseAppName);
            cube.setType(HspConstants.ASO_CUBE);
        } else {
            cube.setType(HspConstants.GENERIC_CUBE);
            cube.setAppName(planning.getCurrentApplication());
        }
        planning.addCube(cube);
    }


    private static String prepareOLUStatusToString(HspOutlineLoadStatus oluStatus, HyperionPlanningBean planning) {
        StringBuilder result = new StringBuilder();
        result.append(HspUtils.getLocalizedMessage(LABEL_PROCESS_OTL_PROCESSING_LOG,
                                                   HspUtils.createProperties(DIM_NAME, oluStatus.getDimensionName(), REC_READ, String.valueOf(oluStatus.getRecordsRead()), REC_REJECT, String.valueOf(oluStatus.getRecordsRejected()), REC_PROCESSED, String.valueOf(oluStatus.getRecordsProcessed())),
                                                   new Locale[] { planning.getMyLocale() }));
        return result.toString();
    }

    private static String prepareDBStats(String appName, String cubeName, HyperionPlanningBean planning) {
        StringBuilder builder = new StringBuilder();
        try {
            HspJS hspJS = planning.getHspJS();
            final String userName = hspJS.getAppProperty(HspJSHome.OLAP_USERNAME);
            final String password = hspJS.getAppProperty(HspJSHome.OLAP_PASSWORD);
            final String server = hspJS.getAppProperty(HspJSHome.OLAP_SERVER);

            try (IJAPISession session = getEssbaseSession(userName, password, server, appName, cubeName)) {
                final IMaxlResultSet rs = executeMaxL(session, getMaxLName(new StringBuilder("query database "), appName, cubeName).append("  get dbstats dimension").toString());
                builder.append("\n\n").append(HspUtils.getLocalizedMessage(LABEL_PROCESS_OTL_SRC_OTL_DBSTATS_SRC, null, new Locale[] { planning.getMyLocale() })).append("\n");
                while (rs.next()) {
                    final String dimName = rs.getValue(1).getObject().toString();
                    String tmp = rs.getValue(2).getObject().toString();
                    int dimType = Integer.parseInt(tmp.substring(0, tmp.indexOf(StringConstants.DOT)));
                    tmp = rs.getValue(3).getObject().toString();
                    final long actualMembersCount = Long.parseLong(tmp.substring(0, tmp.indexOf(StringConstants.DOT)));
                    tmp = rs.getValue(4).getObject().toString();
                    final long storedMembersCount = Long.parseLong(tmp.substring(0, tmp.indexOf(StringConstants.DOT)));
                    if (dimType > 1) {
                        dimType = 2;
                    }
                    builder.append("\n").append(HspUtils.getLocalizedMessage(LABEL_PROCESS_OTL_SRC_OTL_DBSTATS_ONE_LINE,
                                                                             HspUtils.createProperties(DIM_NAME, dimName, DIM_TYPE, IEssDimension.EEssDimensionStorageType.sm_fromInt(dimType).stringValue(), TOT_MEMS, String.valueOf(actualMembersCount), MEM_STORED,
                                                                                                       String.valueOf(storedMembersCount)), new Locale[] { planning.getMyLocale() }));
                }
                builder.append("\n");
            }
        } catch (Exception e) {
        }
        return builder.toString();
    }

    private static IJAPISession getEssbaseSession(String userName, String password, String server, String appName, String cubeName) throws Exception {
        return new DefaultJAPISession(userName, password, false, server, appName, cubeName);
    }

    private static StringBuilder getMaxLName(StringBuilder b, String application, String cubeName) {
        b.append("'").append(application).append("'");
        if (HspUtils.isNotNullOrEmpty(cubeName)) {
            b.append('.').append("'").append(cubeName).append("'");
        }
        return b;
    }

    private static IMaxlResultSet executeMaxL(IJAPISession japiSession, String maxl) throws Exception {
        IEssMaxlSession essMaxlSession = null;
        try {
            CMMaxlResultSet crs = new CMMaxlResultSet(maxl);
            essMaxlSession = japiSession.getMaxlSession(japiSession.getServerName());
            boolean status = essMaxlSession.execute(maxl);
            crs.setStatus(status);
            crs.setResultSet(essMaxlSession.getResultSet());
            return crs;
        } catch (EssException e) {
            logger.warn("Executing MaxL", e, null);
            throw e;
        } finally {
            if (null != essMaxlSession) {
                essMaxlSession.close();
            }
        }
    }

    private static final String LABEL_PROCESS_OTL_PROCESSING_ERR_MSG = "LABEL_PROCESS_OTL_PROCESSING_ERR_MSG";
    private static final String ERROR_MSG = "ERROR_MSG";
    private static final String DIM_NAME = "DIM_NAME";
    private static final String LABEL_PROCESS_OTL_DIM_ERR_MSG = "LABEL_PROCESS_OTL_DIM_ERR_MSG";
    private static final String LABEL_PROCESS_OTL_PROCESSING_FAILED = "LABEL_PROCESS_OTL_PROCESSING_FAILED";
    private static final String LABEL_PROCESS_OTL_PROCESSING_SUCCESS = "LABEL_PROCESS_OTL_PROCESSING_SUCCESS";
    private static final String LABEL_PROCESS_OTL_EXTRACT_FAILED = "LABEL_PROCESS_OTL_EXTRACT_FAILED";
    private static final String LABEL_PROCESS_OTL_PROCESSING_COMPLETED_WARN = "LABEL_PROCESS_OTL_PROCESSING_COMPLETED_WARN";
    private static final String LABEL_PROCESS_OTL_PROCESSING_LOG = "LABEL_PROCESS_OTL_PROCESSING_LOG";
    private static final String LABEL_PROCESS_OTL_PROCESSING_STARTED = "LABEL_PROCESS_OTL_PROCESSING_STARTED";
    private static final String REC_READ = "REC_READ";
    private static final String REC_REJECT = "REC_REJECT";
    private static final String REC_PROCESSED = "REC_PROCESSED";
    private static final String LABEL_PROCESS_OTL_SRC_OTL_DBSTATS_SRC = "LABEL_PROCESS_OTL_SRC_OTL_DBSTATS_SRC";
    private static final String LABEL_PROCESS_OTL_SRC_OTL_DBSTATS_ONE_LINE = "LABEL_PROCESS_OTL_SRC_OTL_DBSTATS_ONE_LINE";
    private static final String DIM_TYPE = "DIM_TYPE";
    private static final String TOT_MEMS = "TOT_MEMS";
    private static final String MEM_STORED = "MEM_STORED";
    private static final String LABEL_PROCESS_OTL_SRC_OTL_PROCESS_LOG = "LABEL_PROCESS_OTL_SRC_OTL_PROCESS_LOG";
}
