package org.sujavabot.plugin.markov;

import java.util.Arrays;

public class EditDistancer {
	public static interface Op {
		public static final byte STOP = 0;
		public static final byte DELETE = 1;
		public static final byte INSERT = 2;
		public static final byte NEXT = 3;
	}
	
	/**
	 * The operation type
	 */
	protected byte[] flags;
	/**
	 * The path length
	 */
	protected short[] lengths;
	/**
	 * The original byte sequence
	 */
	protected byte[] xval;
	/**
	 * The target byte sequence
	 */
	protected byte[] yval;
	
	/**
	 * Create a new {@link EditDistancer} with the argument buffer size
	 * @param size
	 */
	public EditDistancer() {
	}
	
	/**
	 * Compute the {@link Op} graph for the argument original and target byte arrays.
	 * @param orig
	 * @param target
	 */
	public byte[] compute(byte[] orig, byte[] target) {
		int size = (orig.length + 1) * (target.length + 1);
		
		if(flags == null || size > flags.length) {
			flags = new byte[size];
		} else {
			Arrays.fill(flags, (byte) 0);
		}
		if(lengths == null || size > lengths.length) {
			lengths = new short[size];
		} else {
			Arrays.fill(lengths, (short) 0);
		}
		lengths[0] = Short.MIN_VALUE;
		
		if(xval == null || orig.length + 1 > xval.length)
			xval = new byte[orig.length + 1];
		System.arraycopy(orig, 0, xval, 1, orig.length);

		if(yval == null || target.length + 1 > yval.length)
			yval = new byte[target.length + 1];
		System.arraycopy(target, 0, yval, 1, target.length);
		
		for(int y = 0; y < yval.length; y++) {
			for(int x = 0; x < xval.length; x++) {
				if(x == 0 && y == 0)
					continue;
				int pos = x + y * xval.length;
				if(x > 0 && y > 0 && xval[x] == yval[y]) {
					flags[pos] = Op.NEXT;
					lengths[pos] = (short) (1 + lengths[pos - xval.length - 1]);
					continue;
				}
				short dlen = x > 0 ? (short)(1 + lengths[pos-1]) : Short.MAX_VALUE;
				short ilen = y > 0 ? (short)(1 + lengths[pos - xval.length]) : Short.MAX_VALUE;
				if(dlen <= ilen) {
					flags[pos] = Op.DELETE;
					lengths[pos] = dlen;
				} else {
					flags[pos] = Op.INSERT;
					lengths[pos] = ilen;
				}
				
			}
		}

		byte[] ret = new byte[xval.length + yval.length];
		int rpos = 0;

		int pos = xval.length * yval.length - 1;
		while(pos > 0) {
			byte op = flags[pos];
			ret[rpos++] = op;
			if(op == Op.INSERT) {
				pos -= xval.length;
			}
			if(op == Op.DELETE) {
				pos -= 1;
			}
			if(op == Op.NEXT) {
				pos -= xval.length + 1;
			}
			if(op == Op.STOP)
				break;
		}
		
		return Arrays.copyOfRange(ret, 0, rpos);
	}

}
