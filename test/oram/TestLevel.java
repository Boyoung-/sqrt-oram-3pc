package oram;

public class TestLevel {

	public static void main(String[] args) {
		Metadata md = new Metadata();
		md.print();
		for (int i = 0; i < md.getNumLevels(); i++) {
			Level level = new Level(i, md, null);
			level.init();
			level.printFresh();
		}
	}

}
