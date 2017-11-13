package com.midcoastmaineiacs.Steamworks.rio;

import com.midcoastmaineiacs.Steamworks.api.Subsystem;
import edu.wpi.first.wpilibj.SpeedController;

import java.lang.reflect.InvocationTargetException;

class Motor extends com.midcoastmaineiacs.Steamworks.api.Motor {
	private SpeedController m;
	private boolean inverted;

	protected Motor(Subsystem parent, int channel, Class<? extends SpeedController> type, boolean inverted) {
		super(parent);
		this.inverted = inverted;
		try {
			m = type.getConstructor(Integer.class).newInstance(channel);
		} catch (InstantiationException | IllegalAccessException | NoSuchMethodException | InvocationTargetException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void stop() {
		m.set(0);
	}

	@Override
	public void set(double speed) {
		if (parent.verifyResponse())
			m.set(inverted ? -speed : speed);
	}
}
