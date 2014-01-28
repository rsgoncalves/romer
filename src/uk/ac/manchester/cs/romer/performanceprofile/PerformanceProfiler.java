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
package uk.ac.manchester.cs.romer.performanceprofile;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

import uk.ac.manchester.cs.romer.Dispatcher;
import uk.ac.manchester.cs.romer.OntologyLoader;
import uk.ac.manchester.cs.romer.reasonertasks.EntailmentGenerator;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 * </p>
 * The <code>PerformanceProfiler</code> determines whether a given ontology-reasoner pair has a performance homogeneous or heterogeneous behaviour,
 * according to a given criteria. This is done by splitting the ontology into a specified number of partitions (4, by default), classifying the partitions
 * as increments (e.g., for ontology O: classify [1/4, 1/2, 3/4] of O, and then O), and finally verify whether the classification time of the partitions 
 * increase roughly linearly w.r.t. the size of the partitions.
 * </p>
 */
public class PerformanceProfiler {
	private OWLOntology ont;
	private boolean verbose;
	private String outputDir, reasonerName;
	private BufferedWriter writer;
	private int nrRuns;
	
	/**
	 * Constructor
	 * @param ont	OWLOntology
	 * @param reasonerName	Reasoner name
	 * @param outputDir	Output directory for ontology partitions and log file
	 * @param nrRuns	Number of times the ontology is split and its partitions benchmarked
	 * @param verbose	Verbose mode
	 */
	public PerformanceProfiler(OWLOntology ont, String reasonerName, String outputDir, int nrRuns, boolean verbose) {
		this.ont = ont;
		this.reasonerName = reasonerName;
		this.outputDir = outputDir;
		this.nrRuns = nrRuns;
		this.verbose = verbose;
		initWriter();
	}
	
	
	/**
	 * Initialise file writer
	 */
	private void initWriter() {
		if(!outputDir.endsWith(File.separator)) outputDir += File.separator;
		new File(outputDir).mkdirs();
		try {
			writer = new BufferedWriter(new FileWriter(new File(outputDir + "log.csv"), true));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	
	// TODO
//	public boolean isPerformanceHomogeneous(int nrPartitions) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
//		PerformanceProfilerResult r = profileOntologyReasonerPair(nrPartitions, nrRuns);
//		Double d = r.getDiffMaxAndMin();
//		if(Math.abs(d) < threshold)
//			return true;
//		else
//			return false;
//	}
	
	
	/**
	 * 
	 * @param nrPartitions
	 * @param timeout
	 * @throws OWLOntologyCreationException
	 * @throws OWLOntologyStorageException
	 * @throws IOException
	 */
	public PerformanceProfilerResult profileOntologyReasonerPair(int nrPartitions, long timeout) 
			throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		List<List<PartitionBenchmarkResult>> outList = new ArrayList<List<PartitionBenchmarkResult>>();
		writer.append(",");
		for(int j = 1; j <= nrRuns; j++) {
			if(verbose) System.out.println("---------------------------------------------\n" +
					"Run " + j + " - " + reasonerName + "\n---------------------------------------------");
			writer.append("\n" + j + ",");
			RandomSubsetGenerator gen = new RandomSubsetGenerator(ont, j, outputDir);
			LinkedHashMap<String,OWLOntology> map = gen.getPartitions(nrPartitions);
			
			List<PartitionBenchmarkResult> runResults = performRun(map, timeout);
			outList.add(runResults);
			writer.flush();
		}
		System.out.println("\nfinished");
		writer.close();
		return new PerformanceProfilerResult(outList);
	}
	
	
	/**
	 * Execute classification over each partition
	 * @param map	Map of ontology file paths to ontology objects (though the latter arent used)
	 * @param timeout	Timeout for classifiation
	 * @return List of benchmark results for all partitions
	 * @throws IOException 
	 */
	private List<PartitionBenchmarkResult> performRun(LinkedHashMap<String,OWLOntology> map, long timeout) throws IOException {
		List<PartitionBenchmarkResult> results = new ArrayList<PartitionBenchmarkResult>();
		int partitionNr = 1;
		for(String ontPath : map.keySet()) {
			if(verbose) System.out.print("   Classifying Partition " + partitionNr + "/" + map.size() + "... ");
			
			OWLOntology ont = map.get(ontPath);
			double clTime = benchmarkPartition(ontPath, partitionNr, timeout);
			
			if(verbose) System.out.println("done (" + clTime + " seconds)");
			writer.append(clTime + ",");
			
			PartitionBenchmarkResult presult = new PartitionBenchmarkResult(partitionNr, 
					ontPath.substring(ontPath.lastIndexOf("_")+1, ontPath.length()), ontPath, ont, clTime);
			results.add(presult);
			partitionNr++;
		}
		return results;
	}
	
	
	/**
	 * Benchmark classification time of the partition located in the specified path
	 * @param ontPath	File path to partition file
	 * @param partitionNr	Number of partition
	 * @param timeout	Classification timeout
	 * @return Classification time of partition
	 */
	public double benchmarkPartition(String ontPath, int partitionNr, long timeout) {
		double clTime = 0;
		boolean timedOut = false;
		try {
			ArrayList<String> args = new ArrayList<String>();
			args.add("-ont"); 		args.add(ontPath);
			args.add("-reasoner");	args.add(reasonerName);
			args.add("-cl"); args.add("-v");
			if(timeout != 0) {args.add("-t"); args.add(timeout + "");}
			
			Process p = Dispatcher.executeOperation(EntailmentGenerator.class, false, false, args);
			InputStream stdout = p.getInputStream();
	        BufferedReader reader = new BufferedReader(new InputStreamReader(stdout)); 
	        String line = reader.readLine();
	        while(line != null && !line.trim().equals("--EOF--")) {
	        	if(line.contains("Aborted")) {
	        		System.out.println(line);
	        		timedOut = true;
	        		break;
	        	}
	        	if(line.contains("Classification time"))
	        		clTime = Double.parseDouble(line.substring(line.indexOf(":")+2, line.indexOf(" seconds")));
	        	line = reader.readLine();
	        	if(line == null) break;
	        }
	        p.waitFor();
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
		}
		if(timedOut) clTime = timeout;
		return clTime;
	}
	
	
	/**
	 * @param args
	 * @throws OWLOntologyCreationException 
	 * @throws IOException 
	 * @throws OWLOntologyStorageException 
	 */
	public static void main(String[] args) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		System.out.println("Executing Performance Profiler...");
		String ontFile = null, reasonerName = null, outputDir = null;
		int nrRuns = 5, nrPartitions = 4;
		boolean verbose = false;
		long timeout = 0;
		
		for (int i = 0; i < args.length; i++) {
			String arg = args[i].trim();
			if(arg.equalsIgnoreCase("-ont"))		ontFile = args[++i].trim();
			if(arg.equalsIgnoreCase("-reasoner"))	reasonerName = args[++i].trim();
			if(arg.equalsIgnoreCase("-t"))			timeout = Long.parseLong(args[++i].trim());
			if(arg.equalsIgnoreCase("-o"))			outputDir = args[++i].trim();
			if(arg.equalsIgnoreCase("-r"))			nrRuns = Integer.parseInt(args[++i].trim());
			if(arg.equalsIgnoreCase("-l"))			nrPartitions = Integer.parseInt(args[++i].trim());
			if(arg.equalsIgnoreCase("-v"))			verbose = true;
		}
		
		if(outputDir == null && ontFile != null) outputDir = new File(ontFile).getParent() + File.separator + "output" + File.separator;
		else {
			if(!outputDir.endsWith(File.separator)) outputDir += File.separator;
			outputDir += new File(ontFile).getParentFile().getName() + File.separator;
		}
		
		if(ontFile != null && reasonerName != null) {
			OWLOntology ont = new OntologyLoader(new File(ontFile), verbose).loadOntology();
			PerformanceProfiler profiler = new PerformanceProfiler(ont, reasonerName, outputDir, nrRuns, verbose);
			profiler.profileOntologyReasonerPair(nrPartitions, timeout);
		}
		else
			throw new RuntimeException("Error: Minimum parameters are: -ont ONTOLOGY -reasoner REASONERNAME.\n" +
					"\tPlease review the usage information via the -h flag.");
	}
}
