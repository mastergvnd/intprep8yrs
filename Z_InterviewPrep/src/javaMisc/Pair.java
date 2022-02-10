package javaMisc;

public class Pair<K, V> {
	
	private K val1;
	private V val2;
	public Pair(K val1, V val2) {
		this.val1 = val1;
		this.val2 = val2;
	}
	public K getVal1() {
		return val1;
	}
	public V getVal2() {
		return val2;
	}
	public void setVal1(K val1) {
		this.val1 = val1;
	}
	public void setVal2(V val2) {
		this.val2 = val2;
	}
	
	@Override
	public String toString() {
		return "[" + val1 + ", " + val2 + "]";
	}
}
