package com.midcoastmaineiacs.Steamworks.rio;

import com.midcoastmaineiacs.Steamworks.api.Device;
import com.midcoastmaineiacs.Steamworks.api.Motor;
import com.midcoastmaineiacs.Steamworks.api.RobotAPI;
import com.midcoastmaineiacs.Steamworks.api.Subsystem;
import edu.wpi.first.wpilibj.DriverStation;
import edu.wpi.first.wpilibj.Spark;
import edu.wpi.first.wpilibj.VictorSP;

public class RioAPI extends RobotAPI {
	@Override
	public Motor getMotor(Subsystem parent, Device device, boolean inverted) {
		switch (device) {
			case DRIVE_LEFT:
				return new com.midcoastmaineiacs.Steamworks.rio.Motor(parent, 1, VictorSP.class, inverted);
			case DRIVE_RIGHT:
				return new com.midcoastmaineiacs.Steamworks.rio.Motor(parent, 2, VictorSP.class, inverted);
			case CLIMBER:
				return new com.midcoastmaineiacs.Steamworks.rio.Motor(parent, 3, Spark.class, inverted);
			default:
				throw new RuntimeException("Unknown motor: " + device.name());
		}
	}

	@Override
	public boolean isDisabled() {
		return RioMain.instance.isDisabled();
	}

	@Override
	public boolean isEnabled() {
		return RioMain.instance.isEnabled();
	}

	@Override
	public boolean isAutonomous() {
		return RioMain.instance.isAutonomous();
	}

	@Override
	public boolean isTest() {
		return RioMain.instance.isTest();
	}

	@Override
	public boolean isOperatorControl() {
		return RioMain.instance.isOperatorControl();
	}

	@Override
	public void reportWarning(String message) {
		DriverStation.reportWarning(message, false);
	}

	@Override
	public void reportError(String message) {
		DriverStation.reportError(message, true);
	}
}
