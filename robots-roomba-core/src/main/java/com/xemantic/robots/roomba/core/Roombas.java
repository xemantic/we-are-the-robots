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

package com.xemantic.robots.roomba.core;

public final class Roombas {

  private Roombas() { /* util class, non-instantiable */ }

  public static int signed16BitToInt(byte highByte, byte lowByte) {
    return lowByte & 0xff | (short) (highByte << 8);
  }

  public static int unsigned16BitToInt(byte highByte, byte lowByte) {
    return ((highByte & 0xff) << 8) | (lowByte & 0xff);
  }

  public static boolean bitOnPosition(byte data, byte position) {
    return data << ~position < 0;
  }

}
