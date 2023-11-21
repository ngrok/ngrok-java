<p>
  <a href="https://ngrok.com">
    <img src="assets/ngrok.png?raw=true" alt="ngrok Logo" width="300" url="https://ngrok.com" />
  </a>
</p>

# The ngrok Agent SDK for Java

[![MIT licensed][mit-badge]][mit-url]
[![Apache-2.0 licensed][apache-badge]][apache-url]
[![Continuous integration][ci-badge]][ci-url]
![Java](https://img.shields.io/badge/Java-11+-orange)
[![Javadoc](https://img.shields.io/badge/JavaDoc-Online-green)](https://ngrok.github.io/ngrok-java/)
![Status](https://img.shields.io/badge/Status-Beta-yellow)

[mit-badge]: https://img.shields.io/badge/license-MIT-blue.svg
[mit-url]: https://github.com/ngrok/ngrok-rs/blob/main/LICENSE-MIT
[apache-badge]: https://img.shields.io/badge/license-Apache_2.0-blue.svg
[apache-url]: https://github.com/ngrok/ngrok-rs/blob/main/LICENSE-APACHE
[ci-badge]: https://github.com/ngrok/ngrok-java/actions/workflows/ci.yml/badge.svg
[ci-url]: https://github.com/ngrok/ngrok-java/actions/workflows/ci.yml

ngrok is a globally distributed reverse proxy commonly used for quickly getting a public URL to a
service running inside a private network, such as on your local laptop. The ngrok agent is usually
deployed inside a private network and is used to communicate with the ngrok cloud service.

This is the ngrok agent in library form, suitable for integrating directly into Java applications.
This allows you to quickly build ngrok into your application with no separate process to manage.

# Installation

## Maven

To use `ngrok-java`, you need to add the following to your `pom.xml`:

```xml
<project>
   <!-- your project definition -->

   <dependencies>
      <dependency>
         <groupId>com.ngrok</groupId>
         <artifactId>ngrok-java</artifactId>
         <version>${ngrok.version}</version>
      </dependency>
      <dependency>
         <groupId>com.ngrok</groupId>
         <artifactId>ngrok-java-native</artifactId>
         <version>${ngrok.version}</version>
         <!-- automatically detect classifier, you can also pin manually -->
         <classifier>${os.detected.classifier}</classifier>
         <scope>runtime</scope>
      </dependency>
   </dependencies>

   <build>
     <extensions>
         <extension>
             <groupId>kr.motd.maven</groupId>
             <artifactId>os-maven-plugin</artifactId>
             <version>1.7.0</version>
         </extension>
     </extensions>
   </build>
</project>
```

If you want to use [jetty](https://www.eclipse.org/jetty/) integration, also add:

```xml
<dependency>
   <groupId>com.ngrok</groupId>
   <artifactId>ngrok-jetty</artifactId>
   <version>${ngrok.version}</version>
</dependency>
```

(Java 17+) If you wish to use ngrok listeners as a [server socket](https://docs.oracle.com/en/java/javase/17/docs/api/java.base/java/net/ServerSocket.html), also add:

```xml
<dependency>
   <groupId>com.ngrok</groupId>
   <artifactId>ngrok-java-17</artifactId>
   <version>${ngrok.version}</version>
</dependency>
```

For example of how to setup your project, check out [ngrok-java-demo](https://github.com/ngrok/ngrok-java-demo/blob/main/pom.xml)

## Gradle

If you use gradle as a build system, you need to add to your build the following:

```kotlin
plugins {
    id("com.google.osdetector").version("1.7.3")
}

var ngrokVersion = "0.4.1"

dependencies {
    implementation("com.ngrok:ngrok-java:${ngrokVersion}")
    implementation("com.ngrok:ngrok-java-native:${ngrokVersion}:${osdetector.classifier}")
}
```

# Documentation

## Quickstart

```java
package com.ngrok.quickstart;

import com.ngrok.Session;
import com.ngrok.Http;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Quickstart {
    public static void main(String[] args) throws IOException {
        // Session.withAuthtokenFromEnv() will create a new session builder, pulling NGROK_AUTHTOKEN env variable.
        // You can get your authtoken by registering at https://dashboard.ngrok.com
        var sessionBuilder = Session.withAuthtokenFromEnv().metadata("my session");
        // Session.Builder let you customize different aspects of the session, see docs for details.
        // After customizing the builder, you connect:
        try (var session = sessionBuilder.connect()) {
            // Creates and configures http listener that will be using oauth to secure it
            var listenerBuilder = session.httpEndpoint().metadata("my listener")
                    .oauthOptions(new Http.OAuth("google"));
            // Now start listening with the above configuration
            try (var listener = listenerBuilder.listen()) {
                System.out.println("ngrok url: " + listener.getUrl());
                var buf = ByteBuffer.allocateDirect(1024);

                while (true) {
                    // Accept a new connection
                    var conn = listener.accept();

                    // Read from the connection
                    conn.read(buf);

                    System.out.println(buf.asCharBuffer());

                    // Or write to it
                    conn.write(buf);
                }
            }
        }
    }
}
```

## Setting Up Java Toolchain

You may either:

1. Copy `./toolchains.xml` into `~/.m2/`, or
2. When running `mvn`, run as `mvn --global-toolchains ./toolchains.xml`

## Configuring Logging

Log level is set from the your `slf4j` implementation's configuration. This level must be assigned before creating a session, as it is read on creation.

As an example, to configure `slf4j-simple`, you can do:

```java
   System.setProperty("org.slf4j.simpleLogger.log.com.ngrok.Runtime", "debug");
```

You can then log through the `Runtime` API:

```java
   Runtime.getLogger().log("info", "myClass", "Hello World");
```

## Connection Callbacks

You may subscribe to session events from ngrok:

```java
var builder = Session.newBuilder()
                .stopCallback(new Session.StopCallback() {
                    @Override
                    public void onStopCommand() {
                        System.out.println("onStopCommand");
                    }
                })
                .updateCallback(new Session.UpdateCallback() {
                    @Override
                    public void onUpdateCommand() {
                        System.out.println("onUpdateCommand");
                    }
                })
                .restartCallback(new Session.RestartCallback() {
                    @Override
                    public void onRestartCommand() {
                        System.out.println("onRestartCommand");
                    }
                });
```

These callbacks may be useful to your application in order to invoke custom logic in response to changes in your active session.

# Platform Support

JARs are provided on GitHub and Maven Central for the following platforms:

| OS      | i686 | x64 | aarch64 | armv7 |
| ------- | ---- | --- | ------- | ----- |
| Windows | ✓    | ✓   |         |       |
| MacOS   |      | ✓   | ✓       |       |
| Linux   | ✓    | ✓   | ✓       |       |
| Android |      |     | ✓       | ✓     |

# Join the ngrok Community

- Check out [our official docs](https://docs.ngrok.com)
- Read about updates on [our blog](https://ngrok.com/blog)
- Open an [issue](https://github.com/ngrok/ngrok-java/issues) or [pull request](https://github.com/ngrok/ngrok-java/pulls)
- Join our [Slack community](https://ngrok.com/slack)
- Follow us on [X / Twitter (@ngrokHQ)](https://twitter.com/ngrokhq)
- Subscribe to our [Youtube channel (@ngrokHQ)](https://www.youtube.com/@ngrokhq)

# License

This project is licensed under either of

- [Apache License, Version 2.0](http://www.apache.org/licenses/LICENSE-2.0)
- [MIT license](http://opensource.org/licenses/MIT)

at your option.

# Contribution

Unless you explicitly state otherwise, any contribution intentionally submitted
for inclusion in ngrok-java by you, as defined in the Apache-2.0 license, shall be
dual licensed as above, without any additional terms or conditions.
