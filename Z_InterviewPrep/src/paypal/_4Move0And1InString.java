package paypal;

public class _4Move0And1InString {

	public static void main(String[] args) {
		StringBuffer content = new StringBuffer("11000100111011");
		int start = 0;
		int end = content.length() - 1;
		while(start < end) {
			if(content.charAt(start) == '1') {
				while(content.charAt(end) != '0')
					end--;
				char temp = content.charAt(start);
				content.setCharAt(start, content.charAt(end));
				content.setCharAt(end, temp);
			}
			start++;
		}
		System.out.println(content);

	}

}
