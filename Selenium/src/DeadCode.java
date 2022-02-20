import java.awt.image.BufferedImage;
import java.io.File;
import java.net.URL;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.OutputType;
import org.openqa.selenium.TakesScreenshot;

import com.itextpdf.text.Image;

public class DeadCode {

	
//	((JavascriptExecutor) driver).executeScript("window.open('" + ele.getAttribute("src") +"');");
//	ArrayList<String> tabs2 = new ArrayList<String> (driver.getWindowHandles());
//    driver.switchTo().window(tabs2.get(1));
//	File screenshot = ((TakesScreenshot)driver).getScreenshotAs(OutputType.FILE);
//	driver.close();
//    driver.switchTo().window(tabs2.get(0));
//	BufferedImage  fullImg = ImageIO.read(screenshot);
//
//	ImageIO.write(fullImg, "png", screenshot);
//
//	File screenshotLocation = new File("D:\\robot\\GoogleLogo_screenshot.png");
//	FileUtils.copyFile(screenshot, screenshotLocation);
//	Image image1 = Image.getInstance("D:\\robot\\GoogleLogo_screenshot.png");
//	
//	image1.scaleAbsolute(500, 500);
//    //PDFFileCreator.writeImage(image1);
//	
//    File f= new File("D:\\robot\\test2.jpg");
//    URL myUrl = new URL(ele.getAttribute("src"));
//	FileUtils.copyURLToFile(myUrl, f);
//	Image i = Image.getInstance("D:\\robot\\testimage.jpg");
//	//image1.scaleAbsolute(500, 500);
	
}
