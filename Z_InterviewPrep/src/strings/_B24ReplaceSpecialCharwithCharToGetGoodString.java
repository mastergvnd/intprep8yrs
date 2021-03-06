package strings;

import java.util.Arrays;

import org.junit.Assert;

public class _B24ReplaceSpecialCharwithCharToGetGoodString {

	public static void main(String[] args) {
		Assert.assertEquals("abcdefghijklmnopqrstuvwxyz", getGoodString("abcdefghijkl?nopqrst?vwxy?"));
	}

	private static String getGoodString(String str) {
		if(str.length() < 26)
			return null;
		System.out.println("String length : " + str.length());
		char s[] = str.toCharArray();
		for(int i = 25; i<str.length(); i++) {
			int count[] = new int[26];
			for(int j=i; j>=0; j--) {
				if(s[j] != '?')
					count[s[j] - 'a']++;
			}
			System.out.println(Arrays.toString(count));
			if(isValid(count)) {
				 int cur = 0;
		         while (cur <= 25 && count[cur] > 0)
		        	cur++;
				for(int j=i-25; j<=i; j++) {
					if(s[j] == '?')  {
						s[j] = (char)(cur + 'a');
						cur++;
						while (cur <= 25 && count[cur] > 0)
							cur++;
					}
				}
				return new String(s);
			}
			
		}
		
		return "";
	}

	private static boolean isValid(int[] count) {
		for (int i = 0; i < count.length; i++) {
			if(count[i] > 1)
				return false;
		}
		return true;
	}

}
