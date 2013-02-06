/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.ikaddoura.utils.pt;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.Time;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Creates a schedule without departures from a corridor network (one transit line, two transit routes (one for each direction), a transit stop on the toNode of each link).
 * 
 * @author ikaddoura
 *
 */
public class ScheduleFromCorridor {
		
	private final static Logger log = Logger.getLogger(ScheduleFromCorridor.class);

	// standard IDs
	private Id transitLineId = new IdImpl("transitLine");
	private Id routeId1 = new IdImpl("route_1");
	private Id routeId2 = new IdImpl("route_2");
	
	private double routeTravelTime;
	private Map<Id, TransitRoute> routeId2transitRoute;
	private Network network;
	
	private TransitScheduleFactory sf = new TransitScheduleFactoryImpl();
	private final TransitSchedule transitSchedule = sf.createTransitSchedule();

	public ScheduleFromCorridor(String networkFile) {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(networkFile);
		this.network = scenario.getNetwork();
	}

	public ScheduleFromCorridor(Network network) {
		this.network = network;
	}
	
	public TransitSchedule getTransitSchedule() {
		return this.transitSchedule;
	}

	/**
	 * Creates the transit schedule without departures. Creates a transit line for a simple corridor network.
	 * The transitRouteMode indicates on which link a transit stop has to be added. The schedule speed and scheduled stop time are used for calculating arrival and departure offsets
	 * based on the length of the corridor links.
	 *
	 */
	public void createTransitSchedule(String transitRouteMode, boolean isBlocking, boolean awaitDeparture, double scheduleSpeed_m_sec, double stopTime_sec) {
				
		Map<Id,List<Id>> routeID2linkIDs = getIDs(this.network, transitRouteMode);
		Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = getStopLinkIDs(this.network, routeID2linkIDs, isBlocking);
		Map<Id, NetworkRoute> routeId2networkRoute = getRouteId2NetworkRoute(routeID2linkIDs);
		Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops = getRouteId2TransitRouteStops(stopTime_sec, scheduleSpeed_m_sec, awaitDeparture, network, routeId2transitStopFacilities);

		setRouteId2TransitRoute(transitRouteMode, routeId2networkRoute, routeId2TransitRouteStops);
		setTransitLine(this.routeId2transitRoute);
				
		int lastStop = this.routeId2transitRoute.get(routeId1).getStops().size()-1;
		this.routeTravelTime = this.routeId2transitRoute.get(routeId1).getStops().get(lastStop).getArrivalOffset();
		log.info("RouteTravelTime: "+ Time.writeTime(routeTravelTime, Time.TIMEFORMAT_HHMMSS));
	}

	private Map<Id,List<Id>> getIDs(Network network, String transitRouteMode) {
		List <Link> busLinks = new ArrayList<Link>();
		Map<Id, List<Id>> routeID2linkIDs = new HashMap<Id, List<Id>>();
		List<Id> linkIDsRoute1 = new LinkedList<Id>();
		List<Id> linkIDsRoute2 = new LinkedList<Id>();
		
		for (Link link : network.getLinks().values()){
			if (link.getAllowedModes().contains(transitRouteMode)){
				busLinks.add(link);
			}
		}
		
		if (busLinks.isEmpty()) throw new RuntimeException("No links found. Allowed link modes have to contain [" + transitRouteMode + "] in order to create the transit stops. Aborting...");
		
		// one direction
		int fromNodeIdRoute1 = 0;
		int toNodeIdRoute1 = 0;
		for (int ii = 0; ii <= busLinks.size(); ii++){
			fromNodeIdRoute1 = ii;
			toNodeIdRoute1 = ii + 1;
			for (Link link : busLinks){
				if (Integer.parseInt(link.getFromNode().getId().toString()) == fromNodeIdRoute1 && Integer.parseInt(link.getToNode().getId().toString()) == toNodeIdRoute1){			
					linkIDsRoute1.add(link.getId());
				}
			}
		}
		// other direction
		int fromNodeIdRoute2 = 0;
		int toNodeIdRoute2 = 0;
		for (int ii = 0; ii <= busLinks.size(); ii++){
			fromNodeIdRoute2 = ii;
			toNodeIdRoute2 = ii - 1;
			for (Link link : busLinks){
				if (Integer.parseInt(link.getFromNode().getId().toString())==fromNodeIdRoute2 && Integer.parseInt(link.getToNode().getId().toString())==toNodeIdRoute2){			
					linkIDsRoute2.add(link.getId());
				}
			}
		}

		List<Id> linkIDsRoute2rightOrder = turnArround(linkIDsRoute2);

		linkIDsRoute1.add(0, linkIDsRoute2rightOrder.get(linkIDsRoute2rightOrder.size()-1));
		linkIDsRoute2rightOrder.add(0, linkIDsRoute1.get(linkIDsRoute1.size()-1));
		routeID2linkIDs.put(routeId1, linkIDsRoute1);
		routeID2linkIDs.put(routeId2, linkIDsRoute2rightOrder);
		return routeID2linkIDs;
	}

	private Map<Id,List<TransitStopFacility>> getStopLinkIDs(Network network, Map<Id, List<Id>> routeID2linkIDs, boolean isBlocking) {
		Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities = new HashMap<Id, List<TransitStopFacility>>();
			
		for (Id routeID : routeID2linkIDs.keySet()){
			List<TransitStopFacility> stopFacilitiesRoute = new ArrayList<TransitStopFacility>();

			for (Id linkID : routeID2linkIDs.get(routeID)){				
				if (transitSchedule.getFacilities().containsKey(linkID)){
					TransitStopFacility transitStopFacility = transitSchedule.getFacilities().get(linkID);
					stopFacilitiesRoute.add(transitStopFacility);
				}
				else {
					TransitStopFacility transitStopFacility = sf.createTransitStopFacility(linkID, network.getLinks().get(linkID).getToNode().getCoord(), isBlocking);
					transitStopFacility.setLinkId(linkID);
					stopFacilitiesRoute.add(transitStopFacility);
					transitSchedule.addStopFacility(transitStopFacility);
				}
			}	
			routeId2transitStopFacilities.put(routeID, stopFacilitiesRoute);
		}
		return routeId2transitStopFacilities;
	}
	
	private Map<Id, NetworkRoute> getRouteId2NetworkRoute(Map<Id, List<Id>> routeID2linkIDs) {
		Map<Id, NetworkRoute> routeId2NetworkRoute = new HashMap<Id, NetworkRoute>();
		for (Id routeId : routeID2linkIDs.keySet()){
			NetworkRoute netRoute = new LinkNetworkRouteImpl(routeID2linkIDs.get(routeId).get(0), routeID2linkIDs.get(routeId).get(routeID2linkIDs.get(routeId).size()-1));	// Start-Link, End-Link	
			netRoute.setLinkIds(routeID2linkIDs.get(routeId).get(0), getMiddleRouteLinkIDs(routeID2linkIDs.get(routeId)), routeID2linkIDs.get(routeId).get(routeID2linkIDs.get(routeId).size()-1)); // Start-link, link-Ids als List, End-link
			routeId2NetworkRoute.put(routeId, netRoute);
		}
		return routeId2NetworkRoute;
	}

	private Map<Id, List<TransitRouteStop>> getRouteId2TransitRouteStops(double stopTime, double scheduleSpeed_m_sec, boolean awaitDeparture, Network network, Map<Id, List<TransitStopFacility>> routeId2transitStopFacilities) {

		Map<Id, List<TransitRouteStop>> routeId2transitRouteStops = new HashMap<Id, List<TransitRouteStop>>();
		
		for (Id routeId : routeId2transitStopFacilities.keySet()){
			double arrivalTime = 0;
			double departureTime = arrivalTime + stopTime;
			List<TransitRouteStop> transitRouteStops = new ArrayList<TransitRouteStop>();
			List<TransitStopFacility> transitStopFacilities = routeId2transitStopFacilities.get(routeId);

			int ii = 0;
			double travelTimeBus = 0;
			for (TransitStopFacility transitStopFacility : transitStopFacilities){
				
				TransitRouteStop transitRouteStop = sf.createTransitRouteStop(transitStopFacility, arrivalTime, departureTime);
				transitRouteStop.setAwaitDepartureTime(awaitDeparture);
				transitRouteStops.add(transitRouteStop);
				
				if (ii==transitStopFacilities.size()-1){
				} else {

//					travelTimeBus = this.network.getLinks().get(transitStopFacilities.get(ii).getId()).getLength() / this.network.getLinks().get(transitStopFacilities.get(ii).getId()).getFreespeed();

					travelTimeBus = network.getLinks().get(transitStopFacilities.get(ii).getId()).getLength() / scheduleSpeed_m_sec;
					if (scheduleSpeed_m_sec > network.getLinks().get(transitStopFacilities.get(ii).getId()).getFreespeed()){
						log.warn("The free speed of a network link is less than the scheduled speed. This will cause schedule delays.");
					}
				}
				
				arrivalTime = departureTime + travelTimeBus;
				departureTime = arrivalTime + stopTime;	
				ii++;
			}
		routeId2transitRouteStops.put(routeId, transitRouteStops);
		}
		return routeId2transitRouteStops;
	}

	
	private void setRouteId2TransitRoute(String transitRouteMode, Map<Id, NetworkRoute> routeId2networkRoute, Map<Id, List<TransitRouteStop>> routeId2TransitRouteStops) {
		
		this.routeId2transitRoute = new HashMap<Id, TransitRoute>();			
		for (Id routeId : routeId2networkRoute.keySet()){
			TransitRoute transitRoute = sf.createTransitRoute(routeId, routeId2networkRoute.get(routeId), routeId2TransitRouteStops.get(routeId), transitRouteMode);
			routeId2transitRoute.put(routeId, transitRoute);
		}
	}
	
	private void setTransitLine(Map<Id, TransitRoute> routeId2transitRoute) {
		
		TransitLine transitLine = sf.createTransitLine(this.transitLineId);
		transitLine.addRoute(routeId2transitRoute.get(this.routeId1));
		transitLine.addRoute(routeId2transitRoute.get(this.routeId2));
		transitSchedule.addTransitLine(transitLine);
	}
	
	private List<Id> turnArround(List<Id> myList) {
		List<Id> turnedArroundList = new ArrayList<Id>();
		for (int n = (myList.size() - 1); n >= 0; n = n - 1){
			turnedArroundList.add(myList.get(n));
		}
		return turnedArroundList;
	}
	
	private List<Id> getMiddleRouteLinkIDs(List<Id> linkIDsRoute) {
		List<Id> routeLinkIDs = new ArrayList<Id>();
		int nr = 0;
		for(Id id : linkIDsRoute){
			if (nr >= 1 & nr <= (linkIDsRoute.size() - 2)){ // links between startLink and endLink
				routeLinkIDs.add(id);
			}
			nr++;
		}
		return routeLinkIDs;
	}

	public void setTransitLineId(Id transitLineId) {
		this.transitLineId = transitLineId;
	}

	public void setRouteId1(Id routeId1) {
		this.routeId1 = routeId1;
	}

	public void setRouteId2(Id routeId2) {
		this.routeId2 = routeId2;
	}

}
