import java.io.File;
import java.util.TreeMap;


public class test {
	public static void main(String[] args) {
		File f = new File("com");
		Object c = new Object();
		TreeMap<Integer, Integer> tm = new TreeMap<Integer, Integer>();
		tm.put(1, -2);
		tm.put(4, -4);
		tm.put(5, -6);
		tm.put(3, -8);
		tm.put(2, -10);
		for(Integer order: tm.keySet()) {
			System.out.println(tm.get(order));
		}
	}
}
