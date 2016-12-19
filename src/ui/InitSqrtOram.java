package ui;

import oram.Metadata;
import oram.SqrtOram;

public class InitSqrtOram {

	public static void main(String[] args) {
		Metadata md = new Metadata();
		md.print();

		SqrtOram oram = new SqrtOram(md, null);
		oram.writeToFile(md.getDefaultSharesName2());

		oram.initWithRecords();
		oram.writeToFile(md.getDefaultSharesName1());
	}

}
