package cc.commandmanager.main;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.chain.Catalog;
import org.apache.commons.chain.Command;
import org.apache.commons.chain.config.ConfigParser;
import org.apache.commons.chain.impl.CatalogFactoryBase;
import org.apache.log4j.FileAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;

import com.google.common.annotations.VisibleForTesting;

/**
 * This class is used for the controlled execution of commands. Commands to be
 * executed are declared in an catalog. Those commands will be ordered and then
 * executed.
 * <p>
 * This class executes specified initial commands needed for further tasks.
 * Arguments will be parsed from command line and may then be accessed.
 * 
 * @author Sebastian Baer
 * 
 */
public class ChainManagement {
    @VisibleForTesting
    public Catalog catalog;
    private final CommunicationContext communicationContext;
    private DependencyCollector dependencyCollector;
    private static Logger logger = Logger.getRootLogger();

    /**
     * Keys {@code path_logFile} and {@code path_dotFile} will be set to
     * defaults. Defaults are {@code path_logFile = logs/Preprocessing.log}
     * respectively {@code path_dotFile =
     * etc/graph.dot}.
     */
    public ChainManagement() {
	this(new CommunicationContext());
    }

    /**
     * @param context
     *            will be checked for keys {@code path_logFile} and key
     *            {@code path_dotFile}. If not found they will be set to
     *            defaults. Defaults are
     *            {@code path_logFile = logs/Preprocessing.log} respectively
     *            {@code path_dotFile = etc/graph.dot}.
     */
    public ChainManagement(CommunicationContext context) {
	this.communicationContext = context;
	context = ensurePreconditions(context);
    }

    private CommunicationContext ensurePreconditions(CommunicationContext context) {
	if (!context.containsKey("path_logFile") || context.get("path_logFile") == null) {
	    context.put("path_logFile", "logs/Preprocessing.log");
	}
	if (!context.containsKey("path_dotFile") || context.get("path_dotFile") == null) {
	    context.put("path_dotFile", "etc/graph.dot");
	}
	return context;
    }

    /**
     * This method takes a location to retrieve a catalog. If there is a valid
     * catalog at the given location, it will set the global catalog variable in
     * this class.
     * 
     * @param catalogLocation
     * @throws CatalogNotInstantiableException
     *             if problems occur while translating the catalog file at the
     *             specified location
     */
    public void setCatalog(String catalogLocation) {
	ConfigParser configParser = new ConfigParser();

	try {
	    logger.info("this.getClass().getResource(catalogLocation)" + this.getClass().getResource(catalogLocation));

	    configParser.parse(this.getClass().getResource(catalogLocation));
	    this.catalog = CatalogFactoryBase.getInstance().getCatalog();

	} catch (Exception e) { // Exception type cannot be more specified, due
				// to parse()-signature
	    logger.error("There is no valid catalog at the given path: " + catalogLocation, e);
	    throw new CatalogNotInstantiableException();
	}
    }

/**
	 * Executes the commands, needed for initialization. It contains commands
	 * that should be executed before other commands or tasks. Information is
	 * saved in the databaseContext.
	 * 
	 * @throws RuntimeException
	 *             if one of command, needed for initialization, throws a
	 *             {@link RuntimeException
	 * @throws IOException
	 *             if there are problems handling the file
	 *             'logs/Preprocessing.log'
	 */
    public void init() {
	try {
	    initializeLogger("logs/Preprocessing.log");
	    Command propertiesCommand = new cc.topicexplorer.chain.commands.PropertiesCommand();
	    Command dbConnectionCommand = new cc.topicexplorer.chain.commands.DbConnectionCommand();

	    propertiesCommand.execute(this.communicationContext);
	    dbConnectionCommand.execute(this.communicationContext);
	} catch (RuntimeException e1) {
	    logger.error("Initialization abborted, due to a critical exception");
	    throw e1;
	} catch (Exception e2) {// Exception type cannot be more specified, due
				// to Command signature
	    logger.warn("Initialization caused a non critical exception", e2);
	}
    }

    private void initializeLogger(String logfileName) {
	try {
	    logger.addAppender(new FileAppender(new PatternLayout("%d-%p-%C-%M-%m%n"), logfileName, false));
	    logger.setLevel(Level.INFO); // ALL | DEBUG | INFO | WARN | ERROR |
					 // FATAL | OFF:
	} catch (IOException e) {
	    logger.error("FileAppender with log file " + logfileName + " could not be constructed.");
	    throw new RuntimeException(e);
	}
    }

    public Map<String, Set<String>> getDependencies() {
	return this.dependencyCollector.getDependencies();
    }

    /**
     * Returns a {@linkplain List<String>} with all commands of a given map of
     * dependencies in an ordered sequence.
     * 
     * @return An ordered {@linkplain List<String>} containing the commands of
     *         the catalog.
     */
    public List<String> getOrderedCommands() {
	return getOrderedCommands(new HashSet<String>(), new HashSet<String>());
    }

    /**
     * Returns a {@linkplain List<String>} with all commands of a given map of
     * dependencies in an ordered sequence.
     */
    public List<String> getOrderedCommands(Map<String, Set<String>> dependencies) {
	return getOrderedCommands(dependencies, new HashSet<String>(), new HashSet<String>());
    }

    /**
     * Returns a {@linkplain List<String>} with all commands of a given map of
     * dependencies in an ordered sequence.
     */
    public List<String> getOrderedCommands(Set<String> startCommands, Set<String> endCommands) {
	this.dependencyCollector = new DependencyCollector(this.catalog);

	Map<String, Set<String>> dependencies = getDependencies();

	Map<String, Set<String>> strongComponents = this.dependencyCollector.getStrongComponents(dependencies,
		startCommands, endCommands);

	return this.dependencyCollector.orderCommands(strongComponents);
    }

    public List<String> getOrderedCommands(Map<String, Set<String>> dependencies, Set<String> startCommands,
	    Set<String> endCommands) {

	this.dependencyCollector = new DependencyCollector();

	dependencies = this.dependencyCollector.getStrongComponents(dependencies, startCommands, endCommands);

	return this.dependencyCollector.orderCommands(dependencies);
    }

    /**
     * Takes a {@linkplain List} of commands and executes them in the list's
     * sequence
     */
    public void executeCommands(List<String> commands) {
	this.executeCommands(commands, this.communicationContext);
    }

    /**
     * Takes a {@linkplain List} of commands and executes them in the list's
     * sequence, using the specified {@linkplain CommunicationContext}
     */
    public void executeCommands(List<String> commands, CommunicationContext localCommunicationContext) {
	for (String commandName : commands) {
	    try {
		Command command;
		command = this.catalog.getCommand(commandName);
		command.execute(localCommunicationContext);
	    } catch (RuntimeException e1) {
		logger.error(String.format("The current command %s caused a critical exception", commandName));
		throw e1;
	    } catch (Exception e2) {// Exception type cannot be more specified,
				    // due to Command-signature
		logger.warn(String.format("The current command %s caused a non critical exception.", commandName), e2);
	    }
	}
    }

    public CommunicationContext getCommunicationContext() {
	return this.communicationContext;
    }

    public static void main(String[] args) {
	ChainManagement chainManager = new ChainManagement();
	cc.topicexplorer.chain.ChainCommandLineParser commandLineParser;

	try {
	    commandLineParser = new cc.topicexplorer.chain.ChainCommandLineParser(args);
	} catch (RuntimeException e) {
	    logger.error("Problems occured while parsing the command line tokens.");
	    throw e;
	}

	List<String> orderedCommands;
	String catalogLocation;
	chainManager.init();

	catalogLocation = commandLineParser.getCatalogLocation();
	chainManager.setCatalog(catalogLocation);
	orderedCommands = chainManager.getOrderedCommands(commandLineParser.getStartCommands(),
		commandLineParser.getEndCommands());

	logger.info("ordered commands: " + orderedCommands);
	if (!commandLineParser.getOnlyDrawGraph()) {
	    chainManager.executeCommands(orderedCommands);
	}
    }
}