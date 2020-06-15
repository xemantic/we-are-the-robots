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

import com.xemantic.robots.roomba.api.ChargingState
import com.xemantic.robots.roomba.api.OiMode
import com.xemantic.robots.roomba.api.RoombaState
import com.xemantic.robots.roomba.api.StasisSensorState
import com.xemantic.state.core.State

class MutableRoombaState(s: State<RoombaState>) : RoombaState(s) {
  override var wheelDropLeft               by s.property(false)
  override var wheelDropRight              by s.property(false)
  override var bumpLeft                    by s.property(false)
  override var bumpRight                   by s.property(false)
  override var wall                        by s.property(false)
  override var cliffLeft                   by s.property(false)
  override var cliffFrontLeft              by s.property(false)
  override var cliffFrontRight             by s.property(false)
  override var cliffRight                  by s.property(false)
  override var leftWheelOvercurrent        by s.property(false)
  override var rightWheelOvercurrent       by s.property(false)
  override var mainBrushOvercurrent        by s.property(false)
  override var sideBrushOvercurrent        by s.property(false)
  override var dirtDetect                  by s.property(0)
  override var infraredCharacterOmni       by s.property(0)
  override var infraredCharacterLeft       by s.property(0)
  override var infraredCharacterRight      by s.property(0)
  override var distance                    by s.property(0)
  override var angle                       by s.property(0)
  override var chargingState               by s.property(ChargingState.NOT_CHARGING)
  override var current                     by s.property(0)
  override var batteryCharge               by s.property(0)
  override var batteryCapacity             by s.property(10)
  override var wallSignal                  by s.property(0)
  override var cliffLeftSignal             by s.property(0)
  override var cliffFrontLeftSignal        by s.property(0)
  override var cliffFrontRightSignal       by s.property(0)
  override var cliffRightSignal            by s.property(0)
  override var oiMode                      by s.property(OiMode.OFF)

  override var lightBumpLeft               by s.property(false)
  override var lightBumpFrontLeft          by s.property(false)
  override var lightBumpCenterLeft         by s.property(false)
  override var lightBumpCenterRight        by s.property(false)
  override var lightBumpFrontRight         by s.property(false)
  override var lightBumpRight              by s.property(false)

  override var lightBumpLeftSignal         by s.property(0)
  override var lightBumpFrontLeftSignal    by s.property(0)
  override var lightBumpCenterLeftSignal   by s.property(0)
  override var lightBumpCenterRightSignal  by s.property(0)
  override var lightBumpFrontRightSignal   by s.property(0)
  override var lightBumpRightSignal        by s.property(0)

  override var stasis                      by s.property(false)
  override var stasisSensorState           by s.property(StasisSensorState.CLEAN)

  override var fullAngle                   by s.property(0)

  override var autonomousMovement          by s.property(false)

  override var roombaBanner                by s.property("")
  override var latestMessage               by s.property("")
}
