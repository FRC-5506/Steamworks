package com.midcoastmaineiacs.Steamworks.api;

import com.midcoastmaineiacs.Steamworks.common.Notifier;
import com.midcoastmaineiacs.Steamworks.common.auto.DriveCommand;

/**
 * Just like a wpilib command, but adds some new features.
 * <ul><li>
 *     Integrates with Subsystem to handle inheritance of command control
 * </li><li>
 *     Adds default isFinished() and end() implementations
 * </li><li>
 *     Modifies start() implementation to properly use Scheduler (and detect if it is started by a parent command)
 * </li><li>
 *     Adds a fully-fledged command hierarchy system
 * </li></ul>
 * For the purposes of this project, a {@link Command} that is also an ActiveCommand (such as a {@link DriveCommand}) is
 * considered an "active command," while a {@link Command} that is <em>not</em> an ActiveCommand (such as a
 * {@link Notifier Notifier}) is considered a "passive command."
 */
@SuppressWarnings("WeakerAccess")
public abstract class ActiveCommand extends Command {
	public boolean controls(Subsystem subsystem) {
		// Note, it is safe to cast "parent" to ActiveCommand here because a passive command cannot start an ActiveCommand
		return subsystem.directlyControlledBy(this) || parent != null && ((ActiveCommand) parent).controls(subsystem);
	}

	/**
	 * {@inheritDoc}
	 *
	 * @throws Scheduler.IllegalPassiveCommandException if started by a passive command
	 */
	@Override
	public void start() {
		super.start();
	}

	@Override
	public void setRunWhenDisabled(boolean run) {
		if (run)
			throw new RuntimeException("Active commands cannot run while disabled!");
		else
			super.setRunWhenDisabled(false);
	}
}
