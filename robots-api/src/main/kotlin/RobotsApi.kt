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

package com.xemantic.robots.api

import com.xemantic.state.core.Events
import com.xemantic.state.core.State

enum class RobotAgency {
  AUTONOMOUS_AGENT,
  HUMAN_PROXY
}

enum class RobotMode {
  FREE,
  DOCKED,
  DOCKING,
  UNDOCKING
}

open class RobotState(s: State<RobotState>) {
  // TODO this one should come from preserved state
  var robotAgency by s.property(RobotAgency.AUTONOMOUS_AGENT)
  var robotMode by s.property(RobotMode.FREE)
  open val autonomousMovement by s.property(false)
}

class MutableRobotState(s: State<RobotState>): RobotState(s) {
  override var autonomousMovement by s.property(false)
}

class RobotStateEvents(e: Events<RobotStateEvents>) {
  val robotAgency by e.events<RobotAgency>()
  val robotMode by e.events<RobotMode>()
  val autonomousMovement by e.events<Boolean>()
}
