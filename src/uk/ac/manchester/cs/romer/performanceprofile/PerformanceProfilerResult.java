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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Rafael S. Goncalves <br/>
 * Information Management Group (IMG) <br/>
 * School of Computer Science <br/>
 * University of Manchester <br/>
 */
public class PerformanceProfilerResult {
	private List<List<PartitionBenchmarkResult>> list;
	private List<Double> timesToSize;
	
	/**
	 * Constructor
	 * @param list	Results list
	 */
	public PerformanceProfilerResult(List<List<PartitionBenchmarkResult>> list) {
		this.list = list;
	}
	
	
//	/**
//	 * Get median time to size ratio of all partitions throughout all runs
//	 * @return Median time to size ratio throughout all runs and partitions 
//	 */
//	public Double getMedianTimeToSizeRatio() {
//		if(timesToSize == null) getAllTimeToSizeRatios();
//		return median(timesToSize.toArray(new Double[timesToSize.size()]));
//	}
//	
//	
//	/**
//	 * Get difference between maximum and minimum time to size ratios
//	 * @return Difference between maximum and minimum time to size ratios
//	 */
//	public Double getDiffMaxAndMin() {
//		if(timesToSize == null) getAllTimeToSizeRatios();
//		Double[] array = timesToSize.toArray(new Double[timesToSize.size()]);
//		return max(array)-min(array);
//	}
	
	
	public Double getStandardDeviationOfPartition(int partitionNr) {
		List<Double> d = getTimeToSizeRatiosOfPartition(partitionNr);
		return getStandardDeviation(d.toArray(new Double[d.size()]));
	}
	
	
	/**
	 * Get the standard deviation of classification time to size ratios of all
	 * partitions in the specified run
	 * @param runNr	Number of profiling run
	 * @return Standard deviation of a specified run
	 */
	public Double getStandardDeviationOfRun(int runNr) {
		List<Double> rList = getTimeToSizeRationsOfRun(runNr);
		return getStandardDeviation(rList.toArray(new Double[rList.size()]));
	}
	
	
	/**
	 * Get the time to size ratio of all partitions throughout all runs
	 * @return List of time to size ratios of all partitions throughout all runs
	 */
	public List<Double> getAllTimeToSizeRatios() {
		timesToSize = new ArrayList<Double>(); 
		for(int i = 0; i < list.size(); i++) {
			for(PartitionBenchmarkResult p : list.get(i)) {
				timesToSize.add( p.getClassificationTime()/p.getSize() );
			}
		}
		return timesToSize;
	}
	
	
	/**
	 * Get the time to size ratios of a specified partition across all runs
	 * @param partitionNr	Number of partition
	 * @return List of time to size ratios of specified partition across all runs
	 */
	public List<Double> getTimeToSizeRatiosOfPartition(int partitionNr) {
		List<PartitionBenchmarkResult> r = getAllResultsForPartition(partitionNr);
		List<Double> ratios = new ArrayList<Double>();
		for(PartitionBenchmarkResult p : r)
			ratios.add( p.getClassificationTime()/p.getSize() );
		return ratios;
	}
	
	
	
	/**
	 * Get the time to size ratios of all partitions in the specified run
	 * @param runNr	Number of profiling run
	 * @return List of time to size ratios of all partitions in a specified run
	 */
	public List<Double> getTimeToSizeRationsOfRun(int runNr) {
		List<Double> ratios = new ArrayList<Double>();
		for(PartitionBenchmarkResult p : list.get(runNr))
			ratios.add( p.getClassificationTime()/p.getSize() );
		return ratios;
	}
	
	
	/**
	 * Get benchmark results of a specified partition throughout all runs
	 * @param partition	Desired partition number
	 * @return List of benchmark results of a given partition over all runs
	 */
	public List<PartitionBenchmarkResult> getAllResultsForPartition(int partition) {
		List<PartitionBenchmarkResult> rList = new ArrayList<PartitionBenchmarkResult>();
		for(int i = 0; i < list.size(); i++) {
			rList.add(getResultForPartition(i, partition));
		}
		return rList;
	}
	
	
	/**
	 * Get benchmark result for a given partition, in the specified run
	 * @param runNr	Number of profiling execution run
	 * @param partition	Number of the partition
	 * @return The benchmark result for the specified partition and run 
	 */
	public PartitionBenchmarkResult getResultForPartition(int runNr, int partition) {
		List<PartitionBenchmarkResult> rList = list.get(runNr);
		return rList.get(partition);
	}
	
	
	/**
	 * Get classification time of a specified partition in a given run
	 * @param runNr	Number of profiling execution run
	 * @param partition	Number of the partition
	 * @return Classification time (in seconds) of a specified partition and run
	 */
	public Double getClassificationTimeForPartition(int runNr, int partition) {
		return getResultForPartition(runNr, partition).getClassificationTime();
	}
	
	
	/**
	 * Get size of a specified partition in a given run
	 * @param runNr	Number of profiling execution run
	 * @param partition	Number of the partition
	 * @return Size (number of logical axioms) of a specified partition and run
	 */
	public Integer getSizeOfPartition(int runNr, int partition) {
		return getResultForPartition(runNr, partition).getSize();
	}
	
	
	/**
	 * Get the list of results of a specified run
	 * @param runNr	Number of profiling execution run
	 * @return List of benchmark results of a specified run
	 */
	public List<PartitionBenchmarkResult> getResults(int runNr) {
		return list.get(runNr);
	}
	
	
	/**
	 * Get median classification time of a specified partition throughout all runs
	 * @param partition	Number of the partition
	 * @return Median classification time of specified partition throughout all runs
	 */
	public Double getMedianClassificationTimeOfPartition(int partition) {
		List<PartitionBenchmarkResult> rList = getAllResultsForPartition(partition);
		Double[] array = new Double[rList.size()];
		for(int i = 0; i < rList.size(); i++) {
			array[i] = rList.get(i).getClassificationTime();
		}
		return median(array);
	}
	

	/**
	 * Get median value of a given set of values
	 * @param m	Array of values
	 * @return Median of a given set of values
	 */
	private Double median(Double[] m) {
		Arrays.sort(m);
	    int middle = m.length/2;
	    if(m.length % 2 == 1)
	        return m[middle];
	    else
	        return (m[middle-1] + m[middle]) / 2.0;
	}
	
	
	/**
	 * Get mean of a given set of values
	 * @param m	Array of values
	 * @return Median of a given set of values
	 */
	private Double mean(Double[] m) {
		double sum = 0;
		for(int i = 0; i < m.length; i++)
			sum += m[i];
		return sum/m.length;
	}
	
	
	/**
	 * Get (population) standard deviation of a set of values
	 * @param m	Array of values
	 * @return (Population) Standard deviation of a set of values
	 */
	private Double getStandardDeviation(Double[] m) {
		double mean = mean(m);
		double sum = 0;
		for(int i = 0; i < m.length; i++)
			sum += Math.pow((m[i] - mean), 2);
		return Math.sqrt(sum/(m.length));
	}
	
	
	/**
	 * Get minimum value of a given set of values
	 * @param m	Array of values
	 * @return Minimum of a given set of values
	 */
	@SuppressWarnings("unused")
	private Double min(Double[] m) {
		Arrays.sort(m);
		return m[0];
	}
	
	
	/**
	 * Get maximum value of a given set of values
	 * @param m	Array of values
	 * @return Maximum of a given set of values
	 */
	@SuppressWarnings("unused")
	private Double max(Double[] m) {
		Arrays.sort(m);
		return m[m.length-1];
	}
	
	
	/**
	 * Get number of profiling runs executed
	 * @return Number of profiling runs executed
	 */
	public int getNumberOfRuns() {
		return list.size();
	}
}