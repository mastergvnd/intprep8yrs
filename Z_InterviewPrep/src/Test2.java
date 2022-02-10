import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Test2 {
	public static void main(String[] args) {
		List<Score> list = new ArrayList<Score>();
		list.add(new Score(3,"Govind"));
		list.add(new Score(6,"Sonali"));
		list.add(new Score(8,"Shivam"));
		list.add(new Score(2,"Dolly"));
		list.add(new Score(2,"ADolly"));
		list.sort((a, b) -> {
			if((b.getScore() - a.getScore()) == 0) {
				return a.getUser().compareTo(b.getUser());
			}
			return b.getScore() - a.getScore();
		});
//		Collections.sort(list, (a, b) -> {
//			if((b.getScore() - a.getScore()) == 0) {
//				return a.getUser().compareTo(b.getUser());
//			}
//			return b.getScore() - a.getScore();
//		});
		System.out.println(list);
	}
}