/*******************************************************************************
 * This file is part of romer.
 * 
 * romer is distributed under the terms of the GNU Lesser General Public License (LGPL), Version 3.0.
 *  
 * Copyright 2011-2014, The University of Manchester
 *  
 * romer is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the 
 * License, or (at your option) any later version.
 *  
 * romer is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even 
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser 
 * General Public License for more details.
 *  
 * You should have received a copy of the GNU Lesser General Public License along with romer.
 * If not, see http://www.gnu.org/licenses/.
 ******************************************************************************/
package uk.ac.manchester.cs.romer;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.semanticweb.owlapi.util.VersionInfo;

import uk.ac.manchester.cs.romer.approximation.ApproximationGenerator;
import uk.ac.manchester.cs.romer.hotspots.HotspotFinder;
import uk.ac.manchester.cs.romer.performanceprofile.PerformanceProfiler;
import uk.ac.manchester.cs.romer.reasonertasks.ConsistencyTester;
import uk.ac.manchester.cs.romer.reasonertasks.EntailmentGenerator;
import uk.ac.manchester.cs.romer.reasonertasks.SATOntologyTester;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class Dispatcher {
	private static final String versionInfo = "1.0b";
	private static final String releaseDate = "13/03/2013";
	private static final String PROGRAM_TITLE = 
			"-------------------------------------------------------------------------------------\n" +
			"	     ROMeR - Robust OWL Meta Reasoner (v" + versionInfo + ", " + releaseDate + ")\n" +
			"-------------------------------------------------------------------------------------\n" +
			"by Rafael Goncalves. Copyright 2011-2013 University of Manchester\n" + 
			"powered by the OWL API version " + VersionInfo.getVersionInfo().getVersion() + "\n";
	
	
	/**
	 * Print usage message 
	 */
	public static void printUsage() {
		System.out.println("Usage:\n\tjava -jar romer.jar -ont ONTOLOGY -reasoner REASONERNAME OPERATION [OPTIONS]");
		System.out.println();
		System.out.println(" ONTOLOGY	An input ontology file");
		System.out.println(" REASONERNAME	Name of reasoner to be used [ fact | jfact | pellet | hermit | elk | jcel | snorocket | trowl ]");
		System.out.println(" OPERATION	Operation to be performed, one of the following:");
		System.out.println("   -sat		SAT Tester: Test satisfiability of all concepts");
		System.out.println("   -sat -c	SAT Tester: Test satisfiability of a concept, where -c should be followed by the concept URI or name");
		System.out.println("   -cons	Check ontology consistency");
		System.out.println("   -cl		Classify given ontology");
		System.out.println("   -rl		Realize given ontology");
		System.out.println("   -prof	Check whether ontology-reasoner pair is performance homogeneous or heterogenous (default: -r 5 -l 4)");
		System.out.println("   -hsf		Search for performance hot spots w.r.t. the given ontology-reasoner pair (default: SAT-based search)");
		System.out.println("   -approx	Approximate input ontology for faster (classification) performance w.r.t. given reasoner");
		System.out.println();
		System.out.println(" OPTIONS:");
		System.out.println("  Hotspot Finder:");
		System.out.println("   -n		Maximum number of hot spots (default: 1)");
		System.out.println("   -m		Maximum number of hot spot tests (default: 1,000)");
		System.out.println("   -s		Hotspot indicator, one of [ SAT | Random ] (default: SAT)");
		System.out.println("   -p		Hotspot search strategy, one of [ SEQ | CON ], as in Sequential or Concurrent (default: SEQ)");
		System.out.println("   -a		Hotspot candidate type, one of [ BOT | STAR | USG ], as in Bottom or Star modules, or Usage closure (default: BOT)");
		System.out.println("  Reasoning task output:");
		System.out.println("   -x		Exclude asserted axioms from entailment generation tasks");
		System.out.println("   -d		Return only direct subsumptions (transitive reduction) from classificiation");
		System.out.println("  Performance Profiler:");
		System.out.println("   -r		Set the number of runs of the Performance Profiler (default: 5)");
		System.out.println("   -l		Number of ontology partitions created in the Performance Profiler (default: 4)");
		System.out.println("  Generic:");
		System.out.println("   -t		Timeout for entire operation (in milliseconds)");
		System.out.println("   -b		Ignore Abox axioms");
		System.out.println("   -o		Output directory for logs and/or ontology files");
		System.out.println("   -v		Print detailed messages");
		System.out.println("   -h		Print this help message\n");
	}
	
//	System.out.println("   -i		Timeout for individual fine-grained operations, e.g., SAT checks");
//	System.out.println("   -f		Force each SAT check to be performed on a new reasoner instance");
	
	
	/**
	 * Execute specified operation in a sub-process 
	 * @param c	Class to be executed
	 * @param redirectIO	Redirects standard I/O to master process
	 * @param waitTermination	Wait for the executed process to finish
	 * @param args	List of additional parameters (e.g., timeouts)
	 * @return Exit value (0 normal operation)
	 * @throws IOException
	 * @throws InterruptedException
	 */
	public static Process executeOperation(Class<? extends Object> c, boolean redirectIO, boolean waitTermination, List<String> args) 
			throws IOException, InterruptedException {
		String javaHome = System.getProperty("java.home");
		String javaBin = javaHome + File.separator + "bin" + File.separator + "java";
		String classPath = System.getProperty("java.class.path");
		String className = c.getCanonicalName();
		
		String libPath = "";
		StringTokenizer parser = new StringTokenizer(classPath, ":;");
		loopArgs:
			while (parser.hasMoreTokens()) {
				String token = parser.nextToken();
				if(token.contains("lib")) {
					libPath = token.substring(0, token.lastIndexOf("lib")+3);
					break loopArgs;
				}
			}
		
		ArrayList<String> cmdArgs = new ArrayList<String>();
		cmdArgs.add(javaBin);
		cmdArgs.addAll(getRuntimeParameters());
		cmdArgs.add("-cp");
		cmdArgs.add(classPath);
		cmdArgs.add(className);
		cmdArgs.addAll(args);

		ProcessBuilder builder = new ProcessBuilder(cmdArgs);
		builder.redirectError(Redirect.INHERIT);
		builder.directory(new File(libPath));
		builder.redirectOutput(Redirect.PIPE);
		
		if(redirectIO)
			builder.redirectOutput(Redirect.INHERIT);

		Process process = builder.start();
		if(waitTermination)
			process.waitFor();

		return process;
	}
	
	
	/**
	 * Get the JVM parameters passed on to the dispatcher
	 * @return JVM parameters
	 */
	public static List<String> getRuntimeParameters() {
		RuntimeMXBean bean = ManagementFactory.getRuntimeMXBean();
		return bean.getInputArguments();
	}


	/**
	 * @param args
	 * @throws IOException 
	 * @throws InterruptedException 
	 */
	public static void main(String[] args) throws IOException, InterruptedException {
		System.out.println(PROGRAM_TITLE);
		
		String ontFile = null, reasonerName = null, outputDir = null, verbose = "false", nrHotspots = null, fork = "false",
				maxHotspotTests = null, timeout = null, opTimeout = null, nrProfilerRuns = null, indicatorStrategy = null,
				searchStrategy = null, hsType = null, excludeAsserted = "false", directOnly = "false", entOp = null, 
				ignoreAbox = "false", nrPartitions = null;
		Class<? extends Object> operation = null;
		
		for(int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			// Basic parameters
			if(arg.equalsIgnoreCase("-ont"))		ontFile = args[++i].trim();
			if(arg.equalsIgnoreCase("-reasoner"))	reasonerName = args[++i].trim();
			
			// Operations
			if(arg.equalsIgnoreCase("-hsf"))	operation = HotspotFinder.class;
			if(arg.equalsIgnoreCase("-sat"))	operation = SATOntologyTester.class;
			if(arg.equalsIgnoreCase("-cons"))	operation = ConsistencyTester.class;
			if(arg.equalsIgnoreCase("-cl"))		{operation = EntailmentGenerator.class; entOp = "-cl";}
			if(arg.equalsIgnoreCase("-rl"))		{operation = EntailmentGenerator.class; entOp = "-rl";}
			if(arg.equalsIgnoreCase("-approx"))	operation = ApproximationGenerator.class;
			if(arg.equalsIgnoreCase("-prof"))	operation = PerformanceProfiler.class;

			/* OPTIONS */
			
			// if(arg.equalsIgnoreCase("-f"))	fork = "true";
			
			// Reasoning task output
			if(arg.equalsIgnoreCase("-x"))	excludeAsserted = "true";
			if(arg.equalsIgnoreCase("-d"))	directOnly = "true";
			
			// Hotspot Finder
			if(arg.equalsIgnoreCase("-s"))	indicatorStrategy = args[++i].trim();
			if(arg.equalsIgnoreCase("-p"))	searchStrategy = args[++i].trim();
			if(arg.equalsIgnoreCase("-a"))	hsType = args[++i].trim();
			if(arg.equalsIgnoreCase("-n"))	nrHotspots = args[++i].trim();
			if(arg.equalsIgnoreCase("-m"))	maxHotspotTests = args[++i].trim();
			
			// Performance Profiler
			if(arg.equalsIgnoreCase("-r"))	nrProfilerRuns = args[++i].trim();
			if(arg.equalsIgnoreCase("-l"))	nrPartitions = args[++i].trim();
			
			// Generic
			if(arg.equalsIgnoreCase("-t"))	timeout = args[++i].trim();
			if(arg.equalsIgnoreCase("-i"))	opTimeout = args[++i].trim();
			if(arg.equalsIgnoreCase("-o"))	outputDir = args[++i].trim();
			if(arg.equalsIgnoreCase("-b"))	ignoreAbox = "true";
			if(arg.equalsIgnoreCase("-v"))	verbose = "true";
			if(arg.equalsIgnoreCase("-h"))	{printUsage(); System.exit(0);}
		}
		
		if(ontFile != null && reasonerName != null && operation != null) {
			List<String> params = new ArrayList<String>();
			params.add("-ont"); params.add(ontFile);
			params.add("-reasoner"); params.add(reasonerName);
			
			if(entOp != null)					{params.add(entOp);}
			if(fork.equals("true"))				{params.add("-f");}
			// Hotspot Finder
			if(nrHotspots != null)				{params.add("-n"); params.add(nrHotspots);}
			if(maxHotspotTests != null)			{params.add("-m"); params.add(maxHotspotTests);}
			if(indicatorStrategy != null)		{params.add("-s"); params.add(indicatorStrategy);}
			if(searchStrategy != null)			{params.add("-p"); params.add(searchStrategy);}
			if(hsType != null)					{params.add("-a"); params.add(hsType);}
			
			// Reasoning task output
			if(excludeAsserted.equals("true"))	{params.add("-x");}
			if(directOnly.equals("true"))		{params.add("-d");}
			
			// Performance Profiler
			if(nrProfilerRuns != null)			{params.add("-r"); params.add(nrProfilerRuns);}
			if(nrPartitions != null)			{params.add("-l"); params.add(nrPartitions);}
			
			// Generic
			if(verbose.equals("true"))			{params.add("-v");}
			if(timeout != null)					{params.add("-t"); params.add(timeout);}
			if(opTimeout != null)				{params.add("-i"); params.add(opTimeout);}
			if(ignoreAbox.equals("true"))		{params.add("-b");}
			if(outputDir != null) {
				if(!outputDir.endsWith(File.separator)) outputDir += File.separator;
				params.add("-o"); params.add(outputDir);
			}
			else {
				outputDir = ontFile.substring(0, ontFile.lastIndexOf(".")) + File.separator; 
				params.add("-o"); params.add(outputDir);
			}

			Dispatcher.executeOperation(operation, true, true, params);
			System.out.println("finished");
		}
		else {
			System.out.println("Error: Minimum parameters are: -ont OntologyFilePath, -reasoner ReasonerName, " +
					"and Operation (e.g., -sat)\n");
			printUsage();
		}
	}
}