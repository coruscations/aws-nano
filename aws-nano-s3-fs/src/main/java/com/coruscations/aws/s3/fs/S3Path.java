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

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Iterator;

public class S3Path implements Path {

  @Override
  public FileSystem getFileSystem() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public boolean isAbsolute() {
    // Todo: Create an implementation for this method.
    return false;
  }

  @Override
  public Path getRoot() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path getFileName() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path getParent() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public int getNameCount() {
    // Todo: Create an implementation for this method.
    return 0;
  }

  @Override
  public Path getName(int index) {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path subpath(int beginIndex, int endIndex) {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public boolean startsWith(Path other) {
    // Todo: Create an implementation for this method.
    return false;
  }

  @Override
  public boolean startsWith(String other) {
    // Todo: Create an implementation for this method.
    return false;
  }

  @Override
  public boolean endsWith(Path other) {
    // Todo: Create an implementation for this method.
    return false;
  }

  @Override
  public boolean endsWith(String other) {
    // Todo: Create an implementation for this method.
    return false;
  }

  @Override
  public Path normalize() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path resolve(Path other) {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path resolve(String other) {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path resolveSibling(Path other) {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path resolveSibling(String other) {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path relativize(Path other) {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public URI toUri() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path toAbsolutePath() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Path toRealPath(LinkOption... options) throws IOException {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public File toFile() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public WatchKey register(WatchService watcher, WatchEvent.Kind<?>[] events,
                           WatchEvent.Modifier... modifiers) throws IOException {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public WatchKey register(WatchService watcher, WatchEvent.Kind<?>... events) throws IOException {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public Iterator<Path> iterator() {
    // Todo: Create an implementation for this method.
    return null;
  }

  @Override
  public int compareTo(Path other) {
    // Todo: Create an implementation for this method.
    return 0;
  }
}
