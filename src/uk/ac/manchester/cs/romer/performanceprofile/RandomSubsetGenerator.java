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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLLogicalAxiom;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 * <p>
 * <code>RandomSubsetGenerator</code> splits the ontology into a specified number of partitions randomly. It will name and serialize the 
 * partitions according to the number of runs (specified by <code>PerformanceProfiler</code>). 
 * </p>
 */
public class RandomSubsetGenerator {
	private OWLOntology ont;
	private OWLOntologyManager man;
	private String outputDir;
	private int runNr;

	/**
	 * Constructor
	 * @param ont	OWLOntology
	 * @param outputDir	Output directory
	 * @throws IOException
	 */
	public RandomSubsetGenerator(OWLOntology ont, int runNr, String outputDir) {
		this.ont = ont;
		if(!outputDir.endsWith(File.separator)) outputDir += File.separator;
		this.outputDir = outputDir;
		this.runNr = runNr;
		this.man = ont.getOWLOntologyManager();
	}


	/**
	 * Divide ontology into specified number of partitions
	 * @param nrPartitions	Number of ontology partitions
	 * @return List of partitions (ontologies) as increments 
	 * @throws OWLOntologyCreationException
	 * @throws IOException 
	 * @throws OWLOntologyStorageException 
	 */
	public LinkedHashMap<String, OWLOntology> getPartitions(int nrPartitions) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		LinkedHashMap<String,OWLOntology> map = new LinkedHashMap<String,OWLOntology>();
		Set<OWLLogicalAxiom> axioms = ont.getLogicalAxioms();
		double pSize = (double)axioms.size()/(double)nrPartitions;
		List<OWLAxiom> axList = new ArrayList<OWLAxiom>(axioms);
		Collections.shuffle(axList);

		Set<OWLAxiom> lastSet = new HashSet<OWLAxiom>();
		for(int index = 1; index <= nrPartitions-1; index++) {
			String id = UUID.randomUUID().toString();
			int count = index + 1;  
			int fromIndex = (int) Math.max(Math.round((count-1)*pSize), 0);
			int toIndex = (int) Math.min(Math.round(count*pSize), axioms.size());

			Set<OWLAxiom> subset = new HashSet<OWLAxiom>(axList.subList(fromIndex, toIndex));
			lastSet.addAll(subset);
			String outFile = savePartition(lastSet, runNr, index, id);
			map.put(outFile, man.createOntology(lastSet));
		}
		return map;
	}


	/**
	 * Serialize set of axioms
	 * @param axioms	Set of OWL axioms
	 * @param runNr	Number of run
	 * @param partitionNr	Number of partition being saved
	 * @param id	Identifier of partition
	 * @return File path to ontology partition
	 */
	private String savePartition(Set<OWLAxiom> axioms, int runNr, int partitionNr, String id) throws OWLOntologyCreationException, OWLOntologyStorageException, IOException {
		OWLOntology ont = man.createOntology(axioms);
		String outputFile = outputDir + "/" + runNr + "/" + partitionNr + "_" + id + ".owl";
		man.saveOntology(ont, IRI.create("file:" + outputFile));
		return outputFile;
	}
}
