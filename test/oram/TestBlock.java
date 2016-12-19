package oram;

import java.util.Random;

import oram.Block;;

public class TestBlock {

	public static void main(String[] args) {
		Random rand = new Random();
		Metadata md = new Metadata();
		for (int i = 0; i < md.getNumLevels(); i++) {
			Block block = new Block(md.getLBytes(i), md.getFBytes(i), md.getPBytes(i), md.getRecBytes(i),
					md.getTwoTauPow(), rand);
			System.out.println(block);
		}
	}

}
