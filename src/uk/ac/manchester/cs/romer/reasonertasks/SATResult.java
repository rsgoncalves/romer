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
package uk.ac.manchester.cs.romer.reasonertasks;

import org.semanticweb.owlapi.model.OWLClassExpression;

/**
 * @author Rafael S. Goncalves
 * Information Management Group (IMG)
 * School of Computer Science
 * University of Manchester
 */
public class SATResult {
	private OWLClassExpression c;
	private boolean sat;
	private String reasonerName;
	private Double satTime, elapsedTime;
	
	/**
	 * Constructor
	 * @param c	Concept
	 * @param sat	isSatisfiable
	 * @param satTime	SAT checking time (in seconds)
	 */
	public SATResult(OWLClassExpression c, boolean sat, Double satTime, String reasonerName) {
		this.c = c;
		this.sat = sat;
		this.satTime = satTime;
		this.reasonerName = reasonerName;
	}
	
	
	public SATResult(OWLClassExpression c, Double satTime) {
		this.c = c;
		this.satTime = satTime;
	}
	
	
	/**
	 * Get concept
	 * @return Concept
	 */
	public OWLClassExpression getConcept() {
		return c;
	}
	
	
	/**
	 * Check if concept is satisfiable
	 * @return true if satisfiable, false otherwise
	 */
	public boolean isSatisfiable() {
		return sat;
	}
	
	
	/**
	 * Get SAT test time
	 * @return SAT test time (in seconds)
	 */
	public Double getSatTestTime() {
		return satTime;
	}
	
	
	/**
	 * Set elapsed time until getting a SAT result
	 * @param t	Time elapsed (in seconds)
	 */
	public void setElapsedTime(Double t) {
		elapsedTime = t;
	}
	
	
	/**
	 * Get elapsed time (in seconds)
	 * @return Elapsed time (in seconds) until getting a SAT result
	 */
	public Double getElapsedTime() {
		return elapsedTime;
	}
	
	
	/**
	 * Get reasoner used
	 * @return Reasoner used
	 */
	public String getReasonerUsed() {
		return reasonerName;
	}
}