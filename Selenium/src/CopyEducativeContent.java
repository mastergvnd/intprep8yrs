import java.io.IOException;
import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Image;

public class CopyEducativeContent {
	
	private PDFFileWriter pdfWriter = null;

	public static void main(String[] args) {
		CopyEducativeContent scraper = new CopyEducativeContent();
		scraper.scrapeData();
        
	}
	
	private void scrapeData() {
		System.setProperty("webdriver.gecko.driver","D:\\chromedriver\\chromedriver.exe");
	    System.setProperty("http.agent", "Chrome");
		WebDriver driver = new FirefoxDriver();
		driver.manage().window().maximize();
		try{
			pdfWriter = new PDFFileWriter("testSummaryOOPs");
	//        String url = "https://www.educative.io/";
	//        driver.get(url);
	//        Thread.sleep(10000);         
	//        driver.findElement(By.xpath("//*[@id=\"app\"]/div[1]/div[3]/nav/div[3]/button[2]")).click();
	//        driver.findElement(By.id("loginform-email")).sendKeys("mastergvnd.378@gmail.com");
	//        driver.findElement(By.id("loginform-password")).sendKeys("Govind@123");
	//        driver.findElement(By.id("modal-login")).click();
	//        //driver.get("https://www.educative.io/track/ace-java-coding-interview");
	//        driver.get("https://www.educative.io/courses/java-multithreading-for-senior-engineering-interviews");
	//        driver.findElement(By.xpath("//*[@id=\"collection-tabs-pane-1\"]/div[1]/menu/div[2]/a[1]")).click(); 
	        //driver.get("https://www.educative.io/courses/java-multithreading-for-senior-engineering-interviews/m2G48X18NDO");
			driver.get("https://www.educative.io/courses/grokking-the-object-oriented-design-interview/gxM3gRxmr8Z");
	        Thread.sleep(3000);
	        System.out.println("URL is opened");
	        
	        WebElement completeTextEle = driver.findElement(By.xpath("//*[@id=\"handleArticleScroll\"]/div/div/div/div"));
	        WebElement summaryEle = completeTextEle.findElement(By.className("summary"));
	        System.out.println("Found summary element");
	        pdfWriter.writeSummary(summaryEle);
	        
	        List<WebElement> allDivs = completeTextEle.findElements(By.className("styles__ViewerComponentViewStyled-sc-1xosrua-0"));
	        
	        for(WebElement div : allDivs) {
	        	writeParagraphsWithHeading(div);
	        }
	        //pdfWriter.closeWriter();
		} catch(Exception e) {
			//pdfWriter.closeWriter();
			e.printStackTrace();
		}
		finally{
			pdfWriter.closeWriter();
			driver.quit();
		}
	}

	private void writeParagraphsWithHeading(WebElement div) throws DocumentException, IOException, InterruptedException {
		List<WebElement> allDivs = div.findElements(By.tagName("div"));
		
		for(WebElement ele : allDivs) {
			String classAttValue = ele.getAttribute("className");
			if(classAttValue != null && classAttValue != "") {
				if("card".equals(classAttValue)) {
					pdfWriter.writeParagraphsWithHeading(ele);
				} else if("code-container".equals(classAttValue)) {
					pdfWriter.writeCodeSnippet(ele);
				} else if(classAttValue.contains("styles__ImageWrap-sc-1h2l6dw-0")) {
					System.out.println("Image found");
					ele = ele.findElement(By.tagName("img"));
					Image image = ImageDownloaderAndScaler.downloadAndScaleImage(ele.getAttribute("src"));
					pdfWriter.writeImage(image);
				}
			}
		}
	}

}
