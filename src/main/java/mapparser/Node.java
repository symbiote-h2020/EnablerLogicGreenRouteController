package mapparser;

public class Node {
	public String id;
	public double lat;
	public double lon;

	public Node() {
	}
	
	public Node(Node another) {
		this.id = another.id;
		this.lat = another.lat;
		this.lon = another.lon;
	}
}
