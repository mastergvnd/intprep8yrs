package com.test;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssIterator;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssOlapApplication;
import com.essbase.api.datasource.IEssOlapFileObject;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.domain.IEssDomain;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.metadata.IEssMember;
import com.essbase.api.session.IEssbase;

public class OutLineType {
	private static String s_userName = "admin";
    private static String s_password = "password";
    private static String s_olapSvrName = "slc14vmw.us.oracle.com";
    private static String s_provider = "Embedded"; // Default
    
    private static String appName = "ERfe";
    private static String cubeName = "TempC";
    public OutLineType() {
    }
    
    public static void main(String[] args) {
        IEssbase ess = null;
        IEssOlapServer olapSvr = null;
        IEssOlapApplication essbaseApplication = null;
        try {
            ess = IEssbase.Home.create(IEssbase.JAPI_VERSION);
            IEssDomain dom 
                = ess.signOn(s_userName, s_password, false, null, s_provider);
            olapSvr = (IEssOlapServer)dom.getOlapServer(s_olapSvrName);
            olapSvr.connect();
        	boolean isASO = determineOutlineIfAso(olapSvr);
        	if(isASO){
        		essbaseApplication = olapSvr.createApplication(appName, (short)4, "utf8");
        		olapSvr.convertApplicationToUnicode(appName);
        		IEssCube cube = essbaseApplication.createCube(cubeName, IEssCube.EEssCubeType.ASO, true);
        		final boolean unlock = true;
        		cube.lockOlapFileObject(IEssOlapFileObject.TYPE_OUTLINE, cubeName);
        		cube.copyOlapFileObjectToServer(IEssOlapFileObject.TYPE_OUTLINE, cubeName, "C:\\Users\\govgupta.ORADEV\\Desktop\\OP\\LM1C.otl", unlock);
        		cube.setActive();
        		cube.restructure((short)IEssCube.EEssRestructureOption.DISCARD_ALL_DATA_INT_VALUE);
        	}

        } catch (EssException x) {
        	System.out.println("Exception Occurred");
            System.err.println("Error: " + x.getMessage());
            x.printStackTrace();
        } finally {
        	comeOutClean(ess, olapSvr);
        }
        System.out.println("Completed");
    }

	private static void deleteApplication(IEssOlapServer olapSvr) throws EssException {
		IEssOlapApplication olapApp = olapSvr.getApplication(appName);
		olapApp.delete();
	}

	private static boolean determineOutlineIfAso(IEssOlapServer olapSvr) throws EssException {
		boolean isASO = false;
		try{
			IEssOlapApplication essbaseApplication;
			essbaseApplication = olapSvr.createApplication(appName, "utf8");
			olapSvr.convertApplicationToUnicode(appName);
			IEssCube cube = essbaseApplication.createCube(cubeName, isASO ? IEssCube.EEssCubeType.ASO : IEssCube.EEssCubeType.NORMAL, true);
			final boolean unlock = true;
			cube.lockOlapFileObject(IEssOlapFileObject.TYPE_OUTLINE, cubeName);
			cube.copyOlapFileObjectToServer(IEssOlapFileObject.TYPE_OUTLINE, cubeName, "C:\\Users\\govgupta.ORADEV\\Desktop\\OP\\LM1C.otl", unlock);
			cube.setActive();
			cube.restructure((short)IEssCube.EEssRestructureOption.DISCARD_ALL_DATA_INT_VALUE);
		}catch(EssException e){
			if(e.getNativeCode() == 1019005){
				isASO = true;
				deleteApplication(olapSvr);
			}else{
				throw e;
			}
		}
		return isASO;
	}

    public static void comeOutClean(IEssbase ess, IEssOlapServer olapSvr) {
        try {
            if (olapSvr != null && olapSvr.isConnected() == true)
                olapSvr.disconnect();
        } catch (EssException x) {
            System.err.println("Error: " + x.getMessage());
        }

        try {
            if (ess != null && ess.isSignedOn() == true)
                ess.signOff();
        } catch (EssException x) {
            System.err.println("Error: " + x.getMessage());
        }
	}
}

