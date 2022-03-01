package designPatterns.builder;

public class Book {
	private final String isbn;
	private final String title;
	private final String author;
	private final String description;
	
	public Book() {
		this.isbn = null;
		this.title = null;
		this.author = null;
		this.description = null;
	}
}
