
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "smartLists")
public class EssbaseSmartLists {
    @XmlElement(name = "enumeration")
    private List<EssbaseSmartList> smartList;

	public List<EssbaseSmartList> getSmartLists() {
		return smartList;
	}

	public void setSmartList(List<EssbaseSmartList> smartLists) {
		this.smartList = smartLists;
	}
}
