package oram;

import java.io.Serializable;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import exceptions.IndexNotMatchException;
import exceptions.LengthNotMatchException;
import util.Array64;
import util.Util;

public class Level implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	
	private Metadata md;
	private int index;

	private Array64<Block> fresh;
	private List<Block> stash;
	private List<Integer> used;

	public Level(int index, Metadata md, Random rnd) {
		this.index = index;
		this.md = md;
		fresh = new Array64<Block>(md.getNumBlocks(index));
		stash = new ArrayList<Block>(md.getPeriod());
		used = new ArrayList<Integer>(md.getPeriod());
		
		for (long i=0; i<md.getNumBlocks(index); i++) {
			fresh.set(i, new Block(md.getLBytes(index), md.getFBytes(index), md.getPBytes(index), md.getDBytes(index), md.getTwoTauPow(), rnd));
		}
	}
	
	public void init() {
		if (index == md.getNumLevels() - 1) {
			for (long i=0; i<md.getNumBlocks(index); i++) {
				byte[] l = Util.rmSignBit(BigInteger.valueOf(i).toByteArray());
				Block target = fresh.get(i);
				target.setL(l);
				target.setRec(l.clone());
			}
		}
		else {
			long cnt = 0;
			for (long i=0; i<md.getNumBlocks(index); i++) {
				byte[] l = Util.rmSignBit(BigInteger.valueOf(i).toByteArray());
				Block target = fresh.get(i);
				target.setL(l);
				for (int j=0; j<md.getTwoTauPow(); j++) {
					target.setP(j, Util.rmSignBit(BigInteger.valueOf(cnt).toByteArray()));
					cnt++;
				}
			}
		}
	}
	
	public Array64<Block> getFresh() {
		return fresh;
	}
	
	public List<Block> getStash() {
		return stash;
	}
	
	public List<Integer> getUsed() {
		return used;
	}

	public Block getFreshBlock(long i) {
		return fresh.get(i);
	}
	
	public Block getStashBlock(int i) {
		return stash.get(i);
	}
	
	public Integer getUsedAt(int i) {
		return used.get(i);
	}
	
	public void setFresh(Array64<Block> fresh) {
		this.fresh = fresh;
	}
	
	public void setStash(List<Block> stash) {
		this.stash = stash;
	}
	
	public void setUsed(List<Integer> used) {
		this.used = used;
	}
	
	public void setFreshBlock(long i, Block b) {
		fresh.set(i, b);
	}
	
	public void setStashBlock(int i, Block b) {
		stash.set(i, b);
	}
	
	public void setUsedAt(int i, Integer n) {
		used.set(i, n);
	}
	
	public void addToStash(Block b) {
		stash.add(b);
	}
	
	public void addToUsed(Integer n) {
		used.add(n);
	}
	
	public void emptyStash() {
		stash.clear();
	}
	
	public void emptyUsed() {
		used.clear();
	}
	
	public void printFresh() {
		for (long i=0; i<fresh.size(); i++) {
			System.out.println(fresh.get(i));
		}
	}
	
	public void printStash() {
		for (int i=0; i<stash.size(); i++) {
			System.out.println(stash.get(i));
		}
	}
	
	public void printUsed() {
		for (int i=0; i<used.size(); i++) {
			System.out.println(used.get(i));
		}
	}

	public void setXor(Level l) {
		if (index != l.index)
			throw new IndexNotMatchException(index + " != " + l.index);
		if (stash.size() != l.stash.size())
			throw new LengthNotMatchException(stash.size() + " != " + l.stash.size());
		
		for (long i = 0; i < fresh.size(); i++)
			fresh.get(i).setXor(l.fresh.get(i));
		for (int i=0; i<stash.size(); i++)
			stash.get(i).setXor(l.stash.get(i));
	}
}
