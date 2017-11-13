package com.midcoastmaineiacs.Steamworks.common;

import com.midcoastmaineiacs.Steamworks.api.Device;
import com.midcoastmaineiacs.Steamworks.api.Motor;
import com.midcoastmaineiacs.Steamworks.api.Subsystem;

public class Climber extends Subsystem {
	private final Motor climber = Robot.api.getMotor(this, Device.CLIMBER, false);

	public void set(double speed) {
		if (verifyResponse())
			climber.set(speed);
	}

	@Override
	public void stop() {
		climber.stop();
	}
}
