import java.io.FileNotFoundException;
import java.io.FileOutputStream;

import org.openqa.selenium.By;
import org.openqa.selenium.WebElement;

import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Chunk;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Image;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;

public class PDFFileWriter {
	static PdfWriter writer = null;
	static Document document = null;
	
	public PDFFileWriter() {
		
	}
	
	public PDFFileWriter(String name) {
		document = new Document();
	      try
	      {
	         writer = PdfWriter.getInstance(document, new FileOutputStream("D:\\robot\\" + name + ".pdf"));
	         document.open();
	         System.out.println("PDF file is created and opened");
	      } catch (DocumentException e)
	      {
	    	 System.out.println("DocumentException");
	         e.printStackTrace();
	      } catch (FileNotFoundException e)
	      {
	    	  System.out.println("FileNotFoundException");
	         e.printStackTrace();
	      }
	}
	
	public void writeContent(WebElement content) {
		try {
			document.add(new Paragraph(content.getText()));
			System.out.println("Content is written to PDF file");
		} catch (DocumentException e) {
			System.out.println("DocumentException2");
			e.printStackTrace();
		}
	}
	
	public void writeSummary(WebElement element) throws DocumentException {
		String topicHeader = element.findElement(By.tagName("h1")).getText();
		String summary = element.getText();
		if(summary != null) {
			summary = summary.replace(topicHeader, "");
		}
		writeHeaderLine(topicHeader);
		writeSummaryLine(summary);
	}
	
	private void writeSummaryLine(String summary) throws DocumentException {
		Paragraph p = new Paragraph(summary);
		document.add(p);
	}

	private void writeHeaderLine(String topicHeader) throws DocumentException {
		Font font = FontFactory.getFont(FontFactory.TIMES, 24, Font.BOLD, BaseColor.BLACK);
		Chunk chunk = new Chunk(topicHeader, font);
		Paragraph p = new Paragraph(chunk);
		document.add(p);
	}

	public void closeWriter() {
        document.close();
        writer.close();
        System.out.println("Pdf file is closed");
	}

	public void writeParagraphsWithHeading(WebElement div) throws DocumentException {
		
		String topicSubHeader = div.findElement(By.tagName("h4")).getText();
		String topicDetails = div.getText();
		if(topicDetails != null) {
			topicDetails = topicDetails.replaceFirst(topicSubHeader, "").trim();
		}
		writeSubHeaderLine(topicSubHeader);
		writeTopicDetails(topicDetails);
	}

	private void writeSubHeaderLine(String topicSubHeader) throws DocumentException {
		Font font = FontFactory.getFont(FontFactory.TIMES, 12, Font.BOLD, BaseColor.BLACK);
		Chunk chunk = new Chunk(topicSubHeader, font);
		chunk.setBackground(BaseColor.LIGHT_GRAY);
		Paragraph p = new Paragraph(chunk);
		document.add(p);
	}
	
	private void writeTopicDetails(String topicDetails) throws DocumentException {
		Paragraph p = new Paragraph(topicDetails);
		document.add(p);
	}

	public void writeCodeSnippet(WebElement ele) throws DocumentException {
		Paragraph p = new Paragraph(ele.getText());
		document.add(p);
	}

	public void writeImage(Image image2) throws DocumentException {
		Paragraph p = new Paragraph("");
		p.add(image2);
		document.add(p);
		document.newPage();
	}
	
	public Document getDocument(){
		return document;
	}

}
