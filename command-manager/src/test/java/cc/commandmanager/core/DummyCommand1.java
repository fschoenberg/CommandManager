package cc.commandmanager.core;

import org.apache.commons.chain.Context;

public final class DummyCommand1 extends DependencyCommand {

    @Override
    public void specialExecute(Context context) {
	System.err.println("DummyCommad1 was called.");
    }
}
