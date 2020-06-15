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

package com.xemantic.robots.roomba.core

import com.xemantic.robots.roomba.api.ChargingState
import com.xemantic.robots.roomba.api.OiMode
import com.xemantic.robots.roomba.api.StasisSensorState
import io.reactivex.rxjava3.core.Completable
import io.reactivex.rxjava3.core.Scheduler
import mu.KotlinLogging
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.concurrent.thread

private const val SENSOR_STREAM_PACKET: UByte = 19u

private const val SENSOR_DATA_SIZE: UByte = 64u

private const val ZERO: UByte = 0u

private const val ONE: UByte = 1u

class Roomba(
    private val serialPort: String,
    private val state: MutableRoombaState,
    private val scheduler: Scheduler,
    private val slowerSensorUpdateInterval: Long = 1000
) {

  enum class Packet(val code: UByte) {
    WHEEL_OVERCURRENTS            (14u),
    DIRT_DETECT                   (15u),
    INFRARED_CHARACTER_OMNI       (17u),
    DISTANCE                      (19u),
    ANGLE                         (20u),
    INFRARED_CHARACTER_LEFT       (52u),
    INFRARED_CHARACTER_RIGHT      (53u),
    CHARGING_STATE                (21u),
    CURRENT                       (23u),
    BATTERY_CHARGE                (25u),
    BATTERY_CAPACITY              (26u),
    WALL_SIGNAL                   (27u),
    CLIFF_LEFT_SIGNAL             (28u),
    CLIFF_FRONT_LEFT_SIGNAL       (29u),
    CLIFF_FRONT_RIGHT_SIGNAL      (30u),
    CLIFF_RIGHT_SIGNAL            (31u),
    OI_MODE                       (35u),
    LIGHT_BUMPER                  (45u),
    LIGHT_BUMP_LEFT_SIGNAL        (46u),
    LIGHT_BUMP_FRONT_LEFT_SIGNAL  (47u),
    LIGHT_BUMP_CENTER_LEFT_SIGNAL (48u),
    LIGHT_BUMP_CENTER_RIGHT_SIGNAL(49u),
    LIGHT_BUMP_FRONT_RIGHT_SIGNAL (50u),
    LIGHT_BUMP_RIGHT_SIGNAL       (51u),
    BUMPS_AND_WHEEL_DROPS         (7u),
    WALL                          (8u),
    CLIFF_LEFT                    (9u),
    CLIFF_FRONT_LEFT              (10u),
    CLIFF_FRONT_RIGHT             (11u),
    CLIFF_RIGHT                   (12u),
    STASIS                        (58u);

    companion object {
      fun arrayOf(vararg packets: Packet): UByteArray = packets
          .map { packet -> packet.code }
          .toUByteArray()
    }

  }

  enum class Command(val code: UByte) {
    START(128u),
    RESET(7u),
    STOP(173u),

    SAFE(131u),
    FULL(132u),

    CLEAN(135u),
    MAX(136u), // max clean
    SPOT(134u),
    SEEK_DOCK(143u),
    POWER(133u), // power down
    SCHEDULE(167u),
    SET_DAY_TIME(168u),

    DRIVE(137u),        // velocity + radius
    DRIVE_DIRECT(145u), // left velocity + right velocity - mm / s
    DRIVE_PWM(146u),
    MOTORS(138u),
    PWM_MOTORS(144u),

    LEDS(139u),
    SCHEDULING_LEDS(162u),
    DIGIT_LEDS_RAW(163u),
    BUTTONS(165u),
    DIGIT_LEDS_ASCII(164u),
    SONG(140u),
    PLAY(141u),

    SENSORS(142u),
    QUERY_LIST(149u),
    STREAM(148u),
    PAUSE_RESUME_STREAM(150u);

    fun with(vararg args: UByte): UByteArray {
      return ubyteArrayOf(code) + args
    }
  }

  private val sensorPackets = ubyteArrayOf(
      Packet.DIRT_DETECT.code,
      Packet.INFRARED_CHARACTER_OMNI.code,
      Packet.INFRARED_CHARACTER_LEFT.code,
      Packet.INFRARED_CHARACTER_RIGHT.code,
      Packet.CHARGING_STATE.code,
      Packet.CURRENT.code,
      Packet.WALL_SIGNAL.code,
      Packet.CLIFF_LEFT_SIGNAL.code,
      Packet.CLIFF_FRONT_LEFT_SIGNAL.code,
      Packet.CLIFF_FRONT_RIGHT_SIGNAL.code,
      Packet.CLIFF_RIGHT_SIGNAL.code,
      Packet.OI_MODE.code,
      Packet.LIGHT_BUMPER.code,
      Packet.LIGHT_BUMP_LEFT_SIGNAL.code,
      Packet.LIGHT_BUMP_FRONT_LEFT_SIGNAL.code,
      Packet.LIGHT_BUMP_CENTER_LEFT_SIGNAL.code,
      Packet.LIGHT_BUMP_CENTER_RIGHT_SIGNAL.code,
      Packet.LIGHT_BUMP_FRONT_RIGHT_SIGNAL.code,
      Packet.LIGHT_BUMP_RIGHT_SIGNAL.code,
      Packet.BUMPS_AND_WHEEL_DROPS.code,
      Packet.WALL.code,
      Packet.CLIFF_LEFT.code,
      Packet.CLIFF_FRONT_LEFT.code,
      Packet.CLIFF_FRONT_RIGHT.code,
      Packet.CLIFF_RIGHT.code,
      Packet.STASIS.code
  )

  private val logger = KotlinLogging.logger {}

  private val serial = RoombaSerial(serialPort)

  private val streaming = AtomicBoolean(false)

  private var nextSlowerSensorUpdate = System.currentTimeMillis()

  private lateinit var streamReader: Thread

  fun start(): Completable = Completable
      .fromAction {
        logger.info("Starting communication with Roomba device on serial port: $serialPort")
        serial.start()
        logger.debug { "Sending RESET" }
        serial.sendBytes(Command.RESET.code)
      }
      .delay(6000, TimeUnit.MILLISECONDS, scheduler)
      .doOnComplete {
        streaming.set(true)
        streamReader = thread(start = true, name = "Serial", priority = Thread.MAX_PRIORITY) {
          logger.debug { "Starting Roomba stream reader thread" }
          val banner = String(serial.readBytes().asByteArray(), Charsets.US_ASCII)
          logger.info { "Roomba banner: $banner" }
          state.roombaBanner = banner.substringAfter("Soft reset!\n�")
          readStream()
          logger.debug { "Roomba stream reader thread ends" }
        }
        logger.debug { "Sending START+SAFE" }
        serial.sendBytes(Command.START.code, Command.SAFE.code)
      }
      .delay(1000, TimeUnit.MILLISECONDS, scheduler) // TODO do we need this delay?
      .doOnComplete { streamSensors() }
      .subscribeOn(scheduler)

  fun stop(): Completable = Completable
      .fromAction {
        logger.info("Stop streaming sensor data")
        streaming.set(false)
      }
      .delay(1000, TimeUnit.MILLISECONDS, scheduler)
      .doOnComplete {
        serial.writeBytes(Command.PAUSE_RESUME_STREAM.with(0u)) // 0 means stop, 1 resume
      }
      .doOnComplete {
        streamReader.join()
        driveDirect(0, 0)
        serial.stop()
      }
      .subscribeOn(scheduler)

  fun driveDirect(rightVelocity: Int, leftVelocity: Int) {
    // Validate argument values
    require(!(rightVelocity < -500 || rightVelocity > 500 || leftVelocity < -500 || leftVelocity > 500)) {
      "Velocity should be between -500 and 500"
    }
    logger.trace { "driveDirect($rightVelocity, $leftVelocity)" }
    serial.writeBytes(
        Command.DRIVE_DIRECT.with(
            (rightVelocity ushr 8).toUByte(),
            rightVelocity.toUByte(),
            (leftVelocity ushr 8).toUByte(),
            leftVelocity.toUByte()
        )
    )
  }

  fun seekDock() = serial.sendBytes(Command.SEEK_DOCK.code)

  fun fullMode() = serial.sendBytes(Command.FULL.code)

  fun safeMode() = serial.sendBytes(Command.SAFE.code)

  private fun streamSensors() = serial.writeBytes(
      Command.STREAM.with(sensorPackets.size.toUByte()) + sensorPackets
  )

  private val slowerSensorPackets =   Packet.arrayOf(
      Packet.WALL,     // just as a marker of 0 or 1 to distinguish from the other packet
      Packet.WALL,     // just as a marker of 0 or 1 to distinguish from the other packet
      Packet.WALL,     // just as a marker of 0 or 1 to distinguish from the other packet
      Packet.WALL,     // just as a marker of 0 or 1 to distinguish from the other packet
      Packet.OI_MODE,  // OI mode - another marker, we need to discard 2 bytes // TODO this one no longer needed, or?
      Packet.DISTANCE,
      Packet.ANGLE,
      Packet.WHEEL_OVERCURRENTS,
      Packet.BATTERY_CHARGE,
      Packet.BATTERY_CAPACITY,
      Packet.WALL,     // just as a marker of 0 or 1 to distinguish from the other packet
      Packet.WALL,     // just as a marker of 0 or 1 to distinguish from the other packet
      Packet.WALL,     // just as a marker of 0 or 1 to distinguish from the other packet
      Packet.WALL      // just as a marker of 0 or 1 to distinguish from the other packet
  )

  private val slowerSensorCommand =
      Command.QUERY_LIST.with(slowerSensorPackets.size.toUByte()) + slowerSensorPackets


  private fun querySlowerSensors() {
    serial.writeBytes(slowerSensorCommand)
  }

  private fun maybeNextSlowerSensorUpdate() {
    val now = System.currentTimeMillis()
    if (now >= nextSlowerSensorUpdate) {
      nextSlowerSensorUpdate = now + slowerSensorUpdateInterval
      querySlowerSensors()
    }
  }

  private fun isValidStateData(state: UByteArray) =
      (state[0] == Packet.DIRT_DETECT.code)
          && (state[2] == Packet.INFRARED_CHARACTER_OMNI.code)
          && (state[4] == Packet.INFRARED_CHARACTER_LEFT.code)
          && (state[6] == Packet.INFRARED_CHARACTER_RIGHT.code)
          && (state[8] == Packet.CHARGING_STATE.code)
          && (state[10] == Packet.CURRENT.code)
          && (state[13] == Packet.WALL_SIGNAL.code)
          && (state[16] == Packet.CLIFF_LEFT_SIGNAL.code)
          && (state[19] == Packet.CLIFF_FRONT_LEFT_SIGNAL.code)
          && (state[22] == Packet.CLIFF_FRONT_RIGHT_SIGNAL.code)
          && (state[25] == Packet.CLIFF_RIGHT_SIGNAL.code)
          && (state[28] == Packet.OI_MODE.code)
          && (state[30] == Packet.LIGHT_BUMPER.code)
          && (state[32] == Packet.LIGHT_BUMP_LEFT_SIGNAL.code)
          && (state[35] == Packet.LIGHT_BUMP_FRONT_LEFT_SIGNAL.code)
          && (state[38] == Packet.LIGHT_BUMP_CENTER_LEFT_SIGNAL.code)
          && (state[41] == Packet.LIGHT_BUMP_CENTER_RIGHT_SIGNAL.code)
          && (state[44] == Packet.LIGHT_BUMP_FRONT_RIGHT_SIGNAL.code)
          && (state[47] == Packet.LIGHT_BUMP_RIGHT_SIGNAL.code)
          && (state[50] == Packet.BUMPS_AND_WHEEL_DROPS.code)
          && (state[52] == Packet.WALL.code)
          && (state[54] == Packet.CLIFF_LEFT.code)
          && (state[56] == Packet.CLIFF_FRONT_LEFT.code)
          && (state[58] == Packet.CLIFF_FRONT_RIGHT.code)
          && (state[60] == Packet.CLIFF_RIGHT.code)
          && (state[62] == Packet.STASIS.code)

  private fun isValidSlowerSensorsStateData(state: UByteArray) =
      (state[10] == ZERO
          && state[11] == ZERO
          && state[12] == ZERO
          && state[13] == ZERO
      )
      ||
      (state[10] == ONE
          && state[11] == ONE
          && state[12] == ONE
          && state[13] == ONE
      )

  private fun propagateState(data: UByteArray) {
    logger.trace("Propagating state")
    state.dirtDetect = data[1].toInt()
    state.infraredCharacterOmni = data[3].toUByte().toInt()
    state.infraredCharacterLeft = data[5].toUByte().toInt()
    state.infraredCharacterRight = data[7].toUByte().toInt()
    state.chargingState = ChargingState.fromCode(data[9])
    state.current = data.asInt(11)
    state.wallSignal = data.asUInt(14)
    state.cliffLeftSignal = data.asUInt(17)
    state.cliffFrontLeftSignal = data.asUInt(20)
    state.cliffFrontRightSignal = data.asUInt(23)
    state.cliffRightSignal = data.asUInt(26)
    state.oiMode = OiMode.fromCode(data[29]) // TODO it's redundant now
    state.lightBumpRight = data[31].bit(5)
    state.lightBumpFrontRight = data[31].bit(4)
    state.lightBumpCenterRight = data[31].bit(3)
    state.lightBumpCenterLeft = data[31].bit(2)
    state.lightBumpFrontLeft = data[31].bit(1)
    state.lightBumpLeft = data[31].bit(0)
    state.lightBumpLeftSignal = data.asUInt(33)
    state.lightBumpFrontLeftSignal = data.asUInt(36)
    state.lightBumpCenterLeftSignal = data.asUInt(39)
    state.lightBumpCenterRightSignal = data.asUInt(42)
    state.lightBumpFrontRightSignal = data.asUInt(45)
    state.lightBumpRightSignal = data.asUInt(48)
    state.wheelDropLeft = data[51].bit(3)
    state.wheelDropRight = data[51].bit(2)
    state.bumpLeft = data[51].bit(1)
    state.bumpRight = data[51].bit(0)
    state.wall = data[53].toBoolean()
    state.cliffLeft = data[55].toBoolean()
    state.cliffFrontLeft = data[57].toBoolean()
    state.cliffFrontRight = data[59].toBoolean()
    state.cliffRight = data[61].toBoolean()
    state.stasis = !data[63].bit(0)
    state.stasisSensorState =
        if (data[63].bit(1)) StasisSensorState.DIRTY
        else StasisSensorState.CLEAN
  }

  private fun propagateSlowerSensorState(data: UByteArray) {
    logger.trace("Propagating slower sensor state")
    try {
      state.oiMode = OiMode.fromCode(data[0])
    } catch (e: IllegalArgumentException) {
      logger.error { "wrong OI mode ${data.joinToString(",")}" }
      return
    }
    state.distance = data.asInt(1)
    state.angle = data.asInt(3)
    state.leftWheelOvercurrent = data[5].bit(4)
    state.rightWheelOvercurrent = data[5].bit(3)
    state.mainBrushOvercurrent = data[5].bit(2)
    state.sideBrushOvercurrent = data[5].bit(0)
    state.batteryCharge = data.asUInt(6)
    state.batteryCapacity = data.asUInt(8)
  }

  private fun readStream() {
    var latestReady = false
    val textBuffer = StringBuilder()
    var latest: UByteArray = ubyteArrayOf(0u, 0u)
    fun isSensorStreamHeader() =
        (latest[0] == SENSOR_STREAM_PACKET) && (latest[1] == SENSOR_DATA_SIZE)
    fun isSlowerSensorsHeader() =
        ((latest[0] == ZERO) && (latest[1] == ZERO))
            || ((latest[0] == ONE) && (latest[1] == ONE))
    while (streaming.get()) {
      if (latestReady) {
        latestReady = false
      } else {
        latest = serial.readBytes(2)
      }
      while (!isSensorStreamHeader() && !isSlowerSensorsHeader()) {
        if (latest[1] == SENSOR_STREAM_PACKET) {
          logger.debug { "Sensor packet after byte 0 (normally slow sensors)" }
          // we need to skip the following corrupted bytes
          do {
            latest[0] = latest[1]
            latest[1] = serial.readBytes(1)[0]
          } while (!isSensorStreamHeader())
        } else {
          textBuffer.append(latest[0].toByte().toChar())
          latest[0] = latest[1]
          latest[1] = serial.readBytes(1)[0]
        }
      }
      if (textBuffer.isNotEmpty()) {
        logger.debug { "Unknown packet received: $textBuffer" }
        state.latestMessage = textBuffer.toString()
        textBuffer.clear()
      }
      if (latest[0] == SENSOR_STREAM_PACKET) {
        val state = serial.readBytes(SENSOR_DATA_SIZE.toInt() + 1) // +1 for control sum
        if (isValidStateData(state)) {
          propagateState(state)
        } else {
          logger.error { "Invalid sensor data:  [${state.joinToString(",")}]" }
        }
      } else if ((latest[0] == ZERO) || (latest[0] == ONE)) { // space query
        logger.trace { "Space query candidate starts with: ${latest.joinToString(",") }" }
        latest = serial.readBytes(2)
        if (isSensorStreamHeader()) {
          logger.debug { "After space query candidate there was sensor stream packet (19)" }
          latestReady = true
        } else if (isSlowerSensorsHeader()) {
          val state = serial.readBytes(14)
          if (isValidSlowerSensorsStateData(state)) {
            propagateSlowerSensorState(state)
          } else {
            logger.debug { "Invalid slow sensors data, must end with 4 x 0 or 4 x 1: ${state.joinToString(",")}" }
            latest[0] = state[12]
            latest[1] = state[13]
            latestReady = true
          }
        } else {
          logger.debug { "Invalid slow sensors header, must start with 4 zeros, skipping" }
          latestReady = true
        }
      } else {
        logger.error { "Should never happen: ${latest.joinToString(",") }" }
      }
      maybeNextSlowerSensorUpdate()
    }
  }

  private fun UByte.bit(position: Byte): Boolean {
    return Roombas.bitOnPosition(this.toByte(), position)
  }

  private fun UByteArray.asInt(index: Int): Int {
    return Roombas.signed16BitToInt(this[index].toByte(), this[index + 1].toByte())
  }

  private fun UByteArray.asUInt(index: Int): Int {
    return Roombas.unsigned16BitToInt(this[index].toByte(), this[index + 1].toByte())
  }

  private fun UByte.toBoolean() = when {
    this == ZERO -> false
    this == ONE -> true
    else -> throw IllegalArgumentException("Cannot convert Int to Boolean: $this")
  }

}

