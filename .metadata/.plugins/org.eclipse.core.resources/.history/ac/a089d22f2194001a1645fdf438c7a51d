import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import javax.imageio.ImageIO;

import org.apache.commons.io.FileUtils;

import com.itextpdf.text.BadElementException;

public class ImageDownloaderAndScaler {

	public static com.itextpdf.text.Image downloadAndScaleImage(String url) throws BadElementException, MalformedURLException, IOException {
	    File f= new File("D:\\robot\\image.jpg");
	    System.setProperty("http.agent", "Chrome");
	    URL myUrl;
		try {
			myUrl = new URL(url);
			FileUtils.copyURLToFile(myUrl, f);
			int size = 200;// size of the new image.
            //take the file as inputstream.
            InputStream imageStream = new FileInputStream(f);
            //read the image as a BufferedImage.
            BufferedImage image = javax.imageio.ImageIO.read(imageStream); 
            //cal the sacleImage method.
            BufferedImage newImage = scaleImage(image, size);

            File file = new File("D:\\robot\\", "scaledImage.jpg");
            ImageIO.write(newImage, "JPG", file);

		} catch (Exception e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}
		return com.itextpdf.text.Image.getInstance("D:\\robot\\scaledImage.jpg");

	}
	
	private static BufferedImage scaleImage(BufferedImage bufferedImage, int size) {
        double boundSize = size;
           int origWidth = bufferedImage.getWidth();
           int origHeight = bufferedImage.getHeight();
           System.out.println("origWidth  : " + origWidth);
           System.out.println("origHeight  : " + origHeight);
           double scale;
           if (origHeight > origWidth)
               scale = boundSize / origHeight;
           else
               scale = boundSize / origWidth;
            
           System.out.println("Scale : " + scale);
           
           scale = 0.35;
           
           if (scale > 1.0)
               return (bufferedImage);
           int scaledWidth = (int) (scale * origWidth);
           int scaledHeight = (int) (scale * origHeight);

           Image scaledImage = bufferedImage.getScaledInstance(scaledWidth, scaledHeight, Image.SCALE_SMOOTH);

           BufferedImage scaledBI = new BufferedImage(scaledWidth, scaledHeight, BufferedImage.TYPE_INT_RGB);
           Graphics2D g = scaledBI.createGraphics();
                g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,RenderingHints.VALUE_INTERPOLATION_BILINEAR);
           g.drawImage(scaledImage, 0, 0, null);
           g.dispose();
           return (scaledBI);
   }
	
	
	public static void main(String[] args) {
	    File f= new File("D:\\robot\\test.jpg");
	    System.setProperty("http.agent", "Chrome");
	    URL myUrl;
		try {
			myUrl = new URL("https://www.educative.io/api/collection/5307417243942912/5707702298738688/page/5741031244955648/image/6209426836946944.png");
			FileUtils.copyURLToFile(myUrl, f);
			int size = 200;// size of the new image.
            //take the file as inputstream.
            InputStream imageStream = new FileInputStream(f);
            //read the image as a BufferedImage.
            BufferedImage image = javax.imageio.ImageIO.read(imageStream); 
            //cal the sacleImage method.
            BufferedImage newImage = scaleImage(image, size);

            File file = new File("D:\\robot\\", "testimage.jpg");
            ImageIO.write(newImage, "JPG", file);
            System.out.println("Completed");

		} catch (Exception e) {
			System.out.println("Exception caught");
			e.printStackTrace();
		}

	}
}
