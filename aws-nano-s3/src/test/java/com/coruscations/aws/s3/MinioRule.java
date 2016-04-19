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

import com.coruscations.aws.ConfigurationProvider;
import com.coruscations.aws.Endpoint;

import org.junit.Assume;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.file.attribute.PosixFilePermission.OWNER_EXECUTE;
import static java.nio.file.attribute.PosixFilePermission.OWNER_READ;

public class MinioRule extends TemporaryFolder {

  private static final Logger LOG = Logger.getLogger(MinioRule.class.getName());
  private static final Pattern LOCALHOST_PATTERN =
      Pattern.compile("^(localhost)(?::(\\d{1,5}))?$|" +
                      "^(127(?:\\.[0-9]+){0,2}\\.[0-9]+)(?::(\\d{1,5}))?$|" +
                      "^\\[((?:0*:)*?:?0*1)\\](?::(\\d{1,5}))?$|" +
                      "^((?:0*:)*?:?0*1)$",
                      Pattern.CASE_INSENSITIVE);

  private static final int MAX_STARTUP_WAIT_MILLISECONDS = 2000;

  private final boolean useMinio;
  private final String minioHost;
  private final int minioPort;
  private final String minioAccessKey;
  private final String minioSecretKey;

  private Process minioProcess;
  private Path minioLog;

  public MinioRule(ConfigurationProvider configurationProvider, String serviceName) {
    Endpoint endpoint = configurationProvider.getEndpoint(serviceName);
    Matcher matcher = LOCALHOST_PATTERN.matcher(endpoint.getHost());
    final boolean useMinio;
    final String host;
    final int port;
    if (matcher.matches()) {
      if (hasText(matcher.group(1))) {
        host = matcher.group(1);
        port = hasText(matcher.group(2)) ? Integer.parseInt(matcher.group(2)) : 0;
      } else if (hasText(matcher.group(3))) {
        host = matcher.group(3);
        port = hasText(matcher.group(4)) ? Integer.parseInt(matcher.group(4)) : 0;
      } else if (hasText(matcher.group(5))) {
        host = matcher.group(5);
        port = hasText(matcher.group(6)) ? Integer.parseInt(matcher.group(6)) : 0;
      } else if (hasText(matcher.group(7))) {
        host = matcher.group(7);
        port = 0;
      } else {
        host = null;
        port = -1;
      }
      useMinio = host != null && port > 0;
    } else {
      host = null;
      port = -1;
      useMinio = false;
    }
    this.useMinio = useMinio;
    if (useMinio) {
      this.minioHost = host;
      this.minioPort = port;
      this.minioAccessKey = configurationProvider.getAccessKey();
      this.minioSecretKey = configurationProvider.getSecretKey();
    } else {
      this.minioHost = null;
      this.minioPort = -1;
      this.minioAccessKey = null;
      this.minioSecretKey = null;
    }
  }

  private String zeroPad(String s, int length) {
    while (s.length() < length) {
      s = "0" + s;
    }
    return s;
  }

  private boolean hasText(String s) {
    return s != null && !s.isEmpty();
  }

  @Override
  protected void before() throws Throwable {
    if (!this.useMinio) {
      return;
    }
    super.before();
    try {
      Path tmpDir = getRoot().toPath();
      Path minioExecutable = getMinioExecutable(tmpDir);
      this.minioProcess = startMinio(tmpDir, minioExecutable);
      Runtime.getRuntime().addShutdownHook(new Thread(this::stopMinio));
      waitForMinioStart();
    } catch (Exception e) {
      stopMinio();
      Assume.assumeNoException("Could not start Minio", e);
    }
  }

  private void waitForMinioStart() throws InterruptedException {
    long now = System.currentTimeMillis();
    Socket socket = new Socket();
    InetSocketAddress sa = new InetSocketAddress(this.minioHost, this.minioPort);
    while (System.currentTimeMillis() - now < MAX_STARTUP_WAIT_MILLISECONDS) {
      try {
        // Do nothing until the log starts
        if (Files.size(this.minioLog) == 0) {
          Thread.sleep(50);
          continue;
        }
        socket.connect(sa);
        try {
          socket.close();
        } catch (IOException e) {
          LOG.log(Level.WARNING, "Failed to close test connection to minio", e);
        }
        return;
      } catch (IOException e) {
        Thread.sleep(50);
      }
    }
    if (LOG.isLoggable(Level.FINE) && this.minioLog != null) {
      try {
        LOG.fine("Minio log:\n" + new String(Files.readAllBytes(this.minioLog),
                                             StandardCharsets.UTF_8));
      } catch (IOException e) {
        LOG.log(Level.WARNING, "Failed to read minio log", e);
      }
    }
    throw new IllegalStateException("Minio failed to start after " +
                                    MAX_STARTUP_WAIT_MILLISECONDS + "ms");
  }

  private Process startMinio(Path tmpDir, Path minioExecutable) throws IOException {
    Path dataPath = tmpDir.resolve("data").toAbsolutePath();
    Files.createDirectories(dataPath);
    ProcessBuilder pb = new ProcessBuilder(minioExecutable.toAbsolutePath().toString(), "server",
                                           "--address", this.minioHost + ":" + this.minioPort,
                                           dataPath.toString());
    Map<String, String> env = pb.environment();
    env.remove("HOME");
    env.put("HOME", tmpDir.resolve("minio-home").toAbsolutePath().toString());
    env.put("MINIO_ACCESS_KEY", this.minioAccessKey);
    env.put("MINIO_SECRET_KEY", this.minioSecretKey);
    pb.directory(tmpDir.toFile());
    this.minioLog = tmpDir.resolve("out");
    pb.redirectErrorStream(true);
    pb.redirectOutput(ProcessBuilder.Redirect.appendTo(this.minioLog.toFile()));
    return pb.start();
  }

  private Path getMinioExecutable(Path tmpDir) throws URISyntaxException, IOException {
    URL minioUrl = resolveMinioUrl();
    Path minio;
    if ("file".equals(minioUrl.toURI().getScheme())) {
      minio = Paths.get(minioUrl.toURI());
    } else {
      // Copy minio to the temp folder
      minio = tmpDir.resolve(Paths.get(minioUrl.getFile()).getFileName());
      try (InputStream is = minioUrl.openStream()) {
        Files.copy(is, minio);
      }
    }
    if (!Files.isExecutable(minio)) {
      Files.setPosixFilePermissions(minio, new HashSet<>(Arrays.asList(OWNER_READ, OWNER_EXECUTE)));
    }
    return minio;
  }

  @Override
  protected void after() {
    if (!this.useMinio) {
      return;
    }
    stopMinio();
    super.after();
  }

  private void stopMinio() {
    Process minioProcess = this.minioProcess;
    if (minioProcess == null) {
      return;
    }
    this.minioProcess = null;
    minioProcess.destroy();
    try {
      if (!minioProcess.waitFor(500, TimeUnit.MILLISECONDS)) {
        LOG.log(Level.WARNING, "Minio failed to exit after being destroyed");
        if (!minioProcess.destroyForcibly().waitFor(500, TimeUnit.MILLISECONDS)) {
          LOG.log(Level.SEVERE, "Minio failed to exit after being forceably destroyed");
        }
        ;
      }
    } catch (InterruptedException e) {
      LOG.log(Level.WARNING, "Interrupted waiting for minio to exit", e);
    }
  }

  private URL resolveMinioUrl() throws URISyntaxException {
    String minioName = String.format("minio-%s-%s", getNormalizedName(), getNormalizedArch());
    URL minioUrl = getClass().getResource("/minio/" + minioName);
    Objects.requireNonNull(minioUrl, "Cannot find minio for current OS and arch");
    return minioUrl;
  }

  private String getNormalizedName() {
    String name = System.getProperty("os.name");
    Objects.requireNonNull(name, "Missing os.name");
    String normalizedName;
    String lowerCaseName = name.toLowerCase();
    if (lowerCaseName.contains("windows")) {
      normalizedName = "windows";
    } else if (lowerCaseName.contains("mac os")) {
      normalizedName = "darwin";
    } else if (lowerCaseName.contains("linux")) {
      normalizedName = "linux";
    } else if (lowerCaseName.contains("freebsd")) {
      normalizedName = "freebsd";
    } else {
      throw new IllegalStateException("Cannot normalize OS name");
    }
    return normalizedName;
  }

  private String getNormalizedArch() {
    String arch = System.getProperty("os.arch");
    Objects.requireNonNull(arch, "Missing os.arch");
    String lowerCaseArch = arch.toLowerCase();
    String normalizedArch;
    if (lowerCaseArch.contains("64")) {
      normalizedArch = "amd64";
    } else if (lowerCaseArch.contains("arm")) {
      normalizedArch = "arm";
    } else if (lowerCaseArch.contains("x86")) {
      normalizedArch = "386";
    } else {
      throw new IllegalStateException("Cannot normalize OS name");
    }
    return normalizedArch;
  }

  public boolean isUseMinio() {
    return this.useMinio;
  }
}
