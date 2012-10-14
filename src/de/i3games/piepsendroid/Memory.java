package de.i3games.piepsendroid;

public class Memory {
	private static final int DEFAULT_SIZE = 10;
	private int mSize;
	private int mPos;
	private float[] mValues;

	public Memory(int size) {
		mSize = size;
		mValues = new float[size];
		mPos = 0;
	}

	public Memory() {
		this(DEFAULT_SIZE);
	}

	void add(float val) {
		mValues[mPos] = val;
		mPos = (mPos + 1) % mSize;
	}

	float getAverage() {
		float sum = 0.0f;
		for (int i = 0; i < mSize; i++) {
			sum = sum + mValues[i];
		}
		return sum / mSize;
	}
	
	float getVariance() {
		float sum = 0.0f, diff = 0.0f;
		final float average = getAverage();
		
		for (int i = 0; i < mSize; i++) {
			diff = average - mValues[i];
			sum = sum + diff * diff;
		}
		
		return sum / mSize;
		
	}
	

}
