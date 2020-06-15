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

@file:Suppress("EXPERIMENTAL_API_USAGE", "EXPERIMENTAL_UNSIGNED_LITERALS")

package com.xemantic.robots.roomba.api

import com.xemantic.state.core.Events
import com.xemantic.state.core.State

enum class StasisSensorState {
  CLEAN,
  DIRTY
}

enum class ChargingState(val code: UByte) {

  NOT_CHARGING(0u),
  RECONDITIONING_CHARGING(1u),
  FULL_CHARGING(2u),
  TRICKLE_CHARGING(3u),
  WAITING(4u),
  CHARGING_FAULT_CONDITION(5u);

  companion object {
    private val map = values().map { value -> Pair(value.code, value) }.toMap()
    fun fromCode(code: UByte) =
        map[code] ?: throw IllegalArgumentException("Unknown ChargingState code: $code")
  }

}

enum class OiMode(val code: UByte) {
  OFF(0u),
  PASSIVE(1u),
  SAFE(2u),
  FULL(3u);

  companion object {
    private val map = OiMode.values().map { value -> Pair(value.code, value) }.toMap()
    fun fromCode(code: UByte) =
        map[code] ?: throw IllegalArgumentException("Unknown OiMode code: $code")
  }

}

open class RoombaState(s: State<RoombaState>) {

  open val wheelDropLeft               by s.property(false)
  open val wheelDropRight              by s.property(false)
  open val bumpLeft                    by s.property(false)
  open val bumpRight                   by s.property(false)
  open val wall                        by s.property(false)
  open val cliffLeft                   by s.property(false)
  open val cliffFrontLeft              by s.property(false)
  open val cliffFrontRight             by s.property(false)
  open val cliffRight                  by s.property(false)
  open val leftWheelOvercurrent        by s.property(false)
  open val rightWheelOvercurrent       by s.property(false)
  open val mainBrushOvercurrent        by s.property(false)
  open val sideBrushOvercurrent        by s.property(false)
  open val dirtDetect                  by s.property(0)
  open val infraredCharacterOmni       by s.property(0)
  open val infraredCharacterLeft       by s.property(0)
  open val infraredCharacterRight      by s.property(0)
  open val distance                    by s.property(0)
  open val angle                       by s.property(0)
  open val chargingState               by s.property(ChargingState.NOT_CHARGING)
  open val current                     by s.property(0)
  open val batteryCharge               by s.property(0)
  open val batteryCapacity             by s.property(10)
  open val wallSignal                  by s.property(0)
  open val cliffLeftSignal             by s.property(0)
  open val cliffFrontLeftSignal        by s.property(0)
  open val cliffFrontRightSignal       by s.property(0)
  open val cliffRightSignal            by s.property(0)
  open val oiMode                      by s.property(OiMode.OFF)

  open val lightBumpLeft               by s.property(false)
  open val lightBumpFrontLeft          by s.property(false)
  open val lightBumpCenterLeft         by s.property(false)
  open val lightBumpCenterRight        by s.property(false)
  open val lightBumpFrontRight         by s.property(false)
  open val lightBumpRight              by s.property(false)

  open val lightBumpLeftSignal         by s.property(0)
  open val lightBumpFrontLeftSignal    by s.property(0)
  open val lightBumpCenterLeftSignal   by s.property(0)
  open val lightBumpCenterRightSignal  by s.property(0)
  open val lightBumpFrontRightSignal   by s.property(0)
  open val lightBumpRightSignal        by s.property(0)

  open val stasis                      by s.property(false)
  open val stasisSensorState           by s.property(StasisSensorState.CLEAN)

  open val fullAngle                   by s.property(0)

  open val autonomousMovement          by s.property(false)

  open val roombaBanner                by s.property("")
  open val latestMessage               by s.property("")

  var velocity                    by s.property(Pair(0, 0))
}

// this should be generated
class RoombaStateEvents(e: Events<RoombaStateEvents>) {
  val wheelDropLeft by e.events<Boolean>()
  val wheelDropRight by e.events<Boolean>()
  val bumpLeft by e.events<Boolean>()
  val bumpRight by e.events<Boolean>()
  val wall by e.events<Boolean>()
  val cliffLeft by e.events<Boolean>()
  val cliffFrontLeft by e.events<Boolean>()
  val cliffFrontRight by e.events<Boolean>()
  val cliffRight by e.events<Boolean>()
  val leftWheelOvercurrent by e.events<Boolean>()
  val rightWheelOvercurrent by e.events<Boolean>()
  val mainBrushOvercurrent by e.events<Boolean>()
  val sideBrushOvercurrent by e.events<Boolean>()
  val dirtDetect by e.events<Int>()
  val infraredCharacterOmni by e.events<Int>()
  val infraredCharacterLeft by e.events<Int>()
  val infraredCharacterRight by e.events<Int>()
  val distance by e.events<Int>()
  val angle by e.events<Int>()
  val chargingState by e.events<ChargingState>()
  val current by e.events<Int>()
  val batteryCharge by e.events<Int>()
  val batteryCapacity by e.events<Int>()
  val wallSignal by e.events<Int>()
  val cliffLeftSignal by e.events<Int>()
  val cliffFrontLeftSignal by e.events<Int>()
  val cliffFrontRightSignal by e.events<Int>()
  val cliffRightSignal by e.events<Int>()
  val oiMode by e.events<OiMode>()

  val lightBumpLeft by e.events<Boolean>()
  val lightBumpFrontLeft by e.events<Boolean>()
  val lightBumpCenterLeft by e.events<Boolean>()
  val lightBumpCenterRight by e.events<Boolean>()
  val lightBumpFrontRight by e.events<Boolean>()
  val lightBumpRight by e.events<Boolean>()

  val lightBumpLeftSignal by e.events<Int>()
  val lightBumpFrontLeftSignal by e.events<Int>()
  val lightBumpCenterLeftSignal by e.events<Int>()
  val lightBumpCenterRightSignal by e.events<Int>()
  val lightBumpFrontRightSignal by e.events<Int>()
  val lightBumpRightSignal by e.events<Int>()

  val stasis by e.events<Boolean>()
  val stasisSensorState by e.events<StasisSensorState>()

  val fullAngle by e.events<Int>()

  val roombaBanner by e.events<String>()
  val latestMessage by e.events<String>()

  val velocity by e.events<Pair<Int, Int>>()
}
