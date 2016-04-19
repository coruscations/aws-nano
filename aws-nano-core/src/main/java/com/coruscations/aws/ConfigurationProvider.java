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

package com.coruscations.aws;

import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nullable;
import javax.annotation.ParametersAreNonnullByDefault;

/**
 * This is based heavily on the AWS SDK code to parse these files, distributed under the Apache 2.0
 * license at https://github.com/aws/aws-sdk-java.
 */
@ParametersAreNonnullByDefault
public class ConfigurationProvider {

//    Environment Variables - AWS_ACCESS_KEY_ID and AWS_SECRET_ACCESS_KEY (RECOMMENDED since they are recognized by all the AWS SDKs and CLI except for .NET), or AWS_ACCESS_KEY and AWS_SECRET_KEY (only recognized by Java SDK)
//    Java System Properties - aws.accessKeyId and aws.secretKey
//    Credential profiles file at the default location (~/.aws/credentials) shared by all AWS SDKs and the AWS CLI
//    TODO: AWS_DEFAULT_PROFILE can be used to set the profile
//    TODO: AWS_CONFIG_FILE/AWS_CREDENTIAL_PROFILES_FILE can be used to set the files
//    Instance profile credentials delivered through the Amazon EC2 metadata service

  private static final Logger LOG = Logger.getLogger(ConfigurationProvider.class.getName());

  private static final Pattern LOCALHOST_PATTERN =
      Pattern.compile("^(?:https?://)?(localhost)(?::(\\d{1,5}))?$|" +
                      "^(?:https?://)?(127(?:\\.[0-9]+){0,2}\\.[0-9]+)(?::(\\d{1,5}))?$|" +
                      "^(?:https?://)?\\[((?:0*:)*?:?0*1)\\](?::(\\d{1,5}))?$|" +
                      "^(?:https?://)?((?:0*:)*?:?0*1)$",
                      Pattern.CASE_INSENSITIVE);

  private static final Set<String> SUPPORTED_PROPERTIES =
      Collections.unmodifiableSet(Arrays.stream(AwsCredentialProperty.values())
                                      .map(AwsCredentialProperty::getIniFileName)
                                      .filter(Objects::nonNull).collect(Collectors.toSet()));

  private final Map<String, ?> env;
  private final String accessKey;
  private final String secretKey;
  private final Endpoint endpoint;
  private final String region;
  // TODO: Deal with these
  //private String sessionToken;
  //private final Instant expiration;

  public ConfigurationProvider(Map<String, ?> env) {
    this.env = Collections.unmodifiableMap(env);
    // The endpoint can only come from the properties and is not always configured.
    String endpointString = get(AwsCredentialProperty.AWS_ENDPOINT, env);
    String region = get(AwsCredentialProperty.AWS_DEFAULT_REGION, env);
    // Find the required properties
    String accessKey = get(AwsCredentialProperty.AWS_ACCESS_KEY_ID, env);
    String secretKey = get(AwsCredentialProperty.AWS_SECRET_KEY, env);
    if (accessKey != null && secretKey != null) {
      this.accessKey = accessKey;
      this.secretKey = secretKey;
      this.region = region == null ? "us-east-1" : region;
      this.endpoint = createEndpoint(region, endpointString);
      return;
    }
    Map<String, String> iniProperties = getIniProperties(env);
    if (iniProperties != null) {
      accessKey = iniProperties.get(AwsCredentialProperty.AWS_ACCESS_KEY_ID.getIniFileName());
      secretKey = iniProperties.get(AwsCredentialProperty.AWS_SECRET_KEY.getIniFileName());
      region = iniProperties.get(AwsCredentialProperty.AWS_DEFAULT_REGION.getIniFileName());
    }
    if (accessKey != null && secretKey != null) {
      this.accessKey = accessKey;
      this.secretKey = secretKey;
      this.region = region == null ? "us-east-1" : region;
      this.endpoint = createEndpoint(region, endpointString);
      return;
    }
    // TODO: Parse instance profile credentials
    throw new IllegalStateException("Credentials not found (EC2 metadata not yet parsed");
  }

  @Nullable
  private Endpoint createEndpoint(String region, String endpointString) {
    try {
      boolean isLocalhost = endpointString != null &&
                            LOCALHOST_PATTERN.matcher(endpointString).matches();
      // Assume localhost is really minio
      return region == null || endpointString == null ? null :
             new Endpoint(region, endpointString, !isLocalhost, !isLocalhost);
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid endpoint found in environment: " +
                                         endpointString, e);
    }
  }

  public String get(AwsCredentialProperty credentialProperty, Map<String, ?> env) {
    return get(credentialProperty, env, null);
  }

  public String get(AwsCredentialProperty credentialProperty, Map<String, ?> env,
                    @Nullable Predicate<String> predicate) {
    String envVal = (String) env.get(credentialProperty.getEnvName());
    if (envVal != null && (predicate == null || predicate.test(envVal))) {
      LOG.log(Level.FINE, "Using value from passed map for: {0}", credentialProperty.getEnvName());
      return envVal;
    }
    String systemPropertyName = credentialProperty.getSystemPropertyName();
    if (systemPropertyName != null) {
      String systemPropertyValue = System.getProperty(systemPropertyName);
      if (systemPropertyValue != null && (predicate == null || predicate.test(envVal))) {
        LOG.log(Level.FINE, "Using value from system property for: {0}",
                credentialProperty.getSystemPropertyName());
        return systemPropertyValue;
      }
    }
    for (String name : credentialProperty.getSystemEnvNames()) {
      String systemEnvVal = System.getenv(name);
      if (systemEnvVal != null && (predicate == null || predicate.test(envVal))) {
        LOG.log(Level.FINE, "Using value from environment property for: {0}", name);
        return systemEnvVal;
      }
    }
    return null;
  }

  private Map<String, String> getIniProperties(Map<String, ?> env) {
    String iniPathName = get(AwsCredentialProperty.AWS_CREDENTIAL_PROFILES_FILE, env,
                             n -> isReadable(Paths.get(n)));
    if (iniPathName == null) {
      String homeDir = System.getProperty("user.home");
      Path defaultCredentialsPath = homeDir == null ? null :
                                    Paths.get(homeDir, ".aws", "credentials");
      if (isReadable(defaultCredentialsPath)) {
        iniPathName = defaultCredentialsPath.toAbsolutePath().toString();
      } else {
        Path defaultConfigPath = homeDir == null ? null :
                                 Paths.get(homeDir, ".aws", "config");
        if (isReadable(defaultConfigPath)) {
          iniPathName = defaultConfigPath.toAbsolutePath().toString();
        } else {
          return null;
        }
      }
    }
    String profileName = get(AwsCredentialProperty.AWS_DEFAULT_PROFILE, env);
    profileName = profileName == null ? "default" : profileName;

    // We always want to read the config file first, if it exists,
    //   then override values from the credentials file, if that exists.

    Path iniPath = Paths.get(iniPathName);
    if ("config".equals(iniPath.getFileName().toString())) {
      Path credentialsPath = iniPath.resolveSibling("credentials");
      if (isReadable(credentialsPath)) {
        iniPath = credentialsPath;
      }
    }
    if ("credentials".equals(iniPath.getFileName().toString())) {
      Map<String, String> configIni = null;
      Path configPath = iniPath.resolveSibling("config");
      if (isReadable(configPath)) {
        configIni = readIni(configPath, profileName);
        configIni.putAll(readIni(iniPath, profileName));
        return configIni;
      }
    }
    return readIni(iniPath, profileName);
  }

  public Map<String, ?> getEnv() {
    return this.env;
  }

  private boolean isReadable(Path path) {
    return Files.isRegularFile(path) && Files.isReadable(path);
  }

  /**
   * @param path    The path of the properties file.
   * @param profile The name of the profile to use.
   */
  private Map<String, String> readIni(Path path, String profile) {
    String currentProfile = null;
    try (InputStream source = Files.newInputStream(path);) {
      Map<String, String> properties = new HashMap<>();
      Scanner scanner = new Scanner(source);
      while (scanner.hasNextLine()) {
        String line = scanner.nextLine().trim();
        if (line.isEmpty() && line.startsWith("#")) {
          continue;
        }
        if (line.startsWith("[") && line.endsWith("]")) {
          currentProfile = line.substring(1, line.length() - 1).trim();
          if (currentProfile.startsWith("profile ")) {
            currentProfile = currentProfile.substring(currentProfile.lastIndexOf(' ') + 1);
          }
          continue;
        }
        if (currentProfile == null) {
          throw new IllegalArgumentException(
              "Property is defined without a preceding profile name. Current line: " + line);
        }
        if (!currentProfile.equals(profile)) {
          continue;
        }
        String[] pair = line.split("=", 2);
        if (pair.length != 2) {
          throw new IllegalArgumentException(
              String.format("Invalid property format: no '=' character is found in the line [%s].",
                            line));
        } else {
          String key = pair[0].trim();
          if (key.isEmpty() || !SUPPORTED_PROPERTIES.contains(key)) {
            continue;
          }
          properties.put(key, pair[1].trim());
        }
      }
      if (!properties.isEmpty()) {
        return properties;
      }
    } catch (IOException e) {
      if (LOG.isLoggable(Level.WARNING)) {
        LOG.log(Level.WARNING, "Error reading AWS credential file: " + path, e);
      }
    }
    return null;
  }

  public String getAccessKey() {
    return this.accessKey;
  }

  public String getSecretKey() {
    return this.secretKey;
  }

  public String getRegion() {
    return this.region;
  }

  public Endpoint getEndpoint(String serviceName) {
    if (this.endpoint != null) {
      // Ignore region and just return the configured value
      return this.endpoint;
    }
    return RegionEndpointMappingService.getInstance().getEndpoint(serviceName, this.region);
  }
}
