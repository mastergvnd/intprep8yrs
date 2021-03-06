import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.List;
import com.itextpdf.text.Paragraph;

public class TestUsingInnerText {

	public static void main(String[] args) throws DocumentException {
		
		//PDFFileWriter pdfWriter = new PDFFileWriter("testSummary3");
		//Document document = pdfWriter.getDocument();
		
		if(innerHTML.contains("<ol>") || innerHTML.contains("<ul>") || false){
			while((innerText != null && innerText.trim().length() > 0) || false) {
				String firstListType = getFirstList(innerHTML);
				
				String firstList = innerHTML.substring(innerHTML.indexOf("<li>"), innerHTML.indexOf("</li>"));
				
				firstList = removeListTag(firstList);
				
				System.out.println("firstList : " + firstList);
				
				String subString = innerText.substring(0, innerText.indexOf(firstList));
				innerText = innerText.replace(subString, "");
				innerHTML = innerHTML.replace(innerHTML.substring(0, innerHTML.indexOf(firstListType)), "");
				
				System.out.println("subText : " + subString);
				System.out.println("leftover inner Text : " + innerText);
				System.out.println("leftover innerHTML : " + innerHTML);
				//document.add(new Paragraph(subString)); 
				//read the list
				String closingTag = getClosingTag(firstListType);
				String listContentHTML = innerHTML.substring(0, innerHTML.indexOf(closingTag) + closingTag.length());
				innerHTML = innerHTML.replace(listContentHTML, "");
				listContentHTML = listContentHTML.replace(firstListType, "").replace(closingTag, "");
				System.out.println("listContentHTML : " + listContentHTML);
				System.out.println("leftover innerHTML : " + innerHTML);
				
				List list = null;
				
				if(firstListType.contains("ol"))
					list = new List(List.ORDERED);
				else
					list = new List(List.UNORDERED);
				
				while(listContentHTML != null && listContentHTML.trim().length() > 0) {
					String listItem = listContentHTML.substring(0, listContentHTML.indexOf("</li>") + "</li>".length()).trim();
					listContentHTML = listContentHTML.replace(listItem, "").trim();
					listItem = listItem.replace("<li>", "").replace("</li>", "");
					innerText = innerText.replaceFirst(listItem, "");
					System.out.println(listItem);
					list.add(listItem);
				}
				
				System.out.println("\n");
				System.out.println("leftover inner Text : " + innerText);
				System.out.println("\n\n");
				//add list to document
				
			}
		}
		//pdfWriter.closeWriter();
		System.out.println("Completed");

	}
	
	private static String getClosingTag(String firstListType) {
		if(firstListType.contains("ul"))
			return "</ul>";
		else if(firstListType.contains("ol"))
			return "</ol>";
		
		return null;
				
	}

	private static String removeListTag(String firstList) {
		return firstList.replaceAll("<li>", "").replaceAll("</li>", "");
	}

	private static String getFirstList(String innerhtml2) {
		return innerhtml2.indexOf("<ol>") > innerhtml2.indexOf("<ul>") ? "<ul>" : "<ol>";
	}

	static String innerText = "Counter Program"+
			"Below is an example highlighting how multi-threading necessitates caution when accessing shared data amongst threads. Incorrect synchronization between threads can lead to wildly varying program outputs depending on in which order threads get executed."+

			"Consider the below snippet of code"+

			"1. int counter = 0;"+
			"2."+
			"3. void incrementCounter() {"+
			"4.   counter++;"+
			"5. }"+
			"The increment on line 4 is likely to be decompiled into the following steps on a computer:"+

			"Read the value of the variable counter from the register where it is stored"+
			"Add one to the value just read"+
			"Store the newly computed value back to the register"+
			"The innocuous looking statement on line 4 is really a three step process!"+

			"Now imagine if we have two threads trying to execute the same function incrementCounter then one of the ways the execution of the two threads can take place is as follows:"+

			"Lets call one thread as T1 and the other as T2. Say the counter value is equal to 7."+

			"T1 is currently scheduled on the CPU and enters the function. It performs step A i.e. reads the value of the variable from the register, which is 7."+

			"The operating system decides to context switch T1 and bring in T2."+

			"T2 gets scheduled and luckily gets to complete all the three steps A, B and C before getting switched out for T1. It reads the value 7, adds one to it and stores 8 back."+

			"T1 comes back and since its state was saved by the operating system, it still has the stale value of 7 that it read before being context switched. It doesn't know that behind its back the value of the variable has been updated. It unfortunately thinks the value is still 7, adds one to it and overwrites the existing 8 with its own computed 8. If the threads executed serially the final value would have been 9."+

			"The problems should be apparent to the astute reader. Without properly guarding access to mutable variables or data-structures, threads can cause hard to find to bugs."+

			"Since the execution of the threads can't be predicted and is entirely up to the operating system, we can't make any assumptions about the order in which threads get scheduled and executed.";
	
	static String innerHTML = "<h4>Counter Program</h4> " +
    "<div class=\"ans\">"+
"<p>Below is an example highlighting how multi-threading necessitates caution when accessing shared data amongst threads. Incorrect synchronization between threads can lead to wildly varying program outputs depending on in which order threads get executed.</p>"+
"<p>Consider the below snippet of code</p>"+
"<b>"+
"<pre class=\"cm-viewer-markdown disable-cursor\" style=\"background-color: #1e1e1e; border-radius: 0px;\"><code class=\"language-java vs-dark\" data-lang=\"java\"><span><span class=\"mtk6\">1</span><span class=\"mtk9\">.</span><span class=\"mtk1\">&nbsp;</span><span class=\"mtk8\">int</span><span class=\"mtk1\">&nbsp;counter&nbsp;</span><span class=\"mtk9\">=</span><span class=\"mtk1\">&nbsp;</span><span class=\"mtk6\">0</span><span class=\"mtk9\">;</span></span><br><span><span class=\"mtk6\">2</span><span class=\"mtk9\">.</span></span><br><span><span class=\"mtk6\">3</span><span class=\"mtk9\">.</span><span class=\"mtk1\">&nbsp;</span><span class=\"mtk8\">void</span><span class=\"mtk1\">&nbsp;incrementCounter</span><span class=\"mtk9\">()</span><span class=\"mtk1\">&nbsp;</span><span class=\"mtk9\">{</span></span><br><span><span class=\"mtk6\">4</span><span class=\"mtk9\">.</span><span class=\"mtk1\">&nbsp;&nbsp;&nbsp;counter</span><span class=\"mtk9\">++;</span></span><br><span><span class=\"mtk6\">5</span><span class=\"mtk9\">.</span><span class=\"mtk1\">&nbsp;</span><span class=\"mtk9\">}</span></span><br></code></pre>"+
"</b>"+
"<p>The increment on <b>line 4</b> is likely to be decompiled into the following"+ 
"steps on a computer:</p>"+
"<ul>"+
"  <li>Read the value of the variable counter from the register where it is stored</li>"+
"  <li>Add one to the value just read</li>"+
"  <li>Store the newly computed value back to the register</li></ul>"+
"<p>The innocuous looking statement on <b>line 4</b> is really a three step process!</p>"+
"<p>Now imagine if we have two threads trying to execute the same function <code>incrementCounter</code> then one of the ways the execution of the two threads can take place is as follows:</p>"+
"<p>Lets call one thread as <b>T1</b> and the other as <b>T2</b>. Say the counter value is equal to 7.</p>"+
"<ol>"+
"  <li><p><b>T1</b> is currently scheduled on the CPU and enters the function. It performs step A i.e. reads the value of the variable from the register, which is 7.</p></li>"+
"  <li><p>The operating system decides to context switch <b>T1</b> and bring in <b>T2</b>.</p></li>"+
"  <li><p><b>T2</b> gets scheduled and luckily gets to complete all the three steps <b>A</b>, <b>B</b> and <b>C</b> before getting switched out for <b>T1</b>. It reads the value 7, adds one to it and stores 8 back.</p></li>"+
"  <li><p><b>T1</b> comes back and since its state was saved by the operating system, it still has the stale value of 7 that it read before being context switched. It doesn't know that behind its back the value of the variable has been updated. It unfortunately thinks the value is still 7, adds one to it and overwrites the existing 8 with its own computed 8. If the threads executed serially the final value would have been 9.</p></li>"+
"</ol>"+
"<p>The problems should be apparent to the astute reader. Without properly guarding access to mutable variables or data-structures, threads can cause hard to find to bugs.</p>"+
"<p>Since the execution of the threads can't be predicted and is entirely up to the operating system, we can't make any assumptions about the order in which threads get scheduled and executed.</p>"+
"</div>";

}
