package com.hyperion.planning;

import com.essbase.api.base.EssException;
import com.essbase.api.datasource.IEssOlapApplication;
import com.essbase.api.datasource.IEssOlapServer;
import com.essbase.api.domain.IEssDomain;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.session.IEssbase;

public class EssbaseConnectionManager {
	private String userName = "admin";
	private String password = "password";
	private String server = "slc14vmw.us.oracle.com";
	//private static String server = "den02ahj.us.oracle.com";
	private String provider = "Embedded";
	private IEssOlapApplication application;
	
	public EssbaseConnectionManager() {
	}
	
	public EssbaseConnectionManager(String userName, String password, String server){
		if(userName != null && userName.length() > 0)
			this.userName = userName;
		if(password != null && password.length() > 0)
			this.password = password;
		if(server != null && server.length() > 0)
			this.server = server;
	}
	
	public String getUserName() {
		return this.userName;
	}

	public String getPassword() {
		return this.password;
	}

	public String getServer() {
		return this.server;
	}

	public String getProvider() {
		return this.provider;
	}

	public void setUserName(String userName) {
		this.userName = userName;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	public void setServer(String server) {
		this.server = server;
	}

	public void setProvider(String provider) {
		this.provider = provider;
	}

	public static void comeOutClean(EssbaseCube cube) {
		IEssbase ess = cube.getEss();
		IEssOlapServer olapServer = cube.getOlapServer();
		IEssCubeOutline cubeOutline = cube.getCubeOutline();
		try{
			olapServer.clearClientCache();
			cubeOutline.clearClientCache();
			System.out.println("Cache cleared");
			if (cubeOutline != null && cubeOutline.isOpen()) 
				cubeOutline.close();
		}catch (EssException x) {
			System.err.println("Error: " + x.getMessage());
		}
		
		try {
			if (olapServer != null && olapServer.isConnected() == true)
				olapServer.disconnect();
		} catch (EssException x) {
			System.err.println("Error: " + x.getMessage());
		}

		try {
			if (ess != null && ess.isSignedOn() == true)
				ess.signOff();
		} catch (EssException x) {
			System.err.println("Error: " + x.getMessage());
		}
		
		System.out.println("Resources cleaned");
	}

	private void getOlapConnection(EssbaseCube cube) throws EssException {
		IEssbase ess = cube.getEss();
		IEssDomain domain = ess.signOn(this.userName, this.password, false, null, this.provider);
		IEssOlapServer olapServer = domain.getOlapServer(server);
		olapServer.connect();
		cube.setOlapServer(olapServer);
		setApplication(olapServer.getApplication(cube.getApplicationName()));
	}

	private static IEssbase getJAPIVersion() throws EssException {
		return IEssbase.Home.create(IEssbase.JAPI_VERSION);
	}

	public void connectToCube(EssbaseCube cube) throws EssException {
		cube.setEss(getJAPIVersion());
		getOlapConnection(cube);
	}

	public IEssOlapApplication getApplication() {
		return application;
	}

	public void setApplication(IEssOlapApplication application) {
		this.application = application;
	}
}
