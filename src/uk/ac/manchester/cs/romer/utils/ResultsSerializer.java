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
package uk.ac.manchester.cs.romer.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Set;

import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.util.SimpleShortFormProvider;

import uk.ac.manchester.cs.owl.owlapi.mansyntaxrenderer.ManchesterOWLSyntaxObjectRenderer;
import uk.ac.manchester.cs.romer.reasonertasks.ClassifierResult;
import uk.ac.manchester.cs.romer.reasonertasks.RealizerResult;
import uk.ac.manchester.cs.romer.reasonertasks.SATResult;

/**
 * @author Rafael S. Goncalves
 * Information Management Group (IMG)
 * School of Computer Science
 * University of Manchester
 */
public class ResultsSerializer {
	private BufferedWriter out;
	private String outputDir, filename;

	/**
	 * Constructor
	 * @param outputDir	Output directory
	 * @param filename	Name of output file
	 */
	public ResultsSerializer(String outputDir, String filename) {
		this.outputDir = outputDir;
		this.filename = filename;
	}


	/**
	 * Serialize a single SAT result
	 * @param r	SATResult
	 * @param includeHeader	Include header labels
	 * @throws IOException 
	 */
	public void serialize(SATResult r, boolean includeHeader, boolean includeElapsedTime) throws IOException {
		if(out == null) prepOutBuffer();
		if(includeHeader) {
			if(includeElapsedTime) out.append("Elapsed Time, Concept,SAT time,isSatisfiable\n");
			else out.append("Concept,SAT time,isSatisfiable\n");
		}
		String sat = "";
		Boolean isSat = r.isSatisfiable();
		if(isSat != null)
			sat = isSat + "";
		else
			sat = "inknown";
		
		if(includeElapsedTime && r.getElapsedTime() != null)
			out.append(r.getElapsedTime() + "," + getManchesterRendering(r.getConcept()) + "," + r.getSatTestTime() + "," + sat + "\n");
		else
			out.append(getManchesterRendering(r.getConcept()) + "," + r.getSatTestTime() + "," + sat + "\n");
		out.flush();
	}


	/**
	 * Serialize a set of SAT results
	 * @param sr	Set of SATResult
	 * @param includeHeader	Include header labels
	 * @throws IOException 
	 */
	public void serialize(Set<SATResult> sr, boolean includeHeader, boolean includeElapsedTime) throws IOException {
		if(out == null) prepOutBuffer();
		if(includeHeader) {
			if(includeElapsedTime) out.append("Elapsed Time, Concept,SAT time,isSatisfiable\n");
			else out.append("Concept,SAT time,isSatisfiable\n");
		}
		for(SATResult r : sr) 
			serialize(r, false, includeElapsedTime);
	}
		
	
	/**
	 * Prepare output buffered writer
	 */
	private void prepOutBuffer() {
		if(!outputDir.endsWith(File.separator)) outputDir += File.separator;
		new File(outputDir).mkdirs();
		try {
//			File f = ;
//			f.mkdirs();
			out = new BufferedWriter(new FileWriter(new File(outputDir + filename)));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


	/**
	 * Serialize classification results
	 * @param cr	Classifier results
	 */
	public String serialize(ClassifierResult cr) {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		String name = prepOutput("_infSubs.owl");
		try {
			man.saveOntology(man.createOntology(cr.getEntailments()), new OWLXMLOntologyFormat(), IRI.create("file:" + name));
		} catch (OWLOntologyStorageException | OWLOntologyCreationException e) {
			e.printStackTrace();
		}
		return name;
	}


	/**
	 * Serialize realization results
	 * @param rr	Realizer results
	 */
	public void serialize(RealizerResult rr) {
		OWLOntologyManager man = OWLManager.createOWLOntologyManager();
		String name = prepOutput("_infAssertions.owl");
		try {
			man.saveOntology(man.createOntology(rr.getEntailments()), new OWLXMLOntologyFormat(), IRI.create(new File(name)));
		} catch (OWLOntologyStorageException | OWLOntologyCreationException e) {
			e.printStackTrace();
		}
	}
	
	
	/**
	 * Prepare output buffer
	 * @param suffix	Suffix for filename
	 */
	private String prepOutput(String suffix) {
		if(!outputDir.endsWith(File.separator))
			outputDir += File.separator;

		this.filename = filename.replaceAll(".owl", "");
		this.filename = filename.replaceAll(".xml", "");
		
		return outputDir + filename + suffix;
	}
	

	/**
	 * Get Manchester syntax of an OWL object
	 * @param obj	Instance of OWLObject
	 */
	public static String getManchesterRendering(OWLObject obj) {
		SimpleShortFormProvider fp = new SimpleShortFormProvider();
		StringWriter wr = new StringWriter();

		ManchesterOWLSyntaxObjectRenderer render = new ManchesterOWLSyntaxObjectRenderer(wr, fp);
		render.setUseWrapping(false);
		obj.accept(render);

		return wr.getBuffer().toString();
	}
}