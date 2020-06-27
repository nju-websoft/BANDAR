package beans;

public class property {
	private String name;
	private double count = 0;//record the number of instances
	
	public void setName(String s) {
		name = s;
	}

	public String getName() {
		return name;
	}
	
	public void add() {
		count++;
	}
	
	public double getCount() {
		return count;
	}
}
