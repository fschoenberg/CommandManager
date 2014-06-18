package cc.commandmanager.core;

import static org.fest.assertions.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;

import cc.commandmanager.core.CommandGraph.CommandGraphBuilder;
import cc.commandmanager.core.CommandGraph.Dependencies;

public class CommandGraphBuilderTest {

	CommandGraphBuilder builder;

	@Before
	public void setUp() {
		builder = new CommandGraphBuilder();
	}

	@Test
	public void testAddCommand() {
		assertThat(builder.addCommand(new CommandClass("A", "className.A"))).isTrue();
		CommandGraph graph = builder.build();
		assertThat(graph.hasCommand("A")).isTrue();
		assertThat(graph.getCommandClass("A")).isEqualTo(new CommandClass("A", "className.A"));
	}

	@Test
	public void testAddCommandReturnsFalse() {
		assertThat(builder.addCommand(new CommandClass("A", "className.A"))).isTrue();
		assertThat(builder.addCommand(new CommandClass("A", "className.A"))).isFalse();
		assertThat(builder.addCommand(new CommandClass("A", "otherClassName.A"))).isFalse();
		assertThat(builder.addCommand(new CommandClass("B", "className.A"))).isTrue();
	}

	@Test
	public void testAddMandatoryDependency() {
		assertThat(builder.addCommand(new CommandClass("Source", "className.Source"))).isTrue();
		assertThat(builder.addCommand(new CommandClass("Target", "className.Target"))).isTrue();
		assertThat(builder.addMandatoryDependency("Source", "Target")).isTrue();

		assertThat(builder.build().getDependencies("Source")).containsOnly(
				new CommandClass("Target", "className.Target"));
	}

	@Test
	public void testAddMandatoryDependency_circularDependency() {
		assertThat(builder.addCommand(new CommandClass("source", "className.source"))).isTrue();
		assertThat(builder.addCommand(new CommandClass("target", "className.target"))).isTrue();
		assertThat(builder.addMandatoryDependency("source", "target")).isTrue();
		assertThat(builder.addMandatoryDependency("target", "source")).isFalse();
	}

	@Test
	public void testAddMandatoryDependency_dependencyWithoutCommand() {
		assertThat(builder.addCommand(new CommandClass("source", "className.source"))).isTrue();
		assertThat(builder.addMandatoryDependency("source", "nowhere")).isFalse();
		assertThat(builder.addMandatoryDependency("nowhere", "source")).isFalse();
	}

	@Test
	public void testAddMandatoryDependency_sameDependencyTwice() {
		assertThat(builder.addCommand(new CommandClass("source", "className.source"))).isTrue();
		assertThat(builder.addCommand(new CommandClass("target", "className.target"))).isTrue();
		assertThat(builder.addMandatoryDependency("source", "target")).isTrue();
		assertThat(builder.addMandatoryDependency("source", "target")).isFalse();
	}

	@Test
	public void testHasCommand() {
		builder.addCommand(new CommandClass("A", "className.A"));
		CommandGraph graph = builder.build();
		assertThat(graph.hasCommand("A")).isTrue();
		assertThat(graph.hasCommand("not there")).isFalse();
	}

}
