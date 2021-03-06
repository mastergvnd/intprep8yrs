package com.hyperion.getPlanning;


import static org.junit.Assert.fail;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.essbase.api.datasource.IEssCalcList;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssCube.IEssSecurityFilter;
import com.essbase.api.datasource.IEssOlapUser;
import com.essbase.api.datasource.IEssOlapUser.EEssAccess;
import com.essbase.api.datasource.IEssOlapFileObject;
import com.essbase.api.datasource.IEssPerformAllocation;
import com.essbase.api.datasource.IEssPerformCustomCalc;
import com.hyperion.planning.HspUtils;
import com.hyperion.planning.olap.HspMbrError;
import com.hyperion.planning.olap.japi.EssbaseConnection;
import com.hyperion.planning.olap.japi.EssbaseMainAPI;
import com.hyperion.planning.olap.japi.HspEssbasePool;


public final class EssbaseMainAPITest {
    // Increment this version each time the createModelAndDeploy
    // method is changed.
    private static final int CREATE_MODEL_AND_DEPLOY_VERSION = 1;
    private final String server1 = "localhost";
    private final String server2 = "den02ahj.us.oracle.com";
    
    private int sessionId;

    public EssbaseMainAPITest() {
        super();
    }
    
    private void validateEssbaseConnection(final EssbaseConnection connection) {
        HspUtils.verifyArgumentNotNull(connection, ESS_CONN);
        connection.connect();
        connection.verifyConnected();
    }
    
    private Object getPlanning(){
    	return null;
    }

    @Test
    public void testSetActive() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            String[] allActives = connection.getOlapServer().getActive();
            Assert.assertNotNull(allActives);
            Assert.assertNull(allActives[0]);
            Assert.assertNull(allActives[1]);

            connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
            EssbaseMainAPI.setActive(connection, "Vision", "Plan1");

            String[] nowAllActives = connection.getOlapServer().getActive();
            Assert.assertNotNull(nowAllActives);
            Assert.assertNotNull(nowAllActives[0]);
            Assert.assertNotNull(nowAllActives[1]);
            Assert.assertEquals("Vision", nowAllActives[0]);
            Assert.assertEquals("Plan1", nowAllActives[1]);
        } finally {
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testClearActive() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final String[] allActives = connection.getOlapServer().getActive();
            Assert.assertNotNull(allActives);
            Assert.assertNull(allActives[0]);
            Assert.assertNull(allActives[1]);

            connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
            EssbaseMainAPI.setActive(connection, "Vision", "Plan1");

            String[] nowAllActives = connection.getOlapServer().getActive();
            Assert.assertNotNull(nowAllActives);
            Assert.assertNotNull(nowAllActives[0]);
            Assert.assertNotNull(nowAllActives[1]);
            Assert.assertEquals("Vision", nowAllActives[0]);
            Assert.assertEquals("Plan1", nowAllActives[1]);

            EssbaseMainAPI.clearActive(connection, "HP1", "Plan1");

            nowAllActives = connection.getOlapServer().getActive();
            Assert.assertNotNull(nowAllActives);
            Assert.assertNull(nowAllActives[0]);
            Assert.assertNull(nowAllActives[1]);
        } finally {
            connection.disconnect();
            connection = null;
        }
    }

/*    @Test
    public void testGetEssbaseServerVersion() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final String essbaseServerVersion = EssbaseMainAPI.getEssbaseServerVersion(connection);
            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display system version");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), essbaseServerVersion);
        } finally {
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testCreateApplicationNonunicode() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testCreateApplicationNonunicodeASO() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);
            Assert.assertEquals(resultSet.getValue(18).getDouble(), 4.0, 0);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testCreateApplicationUnicodeASO() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet.getValue(18).getDouble(), 4.0, 0);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testCreateApplicationUnicode() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 3.0, 0);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testDeleteApplication() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet.getValue(18).getDouble(), 4.0, 0);

            EssbaseMainAPI.deleteApplication(connection, "KGBTRUMP");

            try {
                getPlanning().executeMaxl("display application KGBTRUMP");
                Assert.fail("Application KGBTRUMP doesn't exist. This code should not have reached.");
            } catch (Exception e) {
                Assert.assertEquals(1051030, ((EssException)e).getNativeCode());
            }

        } finally {
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testDeleteNonExistingApplication() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);

            try {
                getPlanning().executeMaxl("display application NOSCHAPP");
                Assert.fail("Application NOSCHAPP doesn't exist. This code should not have reached.");
            } catch (Exception e) {
                Assert.assertEquals(1051030, ((EssException)e).getNativeCode());
            }

            try {
                EssbaseMainAPI.deleteApplication(connection, "NOSCHAPP");
                Assert.fail("Application NOSCHAPP doesn't exist. This code should not have reached.");
            } catch (EssbaseException ee) {
                ee.printStackTrace();
                Assert.assertEquals(1051030, ee.getResultCode());
            }

        } finally {
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testRenameApplicationNonunicode() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            EssbaseMainAPI.renameApplication(connection, "KGBTRUMP", "KGBTRUMB");

            try {
                getPlanning().executeMaxl("display application KGBTRUMP");
                Assert.fail("Application KGBTRUMP doesn't exist anymore as it is renamed to KGBTRUMB. This code should not have reached.");
            } catch (Exception e) {
                Assert.assertEquals(1051030, ((EssException)e).getNativeCode());
            }

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display application KGBTRUMB");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMB");
            Assert.assertEquals(resultSet2.getValue(12).getDouble(), 2.0, 0);

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMB cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testLockObject() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            createdApp.createCube("ACUBE", IEssCube.EEssCubeType.NORMAL);

            EssbaseMainAPI.lockObject(connection, "KGBTRUMP", "ACUBE", "ACUBE", IEssOlapFileObject.TYPE_OUTLINE);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display object KGBTRUMP.ACUBE.ACUBE of type outline");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getString(3), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(4).getDouble(), 1.0, 0);
            Assert.assertTrue(resultSet2.getValue(5).getBoolean());
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testUnlockObject() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            createdApp.createCube("ACUBE", IEssCube.EEssCubeType.NORMAL);

            EssbaseMainAPI.lockObject(connection, "KGBTRUMP", "ACUBE", "ACUBE", IEssOlapFileObject.TYPE_OUTLINE);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display object KGBTRUMP.ACUBE.ACUBE of type outline");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getString(3), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(4).getDouble(), 1.0, 0);
            Assert.assertTrue(resultSet2.getValue(5).getBoolean());

            EssbaseMainAPI.unlockObject(connection, "KGBTRUMP", "ACUBE", "ACUBE", IEssOlapFileObject.TYPE_OUTLINE);

            final IEssMaxlResultSet resultSet3 = getPlanning().executeMaxl("display object KGBTRUMP.ACUBE.ACUBE of type outline");
            resultSet3.next();
            Assert.assertEquals(resultSet3.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet3.getString(2), "ACUBE");
            Assert.assertEquals(resultSet3.getString(3), "ACUBE");
            Assert.assertEquals(resultSet3.getValue(4).getDouble(), 1.0, 0);
            Assert.assertFalse(resultSet3.getValue(5).getBoolean());
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testUnloadDatabase() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            createdApp.createCube("ACUBE", IEssCube.EEssCubeType.NORMAL);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            EssbaseMainAPI.unloadDatabase(connection, "KGBTRUMP", "ACUBE");

            final IEssMaxlResultSet resultSet3 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet3.next();
            Assert.assertEquals(resultSet3.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet3.getString(2), "ACUBE");
            Assert.assertEquals(resultSet3.getValue(32).getDouble(), 0.0, 0);

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testCreateBSONonUniqueAllowedDatabase() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 0, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 0.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testCreateASONonUniqueAllowedDatabase() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 3.0, 0);

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testDeleteDatabase() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssCube cube = EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);
            Assert.assertNotNull(cube);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet.getValue(19).getDouble(), 1.0, 0);

            EssbaseMainAPI.deleteDatabase(connection, "KGBTRUMP", "ACUBE");

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getValue(12).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(19).getDouble(), 0.0, 0);

            try {
                getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
                Assert.fail("Database KGBTRUMP.ACUBE doesn't exist anymore as it is deleted. This code should not have reached.");
            } catch (Exception e) {
                Assert.assertEquals(1056024, ((EssException)e).getNativeCode());
            }

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testRenameDatabase() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet.getValue(19).getDouble(), 0.0, 0);

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet1 = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet1.next();
            Assert.assertEquals(resultSet1.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet1.getValue(12).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet1.getValue(19).getDouble(), 1.0, 0);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            EssbaseMainAPI.renameDatabase(connection, "KGBTRUMP", "ACUBE", "BCUBE");

            final IEssMaxlResultSet resultSet4 = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet4.next();
            Assert.assertEquals(resultSet4.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet4.getValue(12).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet4.getValue(19).getDouble(), 1.0, 0);

            final IEssMaxlResultSet resultSet3 = getPlanning().executeMaxl("display database KGBTRUMP.BCUBE");
            resultSet3.next();
            Assert.assertEquals(resultSet3.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet3.getString(2), "BCUBE");
            Assert.assertEquals(resultSet3.getValue(30).getDouble(), 3.0, 0);

            try {
                getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
                Assert.fail("Database KGBTRUMP.ACUBE doesn't exist anymore as it is renamed to KGBTRUMB.BCUBE. This code should not have reached.");
            } catch (Exception e) {
                Assert.assertEquals(1056024, ((EssException)e).getNativeCode());
            }

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetAppTypeUnicode() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 3.0, 0);

            final String appType = EssbaseMainAPI.getAppType(connection, "KGBTRUMP");
            Assert.assertEquals("utf8", appType);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetAppTypeNonUnicode() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            final String appType = EssbaseMainAPI.getAppType(connection, "KGBTRUMP");
            Assert.assertEquals("native", appType);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetAppStorageInfoBSO() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 3.0, 0);

            final int storageType = EssbaseMainAPI.getAppStorageInfo(connection, "KGBTRUMP");
            Assert.assertEquals(0, storageType);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetAppStorageInfoASO() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 3.0, 0);

            final int storageType = EssbaseMainAPI.getAppStorageInfo(connection, "KGBTRUMP");
            Assert.assertEquals(1, storageType);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetServerLocaleString() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final String localeString = EssbaseMainAPI.getServerLocaleString(connection);
            Assert.assertEquals("English_UnitedStates.Latin1@Binary", localeString.trim());
        } finally {
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetOlapFileObject() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 0, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 0.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            final IEssOlapFileObject fileObj = EssbaseMainAPI.getOlapFileObjectDetails(connection, "KGBTRUMP", "ACUBE", IEssOlapFileObject.TYPE_OUTLINE, "ACUBE");
            Assert.assertNotNull(fileObj);
            Assert.assertEquals("KGBTRUMP", fileObj.getApplicationName());
            Assert.assertEquals("ACUBE", fileObj.getCubeName());
            Assert.assertEquals("ACUBE", fileObj.getName());
            Assert.assertEquals(1, fileObj.getType());
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testListVariableSysLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter system add variable 'NOSUCHVAR' '5'");

            final String[][] allSysLevelVars = EssbaseMainAPI.listVariables(connection, null, null);
            final String allSysVarsStr = Arrays.deepToString(allSysLevelVars);
            Assert.assertTrue(allSysVarsStr.contains("NOSUCHVAR, 5"));
        } finally {
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testListVariable2SysLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter system add variable 'NOSUCHVAR' '5'");

            final HspEssVariable[] allSysLevelVars = EssbaseMainAPI.listVariables2(connection, null, null);

            HspEssVariable thatVar = null;
            boolean found = false;
            for (final HspEssVariable aSysLevelVar : allSysLevelVars) {
                if (aSysLevelVar.getName().equals("NOSUCHVAR")) {
                    thatVar = aSysLevelVar;
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
            Assert.assertNotNull(thatVar);
            Assert.assertNotNull(thatVar.getServer());
            Assert.assertNull(thatVar.getAppName());
            Assert.assertNull(thatVar.getDbName());
            Assert.assertNotNull(thatVar.getName());
            Assert.assertEquals(thatVar.getName(), "NOSUCHVAR");
            Assert.assertEquals(thatVar.getValue(), "5");
        } finally {
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testListVariableAppLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter application 'KGBTRUMP' drop variable 'OSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter application 'KGBTRUMP' add variable 'OSUCHVAR' '8'");

            final String[][] allAppLevelVars = EssbaseMainAPI.listVariables(connection, "KGBTRUMP", null);
            final String allAppVarsStr = Arrays.deepToString(allAppLevelVars);
            Assert.assertTrue(allAppVarsStr.contains("OSUCHVAR, 8, KGBTRUMP"));
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testListVariable2AppLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter application 'KGBTRUMP' drop variable 'OSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter application 'KGBTRUMP' add variable 'OSUCHVAR' '8'");

            final HspEssVariable[] allAppLevelVars = EssbaseMainAPI.listVariables2(connection, "KGBTRUMP", null);

            HspEssVariable thatVar = null;
            boolean found = false;
            for (final HspEssVariable anAppLevelVar : allAppLevelVars) {
                if (anAppLevelVar.getName().equals("OSUCHVAR")) {
                    thatVar = anAppLevelVar;
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
            Assert.assertNotNull(thatVar);
            Assert.assertNotNull(thatVar.getServer());
            Assert.assertNotNull(thatVar.getAppName());
            Assert.assertEquals(thatVar.getAppName(), "KGBTRUMP");
            Assert.assertNull(thatVar.getDbName());
            Assert.assertNotNull(thatVar.getName());
            Assert.assertEquals(thatVar.getName(), "OSUCHVAR");
            Assert.assertEquals(thatVar.getValue(), "8");

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testListVariableDBLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' drop variable 'XSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' add variable 'XSUCHVAR' '9'");

            final String[][] allDBLevelVars = EssbaseMainAPI.listVariables(connection, "KGBTRUMP", "ACUBE");
            final String allDBVarsStr = Arrays.deepToString(allDBLevelVars);
            Assert.assertTrue(allDBVarsStr.contains("XSUCHVAR, 9, KGBTRUMP, ACUBE"));
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testListVariable2DBLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' drop variable 'XSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' add variable 'XSUCHVAR' '9'");

            final HspEssVariable[] allDBLevelVars = EssbaseMainAPI.listVariables2(connection, "KGBTRUMP", "ACUBE");

            HspEssVariable thatVar = null;
            boolean found = false;
            for (final HspEssVariable aDBLevelVar : allDBLevelVars) {
                if (aDBLevelVar.getName().equals("XSUCHVAR")) {
                    thatVar = aDBLevelVar;
                    found = true;
                    break;
                }
            }
            Assert.assertTrue(found);
            Assert.assertNotNull(thatVar);
            Assert.assertNotNull(thatVar.getServer());
            Assert.assertNotNull(thatVar.getAppName());
            Assert.assertEquals(thatVar.getAppName(), "KGBTRUMP");
            Assert.assertNotNull(thatVar.getDbName());
            Assert.assertEquals(thatVar.getDbName(), "ACUBE");
            Assert.assertNotNull(thatVar.getName());
            Assert.assertEquals(thatVar.getName(), "XSUCHVAR");
            Assert.assertEquals(thatVar.getValue(), "9");
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }


    @Test
    public void testListVariableAppLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            final String[][] allAppLevelVars = EssbaseMainAPI.listVariables(connection, "KGBTRUMP", null);
            Assert.assertEquals(0, allAppLevelVars.length);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testListVariable2AppLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            final HspEssVariable[] allAppLevelVars = EssbaseMainAPI.listVariables2(connection, "KGBTRUMP", null);
            Assert.assertEquals(0, allAppLevelVars.length);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testListVariableDBLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            final String[][] allDBLevelVars = EssbaseMainAPI.listVariables(connection, "KGBTRUMP", "ACUBE");
            Assert.assertEquals(0, allDBLevelVars.length);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testListVariable2DBLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            final HspEssVariable[] allDBLevelVars = EssbaseMainAPI.listVariables2(connection, "KGBTRUMP", "ACUBE");
            Assert.assertEquals(0, allDBLevelVars.length);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariableSysLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            getPlanning().executeMaxl("alter system add variable 'NOSUCHVAR' '5'");
            final String value = EssbaseMainAPI.getVariable(connection, null, null, "NOSUCHVAR");
            assertNotNull(value);
            Assert.assertEquals(value, "5");
        } finally {
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariable2SysLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter system add variable 'NOSUCHVAR' '5'");

            final HspEssVariable thatVar = EssbaseMainAPI.getVariable2(connection, null, null, "NOSUCHVAR");

            Assert.assertNotNull(thatVar);
            Assert.assertNotNull(thatVar.getServer());
            Assert.assertNull(thatVar.getAppName());
            Assert.assertNull(thatVar.getDbName());
            Assert.assertNotNull(thatVar.getName());
            Assert.assertEquals(thatVar.getName(), "NOSUCHVAR");
            Assert.assertEquals(thatVar.getValue(), "5");
        } finally {
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariableSysLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                EssbaseMainAPI.getVariable(connection, null, null, "NOSUCHVAR");
                fail("There is no such variable as NOSUCHVAR. This code should not have got executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariable2SysLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                EssbaseMainAPI.getVariable2(connection, null, null, "NOSUCHVAR");
                fail("There is no such variable as NOSUCHVAR. This code should not have got executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariableAppLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter application 'KGBTRUMP' drop variable 'OSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter application 'KGBTRUMP' add variable 'OSUCHVAR' '8'");

            final String value = EssbaseMainAPI.getVariable(connection, "KGBTRUMP", null, "OSUCHVAR");
            Assert.assertNotNull(value);
            Assert.assertEquals("8", value);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariable2AppLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter application 'KGBTRUMP' drop variable 'OSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter application 'KGBTRUMP' add variable 'OSUCHVAR' '8'");

            final HspEssVariable thatVar = EssbaseMainAPI.getVariable2(connection, "KGBTRUMP", null, "OSUCHVAR");
            Assert.assertNotNull(thatVar);
            Assert.assertNotNull(thatVar.getServer());
            Assert.assertNotNull(thatVar.getAppName());
            Assert.assertEquals(thatVar.getAppName(), "KGBTRUMP");
            Assert.assertNull(thatVar.getDbName());
            Assert.assertNotNull(thatVar.getName());
            Assert.assertEquals(thatVar.getName(), "OSUCHVAR");
            Assert.assertEquals(thatVar.getValue(), "8");

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariableAppLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            try {
                EssbaseMainAPI.getVariable(connection, "KGBTRUMP", null, "OSUCHVAR");
                fail("OSUCHVAR doesn't exist for the application. This code shouldn't have executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariable2AppLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            try {
                EssbaseMainAPI.getVariable2(connection, "KGBTRUMP", null, "OSUCHVAR");
                fail("OSUCHVAR doesn't exist. This code shouldn't have got executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariableDBLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' drop variable 'XSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' add variable 'XSUCHVAR' '9'");

            final String value = EssbaseMainAPI.getVariable(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");
            Assert.assertNotNull(value);
            Assert.assertEquals("9", value);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariable2DBLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' drop variable 'XSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' add variable 'XSUCHVAR' '9'");

            final HspEssVariable thatVar = EssbaseMainAPI.getVariable2(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");
            Assert.assertNotNull(thatVar);
            Assert.assertNotNull(thatVar.getServer());
            Assert.assertNotNull(thatVar.getAppName());
            Assert.assertEquals(thatVar.getAppName(), "KGBTRUMP");
            Assert.assertNotNull(thatVar.getDbName());
            Assert.assertEquals(thatVar.getDbName(), "ACUBE");
            Assert.assertNotNull(thatVar.getName());
            Assert.assertEquals(thatVar.getName(), "XSUCHVAR");
            Assert.assertEquals(thatVar.getValue(), "9");
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariableDBLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);
            try {
                EssbaseMainAPI.getVariable(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");
                fail("XSUCHVAR doesn't exist at this database level. This code shouldn't have got executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testGetVariable2DBLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);
            try {
                EssbaseMainAPI.getVariable2(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");
                fail("XSUCHVAR doesn't exist at this database level. This code shouldn't have got executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }


    @Test
    public void testDeleteVariableSysLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            getPlanning().executeMaxl("alter system add variable 'NOSUCHVAR' '5'");
            final String value = EssbaseMainAPI.getVariable(connection, null, null, "NOSUCHVAR");
            assertNotNull(value);
            Assert.assertEquals(value, "5");

            EssbaseMainAPI.deleteVariable(connection, null, null, "NOSUCHVAR");

            try {
                EssbaseMainAPI.getVariable(connection, null, null, "NOSUCHVAR");
                fail("There is no such variable as NOSUCHVAR. This code should not have got executed.");
            } catch (EssbaseException ee) {
            }

        } finally {
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testDeleteVariableSysLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                EssbaseMainAPI.deleteVariable(connection, null, null, "NOSUCHVAR");
                fail("There is no such variable as NOSUCHVAR. This code should not have got executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testDeleteVariableAppLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter application 'KGBTRUMP' drop variable 'OSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter application 'KGBTRUMP' add variable 'OSUCHVAR' '8'");

            final String value = EssbaseMainAPI.getVariable(connection, "KGBTRUMP", null, "OSUCHVAR");
            Assert.assertNotNull(value);
            Assert.assertEquals("8", value);

            EssbaseMainAPI.deleteVariable(connection, "KGBTRUMP", null, "OSUCHVAR");
            try {
                EssbaseMainAPI.getVariable(connection, "KGBTRUMP", null, "OSUCHVAR");
                fail("There is no such variable as OSUCHVAR at application level. This code should not have got executed.");
            } catch (EssbaseException ee) {
            }

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testDeleteVariableAppLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            try {
                EssbaseMainAPI.deleteVariable(connection, "KGBTRUMP", null, "OSUCHVAR");
                fail("OSUCHVAR doesn't exist for the application. This code shouldn't have executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testDeleteVariableDBLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' drop variable 'XSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' add variable 'XSUCHVAR' '9'");

            final String value = EssbaseMainAPI.getVariable(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");
            Assert.assertNotNull(value);
            Assert.assertEquals("9", value);

            EssbaseMainAPI.deleteVariable(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");

            try {
                EssbaseMainAPI.getVariable(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");
                fail("There is no such variable as XSUCHVAR at database level. This code should not have got executed.");
            } catch (EssbaseException ee) {
            }

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testDeleteVariableDBLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);
            try {
                EssbaseMainAPI.deleteVariable(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");
                fail("XSUCHVAR doesn't exist at this database level. This code shouldn't have got executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }


    @Test
    public void testSetVariableSysLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            getPlanning().executeMaxl("alter system add variable 'NOSUCHVAR' '5'");
            final String value = EssbaseMainAPI.getVariable(connection, null, null, "NOSUCHVAR");
            assertNotNull(value);
            Assert.assertEquals(value, "5");

            EssbaseMainAPI.setVariable(connection, null, null, "NOSUCHVAR", "55");

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display variable NOSUCHVAR");
            resultSet2.next();
            Assert.assertNull(resultSet2.getString(1));
            Assert.assertNull(resultSet2.getString(2));
            Assert.assertEquals(resultSet2.getString(3), "NOSUCHVAR");
            Assert.assertEquals(resultSet2.getString(4), "55");

        } finally {
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testSetVariableSysLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            try {
                EssbaseMainAPI.deleteVariable(connection, null, null, "NOSUCHVAR");
            } catch (Exception e) {
            }

            EssbaseMainAPI.setVariable(connection, null, null, "NOSUCHVAR", "55");

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display variable NOSUCHVAR");
            resultSet2.next();
            Assert.assertNull(resultSet2.getString(1));
            Assert.assertNull(resultSet2.getString(2));
            Assert.assertEquals(resultSet2.getString(3), "NOSUCHVAR");
            Assert.assertEquals(resultSet2.getString(4), "55");
        } finally {
            try {
                getPlanning().executeMaxl("alter system drop variable 'NOSUCHVAR'");
            } catch (Exception e) {

            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testSetVariableAppLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter application 'KGBTRUMP' drop variable 'OSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter application 'KGBTRUMP' add variable 'OSUCHVAR' '8'");

            final String value = EssbaseMainAPI.getVariable(connection, "KGBTRUMP", null, "OSUCHVAR");
            Assert.assertNotNull(value);
            Assert.assertEquals("8", value);

            EssbaseMainAPI.setVariable(connection, "KGBTRUMP", null, "OSUCHVAR", "88");

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display variable KGBTRUMP.OSUCHVAR");
            resultSet2.next();
            Assert.assertNotNull(resultSet2.getString(1));
            Assert.assertEquals("KGBTRUMP", resultSet2.getString(1));
            Assert.assertNull(resultSet2.getString(2));
            Assert.assertEquals(resultSet2.getString(3), "OSUCHVAR");
            Assert.assertEquals(resultSet2.getString(4), "88");

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testsetVariableAppLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createApplication(connection, "KGBTRUMP", false);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet.getValue(12).getDouble(), 2.0, 0);

            EssbaseMainAPI.setVariable(connection, "KGBTRUMP", null, "OSUCHVAR", "99");

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display variable KGBTRUMP.OSUCHVAR");
            resultSet2.next();
            Assert.assertNotNull(resultSet2.getString(1));
            Assert.assertEquals("KGBTRUMP", resultSet2.getString(1));
            Assert.assertNull(resultSet2.getString(2));
            Assert.assertEquals(resultSet2.getString(3), "OSUCHVAR");
            Assert.assertEquals(resultSet2.getString(4), "99");

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }


    @Test
    public void testSetVariableDBLevel() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            try {
                getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' drop variable 'XSUCHVAR'");
            } catch (Exception e) {

            }

            getPlanning().executeMaxl("alter database 'KGBTRUMP'.'ACUBE' add variable 'XSUCHVAR' '9'");

            final String value = EssbaseMainAPI.getVariable(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");
            Assert.assertNotNull(value);
            Assert.assertEquals("9", value);

            EssbaseMainAPI.setVariable(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR", "99");

            final IEssMaxlResultSet resultSet3 = getPlanning().executeMaxl("display variable KGBTRUMP.ACUBE.XSUCHVAR");
            resultSet3.next();
            Assert.assertNotNull(resultSet3.getString(1));
            Assert.assertEquals("KGBTRUMP", resultSet3.getString(1));
            Assert.assertNotNull(resultSet3.getString(2));
            Assert.assertEquals("ACUBE", resultSet3.getString(2));
            Assert.assertEquals(resultSet3.getString(3), "XSUCHVAR");
            Assert.assertEquals(resultSet3.getString(4), "99");

        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testSetVariableDBLevelEmpty() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);
            try {
                EssbaseMainAPI.deleteVariable(connection, "KGBTRUMP", "ACUBE", "XSUCHVAR");
                fail("XSUCHVAR doesn't exist at this database level. This code shouldn't have got executed.");
            } catch (EssbaseException ee) {
            }
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }

    @Test
    public void testIsDBinArchiveMode() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            final IEssOlapApplication createdApp = EssbaseMainAPI.createStorageTypeApplicationEx(connection, "KGBTRUMP", 4, true);
            Assert.assertNotNull(createdApp);

            final IEssMaxlResultSet resultSet = getPlanning().executeMaxl("display application KGBTRUMP");
            resultSet.next();
            Assert.assertEquals(resultSet.getString(1), "KGBTRUMP");

            EssbaseMainAPI.createDatabaseEx(connection, "KGBTRUMP", "ACUBE", 2, true);

            final IEssMaxlResultSet resultSet2 = getPlanning().executeMaxl("display database KGBTRUMP.ACUBE");
            resultSet2.next();
            Assert.assertEquals(resultSet2.getString(1), "KGBTRUMP");
            Assert.assertEquals(resultSet2.getString(2), "ACUBE");
            Assert.assertEquals(resultSet2.getValue(30).getDouble(), 3.0, 0);
            Assert.assertEquals(resultSet2.getValue(32).getDouble(), 2.0, 0);

            final IEssCube cube = connection.getCube("KGBTRUMP", "ACUBE");
            Assert.assertNotNull(cube);
            File temp = File.createTempFile("testEssArchiveFile", ".tmp");
            cube.archiveBegin(temp.getAbsolutePath());

            boolean isInArchiveMode = EssbaseMainAPI.isDBinArchiveMode(connection, "KGBTRUMP", "ACUBE");
            Assert.assertTrue(isInArchiveMode);

            cube.archiveEnd();

            isInArchiveMode = EssbaseMainAPI.isDBinArchiveMode(connection, "KGBTRUMP", "ACUBE");
            Assert.assertFalse(isInArchiveMode);
        } finally {
            try {
                getPlanning().executeMaxl("drop application KGBTRUMP cascade force");
            } catch (Exception e) {
                fail();
            }
            connection.disconnect();
            connection = null;
        }
    }
*/    

    @Test
    public void testCalcWithName() throws Exception {
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        try {
            validateEssbaseConnection(connection);
            EssbaseMainAPI.calcWithName(connection, "Sample", "Basic", false, "testCalc", "FIX(Product)\n" + 
            "	Budget = 2*Actual;\n" + 
            "ENDFIX");
        } catch (Exception e) {
                fail();
        } finally {
            connection.disconnect();
            connection = null;
        }
    }
    
     @Test
     public void testLogin() throws Exception{

         EssbaseConnection connection = null;
         try {
             String apps[][] = new String[2][25];
             connection = EssbaseMainAPI.login(server1, "admin", "password", apps);
             System.out.println(Arrays.deepToString(apps));
             
             boolean isAppAvailable = Arrays.asList(apps[0]).contains("Vision");
             Assert.assertTrue("Application name is not available", isAppAvailable);
             
             boolean isCubeAvailable = Arrays.asList(apps[1]).contains("Plan1");
             Assert.assertTrue("Plan1 cube not is available", isCubeAvailable);
             
         } catch (Exception e) {
             fail();
         } finally {
             connection.disconnect();
             connection = null;
         }
     }
    
     @Test
     public void testGetUserType() throws Exception{

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         short oldUserType = -1;
         try {
            validateEssbaseConnection(connection);
            oldUserType = EssbaseMainAPI.getUserType(connection, "admin");
            short userTypeToSet = 3;
            EssbaseMainAPI.setUserType(connection, "admin", userTypeToSet);
            short retrievedUserType = EssbaseMainAPI.getUserType(connection, "admin");
            Assert.assertEquals("User Type is not expected", retrievedUserType, userTypeToSet);
         } catch (Exception e) {
             fail();
         } finally {
             if(oldUserType != -1){
                 EssbaseMainAPI.setUserType(connection, "admin", oldUserType);
             }
             connection.disconnect();
             connection = null;
         }
     }
    
     @Test
     public void testGetCalcList() throws Exception{

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         URL url = HspUtils.findURL("jnitojapi");
         if(url == null){
      	   File myFile=new File("D:\\CodeBase\\govgupta_view_main\\planning\\HspJS\\TestResources\\jnitojapi");
      	   url = myFile.toURI().toURL();
         }
         String calcFilePath1 = url.getPath() + "/testCalc1.csc";
         String calcFilePath2 = url.getPath() + "/testCalc2.csc";
         try {
            validateEssbaseConnection(connection);
            try{
                EssbaseMainAPI.putObject(connection, "Sample", "Basic", "tCalc1", IEssOlapFileObject.TYPE_CALCSCRIPT, calcFilePath1, true);
            }catch(Exception e){
            }
             
            try{
                EssbaseMainAPI.putObject(connection, "Sample", "Basic", "tCalc2", IEssOlapFileObject.TYPE_CALCSCRIPT, calcFilePath2, true);
            }catch(Exception e){
            }
            String calcList[] = EssbaseMainAPI.getCalcList(connection, "Sample", "Basic", "admin");
            Assert.assertEquals("Calculation script1 is not correct.", "tCalc1", calcList[0]);
            Assert.assertEquals("Calculation script2 is not correct.", "tCalc2", calcList[1]);
         } catch (Exception e) {
             fail();
         } finally {
             try{
                 EssbaseMainAPI.deleteObject(connection, "Sample", "Basic", "tCalc1", IEssOlapFileObject.TYPE_CALCSCRIPT);
             }catch(Exception e){
             }
             try{
                 EssbaseMainAPI.deleteObject(connection, "Sample", "Basic", "tCalc2", IEssOlapFileObject.TYPE_CALCSCRIPT);
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
     @Test
     public void testUpdateCell() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         File file = null;
         try {
            validateEssbaseConnection(connection);
            String data = "Product Jan Sales Actual Market 7777\n" + "Product Feb Sales Actual Market 8888\n" + "Product Mar Sales Actual Market 9999";
            String[] dataArray = {data};
            EssbaseMainAPI.updateCell(connection, "Sample", "Basic", dataArray);
            String tempDir = System.getProperty("java.io.tmpdir");  
            connection.getApp("Sample").getCube("Basic").exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.ALL, true);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
            boolean contains = fileData.toString().contains("Product");
            Assert.assertTrue("File data does not contain Product", contains);
            contains = fileData.toString().contains("8888");
            Assert.assertTrue("File data does not contain 8888", contains);
            contains = fileData.toString().contains("Market");
            Assert.assertTrue("File data does not contain Market", contains);
         } catch (Exception e) {
         } finally {
             if(file != null){
                file.delete();
             }
             try{
                 EssbaseMainAPI.deleteObject(connection, "Sample", "Basic", "testRule", IEssOlapFileObject.TYPE_RULES);
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }

     @Test
     public void testDataLoad() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         File file = null;
         try {
            validateEssbaseConnection(connection);
            String data = "Jan,Sales,100-10,Connecticut,Actual,125" + System.lineSeparator() + "Feb,COGS,100-30,Florida,Budget,123";
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
         	   File myFile=new File("D:\\CodeBase\\govgupta_view_main\\planning\\HspJS\\TestResources\\jnitojapi");
         	   url = myFile.toURI().toURL();
            }
            String ruleFilePath = url.getPath() + "/testRule.rul";
            EssbaseMainAPI.putObject(connection, "Sample", "Basic", "testRule", IEssOlapFileObject.TYPE_RULES, ruleFilePath, true);
            IEssCube existingCube = EssbaseMainAPI.beginDataLoad(connection, "Sample", "Basic", "testRule", IEssOlapFileObject.TYPE_RULES, true, true, false);
            EssbaseMainAPI.sendString(connection, existingCube, data);
            HspMbrError[] mbrErrors = EssbaseMainAPI.endDataload(connection, existingCube);
            String tempDir = System.getProperty("java.io.tmpdir"); 
            connection.getApp("Sample").getCube("Basic").exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.ALL, true);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
            Assert.assertNull("Errors array is not null", mbrErrors);
            boolean contains = fileData.toString().contains("Connecticut");
            Assert.assertTrue("File data does not contain Connecticut", contains);
            contains = fileData.toString().contains("125");
            Assert.assertTrue("File data does not contain 125", contains);
            contains = fileData.toString().contains("Florida");
            Assert.assertTrue("File data does not contain Florida", contains);
         } catch (Exception e) {
         } finally {
             if(file != null){
                file.delete();
             }
             try{
                 connection.getApp("Sample").getCube("Basic").clearAllData();
             }catch(Exception e){
             }
             try{
                 EssbaseMainAPI.deleteObject(connection, "Sample", "Basic", "testRule", IEssOlapFileObject.TYPE_RULES);
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
    @Test
    public void testDataLoadComposite() throws Exception{                                                             
        EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
        File file = null;
        try {
           validateEssbaseConnection(connection);
           String data = "Jan,Sales,100-10,Connecticut,Actual,125" + System.lineSeparator() + "Feb,COGS,100-30,Florida,Budget,123";
           URL url = HspUtils.findURL("jnitojapi");
           if(url == null){
        	   File myFile=new File("D:\\CodeBase\\govgupta_view_main\\planning\\HspJS\\TestResources\\jnitojapi");
        	   url = myFile.toURI().toURL();
           }
           String ruleFilePath = url.getPath() + "/testRule.rul";
           EssbaseMainAPI.putObject(connection, "Sample", "Basic", "testRule", IEssOlapFileObject.TYPE_RULES, ruleFilePath, true);
           HspMbrError[] mbrErrors = EssbaseMainAPI.dataLoad(connection, "Sample", "Basic", data, "testRule", IEssOlapFileObject.TYPE_RULES, true, true, false);
           String tempDir = System.getProperty("java.io.tmpdir");
           connection.getApp("Sample").getCube("Basic").exportData(tempDir+"exportedData.txt", IEssCube.EEssDataLevel.ALL, true);
           file = new File(tempDir+"/exportedData.txt");
           FileReader fileReader = new FileReader(file);
           BufferedReader br = new BufferedReader(fileReader);  
           String line;
           StringBuilder fileData = new StringBuilder();
           while ((line = br.readLine()) != null){
                fileData.append(line);
           }
           fileReader.close();
           br.close();
           System.out.println("File Data is : "+fileData);
           Assert.assertNull("Errors array is not null", mbrErrors);
           boolean contains = fileData.toString().contains("Connecticut");
           Assert.assertTrue("File data does not contain Connecticut", contains);
           contains = fileData.toString().contains("125");
           Assert.assertTrue("File data does not contain 125", contains);
           contains = fileData.toString().contains("Florida");
           Assert.assertTrue("File data does not contain Florida", contains);
        } catch (Exception e) {
        } finally {
            if(file != null){
               file.delete();
            }
            try{
                connection.getApp("Sample").getCube("Basic").clearAllData();
            }catch(Exception e){
            }
            try{
                EssbaseMainAPI.deleteObject(connection, "Sample", "Basic", "testRule", IEssOlapFileObject.TYPE_RULES);
            }catch(Exception e){
            }
            connection.disconnect();
            connection = null;
        }
    }
    
     @Test
     public void testDataLoadWithDataFileAndBufferId() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         File file = null;
         try {
            validateEssbaseConnection(connection);
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
         	   File myFile=new File("D:\\CodeBase\\govgupta_view_main\\planning\\HspJS\\TestResources\\jnitojapi");
         	   url = myFile.toURI().toURL();
            }
            String ruleFilePath = url.getPath() + "/testRule.rul";
            String dataFilePath = url.getPath() + "/testData.txt";
            EssbaseMainAPI.putObject(connection, "Sample", "Basic", "testRule", IEssOlapFileObject.TYPE_RULES, ruleFilePath, true);
            HspMbrError[] mbrErrors = EssbaseMainAPI.beginDataload(connection, "Sample", "Basic", "testRule", IEssOlapFileObject.TYPE_RULES, dataFilePath, IEssOlapFileObject.TYPE_TEXT, false, 4);
            String tempDir = System.getProperty("java.io.tmpdir");             
            connection.getApp("Sample").getCube("Basic").exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.ALL, true);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
            Assert.assertNull("Errors array is not null", mbrErrors);
            boolean contains = fileData.toString().contains("Connecticut");
            Assert.assertTrue("File data does not contain Connecticut", contains);
            contains = fileData.toString().contains("125");
            Assert.assertTrue("File data does not contain 125", contains);
            contains = fileData.toString().contains("Florida");
            Assert.assertTrue("File data does not contain Florida", contains);
         } catch (Exception e) {
         } finally {
             if(file != null){
                file.delete();
             }
             try{
                 connection.getApp("Sample").getCube("Basic").clearAllData();
             }catch(Exception e){
             }
             try{
                 EssbaseMainAPI.deleteObject(connection, "Sample", "Basic", "testRule", IEssOlapFileObject.TYPE_RULES);
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
    
     @Test
     public void testImportData() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         File file = null;
         try {
            validateEssbaseConnection(connection);
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
         	   File myFile=new File("D:\\CodeBase\\govgupta_view_main\\planning\\HspJS\\TestResources\\jnitojapi");
         	   url = myFile.toURI().toURL();
            }
            String ruleFilePath = url.getPath() + "/testRule.rul";
            String dataFilePath = url.getPath() + "/testData.txt";
            HspMbrError[] mbrErrors = EssbaseMainAPI.importData(connection, "Sample", "Basic", ruleFilePath, IEssOlapFileObject.TYPE_RULES, dataFilePath, IEssOlapFileObject.TYPE_TEXT, false);
            String tempDir = System.getProperty("java.io.tmpdir");             
            connection.getApp("Sample").getCube("Basic").exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.ALL, true);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
            Assert.assertNull("Errors array is not null", mbrErrors);
            boolean contains = fileData.toString().contains("Connecticut");
            Assert.assertTrue("File data does not contain Connecticut", contains);
            contains = fileData.toString().contains("125");
            Assert.assertTrue("File data does not contain 125", contains);
            contains = fileData.toString().contains("Florida");
            Assert.assertTrue("File data does not contain Florida", contains);
         } catch (Exception e) {
         } finally {
             if(file != null){
                file.delete();
             }
             try{
                 connection.getApp("Sample").getCube("Basic").clearAllData();
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
     @Test
     public void testUpdateFileExWithoutStoreAndUnlock() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         File file = null;
         try {
            validateEssbaseConnection(connection);
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
         	   File myFile=new File("D:\\CodeBase\\govgupta_view_main\\planning\\HspJS\\TestResources\\jnitojapi");
         	   url = myFile.toURI().toURL();
            }
            String dataFilePath = url.getPath() + "/testDataWithoutRule.txt";
            HspMbrError[] mbrErrors = EssbaseMainAPI.updateFileEx(connection, "Sample", "Basic", null, dataFilePath, IEssOlapFileObject.TYPE_TEXT, false);
            String tempDir = System.getProperty("java.io.tmpdir");             
            connection.getApp("Sample").getCube("Basic").exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.ALL, true);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
            Assert.assertNull("Errors array is not null", mbrErrors);
            boolean contains = fileData.toString().contains("Connecticut");
            Assert.assertTrue("File data does not contain Connecticut", contains);
            contains = fileData.toString().contains("125");
            Assert.assertTrue("File data does not contain 125", contains);
            contains = fileData.toString().contains("Florida");
            Assert.assertTrue("File data does not contain Florida", contains);
         } catch (Exception e) {
         } finally {
             if(file != null){
                file.delete();
             }
             try{
                 connection.getApp("Sample").getCube("Basic").clearAllData();
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
    
     @Test
     public void testUpdateFileExWithStoreAndUnlock() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         File file = null;
         try {
            validateEssbaseConnection(connection);
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
         	   File myFile=new File("D:\\CodeBase\\govgupta_view_main\\planning\\HspJS\\TestResources\\jnitojapi");
         	   url = myFile.toURI().toURL();
            }
            String dataFilePath = url.getPath() + "/testDataWithoutRule.txt";
            HspMbrError[] mbrErrors = EssbaseMainAPI.updateFileEx(connection, "Sample", "Basic", null, dataFilePath, true, false, false);
            String tempDir = System.getProperty("java.io.tmpdir");             
            connection.getApp("Sample").getCube("Basic").exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.ALL, true);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
            Assert.assertNull("Errors array is not null", mbrErrors);
            boolean contains = fileData.toString().contains("Connecticut");
            Assert.assertTrue("File data does not contain Connecticut", contains);
            contains = fileData.toString().contains("125");
            Assert.assertTrue("File data does not contain 125", contains);
            contains = fileData.toString().contains("Florida");
            Assert.assertTrue("File data does not contain Florida", contains);
         } catch (Exception e) {
         } finally {
             if(file != null){
                file.delete();
             }
             try{
                 connection.getApp("Sample").getCube("Basic").clearAllData();
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
     @Test
     public void testUpdateFileASOEx2WithOutAbortOnError() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         File file = null;
         try {
            validateEssbaseConnection(connection);
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
         	   File myFile=new File("D:\\CodeBase\\govgupta_view_main\\planning\\HspJS\\TestResources\\jnitojapi");
         	   url = myFile.toURI().toURL();
            }
            String dataFilePath = url.getPath() + "/Data_ASO.txt";
            HspMbrError[] mbrErrors = EssbaseMainAPI.updateFileASOEx2(connection, "HP1_ASO", "HP1_ASO", null, dataFilePath, IEssOlapFileObject.TYPE_TEXT, true, false, 5, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_COMMIT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE, 10L);
            String tempDir = System.getProperty("java.io.tmpdir");             
            IEssCube cube = connection.getApp("HP1_ASO").getCube("HP1_ASO");
            cube.exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.LEVEL0, false);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
            Assert.assertNull("Errors array is not null", mbrErrors);
            boolean contains = fileData.toString().contains("Employee 1");
            Assert.assertTrue("File data does not contain Employee 1", contains);
            contains = fileData.toString().contains("Employee 2");
            Assert.assertTrue("File data does not contain Employee 2", contains);
            contains = fileData.toString().contains("John S");
            Assert.assertTrue("File data does not contain John S", contains);
         } catch (Exception e) {
         } finally {
             if(file != null){
                file.delete();
             }
             try{
                 connection.getApp("HP1_ASO").getCube("HP1_ASO").clearAllData();
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
     @Test
     public void testUpdateFileASOEx2WithAbortOnError() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server1, null, null);
         File file = null;
         try {
            validateEssbaseConnection(connection);
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
         	   File myFile=new File("D:\\CodeBase\\govgupta_view_main\\planning\\HspJS\\TestResources\\jnitojapi");
         	   url = myFile.toURI().toURL();
            }
            String dataFilePath = url.getPath() + "/Data_ASO.txt";
            HspMbrError[] mbrErrors = EssbaseMainAPI.updateFileASOEx2(connection, "HP1_ASO", "HP1_ASO", null, dataFilePath, IEssOlapFileObject.TYPE_TEXT, true, false, false, 5, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_COMMIT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE, 10L);
            String tempDir = System.getProperty("java.io.tmpdir");             
            IEssCube cube = connection.getApp("HP1_ASO").getCube("HP1_ASO");
            cube.exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.LEVEL0, false);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
            Assert.assertNull("Errors array is not null", mbrErrors);
            boolean contains = fileData.toString().contains("Employee 1");
            Assert.assertTrue("File data does not contain Employee 1", contains);
            contains = fileData.toString().contains("Employee 2");
            Assert.assertTrue("File data does not contain Employee 2", contains);
            contains = fileData.toString().contains("John S");
            Assert.assertTrue("File data does not contain John S", contains);
         } catch (Exception e) {
         } finally {
             if(file != null){
                file.delete();
             }
             try{
                 connection.getApp("HP1_ASO").getCube("HP1_ASO").clearAllData();
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
     @Test
     public void testLoadBufferInitASOAndLoadBufferTerm() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         File file = null;
         try {
            validateEssbaseConnection(connection);
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
          	   File myFile=new File("D:\\CodeBase\\govgupta_view\\planning\\HspJS\\TestResources\\jnitojapi");
          	   url = myFile.toURI().toURL();
            }
            String ruleFilePath = url.getPath() + "/testASO.rul";
            String dataFilePath = url.getPath() + "/Data_ASO.txt";
            EssbaseMainAPI.loadBufferInit(connection, "HP1_ASO", "HP1_ASO", 4, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_DUPLICATES_ADD, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_IGNORE_MISSING_VALUES, 10);
            HspMbrError[] mbrErrors = EssbaseMainAPI.beginDataload(connection, "HP1_ASO", "HP1_ASO", ruleFilePath, IEssOlapFileObject.TYPE_RULES, dataFilePath, IEssOlapFileObject.TYPE_TEXT, false, 4);
            EssbaseMainAPI.loadBufferTerm(connection, "HP1_ASO", "HP1_ASO", 4, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_COMMIT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE);
            String tempDir = System.getProperty("java.io.tmpdir");             
            connection.getApp("HP1_ASO").getCube("HP1_ASO").exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.ALL, true);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
             Assert.assertNull("Errors array is not null", mbrErrors);
             boolean contains = fileData.toString().contains("Employee 1");
             Assert.assertTrue("File data does not contain Employee 1", contains);
             contains = fileData.toString().contains("Employee 2");
             Assert.assertTrue("File data does not contain Employee 2", contains);
             contains = fileData.toString().contains("John S");
             Assert.assertTrue("File data does not contain John S", contains);
         } catch (Exception e) {
         } finally {
             if(file != null){
                file.delete();
             }
             try{
                 connection.getApp("Sample").getCube("Basic").clearAllData();
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
     @Test
     public void testBeginDataLoadASO() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         File file = null;
         boolean isBufferCleared = false;
         HspMbrError[] mbrErrors = null;
         IEssCube existingCube = null;
         try {
            validateEssbaseConnection(connection);
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
          	   File myFile=new File("D:\\CodeBase\\govgupta_view\\planning\\HspJS\\TestResources\\jnitojapi");
          	   url = myFile.toURI().toURL();
            }
            String ruleFilePath = url.getPath() + "/testASO.rul";
            EssbaseMainAPI.putObject(connection, "HP1_ASO", "HP1_ASO", "testRul", IEssOlapFileObject.TYPE_RULES, ruleFilePath, false);
			String data = "Name Jan FY15 Budget \"BU Version_1\" USD NY \"John S\" \"Employee 1\" 250\n" +
					"Name Feb FY15 Budget \"BU Version_1\" USD NY \"John S\" \"Employee 2\" 250";
            EssbaseMainAPI.loadBufferInit(connection, "HP1_ASO", "HP1_ASO", 6, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_DUPLICATES_ADD, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_IGNORE_MISSING_VALUES, 10);
            existingCube = EssbaseMainAPI.beginDataLoadASO(connection, "HP1_ASO", "HP1_ASO", "testRul", IEssOlapFileObject.TYPE_RULES, true, false, false, 6);
            EssbaseMainAPI.sendString(connection, existingCube, data);
            mbrErrors = EssbaseMainAPI.endDataload(connection, existingCube);
            EssbaseMainAPI.loadBufferTerm(connection, "HP1_ASO", "HP1_ASO", 6, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_COMMIT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE);
            isBufferCleared = true;
            String tempDir = System.getProperty("java.io.tmpdir");             
            connection.getApp("HP1_ASO").getCube("HP1_ASO").exportData(tempDir+"/exportedData.txt", IEssCube.EEssDataLevel.ALL, true);
            file = new File(tempDir+"/exportedData.txt");
            FileReader fileReader = new FileReader(file);
            BufferedReader br = new BufferedReader(fileReader);  
            String line;
            StringBuilder fileData = new StringBuilder();
            while ((line = br.readLine()) != null){
                 fileData.append(line);
            }
            fileReader.close();
            br.close();
            System.out.println("File Data is : "+fileData);
             Assert.assertNull("Errors array is not null", mbrErrors);
             boolean contains = fileData.toString().contains("Employee 1");
             Assert.assertTrue("File data does not contain Employee 1", contains);
             contains = fileData.toString().contains("Employee 2");
             Assert.assertTrue("File data does not contain Employee 2", contains);
             contains = fileData.toString().contains("John S");
             Assert.assertTrue("File data does not contain John S", contains);
         } catch (Exception e) {
         } finally {
        	 if(!isBufferCleared){
	            	if(null != mbrErrors){
	                    existingCube.loadBufferTerm(new long[] { 6 }, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_ABORT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE);
	                }else{
	                    existingCube.loadBufferTerm(new long[] { 6 }, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_STORE_DATA, IEssCube.ESS_ASO_DATA_LOAD_BUFFER_COMMIT, IEssCube.ESS_ASO_DATA_LOAD_INCR_TO_MAIN_SLICE);
	                }
        	 }
             if(file != null){
                file.delete();
             }
             try{
            	 
             }catch(Exception e1){
            	 EssbaseMainAPI.deleteObject(connection, "HP1_ASO", "HP1_ASO", "testrul", IEssOlapFileObject.TYPE_RULES);
             }
             try{
                 connection.getApp("Sample").getCube("Basic").clearAllData();
             }catch(Exception e2){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
/*     @Test
     public void testSetFilterEx() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         IEssCube existingCube = null;
         try {
            validateEssbaseConnection(connection);
            EssbaseMainAPI.setFilterEx(connection, "Sample", "Basic", "testFilter1", true, IEssCube.EEssCubeAccess.READ_WRITE_CUBE_DATA_INT_VALUE);
            EssbaseMainAPI.setFilterEx(connection, "Sample", "Basic", "testFilter2", true, IEssCube.EEssCubeAccess.READ_CUBE_DATA_INT_VALUE);
            existingCube = connection.getOlapServer().getApplication("Sample").getCube("Basic");
            IEssIterator iterator = existingCube.getSecurityFiltersWithNameOnly();
        	List<String> filtersList = new ArrayList<String>();
            for (int i=0; i < iterator.getCount(); i++) {
        		IEssCube.IEssSecurityFilter filter = (IEssCube.IEssSecurityFilter)iterator.getAt(i);
        		filtersList.add(filter.getName());
        	}
        	boolean contains = filtersList.contains("testFilter1");
        	Assert.assertTrue("Filter with name testFilter1 is not available.", contains);
        	contains = filtersList.contains("testFilter2");
        	Assert.assertTrue("Filter with name testFilter2 is not available.", contains);
         } catch (Exception e) {
         } finally {
             try{
                 existingCube.deleteSecurityFilter("testFilter1");
             }catch(Exception e){
             }
             try{
                 existingCube.deleteSecurityFilter("testFilter2");
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }*/

     @Test
     public void testSetFilterExAndSetFilterRow() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         IEssSecurityFilter filter = null;
         try {
            validateEssbaseConnection(connection);
            filter = EssbaseMainAPI.setFilterEx(connection, "Sample", "Basic", "testFilter1", true, IEssCube.EEssCubeAccess.READ_WRITE_CUBE_DATA_INT_VALUE);
            EssbaseMainAPI.setFilterRow(connection, filter, "@IALLANCESTORS(Scenario)", (short)IEssCube.EEssCubeAccess.READ_CUBE_DATA_INT_VALUE);
            EssbaseMainAPI.setFilterRow(connection, filter, "@IDESCENDANTS(Scenario)", (short)IEssCube.EEssCubeAccess.READ_CUBE_DATA_INT_VALUE);
            EssbaseMainAPI.setFilterRow(connection, filter, "", (short)0);
            ArrayList<String> filterRows = new ArrayList<String>();
           
            IEssCube.IEssSecurityFilter testFilter = connection.getOlapServer().getApplication("Sample").getCube("Basic").getSecurityFilter("testFilter1");
            IEssCube.IEssSecurityFilter.IEssFilterRow row = testFilter.getFilterRow();
			while(row != null){
				filterRows.add(row.getRowString());
				row = testFilter.getFilterRow();
			}
       
        	boolean contains = filterRows.contains("@IALLANCESTORS(Scenario)");
        	Assert.assertTrue("Filter row with row string @IDESCENDANTS(Scenario) is not available.", contains);
        	contains = filterRows.contains("@IDESCENDANTS(Scenario)");
        	Assert.assertTrue("Filter row with row string @IDESCENDANTS(Scenario) is not available.", contains);
         } catch (Exception e) {
         } finally {
             try{
            	 filter.delete();
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
     @Test
     public void testVerifyFilterAndVerifyFilterRow() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         IEssSecurityFilter filter = null;
         try {
            validateEssbaseConnection(connection);
            String rowStrings[] = {"@IALLANCESTORS(Scenario)", "@IDESCENDANTS(Scenario)"};
            filter = EssbaseMainAPI.setFilterEx(connection, "Sample", "Basic", "testFilter1", true, IEssCube.EEssCubeAccess.READ_WRITE_CUBE_DATA_INT_VALUE);
            EssbaseMainAPI.setFilterRow(connection, filter, rowStrings[0], (short)IEssCube.EEssCubeAccess.READ_CUBE_DATA_INT_VALUE);
            EssbaseMainAPI.setFilterRow(connection, filter, rowStrings[1], (short)IEssCube.EEssCubeAccess.READ_CUBE_DATA_INT_VALUE);
            EssbaseMainAPI.setFilterRow(connection, filter, null, (short)0);
            filter = EssbaseMainAPI.verifyFilter(connection, "Sample", "Basic", "testFilter1", rowStrings);
            for (int i = 0; i < rowStrings.length; i++) {
            	EssbaseMainAPI.verifyFilterRow(connection, filter, rowStrings[i]);
			}
         } catch (Exception e) {
        	 fail();
         } finally {
             try{
            	 filter.delete();
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
     @Test
     public void testAssignFilterToUser() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         IEssOlapUser user1 = null;
         IEssOlapUser user2 = null;
         IEssSecurityFilter filter = null;
         try {
             validateEssbaseConnection(connection);
             user1 = connection.getOlapServer().createOlapUser("testUser9", "password");
             filter = connection.getOlapServer().getApplication("Sample").getCube("Basic").createSecurityFilter("testFilter1");
             EssbaseMainAPI.assignFilterToUser(connection, "Sample", "Basic", user1.getName(), "testFilter1");
             
             user2 = connection.getOlapServer().createOlapUser("testUser10", "password");
             filter = connection.getOlapServer().getApplication("Sample").getCube("Basic").getSecurityFilter("testFilter1");
             EssbaseMainAPI.assignFilterToUser(connection, "Sample", "Basic", user2.getName(), "testFilter1");
             
             final String[] allAccess = filter.getSecurityFilterList();
             boolean contains = Arrays.asList(allAccess).contains(user1.getName()+"@Native Directory");
             Assert.assertTrue("User Name "+user1.getName()+" is not assigned the filter", contains);
             
             contains = Arrays.asList(allAccess).contains(user2.getName()+"@Native Directory");
             Assert.assertTrue("User Name "+user2.getName()+" is not assigned the filter", contains);
             
         } catch (Exception e) {
         } finally {
        	 try{
        		 user1.deleteUser();
        		 user2.deleteUser();
             }catch(Exception e){
             }
             try{
            	 filter.delete();
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
     @Test
     public void testPerformCustomCalc() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         try {
            validateEssbaseConnection(connection);
            IEssPerformCustomCalc customCalc = EssbaseMainAPI.getPerformCustomCalcInstance(connection, "HP1_ASO", "HP1_ASO");
            
            customCalc.setPOV("{([curr year],jan,sale,Cash,[No Promotion],[20 to 25 Years],[50,000-69,999],[Digital Cameras],[004118],[80101])}");
            customCalc.setTarget("([Curr Year],Jan)");
            customCalc.setDebitMember("[Sale]");
            customCalc.setCreditMember("[No Sale]");
            customCalc.setOffset("([Curr Year], Jan, Returns)");
            customCalc.setSourceRegion("crossjoin({Units,transactions},{([prev year],dec)})");
            customCalc.setGroupID(0);
            customCalc.setRuleID(0);
            ArrayList errAndWarnMsgsList = new ArrayList();
            boolean isSuccessful = EssbaseMainAPI.performCustomCalc(customCalc, false, errAndWarnMsgsList);
            System.out.println(isSuccessful);
         } catch (Exception e) {
         } finally {
             connection.disconnect();
             connection = null;
         }
     }
 
     @Test
     public void testSetUserDbAccess() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         IEssOlapUser user1 = null;
         IEssSecurityFilter filter = null;
         try {
            validateEssbaseConnection(connection);
            
            //user1 = connection.getOlapServer().createOlapUser("dbAccessUser9", "password");
            user1 = connection.getOlapServer().getOlapUser("dbAccessUser8");
            user1.setUserType((short)0, (short)0);
            //user1.setUser(EEssAccess.ESS_PRIV_USERCREATE, 0L, false);
            filter = connection.getOlapServer().getApplication("Sample").getCube("Basic").createSecurityFilter("testFilter1");
            URL url = HspUtils.findURL("jnitojapi");
            if(url == null){
         	   File myFile=new File("D:\\CodeBase\\govgupta_view\\planning\\HspJS\\TestResources\\jnitojapi");
         	   url = myFile.toURI().toURL();
            }
            String calcFilePath1 = url.getPath() + "/testCalc1.csc";
            try{
                EssbaseMainAPI.putObject(connection, "Sample", "Basic", "tCalc1", IEssOlapFileObject.TYPE_CALCSCRIPT, calcFilePath1, true);
            }catch(Exception e){
            }
             
            EssbaseMainAPI.assignFilterToUser(connection, "Sample", "Basic", user1.getName(), "testFilter1");
            EssbaseMainAPI.setUserDbAccess(connection, user1.getName(), "Sample", IEssCube.EEssCubeAccess.CALCULATE_CUBE_DATA_INT_VALUE, true);
            String filterUsers[] = filter.getSecurityFilterList();
            System.out.println(Arrays.toString(filterUsers));
            boolean contains = Arrays.asList(filterUsers).contains(user1.getName());
            Assert.assertFalse("Filter is still associated with the user", contains);
            IEssCube.EEssCubeAccess cubeAccess = connection.getCube("Sample", "Basic").getCubeAccess();
            Assert.assertEquals("Cube access is not same", cubeAccess.intValue(), IEssCube.EEssCubeAccess.READ_WRITE_CUBE_DATA_INT_VALUE);
            IEssCalcList calcList = connection.getApp().getCalcList(user1.getName(), connection.getCube("Sample", "Basic"));
            String calcArray[] = calcList.getCalcList();
            System.out.println(Arrays.toString(calcArray));
            contains = Arrays.asList(calcArray).contains("tCalc1");
            Assert.assertTrue("Calculation script is not assigned", contains);
         } catch (Exception e) {
         } finally {
        	 try{
        		 //user1.deleteUser();
             }catch(Exception e){
             }
             try{
            	 filter.delete();
             }catch(Exception e){
             }
             try{
                 EssbaseMainAPI.deleteObject(connection, "Sample", "Basic", "tCalc1", IEssOlapFileObject.TYPE_CALCSCRIPT);
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }

     @Test
     public void testDeleteUser() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         IEssOlapUser user1 = null;
         IEssSecurityFilter filter = null;
         try {
            validateEssbaseConnection(connection);
            
            IEssOlapUser user = connection.getOlapServer().createOlapUser("testUserNew1", "password");
            user.deleteUser();
            try{
            	IEssOlapUser deletedUser = connection.getOlapServer().getOlapUser("testUserNew");
            }catch(Exception ee){
            	System.out.println("Exception while retrieving user : "+ee.getMessage());
            }
            IEssOlapUser newUser = connection.getOlapServer().createOlapUser("testUserNew", "password");
          } catch (Exception e) {
         } finally {
        	 try{
        		 //user1.deleteUser();
             }catch(Exception e){
             }
             try{
            	 filter.delete();
             }catch(Exception e){
             }
             try{
                 EssbaseMainAPI.deleteObject(connection, "Sample", "Basic", "tCalc1", IEssOlapFileObject.TYPE_CALCSCRIPT);
             }catch(Exception e){
             }
             connection.disconnect();
             connection = null;
         }
     }
     
/*     @Test
     public void testPerformAllocationASO() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, server2, null, null);
         try {
            validateEssbaseConnection(connection);
            IEssPerformAllocation performAlloc = connection.getApp("HP1_ASO").getCube("HP1_ASO").getPerformAllocationInstance();
    		performAlloc.setPOV("{[Acc19802]}");
    		performAlloc.setAmount("([ANLT], [OUTT], [PUBT], [FRED], [Feb-08], [Allocations], [Beginning Balance], [ORG63], [CC10000])");
    		performAlloc.setTarget("([ANLT], [OUTT], [PUBT], [FRED], [Feb-08], [Allocations])");
    		performAlloc.setDebitMember("[Beginning Balance Debit]");
    		performAlloc.setCreditMember("[Beginning Balance Credit]");
    		performAlloc.setRange("CrossJoin(Descendants([ORGT], [Organisation].Levels(0)), Descendants([CCT], [Cost Centre].Levels(0)))");
    		performAlloc.setBasis("([ANLT], [OUTT], [PUBT], [FRED], [Feb-05/06], [Beginning Balance], [Actual])");
    		performAlloc.setOffset("([ANLT], [OUTT], [PUBT], [FRED], [Feb-08], [Allocations], [ORG63], [CC10000])");
    		performAlloc.setAmountContext("");
    		performAlloc.setAmountTimeSpan("");
    		performAlloc.setTargetTimeSpan("");
    		performAlloc.setTargetTimeSpanOption(0 IEssPerformAllocation.ESS_ASO_ALLOCATION_TIMESPAN_DIVIDEAMT);
    		performAlloc.setExcludedRange("");
    		performAlloc.setBasisTimeSpan("");
    		performAlloc.setBasisTimeSpanOption(0 IEssPerformAllocation.ESS_ASO_ALLOCATION_TIMESPAN_COMBINE_BASIS);
    		performAlloc.setAllocationMethod(IEssPerformAllocation.ESS_ASO_ALLOCATION_METHOD_SHARE);
    		performAlloc.setSpreadSkipOption(0);
    		performAlloc.setZeroAmountOption(IEssPerformAllocation.ESS_ASO_ALLOCATION_ZEROAMT_NEXTAMT);
    		performAlloc.setZeroBasisOption(IEssPerformAllocation.ESS_ASO_ALLOCATION_ZEROBASIS_NEXTAMT);
    		performAlloc.setNegativeBasisOption(IEssPerformAllocation.ESS_ASO_ALLOCATION_NEGBASIS_NEXTAMT);
    		performAlloc.setRoundMethod(IEssPerformAllocation.ESS_ASO_ALLOCATION_ROUND_NONE);
    		performAlloc.setRoundDigits("");
    		performAlloc.setRoundToLocation("");
    		performAlloc.setGroupID(0);
    		List<Object> errAndWarnMsgsList = new ArrayList<Object>();
    		EssbaseMainAPI.performAllocationASO22(performAlloc, true, errAndWarnMsgsList);
         } catch (Exception e) {
         } finally {
             connection.disconnect();
             connection = null;
         }
     }*/
     
     @Test
     public void testConnectionPool() throws Exception{                                                             

         EssbaseConnection connection = new EssbaseConnection("admin", "password", false, null, "localhost", null, null);
         HspEssbasePool essbasePool = new HspEssbasePool(2, 5, 10, "localhost", "admin", "password", "Sample", 0);
         essbasePool.createObject();
         try {
            validateEssbaseConnection(connection);
          
         } catch (Exception e) {
         } finally {
             connection.disconnect();
             connection = null;
         }
     }
     
    private static final String ESS_CONN = "Essbase Connection";
}