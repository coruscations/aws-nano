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

package com.coruscations.aws.s3.fs;

import com.coruscations.aws.ConfigurationProvider;
import com.coruscations.aws.Constants;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessMode;
import java.nio.file.CopyOption;
import java.nio.file.DirectoryStream;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemAlreadyExistsException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileAttribute;
import java.nio.file.attribute.FileAttributeView;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.logging.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static java.lang.String.format;

public class S3FileSystemProvider extends FileSystemProvider {

  private static final Logger LOG = Logger.getLogger(S3FileSystemProvider.class.getName());

  // TODO: Socket Settings
//  public static final String CONNECTION_TIMEOUT = "CONNECTION_TIMEOUT";
//  public static final String MAX_CONNECTIONS = "MAX_CONNECTIONS";
//  public static final String MAX_ERROR_RETRY = "MAX_ERROR_RETRY";
//  public static final String PROTOCOL = "PROTOCOL";
//  public static final String PROXY_DOMAIN = "PROXY_DOMAIN";
//  public static final String PROXY_HOST = "PROXY_HOST";
//  public static final String PROXY_PASSWORD = "PROXY_PASSWORD";
//  public static final String PROXY_PORT = "PROXY_PORT";
//  public static final String PROXY_USERNAME = "PROXY_USERNAME";
//  public static final String PROXY_WORKSTATION = "PROXY_WORKSTATION";
//  public static final String SOCKET_SEND_BUFFER_SIZE_HINT = "SOCKET_SEND_BUFFER_SIZE_HINT";
//  public static final String SOCKET_RECEIVE_BUFFER_SIZE_HINT = "SOCKET_RECEIVE_BUFFER_SIZE_HINT";
//  public static final String SOCKET_TIMEOUT = "SOCKET_TIMEOUT";

  private final ConcurrentMap<FSKey, S3FileSystem> cache = new ConcurrentSkipListMap<>();

  @Override
  public String getScheme() {
    return Constants.SCHEME;
  }

  @Override
  @Nonnull
  public FileSystem newFileSystem(URI uri, Map<String, ?> env) throws IOException {
    FSKey fsKey = fsKey(uri, env);
    if (this.cache.containsKey(fsKey)) {
      throw new FileSystemAlreadyExistsException(
          format("Connection to endpoint %s via the provided credentials already exists.",
                 fsKey.getEndpoint()));
    }
    return new S3FileSystem(fsKey, env, this);
  }

  @Override
  @Nonnull
  public FileSystem getFileSystem(URI uri) {
    FileSystem fileSystem = findFileSystem(uri);
    if (fileSystem == null) {
      throw new FileSystemNotFoundException(format("No filesystem found for: %s", uri));
    }
    return fileSystem;
  }

  @Nullable
  private FileSystem findFileSystem(URI uri) {
    boolean noAuthority = uri.getAuthority() == null;
    boolean noHost = uri.getHost() == null;
    if (noAuthority && noHost) {
      LOG.fine("No endpoint or credentials provided via host and user info; " +
               "these may be required to get the expected filesystem.");
    } else if (noAuthority) {
      LOG.fine("No credentials provided via user info; " +
               "this may be required to get the expected filesystem.");
    } else if (noHost) {
      LOG.fine("No endpoint provided via host; " +
               "this may be required to get the expected filesystem.");
    }
    FSKey fsKey = fsKey(uri, Collections.emptyMap());
    S3FileSystem s3FileSystem = this.cache.get(fsKey);
    if (s3FileSystem != null) {
      return s3FileSystem;
    }
    try {
      // Finds by best prefix
      s3FileSystem = findByUri(fsKey.toURI(true));
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Invalid URI for: " + uri, e);
    }
    if (s3FileSystem != null) {
      return s3FileSystem;
    }
    return null;
  }

  S3FileSystem remove(FSKey fsKey) {
    return this.cache.remove(fsKey);
  }

  private FSKey fsKey(URI uri, Map<String, ?> env) {
    String[] userInfo = uri.getRawUserInfo() == null ?
                        new String[0] : uri.getRawUserInfo().split(":");
    String accessKey = null;
    String secretKey = null;
    if (userInfo.length == 2 && !userInfo[0].isEmpty() && !userInfo[1].isEmpty()) {
      accessKey = decode(userInfo[0]);
      secretKey = decode(userInfo[1]);
    }
    String endpoint = uri.getHost();
    ConfigurationProvider configurationProvider = null;
    if (accessKey == null || secretKey == null) {
      configurationProvider = new ConfigurationProvider(env);
      accessKey = configurationProvider.getAccessKey();
      secretKey = configurationProvider.getSecretKey();
    }
    if (endpoint == null) {
      if (configurationProvider == null) {
        configurationProvider = new ConfigurationProvider(env);
      }
    }
    return new FSKey(accessKey, secretKey, endpoint, uri.getPath());
  }

  /**
   * When no filesystem matching the URI is found, this creates a filesystem to the endpoint (i.e.
   * all buckets) and returns the path from there.
   *
   * @see FileSystemProvider#getPath(URI)
   */
  @Override
  public Path getPath(URI uri) {
    FileSystem fileSystem = findFileSystem(uri);
    if (fileSystem == null) {
      try {
        URI noPathUri =
            new URI(Constants.SCHEME, uri.getUserInfo(), uri.getHost(), uri.getPort(), null, null, null);
        fileSystem = newFileSystem(noPathUri, Collections.emptyMap());
      } catch (Exception e) {
        FileSystemNotFoundException ex =
            new FileSystemNotFoundException("Cannot create filesystem for: " + uri);
        ex.initCause(e);
        throw ex;
      }
    }
    if (fileSystem == null) {
      throw new FileSystemNotFoundException("Cannot create filesystem for: " + uri);
    }
    return fileSystem.getPath(uri.getPath());
  }

  @Override
  public SeekableByteChannel newByteChannel(Path path, Set<? extends OpenOption> options,
                                            FileAttribute<?>... attrs) throws IOException {
    S3Path s3path = toS3Path(path);
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public DirectoryStream<Path> newDirectoryStream(Path dir,
                                                  DirectoryStream.Filter<? super Path> filter)
      throws IOException {
    S3Path s3dir = toS3Path(dir);
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public void createDirectory(Path dir, FileAttribute<?>... attrs) throws IOException {
    S3Path s3dir = toS3Path(dir);
    // Todo: Create an implementation for this method.

  }

  @Override
  public void delete(Path path) throws IOException {
    S3Path s3path = toS3Path(path);
    // Todo: Create an implementation for this method.

  }

  @Override
  public void copy(Path source, Path target, CopyOption... options) throws IOException {
    S3Path s3Source = toS3Path(source);
    S3Path s3Target = toS3Path(target);
    // Todo: Create an implementation for this method.

  }

  @Override
  public void move(Path source, Path target, CopyOption... options) throws IOException {
    S3Path s3Source = toS3Path(source);
    S3Path s3Target = toS3Path(target);
    // Todo: Create an implementation for this method.

  }

  @Override
  public boolean isSameFile(Path path1, Path path2) throws IOException {
    S3Path s3path1 = toS3Path(path1);
    S3Path s3path2 = toS3Path(path2);
    // Todo: Create an implementation for this method.
    return false;
  }

  @Override
  public boolean isHidden(Path path) throws IOException {
    // No concept of hidden on S3
    return false;
  }

  @Override
  public FileStore getFileStore(Path path) throws IOException {
    // TODO: Operate on specific buckets; error on attempts to modify endpoint???
    return null;
  }

  @Override
  public void checkAccess(Path path, AccessMode... modes) throws IOException {
    S3Path s3path = toS3Path(path);
    // Todo: Create an implementation for this method.

  }

  @Override
  public <V extends FileAttributeView> V getFileAttributeView(Path path, Class<V> type,
                                                              LinkOption... options) {
    S3Path s3path = toS3Path(path);
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public <A extends BasicFileAttributes> A readAttributes(Path path, Class<A> type,
                                                          LinkOption... options)
      throws IOException {
    S3Path s3path = toS3Path(path);
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Map<String, Object> readAttributes(Path path, String attributes, LinkOption... options)
      throws IOException {
    S3Path s3path = toS3Path(path);
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public void setAttribute(Path path, String attribute, Object value, LinkOption... options)
      throws IOException {
    S3Path s3path = toS3Path(path);
    // Todo: Create an implementation for this method.

  }

  private S3FileSystem findByUri(URI uri) {
    String[] userInfo = uri.getUserInfo() == null ? null : uri.getUserInfo().split(":");
    boolean includeUserInfo = userInfo != null && userInfo.length == 2 &&
                              !userInfo[0].isEmpty() && !userInfo[1].isEmpty();
    String uriString = uri.toString();
    try {
      for (Map.Entry<FSKey, S3FileSystem> entry : this.cache.entrySet()) {
        URI u = entry.getKey().toURI(includeUserInfo);
        if (uriString.startsWith(u.toString())) {
          return entry.getValue();
        }
      }
    } catch (URISyntaxException e) {
      throw new IllegalStateException("Invalid FS Key", e);
    }
    return null;
  }

  private S3Path toS3Path(Path path) {
    if (path instanceof S3Path) {
      return (S3Path) path;
    }
    throw new IllegalArgumentException(format("Path must be an instance of %s",
                                              S3Path.class.getName()));
  }

  /**
   * Borrowed from Spring, released under Apache 2.0.
   *
   * @param source Encoded value.
   * @return Decoded value
   */
  public static String decode(String source) {
    int length = source.length();
    ByteArrayOutputStream bos = new ByteArrayOutputStream(length);
    boolean changed = false;
    for (int i = 0; i < length; i++) {
      int ch = source.charAt(i);
      if (ch == '%') {
        if ((i + 2) < length) {
          char hex1 = source.charAt(i + 1);
          char hex2 = source.charAt(i + 2);
          int u = Character.digit(hex1, 16);
          int l = Character.digit(hex2, 16);
          if (u == -1 || l == -1) {
            throw new IllegalArgumentException(format("Invalid encoded sequence \"%s\"",
                                                      source.substring(i)));
          }
          bos.write((char) ((u << 4) + l));
          i += 2;
          changed = true;
        } else {
          throw new IllegalArgumentException(format("Invalid encoded sequence \"%s\"",
                                                    source.substring(i)));
        }
      } else {
        bos.write(ch);
      }
    }
    return (changed ? new String(bos.toByteArray()) : source);
  }

}
