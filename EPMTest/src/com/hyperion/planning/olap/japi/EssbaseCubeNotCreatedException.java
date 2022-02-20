package com.hyperion.planning.olap.japi;


import java.util.Locale;
import java.util.Properties;

public class EssbaseCubeNotCreatedException extends Exception{
	// Constructors	
	public EssbaseCubeNotCreatedException()
	{	//super(myResKey, null, null);
	}
	public EssbaseCubeNotCreatedException(Properties arguments)
	{	//super(myResKey, arguments, null);
	}
	public EssbaseCubeNotCreatedException(Exception innerException)
	{	//super(myResKey, null, innerException);
	}
	public EssbaseCubeNotCreatedException(Properties arguments, Exception innerException)
	{	//super(myResKey, arguments, innerException);
	}
	
	//methods
	public String getMessage()
	{	
		//return super.getLocalizedMessage(Locale.ENGLISH);
		return null;
	}
	//properties
	private static final String myResKey = "MSG_CUBE_NOT_CREATED";
}
