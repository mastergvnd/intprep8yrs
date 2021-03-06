package com.testing;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.*;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.Attr;
import org.w3c.dom.DOMException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.Attributes;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;
public class StructureArifactWPTGStringValidator extends DefaultHandler{
	   private PrintWriter writer = null;
	    private File hspFuseStructureDef;
	    private File hspFuseStructureUsage;
	    private File hspStructureArtifactString_en;
	    private File outputFile;
	    private Locator locator = null;
	    private boolean shouldParse = false;
	    private String category;
	    private Document resourceDoc = null;
	    private Document defDoc = null ;
	    private Document artifactStringDoc = null ;
	    Element rootElement = null;
	    XPath xPath ;
	    public static final String ROOT_ELEMENT = "dataset";
	    public static final String CLUSTER = "FUSE_CLUSTER";
	    public static final String CARD = "FUSE_CARD";
	    public static final String TAB = "FUSE_TAB";

	    public static final String ARTIFACT_TYPE = "ARTIFACT_TYPE";
	    public static final String ARTIFACT_ID = "ARTIFACT_ID";
	    public static final String SECONDARY_ID = "SECONDARY_ID";
	    public static final String PROP_TYPE = "PROP_TYPE";
	    public static final String LABEL = "LABEL";
	    public static final String NAME = "name";
	    public static final String THE_STRING = "THE_STRING";
	    public static final String ROW = "row";
	    public static final String COL = "col";
	    //public Set<String> usageXMLIdsSet=new HashSet<String>(); 
	    public Map<String,String> usageXMLIdsMap=new HashMap<String,String>();
	
	    public Map<String,String> artifactStringIdsMap=new HashMap<String,String>();
	    
	    
	    
  public static void main(String[] argrs)throws Exception {
		System.out.println(System.getProperties());
		StructureArifactWPTGStringValidator resourceXmlGenerator = new StructureArifactWPTGStringValidator();
		resourceXmlGenerator.processXML();
		
		resourceXmlGenerator.compareArtifactStringIdsMapToUsageXMLIdsMap();
		
	}
	
//compare the artifactStringIdsMap and usageXMLIdsMap
  public void compareArtifactStringIdsMapToUsageXMLIdsMap(){
	  final Map<String, String> findMap = new HashMap<String, String>();
	 
	  for (final String key : artifactStringIdsMap.keySet()) {
	      if (usageXMLIdsMap.containsKey(key)) {
	    	  findMap.put(artifactStringIdsMap.get(key), usageXMLIdsMap.get(key));
	      }
	  }
	  
	  //ietrate hm3
	  writer.println("============== Usage XML" +" : " + " Artifact String XML=================");
	  for (final String key : findMap.keySet()) {
		  if(!key.equals( findMap.get(key))){
			  
		  writer.println(findMap.get(key) +" : " +key );
		  }
	  }
	  /////////////////////////////////////////////////////////
	  final Map<String, String> missingFromArtifactMap = new HashMap<String, String>();
	  for (final String key : usageXMLIdsMap.keySet()) {
	      if (!artifactStringIdsMap.containsKey(key)) {
	    	  missingFromArtifactMap.put(key,usageXMLIdsMap.get(key));
	      }
	  }
	  
	  //ietrate hm3
	  writer.println("============== Missing from  Artifact String XML=================");
	  for (final String key : missingFromArtifactMap.keySet()) {
		  //writer.println(key  +" : " +missingFromArtifactMap.get(key) );
		  String s=null;
		  if(key.startsWith("EPM_CL")){
			s="<row><col name=\"ARTIFACT_TYPE\">FUSE_CLUSTER</col><col name=\"ARTIFACT_ID\">Default</col><col name=\"SECONDARY_ID\">"+key+"</col><col name=\"PROP_TYPE\">LABEL</col><col name=\"THE_STRING\">"+missingFromArtifactMap.get(key)+"</col></row>" ; 
		  }
		  if(key.startsWith("EPM_CA")){
				s="<row><col name=\"ARTIFACT_TYPE\">FUSE_CARD</col><col name=\"ARTIFACT_ID\">Default</col><col name=\"SECONDARY_ID\">"+key+"</col><col name=\"PROP_TYPE\">LABEL</col><col name=\"THE_STRING\">"+missingFromArtifactMap.get(key)+"</col></row>" ; 
			  }
		  if(key.startsWith("EPM_TA")){
				s="<row><col name=\"ARTIFACT_TYPE\">FUSE_TAB</col><col name=\"ARTIFACT_ID\">Default</col><col name=\"SECONDARY_ID\">"+key+"</col><col name=\"PROP_TYPE\">LABEL</col><col name=\"THE_STRING\">"+missingFromArtifactMap.get(key)+"</col></row>" ; 
			  }
		 
		  writer.print(s);
	  }
	  
	  /////////////////////////////////////////////
	  final Map<String, String> missingFromUsageMap = new HashMap<String, String>();
	  for (final String key : artifactStringIdsMap.keySet()) {
	      if (!usageXMLIdsMap.containsKey(key)) {
	    	  missingFromUsageMap.put(key,artifactStringIdsMap.get(key));
	      }
	  }
	  
	  //ietrate hm3
	  writer.println("============== Missing from  Usage XML=================");
	  for (final String key : missingFromUsageMap.keySet()) {
		  writer.println(key  +" : " +missingFromUsageMap.get(key));
		 
	  }
	  ///////////////////////////
	  writer.flush();
      writer.close();
  }
	 public void processXML() throws Exception {

			// ///////////////////////////////
			category = "OPRPLAN";
			hspFuseStructureDef = new File("C:\\Users\\govgupta.ORADEV\\Downloads\\StructureUtilsUpdated\\HspFuseStructureDef.xml");
			hspFuseStructureUsage = new File("C:\\Users\\govgupta.ORADEV\\Downloads\\StructureUtilsUpdated\\OprplanFuseStructureUsage.xml");
			outputFile = new File("C:\\Users\\govgupta.ORADEV\\Downloads\\StructureUtilsUpdated\\resource.xml");
			hspStructureArtifactString_en=new File("C:\\Users\\govgupta.ORADEV\\Downloads\\StructureUtilsUpdated\\Oprplan_Structures_ArtifactString_en.dlf");
			File result = new File("C:\\Users\\govgupta.ORADEV\\Downloads\\StructureUtilsUpdated\\usageXMlArtifactResult.xml");
			  try
		        {
		            writer = new PrintWriter(result);
		        }
		        catch(FileNotFoundException e1)
		        {
		            e1.printStackTrace();
		        }
			//////////////////////////////////////////
			
			xPath = XPathFactory.newInstance().newXPath();
			DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
			DocumentBuilder db = dbf.newDocumentBuilder();
			defDoc = db.parse(hspFuseStructureDef);
			artifactStringDoc=db.parse(hspStructureArtifactString_en);
			resourceDoc = db.newDocument();
			rootElement = resourceDoc.createElement(ROOT_ELEMENT);
			resourceDoc.appendChild(rootElement);
			// newXmlDoc = db.
			SAXParserFactory spf = SAXParserFactory.newInstance();
			SAXParser parser;
			try {
			    parser = spf.newSAXParser();
			    writer.println("===============Processing Structure Usage XML=======================");
			    parser.parse(hspFuseStructureUsage, this);
			    System.out.println("done processing");

			    TransformerFactory transformerFactory = TransformerFactory.newInstance();
			    Transformer transformer = transformerFactory.newTransformer();
			    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
			    DOMSource source = new DOMSource(resourceDoc);
			     StreamResult result1 = new StreamResult(outputFile);

			    // Output to console for testing
			  //  StreamResult result = new StreamResult(System.out);

			    transformer.transform(source, result1);

			    System.out.println("File saved!");

			} catch (Exception e) {

			    System.out.println(e.getMessage());
			}
			
			try{
				 parser = spf.newSAXParser();
				 parser.parse(hspStructureArtifactString_en,this);
				   // parser.parse(hspFuseStructureUsage, this);
				    System.out.println("done processing");

				    TransformerFactory transformerFactory = TransformerFactory.newInstance();
				    Transformer transformer = transformerFactory.newTransformer();
				    transformer.setOutputProperty(OutputKeys.INDENT, "yes");
				    DOMSource source = new DOMSource(resourceDoc);
				    // StreamResult result = new StreamResult(outputFile);

				    // Output to console for testing
				  //  StreamResult result = new StreamResult(System.out);

				   // transformer.transform(source, result);

				   // System.out.println("File saved!");
				
				
			}catch(Exception e){
				 System.out.println(e.getMessage());
			}
			
		    }

		    @Override
		    public void setDocumentLocator(Locator locator) {
			this.locator = locator;
		    }

		    @Override
		    public void characters(char[] ch, int start, int length) throws SAXException {
			// TODO Auto-generated method stub

			super.characters(ch, start, length);
		    }

		    @Override
		    public void startElement(String uri, String localName, String qName, Attributes attributes) throws SAXException {

			if ("category".equals(qName)) {
			    String categoryId = attributes.getValue("id");
			    System.out.println(categoryId);
			    shouldParse = true;
			}
			
			//Parse ArtifactString
			if("dataset".equals(qName)){
				 writer.println("===============Processing Structure_Artifact_String=======================");
				try {
					NodeList nodes  =   (NodeList)xPath.compile("/table/dataset/row").evaluate(artifactStringDoc, XPathConstants.NODESET);
					
					 for (int i = 0; i < nodes.getLength(); i++){
						 NodeList rootNodeChilds=nodes.item(i).getChildNodes();
						 String id=null;
						 String label=null;
						 for (int j=0;j<rootNodeChilds.getLength();j++) {
							 Node cl=rootNodeChilds.item(j);
							 if(cl.getAttributes()!=null){
								 if(cl.getAttributes().getNamedItem("name").getNodeValue().equals("SECONDARY_ID")){
									 id=cl.getTextContent();
									
								 }
								 if(cl.getAttributes().getNamedItem("name").getNodeValue().equals("THE_STRING")){
									 label=cl.getTextContent();
									
								 }
							 }
							
						} 
						 if(id!=null){
							 if(!artifactStringIdsMap.containsKey(id)){
								 artifactStringIdsMap.put(id, label);
								}else{
									 writer.println("Id: "+ id +"  for card found duplicate");	
									 
								}
						 }
						
					 }
			             
				} catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}

			if (shouldParse) {
			    // cluster
			    if ("cardCluster".equals(qName)) {
				//System.out.println("processing cluster");
				String id = attributes.getValue("id");
//				if(!usageXMLIdsSet.contains(id)){
//					usageXMLIdsSet.add(id);
//				}else{
//					 writer.println("Id: "+ id +"  for cluster found duplicate");	
//				}
				Element row = resourceDoc.createElement(ROW);
				rootElement.appendChild(row);
				Element col1 = resourceDoc.createElement(COL);
				Attr attr1 = resourceDoc.createAttribute(NAME);
				attr1.setValue(ARTIFACT_TYPE);
				col1.setAttributeNode(attr1);
				col1.appendChild(resourceDoc.createTextNode(CLUSTER));
				row.appendChild(col1);
				
				Element col5 = resourceDoc.createElement(COL);
				Attr attr5 = resourceDoc.createAttribute(NAME);
				attr5.setValue(ARTIFACT_ID);
				col5.setAttributeNode(attr5);
				col5.appendChild(resourceDoc.createTextNode("Default"));
				row.appendChild(col5);
				
				Element col2 = resourceDoc.createElement(COL);
				Attr attr2 = resourceDoc.createAttribute(NAME);
				attr2.setValue(SECONDARY_ID);
				col2.setAttributeNode(attr2);
				col2.appendChild(resourceDoc.createTextNode(id));
				row.appendChild(col2);
				
				Element col3 = resourceDoc.createElement(COL);
				Attr attr3 = resourceDoc.createAttribute(NAME);
				attr3.setValue(PROP_TYPE);
				col3.setAttributeNode(attr3);
				col3.appendChild(resourceDoc.createTextNode(LABEL));
				row.appendChild(col3);
				
				String label = attributes.getValue("label");
				if(label == null)
				{
				    try {
					Node node1 =   (Node)xPath.compile("//cardClusterDef[@id='" +attributes.getValue("refObjectDefId")  +   "']").evaluate(defDoc, XPathConstants.NODE);
					label =  node1.getAttributes().getNamedItem("label").getTextContent();
				    } catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    } catch (DOMException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    }
				}
				
				if(!usageXMLIdsMap.containsKey(id)){
					usageXMLIdsMap.put(id, label);
				}else{
					 writer.println("Id: "+ id +"  for cluster found duplicate");	
				}
				
				 Element col4 = resourceDoc.createElement(COL);
				 Attr attr4 = resourceDoc.createAttribute(NAME);
				 attr4.setValue(THE_STRING);
				 col4.setAttributeNode(attr4);
				 col4.appendChild(resourceDoc.createTextNode(label));
				 row.appendChild(col4);

			    }

			    // card
			    if ("card".equals(qName)) {
				//System.out.println("processing cards");
				String id = attributes.getValue("id");
//				if(!usageXMLIdsSet.contains(id)){
//					usageXMLIdsSet.add(id);
//				}else{
//					 writer.println("Id: "+ id +"  for card found duplicate");	
//				}
				Element row = resourceDoc.createElement(ROW);
				rootElement.appendChild(row);
				Element col1 = resourceDoc.createElement(COL);
				Attr attr1 = resourceDoc.createAttribute(NAME);
				attr1.setValue(ARTIFACT_TYPE);
				col1.setAttributeNode(attr1);
				col1.appendChild(resourceDoc.createTextNode(CARD));
				row.appendChild(col1);
				
				Element col5 = resourceDoc.createElement(COL);
				Attr attr5 = resourceDoc.createAttribute(NAME);
				attr5.setValue(ARTIFACT_ID);
				col5.setAttributeNode(attr5);
				col5.appendChild(resourceDoc.createTextNode("Default"));
				row.appendChild(col5);
				
				Element col2 = resourceDoc.createElement(COL);
				Attr attr2 = resourceDoc.createAttribute(NAME);
				attr2.setValue(SECONDARY_ID);
				col2.setAttributeNode(attr2);
				col2.appendChild(resourceDoc.createTextNode(id));
				row.appendChild(col2);
				Element col3 = resourceDoc.createElement(COL);
				Attr attr3 = resourceDoc.createAttribute(NAME);
				attr3.setValue(PROP_TYPE);
				col3.setAttributeNode(attr3);
				col3.appendChild(resourceDoc.createTextNode(LABEL));
				row.appendChild(col3);
				String label = attributes.getValue("label");
				if(label == null)
				{
				    try {
					Node node1 =   (Node)xPath.compile("//cardDef[@id='" +attributes.getValue("refObjectDefId")  +   "']").evaluate(defDoc, XPathConstants.NODE);
					label =  node1.getAttributes().getNamedItem("label").getTextContent();
				    } catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    } catch (DOMException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    }
				}
				
				if(!usageXMLIdsMap.containsKey(id)){
					usageXMLIdsMap.put(id, label);
				}else{
					 writer.println("Id: "+ id +"  for card found duplicate");	
				}
				 Element col4 = resourceDoc.createElement(COL);
				 Attr attr4 = resourceDoc.createAttribute(NAME);
				 attr4.setValue(THE_STRING);
				 col4.setAttributeNode(attr4);
				 col4.appendChild(resourceDoc.createTextNode(label));
				 row.appendChild(col4);

			    }

			    // tab
			    if ("tab".equals(qName)) {
				//System.out.println("processing tabs");
				String id = attributes.getValue("id");
//				if(!usageXMLIdsSet.contains(id)){
//					usageXMLIdsSet.add(id);
//				}else{
//					 writer.println("Id: "+ id +"  for tab found duplicate");	
//				}
				
				Element row = resourceDoc.createElement(ROW);
				rootElement.appendChild(row);
				Element col1 = resourceDoc.createElement(COL);
				Attr attr1 = resourceDoc.createAttribute(NAME);
				attr1.setValue(ARTIFACT_TYPE);
				col1.setAttributeNode(attr1);
				col1.appendChild(resourceDoc.createTextNode(TAB));
				row.appendChild(col1);
				
				Element col5 = resourceDoc.createElement(COL);
				Attr attr5 = resourceDoc.createAttribute(NAME);
				attr5.setValue(ARTIFACT_ID);
				col5.setAttributeNode(attr5);
				col5.appendChild(resourceDoc.createTextNode("Default"));
				row.appendChild(col5);
				
				Element col2 = resourceDoc.createElement(COL);
				Attr attr2 = resourceDoc.createAttribute(NAME);
				attr2.setValue(SECONDARY_ID);
				col2.setAttributeNode(attr2);
				col2.appendChild(resourceDoc.createTextNode(id));
				row.appendChild(col2);
				Element col3 = resourceDoc.createElement(COL);
				Attr attr3 = resourceDoc.createAttribute(NAME);
				attr3.setValue(PROP_TYPE);
				col3.setAttributeNode(attr3);
				col3.appendChild(resourceDoc.createTextNode(LABEL));
				row.appendChild(col3);
				
				String label = attributes.getValue("label");
				if(label == null)
				{
				    try {
					Node node1 =   (Node)xPath.compile("//tabDef[@id='" +attributes.getValue("refObjectDefId")  +   "']").evaluate(defDoc, XPathConstants.NODE);
					label =  node1.getAttributes().getNamedItem("label").getTextContent();
				    } catch (XPathExpressionException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    } catch (DOMException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				    }
				}
				
				if(!usageXMLIdsMap.containsKey(id)){
					usageXMLIdsMap.put(id, label);
				}else{
					 writer.println("Id: "+ id +"  for tab found duplicate");	
				}
				 Element col4 = resourceDoc.createElement(COL);
				 Attr attr4 = resourceDoc.createAttribute(NAME);
				 attr4.setValue(THE_STRING);
				 col4.setAttributeNode(attr4);
				 col4.appendChild(resourceDoc.createTextNode(label));
				 row.appendChild(col4);

			    }

			}
		    }

		    @Override
		    public void endElement(String uri, String localName, String qName) throws SAXException {
			 if ("category".equals(qName)) {
			 shouldParse = false ;
			 }
			super.endElement(uri, localName, qName);

		    }
		    
}