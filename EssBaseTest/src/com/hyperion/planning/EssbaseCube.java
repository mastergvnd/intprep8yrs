package com.hyperion.planning;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssMaxlSession;
import com.essbase.api.datasource.IEssOlapApplication;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.session.IEssbase;

public class EssbaseCube {
	private String cubeName                    = null;
	private String applicationName             = null;
	
	private IEssCube cube                      = null;
	private IEssCubeOutline cubeOutline        = null;
	private IEssbase ess                       = null;
	private IEssOlapServer olapServer          = null;
	private IEssOlapApplication olapApp        = null;
	private IEssMaxlSession maxlSession		   = null;
	public String getCubeName() {
		return cubeName;
	}

	public String getApplicationName() {
		return applicationName;
	}
	
	public IEssOlapApplication getOlapApp() {
		return olapApp;
	}

	public void setCubeName(String cubeName) {
		this.cubeName = cubeName;
	}

	public void setApplicationName(String applicationName) {
		this.applicationName = applicationName;
	}

	public IEssCube getCube() {
		return cube;
	}

	public IEssCubeOutline getCubeOutline() {
		return cubeOutline;
	}

	public IEssbase getEss() {
		return ess;
	}

	public IEssOlapServer getOlapServer() {
		return olapServer;
	}

	public void setEss(IEssbase ess) {
		this.ess = ess;
	}

	public void setOlapServer(IEssOlapServer olapServer) {
		this.olapServer = olapServer;
	}
	
	public static IEssCubeOutline openCubeOutline(IEssCube cube) throws EssException {
		return cube.openOutline(false, true, true);
	}

	public static IEssOlapApplication getApplication(IEssOlapServer olapServer, String appName) throws EssException {
		return olapServer.getApplication(appName);
	}

	public static IEssCube getCube(IEssOlapApplication olapApp, String cubeName) throws EssException {
		return olapApp.getCube(cubeName);
	}
	
	public void openMaxlSession(String sessionName) throws EssException{
		maxlSession =  olapServer.openMaxlSession(sessionName);
	}
	
	public IEssMaxlSession getMaxlSession(){
		return maxlSession;
	}

	public void load() throws EssException {
		boolean isAppExist = true;//EssbaseUtil.isAppExists(this);
		System.out.println("doesAppExist : "+isAppExist);
		
		if(!isAppExist){
			EssbaseUtil.createApplication(this, "App Comments");
			EssbaseUtil.createDatabase(this, "DB Comments");
			this.olapApp = getApplication(this.olapServer, this.applicationName);
			this.cube = getCube(this.olapApp, this.cubeName);
			EssbaseUtil.createOutline(this);
		}else{
			this.olapApp = getApplication(this.olapServer, this.applicationName);
			this.cube = getCube(this.olapApp, this.cubeName);
			this.cubeOutline = openCubeOutline(cube);
			cube.setClientCachingEnabled(false);
			cubeOutline.setClientCachingEnabled(false);
		}
	}
}
