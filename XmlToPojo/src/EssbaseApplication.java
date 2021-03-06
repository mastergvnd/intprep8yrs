import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement(name = "application")
@XmlAccessorType(XmlAccessType.FIELD)
public class EssbaseApplication {

    @XmlAttribute(name = "name")
    private String name;

    @XmlAttribute(name = "dimCount")
    private int dimCount;


    @XmlElement(name = "smartLists")
    private EssbaseSmartLists smartLists;
    
    public EssbaseSmartLists getSmartLists() {
		return smartLists;
	}

	public void setSmartList(EssbaseSmartLists smartList) {
		this.smartLists = smartList;
	}

	public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDimensionCount() {
        return dimCount;
    }

    public void setDimensionCount(int dimCount) {
        this.dimCount = dimCount;
    }

    public EssbaseApplication() {
        super();
    }

}
