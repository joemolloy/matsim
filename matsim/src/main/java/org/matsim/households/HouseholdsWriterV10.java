/* *********************************************************************** *
 * project: org.matsim.*
 * HouseholdsWriterV1
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.households;

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.collections.Tuple;
import org.matsim.core.utils.io.MatsimXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;


/**
 * @author dgrether
 *
 */
public class HouseholdsWriterV10 extends MatsimXmlWriter {

	private List<Tuple<String, String>> atts = new ArrayList<Tuple<String, String>>();
	private Households households;

	public HouseholdsWriterV10(Households households) {
		this.households = households;
	}

	public void writeFile(String filename) throws UncheckedIOException {
		this.openFile(filename);
		this.writeXmlHead();
		this.writeHouseholds(this.households);
		this.close();
	}

	private void writeHouseholds(Households basicHouseholds) throws UncheckedIOException {
		atts.clear();
		atts.add(this.createTuple(XMLNS, MatsimXmlWriter.MATSIM_NAMESPACE));
		atts.add(this.createTuple(XMLNS + ":xsi", DEFAULTSCHEMANAMESPACELOCATION));
		atts.add(this.createTuple("xsi:schemaLocation", MATSIM_NAMESPACE + " " + DEFAULT_DTD_LOCATION + "households_v1.0.xsd"));
		this.writeStartTag(HouseholdsSchemaV10Names.HOUSEHOLDS, atts);
		for (Household h : basicHouseholds.getHouseholds().values()) {
			this.writeHousehold(h);
		}
		this.writeEndTag(HouseholdsSchemaV10Names.HOUSEHOLDS);
	}

	private void writeHousehold(Household h) throws UncheckedIOException {
		this.atts.clear();
		atts.add(this.createTuple(HouseholdsSchemaV10Names.ID, h.getId().toString()));
		this.writeStartTag(HouseholdsSchemaV10Names.HOUSEHOLD, atts);
		if ((h.getMemberIds() != null) && !h.getMemberIds().isEmpty()){
			this.writeMembers(h.getMemberIds());
		}
		if ((h.getVehicleIds() != null) && !h.getVehicleIds().isEmpty()) {
			this.writeStartTag(HouseholdsSchemaV10Names.VEHICLES, null);
			for (Id id : h.getVehicleIds()){
				atts.clear();
				atts.add(this.createTuple(HouseholdsSchemaV10Names.REFID, id.toString()));
				this.writeStartTag(HouseholdsSchemaV10Names.VEHICLEDEFINITIONID, atts, true);
			}
			this.writeEndTag(HouseholdsSchemaV10Names.VEHICLES);
		}
		if (h.getIncome() != null){
			this.writeIncome(h.getIncome());
		}
		this.writeEndTag(HouseholdsSchemaV10Names.HOUSEHOLD);
	}

	private void writeIncome(Income income) throws UncheckedIOException {
		atts.clear();
		if (income.getCurrency() != null) {
			atts.add(this.createTuple(HouseholdsSchemaV10Names.CURRENCY,income.getCurrency()));
		}
		atts.add(this.createTuple(HouseholdsSchemaV10Names.PERIOD, income.getIncomePeriod().toString()));
		this.writeStartTag(HouseholdsSchemaV10Names.INCOME, atts);
		this.writeContent(Double.toString(income.getIncome()), true);
		this.writeEndTag(HouseholdsSchemaV10Names.INCOME);
	}

	private void writeMembers(List<Id> memberIds) throws UncheckedIOException {
		this.writeStartTag(HouseholdsSchemaV10Names.MEMBERS, null);
		for (Id id : memberIds){
			atts.clear();
			atts.add(this.createTuple(HouseholdsSchemaV10Names.REFID, id.toString()));
			this.writeStartTag(HouseholdsSchemaV10Names.PERSONID, atts, true);
		}
		this.writeEndTag(HouseholdsSchemaV10Names.MEMBERS);
	}




}
