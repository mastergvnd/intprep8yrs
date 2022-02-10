package designPatterns.adapterPattern;

import java.util.Iterator;

public class AmazonIndia {
	public void displayProductCategories(Iterator<String> prodCatItr) {
		while(prodCatItr.hasNext()) {
			System.out.println(prodCatItr.next());
		}
	}
}
