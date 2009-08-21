/* *********************************************************************** *
 * project: org.matsim.*
 * LanesGenerator
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package playground.andreas.intersection.zuerich;

import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.lanes.basic.BasicLaneDefinitionsImpl;
import org.matsim.lanes.basic.BasicLaneImpl;
import org.matsim.lanes.basic.BasicLanesToLinkAssignment;


/**
 * @author dgrether
 *
 */
public class LanesGenerator {
	
	private static final Logger log = Logger.getLogger(LanesGenerator.class);
	
	/**
	 * 
	 * @param spurSpurMapping knotennummer -> (vonspur 1->n nachspur)	
	 * @param knotenSpurLinkMapping knotennummer -> (spurnummer -> linkid)
	 * @return
	 */
	public BasicLaneDefinitions processLaneDefinitions(Map<Integer, Map<Integer, List<Integer>>> spurSpurMapping, Map<Integer, Map<Integer, String>> knotenSpurLinkMapping) {
		//create the lanes ...
		BasicLaneDefinitions laneDefs = new BasicLaneDefinitionsImpl();
		for (Integer nodeId : spurSpurMapping.keySet()) {
			//for all 
			Map<Integer,  List<Integer>> vonSpurToSpurMap = spurSpurMapping.get(nodeId);
			for (Integer fromLaneId : vonSpurToSpurMap.keySet()) {
				//create the id from ???
				String linkIdString = knotenSpurLinkMapping.get(nodeId).get(fromLaneId);
				if (!linkIdString.matches("[\\d]+")) {
					log.error("cannot create link id from string " + linkIdString + " for nodeId: " + nodeId + " and laneId " + fromLaneId);
					continue;
				}
				Id linkId = new IdImpl(linkIdString);
				BasicLanesToLinkAssignment assignment = laneDefs.getLanesToLinkAssignments().get(linkId);
				if (assignment == null){
					assignment = laneDefs.getBuilder().createLanesToLinkAssignment(linkId);
				}

				Id laneId = new IdImpl(fromLaneId.intValue());
				BasicLaneImpl lane = new BasicLaneImpl(laneId);
				lane.setLength(45.0);
				lane.setNumberOfRepresentedLanes(1);
				
				List<Integer> toLanes = vonSpurToSpurMap.get(fromLaneId);
				for (Integer toLaneId : toLanes) {
					if (!knotenSpurLinkMapping.get(nodeId).get(toLaneId).equalsIgnoreCase("-")){
						lane.addToLinkId(new IdImpl(knotenSpurLinkMapping.get(nodeId).get(toLaneId)));	
					}								
				}
				if(lane.getToLinkIds() != null){
					assignment.addLane(lane);
				}
				if (assignment.getLanesList() != null){
					if (!assignment.getLinkId().toString().equalsIgnoreCase("-")){
						laneDefs.addLanesToLinkAssignment(assignment);
					}
				}
			}			
		}
		return laneDefs;
	}

}
