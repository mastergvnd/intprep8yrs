package com.hyperion.planning;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.essbase.api.base.EssException;
import com.essbase.api.base.IEssBaseObject;
import com.essbase.api.base.IEssIterator;
import com.essbase.api.datasource.IEssCube;
import com.essbase.api.datasource.IEssMaxlResultSet;
import com.essbase.api.metadata.IEssCubeOutline;
import com.essbase.api.metadata.IEssDimension;
import com.essbase.api.metadata.IEssMember;
import com.essbase.api.metadata.IEssMemberSelection;

public class EssbaseUtil {

	private static StringBuffer dProperties = new StringBuffer();
	private static List<String> dPropertiesList = new ArrayList<String>();
	private static StringBuffer dOrder = new StringBuffer();
	private static StringBuffer mOrder = new StringBuffer();
	private static StringBuffer mProperties = new StringBuffer(); 
	private static StringBuffer allProperties = new StringBuffer(); 

	private static List<String> mPropertiesList = new ArrayList<String>();
	
	private final static String QUERY_DATABASE = "query database '";
	private final static String APP_DB_SEPARATOR = "'.'";
	private final static String GET_CUBE_SIZE_INFO = "' get cube_size_info;";
	private final static String QUERY_TRACKING_ENABLED = "query_tracking_enabled";
	
	private final static String ALTER_DATABASE = "alter database '";
	private final static String ENABLE_QUERY_TRACKING = "' enable query_tracking;";
	public static List<IEssDimension> getDimensionsAsList(IEssCubeOutline cubeOutLine) throws EssException{
		List<IEssDimension> dimenionsList = new ArrayList<IEssDimension>();
		IEssBaseObject[] dimensions = cubeOutLine.getDimensions().getAll();
		for(int i = 0; i<dimensions.length; i++){
			IEssDimension dimension = (IEssDimension) dimensions[i];
			dimenionsList.add(dimension);
		}
		return dimenionsList;
	}
	
	public static Map<String, IEssDimension> getDimensionsAsMap(IEssCubeOutline cubeOutLine) throws EssException{
		Map<String, IEssDimension> dimenionsList = new LinkedHashMap<String, IEssDimension>();
		IEssBaseObject[] dimensions = cubeOutLine.getDimensions().getAll();
		for(int i = 0; i<dimensions.length; i++){
			IEssDimension dimension = (IEssDimension) dimensions[i]; 
			dimenionsList.put(dimension.getName(), dimension);
		}
		return dimenionsList;
	}
	
	public static Map<String, IEssMember> getMembersAsMap(IEssDimension dimension) throws EssException{
		Map<String, IEssMember> membersList = new HashMap<String, IEssMember>();
		IEssBaseObject[] members = dimension.getDimensionRootMember().getChildMembers().getAll();
		for(int i = 0; i<members.length; i++){
			IEssMember member = (IEssMember) members[i];
			membersList.put(member.getName(), member);
		}
		return membersList;
	}
	
	public static List<IEssMember> getMembers(IEssDimension dimension) throws EssException{
		List<IEssMember> membersList = new ArrayList<IEssMember>();
		IEssBaseObject[] members = dimension.getDimensionRootMember().getChildMembers().getAll();
		for(int i = 0; i<members.length; i++){
			IEssMember member = (IEssMember) members[i];
			membersList.add(member);
		}
		return membersList;
	}
	
	public static Map<String, IEssMember> getSubMembersAsMap(IEssMember member) throws EssException{
		Map<String, IEssMember> membersList = new HashMap<String, IEssMember>();
		IEssBaseObject[] members = member.getChildMembers().getAll();
		for(int i = 0; i<members.length; i++){
			IEssMember subMember = (IEssMember) members[i];
			membersList.put(subMember.getName(), subMember);
		}
		return membersList;
	}
	
	public static List<IEssMember> getSubMembers(IEssMember member) throws EssException{
		List<IEssMember> membersList = new ArrayList<IEssMember>();
		IEssBaseObject[] members = member.getChildMembers().getAll();
		for(int i = 0; i<members.length; i++){
			IEssMember subMember = (IEssMember) members[i];
			membersList.add(subMember);
		}
		return membersList;
	}

	public static void cubeComparator(EssbaseCube sourceCube, EssbaseCube targetCube) throws EssException, IOException {
		Map<String, IEssDimension> sourceDimensionMap = getDimensionsAsMap(sourceCube.getCubeOutline());
		Map<String, IEssDimension> targetDimensionMap = getDimensionsAsMap(targetCube.getCubeOutline());
		
		Writer.write("Source Cube Name : "+sourceCube.getCubeName());
		Writer.write("Target Cube Name : "+targetCube.getCubeName());
		Writer.write("Number of dimensions in source cube : "+sourceDimensionMap.size());
		Writer.write("Number of dimensions in target cube : "+targetDimensionMap.size(), System.lineSeparator()+System.lineSeparator());
		
		System.out.println("Source Cube Name : "+sourceCube.getCubeName());
		System.out.println("Target Cube Name : "+targetCube.getCubeName());
		System.out.println("Number of dimensions in source cube : " + sourceDimensionMap.size());
		System.out.println("Number of dimensions in target cube : " + targetDimensionMap.size());	
		System.out.println();
		
		//getDimensionDifferences(sourceDimensionMap, targetDimensionMap);
		
		for(String dName : sourceDimensionMap.keySet()){
			IEssDimension sDimension = sourceDimensionMap.get(dName);
			IEssDimension tDimension = targetDimensionMap.get(dName);
			if(sDimension != null && tDimension != null)
				getMembersDifferences(sDimension, tDimension);
		}
		System.out.println(mOrder);
		System.out.println(mProperties);
		Writer.write(mOrder.toString());
		Writer.write(mProperties.toString());
		Writer.write(allProperties.toString());
	}

	private static void getMembersDifferences(IEssDimension sDimension, IEssDimension tDimension) throws EssException, IOException {
		Map<String, IEssMember> sourceMembersMap = getMembersAsMap(sDimension);
		Map<String, IEssMember> targetMembersMap = getMembersAsMap(tDimension);
		
		for(String mName : sourceMembersMap.keySet()){
			if(targetMembersMap.containsKey(mName)){
				IEssMember sMember = sourceMembersMap.get(mName);
				System.out.println(Arrays.toString(sMember.getShareOption().getPossibleValues()));
				if(true){
					throw new EssException();
				}
				IEssMember tMember = targetMembersMap.get(mName);
				if(sMember.getMemberNumber() != tMember.getMemberNumber()){
					mOrder.append("Member Names are same but order is different : ("+sMember.getName()+") "+sMember.getMemberNumber()+" , "+tMember.getMemberNumber()+System.lineSeparator());
				}
				checkforMemberProperties(sMember, tMember);
				getSubMembersDifferences(sMember, tMember);
			}else{
				System.out.println("Member is missing in target Member  : "+mName);
				Writer.write("Member is missing in target Member  : "+mName);
			}
		}
		
		for(String mName : targetMembersMap.keySet()){
			if(!sourceMembersMap.containsKey(mName)){
				System.out.println("Member is missing in source Member : "+mName);
				Writer.write("Member is missing in source Member : "+mName);
			}
		}
	}

	private static void getSubMembersDifferences(IEssMember sMember, IEssMember tMember) throws EssException, IOException {
		Map<String, IEssMember> sourceMembersMap = getSubMembersAsMap(sMember);
		Map<String, IEssMember> targetMembersMap = getSubMembersAsMap(tMember);
		
		for(String mName : sourceMembersMap.keySet()){
			if(targetMembersMap.containsKey(mName)){
				IEssMember sSubMember = sourceMembersMap.get(mName);
				IEssMember tSubMember = targetMembersMap.get(mName);
				if(sSubMember.getMemberNumber() != sSubMember.getMemberNumber()){
					System.out.println("Member Names are same but order is different : ("+sSubMember.getName()+") "+sSubMember.getMemberNumber()+" , "+tSubMember.getMemberNumber()+System.lineSeparator());
					mOrder.append("Member Names are same but order is different : ("+sSubMember.getName()+") "+sSubMember.getMemberNumber()+" , "+tSubMember.getMemberNumber()+System.lineSeparator());
				}	
				checkforMemberProperties(sSubMember, tSubMember);
				getSubMembersDifferences(sSubMember, tSubMember);
			}else{
				System.out.println("Member is missing in target Member : "+mName);
				Writer.write("Member is missing in target Member : "+mName);
			}
		}
	}

	private static void checkforMemberProperties(IEssMember sMember, IEssMember tMember) throws EssException {
		Map<String, String> smProperty = readMemberProperties(sMember);
		Map<String, String> tmProperty = readMemberProperties(tMember);
		System.out.println(smProperty.toString());
		for(String pName : smProperty.keySet()){
			allProperties.append(sMember.getName() + "," + pName + "," + smProperty.get(pName) + "," + tmProperty.get(pName)+System.lineSeparator());
			if(tmProperty.containsKey(pName) && !smProperty.get(pName).equals(tmProperty.get(pName)))
				mProperties.append("Member Property is not matching : ("+sMember.getName()+" , "+pName+") -> "+smProperty.get(pName)+" , "+tmProperty.get(pName)+System.lineSeparator());
			else if(!tmProperty.containsKey(pName))
				mProperties.append("Property is missing in target Member : "+pName+System.lineSeparator());
		}
	}
	
	public static Map<String, String> readMemberProperties(List<IEssMember> membersList) throws EssException {
		Map<String, String> propertyMap = new HashMap<String, String>();
		for(IEssMember member : membersList){
			String[] propertyNames = member.getPropertyNames();
			for(String property : propertyNames){
				try{
					String propertyValue = member.getPropertyValueAny(property).toString();
					propertyMap.put(property, propertyValue);
				}catch(EssException e){
					/*Skip this because the property is not found*/
				}
			}
		}
		return propertyMap;
	}
	
	public static Map<String, String> readMemberProperties(IEssMember member) throws EssException {
		List<IEssMember> list = new ArrayList<IEssMember>();
		list.add(member);
		return readMemberProperties(list);
	}

	public static Map<String, String> readDimensionProperties(IEssDimension dimension) throws EssException {
		Map<String, String> propertyMap = new HashMap<String, String>();
		String[] propertyNames = dimension.getPropertyNames();
		for (String property : propertyNames)
			propertyMap.put(property, dimension.getPropertyValueAny(property).toString());
		return propertyMap;
	}
	
	private static void checkforDimensionProperties(IEssDimension sDimension, IEssDimension tDimension) throws EssException, IOException {
		Map<String, String> sdProperty = readDimensionProperties(sDimension);
		Map<String, String> tdProperty = readDimensionProperties(tDimension);
		for (String dName : sdProperty.keySet()) {
			if (tdProperty.containsKey(dName) && !sdProperty.get(dName).equals(tdProperty.get(dName))) {
				dProperties.append("Dimension Property is not matching : (" + sDimension.getName() + " , " + dName
						+ ") -> " + sdProperty.get(dName) + " , " + tdProperty.get(dName) + System.lineSeparator());
				dPropertiesList.add(						sDimension.getName() + "," + dName + "," + sdProperty.get(dName) + "," + tdProperty.get(dName));
			} else if (!tdProperty.containsKey(dName))
				dProperties.append("Property is missing in target Dime : " + dName + System.lineSeparator());
		}
	}

	private static void getDimensionDifferences(Map<String, IEssDimension> sDimensionMap,
			Map<String, IEssDimension> tDimensionMap) throws EssException, IOException {

		for(String dName : sDimensionMap.keySet()){
			if(tDimensionMap.containsKey(dName)){
				IEssDimension sDimension = sDimensionMap.get(dName);
				IEssDimension tDimension = tDimensionMap.get(dName);
				if(sDimension.getDimensionNumber() != tDimension.getDimensionNumber()){
					System.out.println("Dimension Names are same but order is different : ("+sDimension.getName()+") "+sDimension.getDimensionNumber()+tDimension.getDimensionNumber());
					dOrder.append("Dimension Names are same but order is different : ("+sDimension.getName()+") "+sDimension.getDimensionNumber()+tDimension.getDimensionNumber());
				}	
				checkforDimensionProperties(sDimension, tDimension);
			}else{
				System.out.println("Dimension is missing in target cube : "+dName);
				Writer.write("Dimension is missing in target cube : "+dName);
			}
		}
		
		for(String dName : tDimensionMap.keySet()){
			if(!sDimensionMap.containsKey(dName)){
				System.out.println("Dimension is missing in source cube : "+dName);
				Writer.write("Dimension is missing in source cube : "+dName);
			}
		}
		
		System.out.println(dOrder);
		System.out.println(dProperties);
		Writer.write(dOrder.toString());
		Writer.write(dProperties.toString());
	}

	public static List<String> getdPropertiesList() {
		return dPropertiesList;
	}

	public static List<String> getmPropertiesList() {
		return mPropertiesList;
	}
	
	public static boolean isQueryTrackingEnabled(EssbaseCube cube) throws EssException{
		boolean isExecuted = cube.getMaxlSession().execute(QUERY_DATABASE+cube.getApplicationName()+APP_DB_SEPARATOR+cube.getCubeName()+GET_CUBE_SIZE_INFO);
		if(isExecuted){
			IEssMaxlResultSet rs = cube.getMaxlSession().getResultSet();
			while(rs.next()){
				return rs.getBoolean(QUERY_TRACKING_ENABLED);
			}
		}
		return false;
	}

	public static boolean enableQueryTracking(EssbaseCube cube) throws EssException {
		System.out.println("Enabling QT");
		return cube.getMaxlSession().execute(ALTER_DATABASE+cube.getApplicationName()+APP_DB_SEPARATOR+cube.getCubeName()+ENABLE_QUERY_TRACKING);
	}

	public static boolean isAppExists(EssbaseCube cube) throws EssException {
		boolean isExecuted = cube.getMaxlSession().execute("display application all;");
		if(isExecuted){
			IEssMaxlResultSet rs = cube.getMaxlSession().getResultSet();
			while(rs.next()){
				String appName = rs.getString("Application");
				if(appName.equalsIgnoreCase(cube.getApplicationName()))
					return true;
			}
		}
		return false;
	}

	public static void createApplication(EssbaseCube cube, String comment) throws EssException {
		boolean isExecuted = cube.getMaxlSession().execute("create application "+cube.getApplicationName()+" using aggregate_storage comment '"+comment+"';");
		if(!isExecuted)
			throw new EssException("Could not create app.");
	}

	public static void createDatabase(EssbaseCube cube, String comment) throws EssException {
		boolean isExecuted = cube.getMaxlSession().execute("create or replace database "+cube.getApplicationName()+"."+cube.getCubeName()+" comment '"+comment+"';");
		if(!isExecuted)
			throw new EssException("Could not create DB.");
	}

	public static void createOutline(EssbaseCube cube) throws EssException {
		System.out.println("Writing to Outline");
		IEssCubeOutline outline = cube.openCubeOutline(cube.getCube());
        outline.getCube().setActive();
        IEssDimension year = outline.createDimension("Year");
        year.setStorageType(IEssDimension.EEssDimensionStorageType.DENSE);
        year.setCategory(IEssDimension.EEssDimensionCategory.TIME);
        year.updatePropertyValues();
        outline.getCube().setActive();
        outline.save(IEssCube.EEssRestructureOption.sm_fromInt(IEssCube.EEssRestructureOption.KEEP_ALL_DATA_INT_VALUE));
	}
	
	public static void loadCache(EssbaseCube cube, Outline outLine) throws EssException{
		timeStamp(true);
		
		List<IEssDimension> dimensions = getDimensionsAsList(cube.getCubeOutline());
		for(IEssDimension dimension : dimensions){
			Dimension myDimension = new Dimension();
			myDimension.setDimension(dimension);
			outLine.add(myDimension);
			System.out.println(dimension.getName());
			loadCacheMembers(cube, myDimension);
		}
		timeStamp(true);
		System.out.println(outLine.toString());
	}
	
	private static void loadCacheMembers(EssbaseCube cube, Dimension myDimension) throws EssException {
		IEssMemberSelection memSel = null;
		memSel = cube.getCube().openMemberSelection("Member Selection");
		memSel.executeQuery(myDimension.getDimension().getName(), IEssMemberSelection.QUERY_TYPE_CHILDREN, IEssMemberSelection.QUERY_OPTION_FORCEIGNORECASE, null, null, null);
		if(memSel != null){
			final IEssBaseObject[] members = memSel.getMembers().getAll();
			for(int i=0; i<members.length; i++){
				IEssMember member = (IEssMember) members[i];
				Member myMember = new Member();
				myMember.setMember(member);
				myDimension.addMember(myMember);
				System.out.println(member.getName());
				loadCacheSubMembers(cube, myMember);
			}
		}
	}

	private static void loadCacheSubMembers(EssbaseCube cube, Member myMember) throws EssException {
		IEssMemberSelection memSel = null;
		memSel = cube.getCube().openMemberSelection("Member Selection");
		memSel.executeQuery(myMember.getMember().getName(), IEssMemberSelection.QUERY_TYPE_CHILDREN, IEssMemberSelection.QUERY_OPTION_FORCEIGNORECASE, null, null, null);
		if(memSel != null){
			IEssIterator memItr = memSel.getMembers();
			if(memItr != null){
				final IEssBaseObject[] members = memItr.getAll();
				for(int i=0; i<members.length; i++){
					IEssMember member = (IEssMember) members[i];
					Member mySubMember = new Member();
					mySubMember.setMember(member);
					myMember.addSubMember(mySubMember);
					System.out.println(member.getName());
					loadCacheSubMembers(cube, mySubMember);
				}
			}
		}
	}

	public static void timeStamp(boolean isNewLine){
		SimpleDateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(Long.parseLong(""+System.currentTimeMillis()));
		if(isNewLine)
			System.out.println(simpleDateFormat.format(calendar.getTime()));
		else
			System.out.print(simpleDateFormat.format(calendar.getTime())+"     ");
	}
	
	public static StringBuffer getAllProperties() {
		return allProperties;
	}

}