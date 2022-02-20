import java.util.List;

import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import org.openqa.selenium.firefox.FirefoxDriver;

public class WhatsApp {

	public static void main(String[] args) throws InterruptedException {
		System.setProperty("webdriver.gecko.driver","D:\\chromedriver\\chromedriver.exe");
		WebDriver driver = new FirefoxDriver();
        driver.manage().window().maximize();
        String url = "https://web.whatsapp.com";
        driver.get(url);
        Thread.sleep(10000);
        driver.findElement(By.xpath("//span[contains(text(),'Shizuka')]")).click();
        System.out.println("Clicked on Chhota Kalu");
        //List<WebElement> l = driver.findElements(By.className("input"));
        int i = 0;
        while(i++ <= 100){
        	driver.findElement(By.xpath("//*[@id='main']/footer/div[1]/div[2]/div/div[2]")).sendKeys("This is an automated message.");
        	driver.findElement(By.className("_3M-N-")).click();
        }
	}

}
