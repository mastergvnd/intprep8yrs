import java.io.FileNotFoundException;
import java.io.FileReader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

public class MainClass {

	public static void main(String[] args) throws JAXBException, FileNotFoundException {
		JAXBContext context = JAXBContext.newInstance(EssbaseSmartList.class);
		Unmarshaller um = context.createUnmarshaller();
		EssbaseApplication app = (EssbaseApplication) um.unmarshal(new FileReader(
				"C:\\Users\\govgupta.ORADEV\\Desktop\\OP\\Managed Outlines\\CubeXML\\EssbaseJAPI\\SmartListDateMeasures.xml"));
		System.out.println("Number of smart lists : " + app.getSmartLists().getSmartLists().size());
	}

}
