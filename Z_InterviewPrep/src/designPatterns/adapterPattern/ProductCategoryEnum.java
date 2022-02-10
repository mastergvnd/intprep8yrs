package designPatterns.adapterPattern;

import java.util.Enumeration;
import java.util.Vector;

public class ProductCategoryEnum {
	private Vector<String> productCategories;
	
	public ProductCategoryEnum() {
		productCategories = new Vector<String>();
		addProductCategory("Appliances");
		addProductCategory("Books");
		addProductCategory("Furniture");
		addProductCategory("Baby");
	}
	
	public void addProductCategory(String product) {
		productCategories.add(product);
	}
	
	public Enumeration<String> getProductCategories() {
		return productCategories.elements();
	}

}
