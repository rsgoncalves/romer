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

/**
 * @author Rafael S. Goncalves
 * Information Management Group (IMG)
 * School of Computer Science
 * University of Manchester
 */
public class ConsistencyResult {
	private boolean cons;
	private double consTime;
	
	/**
	 * Constructor
	 * @param cons	True if consistent
	 * @param consTime	Consistency checking time (in seconds)
	 */
	public ConsistencyResult(boolean cons, double consTime) {
		this.cons = cons;
		this.consTime = consTime;
	}
	
	
	/**
	 * @return true if consistent, false otherwise
	 */
	public boolean isConsistent() {
		return cons;
	}
	
	
	/**
	 * @return consistency checking time (in seconds)
	 */
	public double getTime() {
		return consTime;
	}
}
