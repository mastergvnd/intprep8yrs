package designPatterns.adapterPattern;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

public class Test {

	public static void main(String[] args) {
		ProductCategoryEnum prodCatEnum = new ProductCategoryEnum();
		
		AmazonUS aUS =  new AmazonUS();
		aUS.displayCategories(prodCatEnum.getProductCategories());
		System.out.println("--------------------------------------------------------------");
		
		AmazonIndia aIndia = new AmazonIndia();
		EnumerationToIterator eIterator = new EnumerationToIterator(prodCatEnum.getProductCategories());
		aIndia.displayProductCategories(eIterator);
		
		
		//Iterator to Enumeration example
		System.out.println("--------------------------------------------------------------");
		ArrayList<String> al = new ArrayList<String>();
		al.add("Govind");
		al.add("Gupta");
		al.add("Sonali");
		al.add("Varshney");

		IteratorToEnumration iToE = new IteratorToEnumration(al.iterator());
		while(iToE.hasMoreElements()) {
			System.out.println(iToE.nextElement());
		}
		
		System.out.println();
		iToE = new IteratorToEnumration(al.iterator());
		while(iToE.hasMoreElements()) {
			String value = iToE.nextElement();
			if(value.equals("Gupta")) {
				iToE.remove();
				continue;
			}
			System.out.println(value);
		}
		
		System.out.println("");
		iToE = new IteratorToEnumration(al.iterator());
		while(iToE.hasMoreElements()) {
			System.out.println(iToE.nextElement());
		}
	}

}
