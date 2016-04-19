/*
 * Copyright (c) 2016 Michael K. Werle
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.coruscations.aws.s3;

import com.coruscations.aws.AwsCredentialProperty;
import com.coruscations.aws.RestCommandResponse;

import org.junit.Assert;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

public class S3CommandIT {

  private static final Logger LOG;
  static {
    setupLogging();
    LOG = Logger.getLogger(S3CommandIT.class.getName());
  }

  // TODO: Make these configurable???
  private static final int DEFAULT_PORT_MIN = 9000;
  private static final int DEFAULT_PORT_MAX = 9999;
  private static final int MAX_PORT_CLOSE_MILLISECONDS = 100;

  private static final String NANO_ENV_SYSTEM_PROPERTY = "nano.env";
  private static final String NANO_ENV_ENVIRONMENT_PROPERTY = "NANO_ENV";

  private static final Set<Integer> SUCCESS_RESPONSE_CODES =
      new HashSet<>(Arrays.asList(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED,
                                  HttpURLConnection.HTTP_ACCEPTED,
                                  HttpURLConnection.HTTP_NO_CONTENT));

  private static final SecureRandom RANDOM = new SecureRandom();

  public static void setupLogging() {
    try (final InputStream is = S3CommandIT.class.getResourceAsStream("/logging.properties")) {
      if (is != null) {
        LogManager.getLogManager().readConfiguration(is);
      }
    } catch (IOException e) {
      throw new IllegalStateException("Failed to load logging.properties", e);
    }
  }

  public static Map<String, Object> createEnvironment() {
    // Use the environment if the env property is set to default
    String envValue = System.getProperty(NANO_ENV_SYSTEM_PROPERTY);
    if (envValue == null) {
      envValue = System.getenv(NANO_ENV_ENVIRONMENT_PROPERTY);
    }
    if ("default".equalsIgnoreCase(envValue)) {
      return Collections.emptyMap();
    }

    // Use a specific properties file if set via env
    if (envValue != null) {
      Path envPath = Paths.get(envValue);
      try (InputStream classPathIS = S3CommandIT.class.getResourceAsStream(envValue);
           InputStream envPathIS = Files.isReadable(envPath) ?
                                   Files.newInputStream(envPath) : null) {
        InputStream is = classPathIS == null ? envPathIS == null ? null : envPathIS : classPathIS;
        if (is != null) {
          Properties properties = new Properties();
          properties.load(is);
          return createEnvironment(properties);
        }
      } catch (IOException e) {
        throw new IllegalStateException("Cannot load properties from: " + envValue);
      }
    }

    // Create defaults appropriate for minio
    Map<String, Object> env = new HashMap<>();
    env.putIfAbsent(AwsCredentialProperty.AWS_ACCESS_KEY_ID.getEnvName(),
                    new BigInteger(100, RANDOM).toString(32));
    env.putIfAbsent(AwsCredentialProperty.AWS_SECRET_KEY.getEnvName(),
                    new BigInteger(200, RANDOM).toString(32));
    env.putIfAbsent(AwsCredentialProperty.AWS_ENDPOINT.getEnvName(),
                    "http://localhost:" + findOpenPort("localhost"));
    env.putIfAbsent(AwsCredentialProperty.AWS_DEFAULT_REGION.getEnvName(), "us-east-1");
    return env;
  }

  public static Map<String, Object> createEnvironment(Properties properties) {
    Map<String, Object> env = new HashMap<>();
    properties.stringPropertyNames().stream().forEach(n -> {
      AwsCredentialProperty credentialProperty = AwsCredentialProperty.fromIniFileName(n);
      if (credentialProperty != null) {
        env.put(credentialProperty.getEnvName(), properties.get(n));
      }
    });
    return env;
  }

  private static int findOpenPort(String host) {
    int foundPort = -1;
    for (int i = DEFAULT_PORT_MIN; i <= DEFAULT_PORT_MAX; i++) {
      try (ServerSocket s = new ServerSocket(i, 0, InetAddress.getByName(host))) {
        assert s.isBound();
        foundPort = i;
        break;

      } catch (IOException e) {
        LOG.log(Level.FINEST, "Port not available: " + i);
      }
    }
    if (foundPort > 0) {
      Socket socket = new Socket();
      InetSocketAddress sa = new InetSocketAddress(host, foundPort);
      long now = System.currentTimeMillis();
      while (System.currentTimeMillis() - now < MAX_PORT_CLOSE_MILLISECONDS) {
        try {
          socket.connect(sa);
          try {
            socket.close();
          } catch (IOException e) {
            LOG.log(Level.WARNING, "Failed to close test connection to minio", e);
          }
          Thread.sleep(10);
        } catch (InterruptedException e) {
          // Keep trying
          continue;
        } catch (IOException e) {
          // Oddly, this is success
          return foundPort;
        }
      }
    }
    throw new IllegalStateException("No ports open between " + DEFAULT_PORT_MIN + " and " +
                                    DEFAULT_PORT_MAX);
  }

  protected static void assertSuccessResponse(RestCommandResponse response) {
    int code = response.getResponseCode();
    Assert.assertTrue("Invalid HTTP response: " + code, SUCCESS_RESPONSE_CODES.contains(code));
  }

  protected static void assertSuccessResponse(RestCommandResponse response, String message) {
    int code = response.getResponseCode();
    Assert.assertTrue(message, SUCCESS_RESPONSE_CODES.contains(code));
  }
}
