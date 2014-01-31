/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.examples.onetaxi;

import org.matsim.contrib.dvrp.passenger.*;
import org.matsim.contrib.dvrp.schedule.*;
import org.matsim.contrib.dvrp.vrpagent.*;
import org.matsim.contrib.dynagent.DynAction;


public class OneTaxiActionCreator
    implements VrpAgentLogic.DynActionCreator
{
    private final PassengerEngine passengerEngine;


    public OneTaxiActionCreator(PassengerEngine passengerEngine)
    {
        this.passengerEngine = passengerEngine;
    }


    @Override
    public DynAction createAction(final Task task, double now)
    {
        switch (task.getType()) {
            case DRIVE:
                return VrpDynLegs.createLegWithOfflineTracker((DriveTask)task);

            case STAY:
                if (task instanceof OneTaxiServeTask) { //PICKUP or DROPOFF
                    final OneTaxiServeTask serveTask = (OneTaxiServeTask)task;
                    final OneTaxiRequest request = serveTask.getRequest();

                    if (serveTask.isPickup()) {
                        return new SinglePassengerPickupActivity(passengerEngine, serveTask,
                                request, OneTaxiOptimizer.PICKUP_DURATION);
                    }
                    else {
                        return new SinglePassengerDropoffActivity(passengerEngine, serveTask,
                                request);
                    }
                }
                else { //WAIT
                    return new VrpActivity("StayTask", (StayTask)task);
                }
        }

        throw new RuntimeException();
    }
}
