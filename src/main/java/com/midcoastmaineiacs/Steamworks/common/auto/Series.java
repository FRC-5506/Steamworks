package com.midcoastmaineiacs.Steamworks.common.auto;

import com.midcoastmaineiacs.Steamworks.api.ActiveCommand;
import com.midcoastmaineiacs.Steamworks.api.Command;
import com.midcoastmaineiacs.Steamworks.api.Scheduler;

/**
 * <p>Runs multiple commands in parallel. Used as a convenient way to instantiate sequences (especially within a Command)
 * without dedicating a class to a new CommandGroup. Also allows inner commands to be cancelled, unlike a CommandGroup.
 *
 * <p>Does not attempt to control subsystems, and so does not use {@link ActiveCommand}'s implementation of
 * {@link Series#end()}.
 */
public class Series extends ActiveCommand {
	Command[] commands;
	private int i = 0;

	public Series(Command... commands) {
		this.commands = commands;
	}

	/**
	 * Add a new command to the sequence. Must <em>not</em> be called while the Series is running.
	 *
	 * @param command Command to add to the list
	 * @throws com.midcoastmaineiacs.Steamworks.api.Scheduler.CommandException if called while the Series is running
	 */
	@SuppressWarnings("unused")
	public void add(Command command) {
		if (isRunning()) {
			throw new Scheduler.CommandException("Cannot add a Command to a Series while said Series is running.\n" +
				"Make sure you add all required commands before adding a Series to the scheduler.");
		}
		commands[commands.length] = command;
	}

	@Override
	protected void initialize() {
		if (commands.length > 0)
			commands[0].start();
		else
			cancel();
	}

	@Override
	protected void execute() {
		if (isCanceled()) return;
		if (!commands[i].isRunning()) {
			i++;
			if (commands.length > i)
				commands[i].start();
			else
				cancel();
		}
	}

	@Override
	protected void end() {
		for (Command i: commands)
			if (i.isRunning() && !i.isCanceled())
				i.cancel();
	}

	/**
	 * Similar to {@link Series} but doesn't not run commands one-after-another. Instead, it runs all commands at the
	 * same time, and ends when all of them have finished. There is no practice limit mechanism, all commands are always
	 * run, regardless of competition mode.
	 */
	@SuppressWarnings({"WeakerAccess", "unused"})
	public static class Parallel extends Series {
		public Parallel(Command... commands) {
			super(commands);
		}

		@Override
		protected void initialize() {
			for (Command i: commands)
				i.start();
		}

		@Override
		protected void execute() {
			if (isCanceled()) return;
			for (Command i: commands)
				if (i.isRunning() && !i.isCanceled())
					return; // a command is running, don't cancel
			cancel(); // we haven't returned, so no command is running, meaning we're done running
		}
	}
}
