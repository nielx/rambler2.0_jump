package nl.simbits.rambler;

public interface JumpStepEventDetector {

	abstract public void detect(final float[] buffer); 
	abstract public void reset();

	public interface eventListener {
		public abstract void onJump(long nanoSeconds);
		public abstract void onJumps(int jumps, long nanoSeconds);
		public abstract void onStep(long nanoSeconds);
	}
}