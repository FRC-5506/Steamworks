package com.midcoastmaineiacs.Steamworks.api;

import com.midcoastmaineiacs.Steamworks.common.DriveTrain;
import com.midcoastmaineiacs.Steamworks.common.Notifier;
import com.midcoastmaineiacs.Steamworks.common.Robot;

import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

/**
 * <p>Not the wpilib {@link edu.wpi.first.wpilibj.command.Scheduler Scheduler}. This is intended to use static timing
 * rather than relying on the {@link edu.wpi.first.wpilibj.IterativeRobot#robotPeriodic() timing of the periodic}
 * functions, among other benefits.
 *
 * <p>Instantiating Scheduler can act as a {@link TimerTask} for timers, all instances will simply call
 * {@link Scheduler#tick() Scheduler.tick()}. Commands MUST override {@link Command#start()}. If this isn't possible
 * they MUST NOT be started with {@link Command#start()}, use {@link Scheduler#add(Command) Scheduler.add()} instead.
 * {@link ActiveCommand} overrides {@link Command#start() start()}, so active commands should not have to worry about this,
 * but passive commands must override {@link Command#start() start} (see {@link Notifier#start()}).
 *
 * <p>However, {@link Command#cancel()} still works to cancel commands, without overrides.
 */
@SuppressWarnings("WeakerAccess")
public class Scheduler extends TimerTask {
	public static boolean enabled = false;
	/** is teleop enabled? */
	public static boolean teleop = false;
	private static final List<Command> schedule = new ArrayList<>();
	public static Command currentCommand;

	/**
	 * Runs commands, similar to wpilib's {@link edu.wpi.first.wpilibj.command.Scheduler#run() getInstance().run()}.
	 */
	public static void tick() {
		for (int i = 0; i < schedule.size(); i++) {
			Command command = schedule.get(i);
			/*currentCommand = command;

			if (!command.willRunWhenDisabled()) {
				if (command.enabled != enabled) command._freeze();
				if (!command.enabled && enabled) command._resume();
				command.enabled = enabled;
			}

			if (command.shouldCancel()) command.cancel();

			// A) allows code to "disable" and B) prevents commands from getting cancelled, just "paused"
			// I expect the Robot ...init() methods to cancel commands as necessary
			// However, add an exception to allow commands to end if they are cancelled
			if (!enabled && !command.isCanceled() && !command.willRunWhenDisabled()) continue;

			// check if the command is cancelled, as command.run won't check if it's cancelled after initialize() and
			// execute() are called, until the next run, so this allows a command to immediately cancel itself in favor
			// of running a different command without relying on isFinished()
			if ((command.hasRun && command.isFinished()) || !command._run() || command.isCanceled()) {
				schedule.remove(i);
				i--;
				command._removed();
			}
			if (command instanceof ActiveCommand) ((ActiveCommand) command).hasRun = true;*/
			if (!command.isRunning()) {
				schedule.remove(i);
				i--;
				continue;
			}
			if (command.shouldCancel() || command.isFinished()) command.cancel();
			if (!command.willRunWhenDisabled() && !command.isCanceled()) {
				if (command.enabled != enabled) command._freeze();
				if (!command.enabled && enabled) command._resume();
				command.enabled = enabled;
			}
			if (enabled || command.isCanceled() || command.willRunWhenDisabled()) {
				if (command.isCanceled() || command._run()) {
					command._removed();
					schedule.remove(command);
					i--;
					if (command instanceof ActiveCommand)
						for (Subsystem subsystem: Robot.subsystems)
							if (subsystem.controlledBy((ActiveCommand) command) && subsystem.relinquishControl((ActiveCommand) command))
								subsystem.stop();
				}
			}
		}
		currentCommand = null;
		if (enabled && Robot.driveTrain.getState() == DriveTrain.State.AUTOPILOT)
			Robot.driveTrain.updateAutopilot();
		if (!enabled) { // since commands won't stop motors when paused, we stop them ourselves
			for (Subsystem i: Robot.subsystems)
				i.stop();
		}
	}

	/**
	 * Starts running a command. Called by {@link ActiveCommand#start()}. <em>Must not</em> be called (directly or
	 * indirectly) by a passive command. <em>Can</em> be called by an active command, or outside of any command all
	 * together.
	 *
	 * <p>Passive commands <em>must</em> override {@link Command#start()} such that it uses this method (e.g.
	 * {@code Scheduler.add(this);}).
	 *
	 * @param command Command to add to the schedule
	 * @throws Scheduler.IllegalPassiveCommandException if called by a passive command
	 * @see ActiveCommand#start()
	 */
	public static void add(Command command) {
		if (command.isRunning()) {
			System.out.println("Command " + command + " not started. It is already running.");
			return;
		}
		System.out.println("Starting command: " + command);
		if (command instanceof ActiveCommand)
			command._start();
		schedule.add(command);
	}

	/**
	 * Runs {@link Scheduler#tick() Scheduler.tick()}, used for TimerTask implementation.
	 */
	@Override
	public void run() {
		tick();
	}

	/**
	 * Cancel all commands (except {@link Notifier}, currently used when robot is disabled during testing or a demonstration.
	 */
	public static void cancelAllCommands() {
		for (Command i: schedule)
			if (i instanceof ActiveCommand) // Don't cancel passive commands such as the Notifier
				i.cancel();
		for (Subsystem i: Robot.subsystems)
			// ensure that no command has control of the subsystems, this will also reset the driveTrain autopilot
			i.takeControl(null);
	}

	public static void enableTeleop(boolean enabled) {
		if (teleop == enabled) return;
		teleop = enabled;
		for (Subsystem i: Robot.subsystems) {
			i.enableTeleop(enabled);
		}
	}

	/**
	 * @return The command that called this method, or null if no command led to this method being called
	 */
	public static Command getCurrentCommand() {
		return Thread.currentThread() == Robot.mainThread ? null : currentCommand;
	}

	/**
	 * A RuntimeException that indicates illegal handling of commands.
	 */
	public static class CommandException extends RuntimeException {
		public CommandException(String message) {
			super(message);
		}
	}

	/**
	 * A {@link CommandException} that indicates that some function which is required to be called by a {@link Command} was
	 * called outside of a command. For a command to be detected, that command must be running through the
	 * {@link Scheduler Midcoast Maineiacs Scheduler}.
	 */
	public static class CommandNotFoundException extends CommandException {
		public CommandNotFoundException(String message) {
			super(message);
		}
	}

	/**
	 * A {@link CommandException} that indicates that a passive command did something that is only permissible to an
	 * active command. A "passive command" is one that is a {@link Command} but not an {@link ActiveCommand} while an "active
	 * command" <em>is</em> an {@link ActiveCommand}.
	 *
	 * <p><em>Passive</em> commands <b>may not</b>:
	 * <ul><li>
	 *     Control (directly or indirectly) a subsystem
	 * </li><li>
	 *     Start an active command
	 * </li></ul>
	 * If a Command needs to do either of these, they <em>must</em> be considered active commands and extend ActiveCommand.
	 */
	public static class IllegalPassiveCommandException extends CommandException {
		public IllegalPassiveCommandException(String message) {
			super(message);
		}
	}
}
