package cc.commandmanager.core;

import org.apache.commons.chain.Context;

public final class DummyCommand4 extends Command {

    @Override
    public void specialExecute(Context context) {
	System.err.println("DummyCommand4 was called.");
    }

    @Override
    public void addDependencies() {
	beforeDependencies.add("DummyCommand1");
	afterDependencies.add("DummyCommand2");
	optionalAfterDependencies.add("DummyCommand3");
    }
}
