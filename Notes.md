# Developing a Custom File System Provider
## Introduction
The NIO.2 API introduced in the Java SE 7 release provides the ability to develop a custom file system provider that can be used to manage file system objects. A file system is essentially a container with organized, homogenous elements referred to as file system objects. A file system provides access to file system objects. A file system object can be a file store, file, or directory. A file store is a volume or partition in which files are stored. For example, in a native file system such as on the Windows platform, commonly known drives like c: or d: are file stores. On the Solaris operating system, / (root) and mounted directories are considered file stores.

The java.nio.file.spi.FileSystemProvider class allows you to develop a custom file system provider. A custom file system provider is useful in the following situations:

- Developing a memory-based or zip-file-based file system
- Developing a fault-tolerant distributed file system
- Replacing or supplementing the default file system provider. The custom provider can augment the default provider by performing specific operations, such as logging all system operations, and delegate to the default provider for other routine operations.

## Overview of the java.nio.file.spi.FileSystemProvider Class

A custom file system provider must implement the java.nio.file.spi.FileSystemProvider class. A file system provider is identified by a URI scheme such as file, jar, memory, cd.

An implementation of the java.nio.file.spi.FileSystemProvider class is a factory for instances of the java.nio.file.FileSystem class. A file system's URI has a URI scheme that matches the URI scheme of the file system provider that created it.

The newFileSystem method is used to create a file system and the getFileSystem method is used to retrieve a reference to an existing file system.

## Implementing a Custom File System Provider

This section describes the high-level steps necessary to create a custom file system provider using the java.nio.file.spi.FileSystemProvider API. The ZipFileSystemProvider class that is shipped in the demo/nio/zipfs of your JDK installation is an example of a custom file system provider. See Resources for information about the zip file system provider.

### Implementing the Custom File System Provider Class

Implementing the custom file system provider class involves the following operations:

- Create a custom file system provider class, such as MyFileSystemProvider, that extends the java.nio.file.spi.FileSystemProvider class.
- Define a URI scheme such as jar for the file system provider. The getScheme method should return the URI scheme of this provider.
- Create an internal cache to keep track of file systems created by this provider.
- Implement the newFileSystem method. The method will create a new custom file system at the specified path and add the file system to cache. This method should throw a java.nio.file.FileSystemAlreadyExistsException exception if a file system already exists at the specified path.
- Implement the getFileSystem method. This method should search the cache and return a previously created instance of a file system that corresponds to the given URI.
- Implement the newFileChannel method or the newAsynchronousFileChannel method depending on the requirements of your file system provider. This method should return a FileChannel object that allows a file to be read or written in the file system.

### Implementing the Custom File System Class

Implementing the custom file system class involves the following operations:

- Create a class, such as MyFileSystem, that extends the java.nio.file.FileSystem class.
- Implement the methods of your file system class depending on the characteristics of the desired file system. Some characteristics of a file system are as follows:
  - Number of roots – A file system can have a single hierarchy of files with one root, or multiple hierarchies.
  - Read and write access – A file system can be read-only or read/write.
  - File store – A file system requires an underlying file store. The attributes that can be set for a file will vary depending on the underlying file store.
