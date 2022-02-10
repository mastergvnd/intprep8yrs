package com.testing;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

public class CreateTestCases extends DefaultHandler{

    private static final String HEALTH_CHECK_STATISTICS = "healthCheckStatistics";
    private static final String CRITERIA = "criteria";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String APPLICATION_TYPE = "applicationType";
    private static final String TENANT_NAME = "tenantName";
    private static final String SERVICE_NAME = "serviceName";
    
    private StringBuffer testCases = new StringBuffer(""); 
    private static final String filename= "C:\\Users\\govgupta.ORADEV\\Desktop\\HealthCheckCriteriaFactory\\Governor_Limit\\HspHealthCheckCriteriaFactoryTest.java";
    private static FileWriter fw = null;
    		
    public static void main(String[] args) {
    	
    	SAXParserFactory saxParserFactory = SAXParserFactory.newInstance();
    	try {
			fw = new FileWriter(filename,true);
		} catch (IOException e1) {
			e1.printStackTrace();
		}
        
    	try {
            SAXParser saxParser = saxParserFactory.newSAXParser();
            CreateTestCases handler = new CreateTestCases();
            String apps[] = {"PBCS", "EPBCS", "TRCS", "FCCS", "SWP"};
            for(String app : apps){
            	saxParser.parse(new File("C:\\Users\\govgupta.ORADEV\\Desktop\\HealthCheckCriteriaFactory\\Governor_Limit\\HealthCheckStatistics_"+app+".xml"), handler);
            	saxParser.parse(new File("C:\\Users\\govgupta.ORADEV\\Desktop\\HealthCheckCriteriaFactory\\Governor_Limit\\HealthCheckStatistics_"+app+"_Customer.xml"), handler);
            }
        } catch (ParserConfigurationException | SAXException | IOException e) {
            e.printStackTrace();
        }
    	try {
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
    }
    
	@Override
    public void startElement(String uri, String localName, String qName, Attributes attributes)
            throws SAXException {
        handleStartElement(qName, attributes);
        assignAttributes(qName, attributes);
	}
	
	@Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
		handleEndElement(qName);
	}

    protected final void handleStartElement(String qName, Attributes attributes) {
        if (HEALTH_CHECK_STATISTICS.equals(qName)) {
        	testCases = new StringBuffer(""); 
            final String applicationType = attributes.getValue(APPLICATION_TYPE);
            String tenantName = attributes.getValue(TENANT_NAME);
            String serviceName = attributes.getValue(SERVICE_NAME);
            
            tenantName = tenantName == null ? "" : tenantName;
            serviceName = serviceName == null ? "" : serviceName;
            
            testCases.append("@Test").append(System.lineSeparator())
            .append("public void testGovernorLimitsFor"+applicationType+tenantName+serviceName.replaceAll("-", "")+"() throws Exception {")
            .append(System.lineSeparator());
            //if((tenantName != null || !tenantName.equals("")) && (serviceName != null || !serviceName.equals("")))
            testCases.append("\tnew MockUp<RegistryManager>() {").append(System.lineSeparator())
            .append("\t\t@Mock").append(System.lineSeparator())
            .append("\t\tpublic String getTenantName() {").append(System.lineSeparator())
            .append("\t\t\treturn \""+tenantName+"\";").append(System.lineSeparator())
            .append("\t\t}").append(System.lineSeparator()).append(System.lineSeparator())
            .append("\t\t@Mock").append(System.lineSeparator())
            .append("\t\tpublic String getServiceName() {").append(System.lineSeparator())
            .append("\t\t\treturn \""+serviceName+"\";").append(System.lineSeparator())
            .append("\t\t}").append(System.lineSeparator())
            .append("\t};").append(System.lineSeparator());
            testCases.append("\tHspHealthCheckCriteriaFactory factory = HspHealthCheckCriteriaFactory.getInstance(ApplicationType.getApplicationType(\""+applicationType+"\"));")
            .append(System.lineSeparator());
        }
    }
                     
    protected final void assignAttributes(String qName, Attributes atts) {
    	if(CRITERIA.equals(qName)){
            final StringBuilder key = new StringBuilder(atts.getValue(NAME));
            final String value = atts.getValue(VALUE);
            testCases.append("\tAssert.assertEquals(\""+key+" does not match : \", "+"factory.get"+key+"(), "+value+");").append(System.lineSeparator());
        }                         
    }
    
    protected final void handleEndElement(String qName) {
    	if (HEALTH_CHECK_STATISTICS.equals(qName)) {
    		testCases.append("}").append(System.lineSeparator());

    		System.out.println(testCases);
    		
		    try {
				fw.write(testCases.toString());
			} catch (IOException e) {
				e.printStackTrace();
			}
    		
    	}                                                                                                                                                                                                                                                                                                                                                                                                                                                                            
    }
}
