/*
 * we-are-the-robots - a base for building any art robots - upcycling
 * useful automata into useless experience
 * Copyright (C) 2020  Kazimierz Pogoda
 *
 * This file is part of we-are-the-robots.
 *
 * we-are-the-robots is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * we-are-the-robots is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with we-are-the-robots.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.xemantic.robots.roomba.core

import com.xemantic.robots.api.*
import com.xemantic.robots.roomba.api.ChargingState
import com.xemantic.robots.roomba.api.OiMode
import com.xemantic.robots.roomba.api.RoombaStateEvents
import io.reactivex.rxjava3.core.Scheduler
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.withLatestFrom
import mu.KotlinLogging
import java.util.concurrent.TimeUnit

// if battery charge drops past certain level, it cannot provide enough
// voltage for Raspberry Pi anymore, this level should be established empirically
// also to give Roomba enough time to find the dock
const val MINIMAL_BATTERY_CHARGE = 2000

/*
 We use current to detect if Roomba is docked or not. When Roomba is undocked
 the current will be around -400, when docked -100..1000

 It should be checked if it depends on particular Roomba model!
 */
val FREE_CURRENT_RANGE = -10000..-200

class RoombaDaemon(
    serialPort: String,
    private val robotState: MutableRobotState,
    private val robotEvents: RobotStateEvents,
    private val roombaState: MutableRoombaState,
    private val roombaEvents: RoombaStateEvents,
    private val scheduler: Scheduler
) {

  private val logger = KotlinLogging.logger {}

  private val roomba = Roomba(serialPort, roombaState, scheduler)

  /**
   * Starts the daemon and blocks.
   */
  fun start() {
    logger.info("Starting Roomba Daemon")

    roomba.start()
        .doOnComplete { logger.info { "Waiting for the sensors to propagate" } }
        // we have to wait a bit until sensors are propagated
        .delay(1200, TimeUnit.MILLISECONDS, scheduler)
        .doOnComplete {
          logger.info("Roomba started, determining RobotMode")
          robotState.mode = if (
              (roombaState.chargingState == ChargingState.NOT_CHARGING)
              && (FREE_CURRENT_RANGE.contains(roombaState.current)) // TODO adjust for Pi
          ) {
            RobotMode.FREE
          } else {
            RobotMode.DOCKED
          }
          logger.info { "Determined: ${robotState.mode}" }
        }
        .subscribeOn(scheduler) // TODO it seems excessive
        .blockingAwait()

    // we are receiving valid events at this point
    logger.info("Defining standard behaviour")

    robotEvents.mode
        .filter { mode -> mode == RobotMode.DOCKING }
        .doOnNext {
          logger.info { "Docking" }
          roomba.seekDock()
        }
        .flatMapMaybe {
          Observables.combineLatest(roombaEvents.oiMode, roombaEvents.current)
              .filter { pair ->
                (pair.first == OiMode.PASSIVE) && (pair.second > 0)
              }
              .firstElement()
        }
        .subscribe {
          robotState.mode = RobotMode.DOCKED
          logger.info { "Docked" }
        }

    robotEvents.mode
        .filter { mode -> mode == RobotMode.UNDOCKING }
        .flatMap { // due to known bug - roomba does not wake up from dock, we have to restart everything
          logger.info("Undocking")
          roomba
              .stop()
              .andThen(roomba.start())
              .andThen(roombaEvents.oiMode
                  .filter { mode -> mode == OiMode.SAFE }
                  .firstElement()
                  .toObservable()
              )
        }
        .doOnNext {
          logger.debug { "Driving backwards for 1s" }
          //roomba.fullMode()
          roomba.driveDirect(-100, -100)
        }
        .delay(1000, TimeUnit.MILLISECONDS, scheduler)
        .doOnNext {
          logger.debug("Turn 180 degrees")
          roomba.safeMode()
          roomba.driveDirect(100, -100)
        }
        .delay(3400, TimeUnit.MILLISECONDS, scheduler)
        .subscribe {
          roomba.driveDirect(0, 0)
          robotState.mode = RobotMode.FREE
          logger.info { "Undocked" }
        }

    roombaEvents.angle
        .subscribe { angle -> roombaState.fullAngle = roombaState.fullAngle + angle }

    // dock when run out of juice
    roombaEvents.batteryCharge
        .filter { charge -> charge <= MINIMAL_BATTERY_CHARGE }
        .filter { robotState.mode == RobotMode.FREE }
        .subscribe {
          logger.info { "Battery depleted to $MINIMAL_BATTERY_CHARGE->DOCKING" }
          robotState.mode = RobotMode.DOCKING
        }

    // undock if autonomous
    roombaEvents.batteryCharge
        .filter { charge -> charge == roombaState.batteryCapacity }
        // should the filter condition be merged?
        .filter { robotState.mode == RobotMode.DOCKED }
        .filter { robotState.agency == RobotAgency.AUTONOMOUS_AGENT }
        .subscribe {
          logger.info("Battery full and AUTONOMOUS_AGENT->UNDOCKING")
          robotState.mode = RobotMode.UNDOCKING
        }

    // stop engines immediately if controlled by human
    robotEvents.agency
        .filter { it == RobotAgency.HUMAN_PROXY }
        .subscribe { roombaState.velocity = Pair(0, 0) }

    // on any event triggering autonomous movement create autonomousMovement state
    Observables
        .combineLatest(robotEvents.mode, robotEvents.agency)
        .map { it.first == RobotMode.FREE && it.second == RobotAgency.AUTONOMOUS_AGENT }
        .subscribe { auto -> robotState.autonomousMovement = auto }

    // set requested velocity to Roomba
    roombaEvents.velocity
        .withLatestFrom(robotEvents.mode)
        .filter { pair -> pair.second == RobotMode.FREE }
        .map { pair -> pair.first }
        .subscribe { velocity ->
          roomba.driveDirect(velocity.first, velocity.second)
        }

    val waitForSafety = Observables
        .combineLatest(
            roombaEvents.cliffLeft,
            roombaEvents.cliffFrontLeft,
            roombaEvents.cliffFrontRight,
            roombaEvents.cliffRight,
            roombaEvents.wheelDropLeft,
            roombaEvents.wheelDropRight
        ) { alert1, alert2, alert3, alert4, alert5, alert6 ->
          (alert1 || alert2 || alert3 || alert4 || alert5 || alert6)
        }
        .filter { danger -> !danger }
        .firstElement()

    // if there is any cliff / wheel drop event, try to bring Roomba back to safety
    roombaEvents.oiMode
        .withLatestFrom(robotEvents.mode)
        .filter { pair -> (pair.first == OiMode.PASSIVE) && (pair.second == RobotMode.FREE) }
        .doOnNext {
          roombaState.latestMessage = "Roomba OI mode switched to PASSIVE due to cliff/wheel " +
              "drop. Waiting for safety to bring OI SAFE mode back. " +
              "Please try to move the robot to a safe spot."
        }
        // switch map will guarantee that we unsubscribe immediately from the previous event handling
        .switchMapMaybe {
          waitForSafety.doOnSuccess {
            if (roombaState.oiMode == OiMode.PASSIVE) {
              roombaState.latestMessage = "All alerts gone, bringing Roomba back to SAFE OI mode"
              roomba.safeMode()
            } else {
              logger.warn {
                "Not bringing Roomba back to SAFE OI mode. All danger sensors are good " +
                    "now, but it's already in mode: ${roombaState.oiMode}"
              }
            }
          }
        }
        .subscribe()

    roombaState.latestMessage = "Roomba Started"

    // log any change to robot modes
    roombaEvents.oiMode
        .subscribe { value -> logger.debug { "Change: oiMode=$value" } }
    robotEvents.mode
        .subscribe { value -> logger.debug { "Change: robotMode=$value" } }
    robotEvents.autonomousMovement
        .subscribe { value -> logger.debug { "Change: autonomousMovement=$value" } }
    robotEvents.agency
        .subscribe { value -> logger.debug { "Change: robotAgency=$value" } }
    roombaEvents.chargingState
        .subscribe { value -> logger.debug { "Change: chargingState=$value" } }
    roombaEvents.latestMessage
        .subscribe { message -> logger.warn { "Latest message: $message" } }

    logger.info("Roomba Daemon started")
  }

  fun stop() {
    logger.info("Stopping Roomba Daemon")
    roomba.stop().blockingAwait()

    logger.info("Shutting down Schedulers")
    scheduler.shutdown()

    logger.info("Roomba Daemon stopped")
  }

}
