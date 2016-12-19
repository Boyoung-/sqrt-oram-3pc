package oram;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.Random;

import exceptions.IndexNotMatchException;
import exceptions.LengthNotMatchException;

public class SqrtOram implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;

	private Metadata md;
	private Level[] levels;

	public SqrtOram(Metadata md, Random rnd) {
		this.md = md;
		if (md == null)
			this.md = new Metadata();
		levels = new Level[md.getNumLevels()];
		for (int i = 0; i < levels.length; i++)
			levels[i] = new Level(i, md, rnd);
	}

	public void initWithRecords() {
		for (int i = 0; i < levels.length; i++)
			levels[i].init();
	}

	public Level getLevel(int i) {
		return levels[i];
	}

	public void setLevel(int i, Level l) {
		if (levels[i].getIndex() != l.getIndex())
			throw new IndexNotMatchException(levels[i].getIndex() + " != " + l.getIndex());
		levels[i] = l;
	}

	public void setXor(SqrtOram oram) {
		if (md.getOramBytes() != oram.md.getOramBytes())
			throw new LengthNotMatchException(md.getOramBytes() + " != " + oram.md.getOramBytes());
		for (int i = 0; i < levels.length; i++)
			levels[i].setXor(oram.levels[i]);
	}

	public void print() {
		System.out.println("===== Sqrt ORAM Levels =====\n");

		for (int i = 0; i < levels.length; i++) {
			System.out.println("***** Level " + i + " *****\n");
			System.out.println("Fresh Blocks:");
			levels[i].printFresh();
			System.out.println("\nStash Blocks:");
			levels[i].printStash();
			System.out.println("\nUsed Indices:");
			levels[i].printUsed();
		}

		System.out.println("\n===== End of ORAM =====\n");
	}

	public void writeToFile() {
		writeToFile(md.getDefaultOramFileName());
	}

	public void writeToFile(String filename) {
		FileOutputStream fos = null;
		ObjectOutputStream oos = null;
		try {
			fos = new FileOutputStream(filename);
			oos = new ObjectOutputStream(fos);
			oos.writeObject(this);
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (oos != null)
				try {
					oos.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
	}

	public static SqrtOram readFromFile(String filename) {
		FileInputStream fis = null;
		ObjectInputStream ois = null;
		SqrtOram oram = null;
		try {
			fis = new FileInputStream(filename);
			ois = new ObjectInputStream(fis);
			oram = (SqrtOram) ois.readObject();
		} catch (IOException | ClassNotFoundException e) {
			e.printStackTrace();
		} finally {
			if (ois != null)
				try {
					ois.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
		}
		return oram;
	}
}
