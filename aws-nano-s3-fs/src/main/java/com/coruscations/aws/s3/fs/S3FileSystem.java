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

import java.io.IOException;
import java.nio.file.FileStore;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.WatchService;
import java.nio.file.attribute.UserPrincipalLookupService;
import java.nio.file.spi.FileSystemProvider;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

public class S3FileSystem extends FileSystem {

  private final FSKey fsKey;
  private final Map<String, ?> env;

  private final S3FileSystemProvider provider;

  private AtomicBoolean open = new AtomicBoolean(true);

  public S3FileSystem(FSKey fsKey, Map<String, ?> env, S3FileSystemProvider provider) {
    this.fsKey = fsKey;
    this.env = env;
    this.provider = provider;
  }

  @Override
  public FileSystemProvider provider() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public void close() throws IOException {
    if (this.open.compareAndSet(true, false)) {
      this.provider.remove(this.fsKey);
      // TODO: Shutdown all connections
    }
  }

  @Override
  public boolean isOpen() {
    return this.open.get();
  }

  @Override
  public boolean isReadOnly() {
    // TODO: Use AWS permissions
    return false;
  }

  @Override
  public String getSeparator() {
    return "/";
  }

  @Override
  public Iterable<Path> getRootDirectories() {
    // TODO: Endpoints - list buckets
    if (isEndpoint()) {

    }
    // TODO: Buckets - list dirs
    // TODO: Prefixes - list sub prefixes
    return null;
  }

  @Override
  public Iterable<FileStore> getFileStores() {
    // TODO: Bucket is largest FileStore
    return null;
  }

  @Override
  public Set<String> supportedFileAttributeViews() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path getPath(String first, String... more) {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public PathMatcher getPathMatcher(String syntaxAndPattern) {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public UserPrincipalLookupService getUserPrincipalLookupService() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public WatchService newWatchService() throws IOException {
    // Todo: Create an implementation for this method.
    return null;
  }

  private boolean isEndpoint() {
    return this.fsKey.getPrefixes().length == 0;
  }

  private boolean isBucket() {
    return this.fsKey.getPrefixes().length == 1;
  }

  private boolean hasPrefix() {
    return this.fsKey.getPrefixes().length > 1;
  }
}
