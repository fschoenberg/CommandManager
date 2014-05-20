package cc.commandmanager.core;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Sets;

public final class DummyCommand4 implements Command {

	@Override
	public void execute(Context context) {
		((List<Class<? extends Command>>) context.get(CommandManagementIntegrationTest.EXECUTED_COMMANDS)).add(this
				.getClass());
	}

	@Override
	public Set<String> getBeforeDependencies() {
		return Sets.newHashSet("DummyCommand1");
	}

	@Override
	public Set<String> getAfterDependencies() {
		return Sets.newHashSet("DummyCommand2");
	}

	@Override
	public Set<String> getOptionalBeforeDependencies() {
		return new HashSet<String>();
	}

	@Override
	public Set<String> getOptionalAfterDependencies() {
		return Sets.newHashSet("DummyCommand3");
	}

}
