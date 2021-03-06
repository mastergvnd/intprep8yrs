package strings;

public class _B32StringPermutations {

	public static void main(String[] args) {
		StringBuilder s = new StringBuilder("ABC");
		printPermutations(s, 0, s.length()-1);
		System.out.println("Done");
	}
	
	private static void printPermutations(StringBuilder s, int l, int r) {
		if(l == r){
			System.out.println(s);
			return;
		}
		for(int i=l; i<=r; i++) {
			swap(s, i, l);
			printPermutations(s, l+1, r);
			swap(s, i, l);
		}
	}
	
	private static void swap(StringBuilder s, int l, int r) {
		char temp = s.charAt(l);
		s.setCharAt(l, s.charAt(r));
		s.setCharAt(r, temp);
	}
}
