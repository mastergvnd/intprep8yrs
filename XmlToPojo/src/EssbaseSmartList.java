import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "enumeration")
@XmlAccessorType(XmlAccessType.FIELD)
public class EssbaseSmartList {
	
    @XmlAttribute(name = "name")
    private String name;
    
    public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	@XmlElement(name = "enumEntry")
    private List<EssbaseSmartListEntry> smartListEntry;

	public List<EssbaseSmartListEntry> getSmartListEntry() {
		return smartListEntry;
	}

	public void setSmartListEntry(List<EssbaseSmartListEntry> smartListEntry) {
		this.smartListEntry = smartListEntry;
	}
}
