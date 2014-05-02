package cc.commandmanager.core;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import net.sf.qualitycheck.Check;

import org.apache.commons.chain.Catalog;
import org.apache.log4j.Logger;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;

/**
 * Collects dependencies of commands mentioned in the catalog and gets them ordered.
 * <p>
 * Each map follows the semantic that a key of a map is dependent of the correspondent value (respectively dependent of
 * each element of the values ArrayList).
 */
public class DependencyCollector {

	private Catalog catalog;
	private final Logger logger = Logger.getLogger(DependencyCollector.class);

	/**
	 * Class constructor taking the catalog argument and sets it in this class.
	 *
	 * @param catalog
	 */
	public DependencyCollector(Catalog catalog) {
		this.catalog = Check.notNull(catalog, "catalog");
	}

	public DependencyCollector() {

	}

	/**
	 * Creates a file in dot format. A -> B means that A depends of B. A dashed line represents an optional dependency.
	 * It accesses the global dependency maps, so it must be executed before the maps are changed, e.g. before executing
	 * the orderCommands method because it changes the maps.
	 */
	private static void makeDotFile(Map<String, Set<String>> composedDependencies,
			Map<String, Set<String>> optionalDependencies, String name) {
		Check.notNull(composedDependencies, "composedDependencies");
		Check.notNull(optionalDependencies, "optionalDependencies");
		Check.notNull(name, "name");

		// TODO StringBuilder
		String dotContent = "digraph G { \n";
		dotContent += "rankdir = BT; \n";
		dotContent += "node [shape=record]; \n";
		dotContent += "edge [arrowhead=vee]; \n";

		for (String key : composedDependencies.keySet()) {
			if (composedDependencies.get(key).isEmpty()) {
				dotContent += key + "; \n";
			} else {
				for (String value : composedDependencies.get(key)) {
					dotContent += (key + " -> " + value + "; \n");
				}
			}
		}
		for (String key : optionalDependencies.keySet()) {
			if (!optionalDependencies.get(key).isEmpty()) {
				for (String value : optionalDependencies.get(key)) {
					dotContent += (key + " -> " + value + " [style = dotted] " + "; \n");
				}
			}
		}

		dotContent += "}";

		try {
			File dir = new File("etc");
			if (!dir.exists()) {
				dir.mkdir();
			}

			BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter("etc/graph" + name + ".dot"));
			bufferedWriter.write(dotContent);
			bufferedWriter.close();
		} catch (IOException e) {
			// TODO use logger
			e.printStackTrace();
		}
	}

	/**
	 * Checks if the name is contained as key in the dependencies map. If it is, it takes the value of the key (an
	 * arrayList) and merges it with the given beforeDependencies. After merging, the compoundBeforeList is set as new
	 * value of the key. Otherwise the name and the beforeDependencies are added as new key-value-pair to the
	 * dependencies.
	 * <p>
	 * If there is an element in the afterDependencies, each element of the list must be added to the dependencies as
	 * key with the name as correspondent value. If that pair is contained the old and the new list have to be merged,
	 * otherwise only the new key-value-pair is added.
	 *
	 * @param name
	 * @param dependencies
	 * @param afterDependencies
	 * @param beforeDependencies
	 */
	@VisibleForTesting
	static void updateDependencies(String name, Map<String, Set<String>> dependencies, Set<String> afterDependencies,
			Set<String> beforeDependencies) {
		Check.notNull(dependencies, "dependencies");
		Check.notNull(afterDependencies, "afterDependencies");
		Check.notNull(beforeDependencies, "beforeDependencies");

		if (dependencies.containsKey(name)) {
			dependencies.get(name).addAll(beforeDependencies);
		} else {
			dependencies.put(name, beforeDependencies);
		}

		if (!afterDependencies.isEmpty()) {
			for (String key : afterDependencies) {
				if (dependencies.containsKey(key)) {
					dependencies.get(key).add(name);
				} else {
					dependencies.put(key, new HashSet<String>(Arrays.asList(name)));
				}
			}
		}
	}

	private static class Dependencies {

		public Map<String, Set<String>> necessaryDependencies = new HashMap<String, Set<String>>();
		public Map<String, Set<String>> optionalDependencies = new HashMap<String, Set<String>>();

		public Dependencies(Map<String, Set<String>> necessaryDependencies,
				Map<String, Set<String>> optionalDependencies) {
			this.necessaryDependencies = necessaryDependencies;
			this.optionalDependencies = optionalDependencies;
		}
	}

	/**
	 * Every command of the catalog will be executed with the dependencyContext. Then a command should set its
	 * dependencies in the dependencyContext.
	 * <p>
	 * Then those per command set dependencies and optional dependencies will be read out and processed by the
	 * updateDependencies method.
	 */
	public Map<String, Set<String>> getDependencies() {
		@SuppressWarnings("unchecked")
		Dependencies dependencies = composeDependencies(catalog.getNames());
		Preconditions.checkNotNull(dependencies.necessaryDependencies);

		Map<String, Set<String>> necessaryDependencies = dependencies.necessaryDependencies;
		Map<String, Set<String>> optionalDependencies = dependencies.optionalDependencies;

		/*
		 * TODO Folgender Code enthält zu überarbeitende Abschnitte. Sie wurden eingefügt, um die korrekte Arbeitsweise
		 * des DependencyCollectors hinsichtlich der Ordnung optionaler Dependencies eingefügt.
		 */
		logger.info("Necessary dependencies " + necessaryDependencies);
		logger.info("Optional dependencies " + optionalDependencies);

		Map<String, Set<String>> composedDependencies = new HashMap<String, Set<String>>(necessaryDependencies);
		// start
		try {

			// ende

			for (String key : optionalDependencies.keySet()) {

				// start
				logger.info(key);
				try {
					// ende

					optionalDependencies.get(key);

					// start
				} catch (Exception e) {
					logger.info("Error 3a");
					logger.error(e.getStackTrace());
				}
				// ende

				// If anweisung als erster Fix eingefuegt. Bitte pruefen ob das
				// richtig ist.
				if (composedDependencies.containsKey(key)) {
					for (String value : optionalDependencies.get(key)) {
						if (composedDependencies.containsKey(value)) {
							// start
							try {
								// ende

								composedDependencies.get(key).add(value);
								// start
							} catch (Exception e) {
								logger.info("Error 3b");
								logger.info("key " + key);
								logger.info("value " + value);
								logger.info("containsKey " + composedDependencies.containsKey(value) + " getKey "
										+ composedDependencies.get(key));
								logger.error(e.getStackTrace());
							}
							// ende
						}
					}
				}
			}
			// start
		} catch (Exception e) {
			logger.info("Error 3");
			logger.error(e.getStackTrace());
		}

		try {
			// ende
			makeDotFile(composedDependencies, optionalDependencies, "");

		} catch (Exception e) {
			// logger.error(e.getMessage());

			// start
			logger.info("Error 4");
			logger.error(e.getStackTrace());
			// ende
		}

		return composedDependencies;
	}

	private Dependencies composeDependencies(Iterator<String> commands) {
		Preconditions.checkArgument(commands.hasNext(), "The number of commands must not be 0.");

		Map<String, Set<String>> necessaryDependencies = new HashMap<String, Set<String>>();
		Map<String, Set<String>> optionalDependencies = new HashMap<String, Set<String>>();
		DependencyContext dependencyContext = new DependencyContext();
		String command;

		while (commands.hasNext()) {
			command = commands.next();
			writeCurrentDependenciesIntoContext(command, dependencyContext);
			updateDependencies(command, necessaryDependencies, dependencyContext.getAfterDependencies(),
					dependencyContext.getBeforeDependencies());
			updateDependencies(command, optionalDependencies, dependencyContext.getOptionalAfterDependencies(),
					dependencyContext.getOptionalBeforeDependencies());
		}

		return new Dependencies(necessaryDependencies, optionalDependencies);
	}

	private void writeCurrentDependenciesIntoContext(String name, DependencyContext dependencyContext) {
		DependencyCommand command = (DependencyCommand) catalog.getCommand(name);
		command.execute(dependencyContext);
	}

	/**
	 * Topologically sorts the composedDependencies and sets the orderedCommands variable.
	 */
	public List<String> orderCommands(Map<String, Set<String>> dependencies) {
		Check.notNull(dependencies, "dependencies");

		Map<String, Set<String>> concurrentDependencies = new ConcurrentHashMap<String, Set<String>>(dependencies);
		List<String> orderedCommands = new ArrayList<String>();
		List<String> helpList = new ArrayList<String>();
		String node = "";

		// find all nodes with no dependencies, put into helpList, remove from
		// HashMap
		for (String key : concurrentDependencies.keySet()) {
			Set<String> list = concurrentDependencies.get(key);

			if (list.isEmpty()) {
				helpList.add(key);
				concurrentDependencies.remove(key);
			}
		}

		// as long as helpList contains a node without dependencies, take one,
		// remove it from helpList, put into commandList
		while (!helpList.isEmpty()) {
			node = helpList.iterator().next();
			helpList.remove(node);
			orderedCommands.add(node);

			// check if there is any edge between the node and another one
			for (String key : concurrentDependencies.keySet()) {
				Set<String> list = concurrentDependencies.get(key);

				// if the node is in a value list, remove it
				if (list.contains(node)) {
					list.remove(node);
					concurrentDependencies.put(key, list);
				}

				// if the node has no other incoming edges, put it into
				// commandList
				if (concurrentDependencies.get(key).isEmpty()) {
					helpList.add(key);
					concurrentDependencies.remove(key);
				}
			}
		}

		// only if the dependencyMap is empty the graph was correct, otherwise
		// there was something wrong with it
		if (!concurrentDependencies.isEmpty()) {
			logger.error("The dependencyMap wasn't empty yet but it should have been: " + concurrentDependencies);
			throw new IllegalStateException();
		}

		return orderedCommands;
	}

	public Map<String, Set<String>> getStrongComponents(Map<String, Set<String>> dependencies,
			Set<String> startCommands, Set<String> endCommands) {
		Check.notNull(dependencies, "dependencies");
		Check.notNull(startCommands, "startCommands");
		Check.notNull(endCommands, "endCommands");

		Map<String, Set<String>> newDependencies = new HashMap<String, Set<String>>();

		logger.info("startCommands " + startCommands + "+++");

		if (startCommands.isEmpty()) {
			newDependencies.putAll(dependencies);
		} else {
			for (String command : startCommands) {
				// pruefen, ob es sich wirklich um Wurzel handelt
				// dazu muss command als key mit leerer value-Menge vorhanden
				// sein
				if (!dependencies.get(command).isEmpty()) {
					logger.error("Given command seems not to be a root.");
					throw new IllegalStateException();
				} else {
					// fuege aktuelles Element mit leeren values hinzu
					newDependencies.put(command, new HashSet<String>());
					iterateDependenciesDown(dependencies, newDependencies, command);
				}
			}
		}

		for (String command : endCommands) {
			iterateDependenciesUp(command);
		}

		makeDotFile(newDependencies, new HashMap<String, Set<String>>(), "_strongComponents");

		return newDependencies;
	}

	private void iterateDependenciesUp(String command) {
		// TODO implement
		throw new UnsupportedOperationException();
	}

	private void iterateDependenciesDown(Map<String, Set<String>> dependencies,
			Map<String, Set<String>> newDependencies, String command) {

		// pruefe welche keys das aktuelle command in value-Liste haben, d.h.
		// welche commands von dem aktuellen abhaengen
		for (String key : dependencies.keySet()) {
			if (dependencies.get(key).contains(command)) {
				// falls enthalten, muss es in neue Map und rekursiv
				// abhanegigkeiten fuer dieses command pruefen
				Set<String> tmp = new HashSet<String>();
				tmp.add(command);
				if (newDependencies.containsKey(key)) {
					tmp.addAll(newDependencies.get(key));
				}
				newDependencies.put(key, tmp);
				iterateDependenciesDown(dependencies, newDependencies, key);
			}
		}
	}
}
