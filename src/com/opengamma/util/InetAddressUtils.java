/**
 * Copyright (C) 2009 - 2010 by OpenGamma Inc.
 *
 * Please see distribution for license.
 */
package com.opengamma.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

import com.opengamma.OpenGammaRuntimeException;

/**
 * 
 */
public class InetAddressUtils {
  
  public static String getLocalHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      throw new OpenGammaRuntimeException("Could not obtain local host", e);
    }
  }

}
