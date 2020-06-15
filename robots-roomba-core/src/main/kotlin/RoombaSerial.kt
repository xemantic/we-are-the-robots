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

@file:Suppress("EXPERIMENTAL_API_USAGE")

package com.xemantic.robots.roomba.core

import jssc.SerialPort
import jssc.SerialPortList
import mu.KLogger
import mu.KotlinLogging

fun findRoombaSerialPort(logger: KLogger): String {
  return SerialPortList.getPortNames()
      ?.map { port ->
        logger.info {"Roomba serial port, must contain USB: $port" }
        port
      }
      ?.find { port -> port.contains("USB") }
      ?: throw IllegalStateException("No serial ports found")
}

class RoombaSerial(private val serialPort: String)  {

  private val logger = KotlinLogging.logger {}

  private val serial = SerialPort(serialPort)

  fun start() {
    logger.debug { "Opening serial port: $serialPort" }
    serial.openPort()
    serial.setParams(
        SerialPort.BAUDRATE_115200,
        SerialPort.DATABITS_8,
        SerialPort.STOPBITS_1,
        SerialPort.PARITY_NONE
    )
    serial.flowControlMode = SerialPort.FLOWCONTROL_NONE
  }

  fun stop() {
    logger.debug { "Closing serial port: $serialPort" }
    serial.closePort()
  }

  fun writeBytes(bytes: UByteArray) {
    logger.trace { "Writing bytes: [${bytes.joinToString(",")}]" }
    serial.writeBytes(bytes.asByteArray())
  }

  fun sendBytes(vararg bytes: UByte) {
    logger.trace { "Sending bytes: [${bytes.joinToString(",")}]" }
    serial.writeBytes(bytes.toByteArray())
  }

  fun readBytes(): UByteArray {
    logger.trace { "Reading all bytes" }
    val bytes = serial.readBytes().asUByteArray()
    logger.trace { "Received bytes: [${bytes.joinToString(",")}]" }
    return bytes
  }

  fun readBytes(count: Int): UByteArray {
    logger.trace { "Reading bytes, count: $count" }
    val bytes = serial.readBytes(count).asUByteArray()
    logger.trace { "Received bytes: [${bytes.joinToString(",")}]" }
    return bytes
  }

}
