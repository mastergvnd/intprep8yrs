import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.itextpdf.text.Chunk;

public class Test {
	
	private static PDFFileWriter pdfWriter = null;

	public static void main(String[] args) {
		System.setProperty("webdriver.gecko.driver","D:\\chromedriver\\chromedriver.exe");
	    System.setProperty("http.agent", "Chrome");
		WebDriver driver = new FirefoxDriver();
		driver.manage().window().maximize();
		try{
			pdfWriter = new PDFFileWriter("testSummary2");

	        driver.get("https://www.educative.io/courses/java-multithreading-for-senior-engineering-interviews/m2G48X18NDO");
	        Thread.sleep(3000);
	        System.out.println("URL is opened");
	        
	        WebElement completeTextEle = driver.findElement(By.xpath("//*[@id=\"handleArticleScroll\"]/div/div/div/div"));
	        WebElement summaryEle = completeTextEle.findElement(By.className("summary"));
	        System.out.println("Found summary element");
	        pdfWriter.writeSummary(summaryEle);
	        
	        List<WebElement> allDivs = completeTextEle.findElements(By.className("styles__ViewerComponentViewStyled-sc-1xosrua-0"));
	        System.out.println("size of alldics : " + allDivs.size());
	        
	        boolean elementPrinted = false;
	        
	        for(int i=0; i<allDivs.size(); i++) {
	        	WebElement ele = allDivs.get(i);
	    		List<WebElement> allDivs2 = ele.findElements(By.tagName("div"));
	    		if(elementPrinted)
	    			break;
	    		for(WebElement ele2 : allDivs2) {
	    			String classAttValue = ele2.getAttribute("className");
	    			if(classAttValue != null && classAttValue != "") {
	    				if("card".equals(classAttValue)) {
	    					
	    					if(ele2.getText() != null && ele.getText().contains("Counter Program"))
	    						continue;
	    					
	    					WebElement headingElement = ele.findElement(By.tagName("h4"));
	    					
	    					System.out.println("Summary is printed : " + headingElement.getText());
	    					
	    					WebElement answer = ele.findElement(By.className("ans"));
	    					
	    					String content = answer.getText();
	    					String innerHTML = answer.getAttribute("innerHTML");
	    					
	    					System.out.println("Number : " + i + "   " + answer.getAttribute("innerHTML"));
	    					
	    					
	    					
	    				}
	    			}
	    		}
	        }
	        
	        
	        pdfWriter.closeWriter();
		} catch(Throwable e) {
			System.out.println("Exception found");
			e.printStackTrace();
			driver.quit();
		}
		finally{
			System.out.println("Inside finally");
			driver.quit();
		}
	}

}
