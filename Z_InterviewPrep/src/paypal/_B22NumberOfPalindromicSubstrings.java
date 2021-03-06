package paypal;

public class _B22NumberOfPalindromicSubstrings {

	public static void main(String[] args) {
		String str = "forgeeksskeegfor";
        System.out.println("Number of palindromic subsequence is: " + numberOfPalindromicSubstr(str));

	}
	
	static int numberOfPalindromicSubstr(String str)
    {
        int n = str.length();
        int count = 0;
        boolean dp[][] = new boolean[n][n];

        for (int index = 0; index < str.length(); index++) {
			for (int i = 0, j = index; j < dp.length; i++, j++) {
				if(index == 0)
					dp[i][j] = true;
				else if(index ==1 && str.charAt(i) == str.charAt(j))
					dp[i][j] = true;
				else
					if(str.charAt(i) == str.charAt(j) && dp[i+1][j-1] == true)
						dp[i][j] = true;
				if(dp[i][j])
					count++;
			}
		}
        printTable(str, dp);
        
        return count;
    }
	
	static void printSubStr(
	        String str, int low, int high)
	    {
	        System.out.println(
	            str.substring(
	                low, high + 1));
	    }
	
	public static void printTable(String str, boolean table[][]) {
		System.out.print("  ");
		for (int i = 0; i < str.length(); i++) {
			System.out.print(str.charAt(i) + "  ");
		}
		System.out.println();
		for (int i = 0; i < table.length; i++) {
			System.out.print(str.charAt(i) + " ");
			for (int j = 0; j < table.length; j++) {
				char c = table[i][j] == true ? 't' : 'f';
				System.out.print(c + "  ");
			}
			System.out.println();
		}
	}

}
