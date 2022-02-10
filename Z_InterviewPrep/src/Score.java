
public class Score {
	int score;
	String user;
	
	public Score(int score, String user) {
		this.score = score;
		this.user = user;
	}

	public int getScore() {
		return score;
	}

	public String getUser() {
		return user;
	}

	public void setScore(int score) {
		this.score = score;
	}

	public void setUser(String user) {
		this.user = user;
	}
	
	@Override
	public String toString() {
		return "[" + score + ", " + user + "]";
	}
}
