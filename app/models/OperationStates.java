package models;

public enum OperationStates {
	RUNNING(true),
	STOPPED(false),
	FAILED(false),
	HOT_STANDBY(true),
	COLD_STANDBY(false),
	START_UP(true),
	IDLE(true),
	TESTING(true),
	OUT_OF_SERVICE(false);
	
	private final boolean state;
	OperationStates(boolean s) {this.state=s;}
	public boolean getState() {return state;}
}
