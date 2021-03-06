## Introduction

The primary goal of this project is to make it easier to write Spring-powered integration tests that rely on PostgreSQL database. This library is responsible for creating and managing isolated embedded databases for each test class or test method, based on test configuration.

## Features

* Supports both `Spring` and `Spring Boot` frameworks
    * Supported versions are Spring 4.3.0+ and Spring Boot 1.4.0+
* Automatic integration with Spring TestContext framework
    * Context caching is fully supported
* Seamless integration with Flyway database migration tool
    * Just place `@FlywayTest` annotation on test class or method
* Optimized initialization and cleaning of embedded databases
    * Database templates are used to reduce the loading time
* Uses lightweight bundles of PostgreSQL binaries with reduced size
    * https://github.com/zonkyio/embedded-postgres-binaries
* Support for running in Docker, including Alpine Linux

## Quick Start

### Maven Configuration

Add the following Maven dependency:

```xml
<dependency>
    <groupId>io.zonky.test</groupId>
    <artifactId>embedded-database-spring-test</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
</dependency>
```

The default version of the embedded database is `PostgreSQL 10.4`, but you can change it using the instructions described in [Changing the version of postgres binaries](#changing-the-version-of-postgres-binaries).

### Basic Usage

The configuration of the embedded database is driven by `@AutoConfigureEmbeddedDatabase` annotation. Just place the annotation on a test class and that's it! The existing data source will be replaced by the testing one, or a new data source will be created.

### Examples

#### Creating a new empty database with a specified bean name

A new data source with a specified name will be created and injected into all related components. You can also inject it into test class as shown below. 

```java
@RunWith(SpringRunner.class)
@AutoConfigureEmbeddedDatabase(beanName = "dataSource")
public class EmptyDatabaseIntegrationTest {
    
    @Autowired
    private DataSource dataSource;
    
    // class body...
}
```

#### Replacing an existing data source with an empty database

In case the test class uses a context configuration that already contains a data source bean, it will be automatically replaced by the testing data source. Please note that if the context contains multiple data sources the bean name must be specified by `@AutoConfigureEmbeddedDatabase(beanName = "dataSource")` to identify the data source that will be replaced. The newly created data source bean will be injected into all related components and you can also inject it into test class.

```java
@RunWith(SpringRunner.class)
@AutoConfigureEmbeddedDatabase
@ContextConfiguration("path/to/app-config.xml")
public class EmptyDatabaseIntegrationTest {
    // class body...
}
```

#### Using `@FlywayTest` annotation on a test class

The library supports the use of `@FlywayTest` annotation. When you use it, the embedded database will be automatically initialized and cleaned by Flyway database migration tool. If you don't specify any custom migration locations the default path `db/migration` will be applied.

```java
@RunWith(SpringRunner.class)
@FlywayTest
@AutoConfigureEmbeddedDatabase
@ContextConfiguration("path/to/app-config.xml")
public class FlywayMigrationIntegrationTest {
    // class body...
}
```

#### Using `@FlywayTest` annotation with additional options

In case you want to apply migrations from some additional locations, you can use `@FlywayTest(locationsForMigrate = "path/to/migrations")` configuration. In this case, the sql scripts from the default location and also sql scripts from the additional locations will be applied. If you need to prevent the loading of the scripts from the default location you can use `@FlywayTest(overrideLocations = true, ...)` annotation configuration.

See [Usage of Annotation FlywayTest](https://github.com/flyway/flyway-test-extensions/wiki/Usage-of-Annotation-FlywayTest) for more information about configuration options of `@FlywayTest` annotation.

```java
@RunWith(SpringRunner.class)
@FlywayTest(locationsForMigrate = "test/db/migration")
@AutoConfigureEmbeddedDatabase
@ContextConfiguration("path/to/app-config.xml")
public class FlywayMigrationIntegrationTest {
    // class body...
}
```

#### Using `@FlywayTest` annotation on a test method

It is also possible to use `@FlywayTest` annotation on a test method. In such case, the isolated embedded database will be created and managed for the duration of the test method. If you don't specify any custom migration locations the default path `db/migration` will be applied.

```java
@RunWith(SpringRunner.class)
@AutoConfigureEmbeddedDatabase
@ContextConfiguration("path/to/app-config.xml")
public class FlywayMigrationIntegrationTest {
    
    @Test
    @FlywayTest(locationsForMigrate = "test/db/migration")
    public void testMethod() {
        // method body...
    }
}
```

#### Using `@AutoConfigureEmbeddedDatabase` and `@DataJpaTest` annotations together

Spring Boot provides several annotations to simplify writing integration tests.
One of them is the `@DataJpaTest` annotation, which can be used when a test focuses only on JPA components.
By default, tests annotated with `@DataJpaTest` will use an embedded in-memory database. **This in-memory database can be H2, Derby or HSQL, but not the PostgreSQL database**.
To change that, you must first disable the default in-memory database by `@AutoConfigureTestDatabase(replace = NONE)` and enable the PostgreSQL database by `@AutoConfigureEmbeddedDatabase` instead. 

```java
@RunWith(SpringRunner.class)
@AutoConfigureTestDatabase(replace = NONE)
@AutoConfigureEmbeddedDatabase
@DataJpaTest
public class SpringDataJpaAnnotationTest {
    // class body...
}
```
You can also consider creating a custom [composed annotation](https://github.com/spring-projects/spring-framework/wiki/Spring-Annotation-Programming-Model#composed-annotations).

## Advanced

#### Changing the version of postgres binaries

Changing the version is performed by importing `embedded-postgres-binaries-bom` in the required version into your dependency management section.

```xml
<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>io.zonky.test.postgres</groupId>
            <artifactId>embedded-postgres-binaries-bom</artifactId>
            <version>10.4.0</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>
```

A list of all available versions of postgres binaries is here: https://mvnrepository.com/artifact/io.zonky.test.postgres/embedded-postgres-binaries-bom

Note that the release cycle of the postgres binaries is independent of the release cycle of this library, so you can upgrade to the new version of postgres binaries immediately after it is released.

#### Enabling support for additional architectures

By default, only the support for `amd64` architecture is enabled.
Support for other architectures can be enabled by adding the corresponding Maven dependency as shown in the example below.

```xml
<dependency>
    <groupId>io.zonky.test.postgres</groupId>
    <artifactId>embedded-postgres-binaries-linux-i386</artifactId>
    <scope>test</scope>
</dependency>
```

**Supported platforms:** `Darwin`, `Windows`, `Linux`, `Alpine Linux`  
**Supported architectures:** `amd64`, `i386`, `arm32v6`, `arm32v7`, `arm64v8`, `ppc64le`

Note that not all architectures are supported by all platforms, look here for an exhaustive list of all available artifacts: https://mvnrepository.com/artifact/io.zonky.test.postgres
  
Since `PostgreSQL 10.0` there are available special `alpine-lite` artifacts, which contain postgres binaries for Alpine Linux with disabled [ICU support](https://blog.2ndquadrant.com/icu-support-postgresql-10/) for further size reduction.

#### Disabling auto-configuration

By default, the library automatically registers all necessary context customizers and test execution listeners.
If this behavior is inappropriate for some reason, you can deactivate it by exclusion of the `embedded-database-spring-test-autoconfigure` dependency.

```xml
<dependency>
    <groupId>io.zonky.test</groupId>
    <artifactId>embedded-database-spring-test</artifactId>
    <version>1.3.0</version>
    <scope>test</scope>
    <exclusions>
        <exclusion>
            <groupId>io.zonky.test</groupId>
            <artifactId>embedded-database-spring-test-autoconfigure</artifactId>
        </exclusion>
    </exclusions>
</dependency>
```

#### Customizing database configuration

If necessary, you can customize the configuration of the embedded database with bean implementing `Consumer<EmbeddedPostgres.Builder>` interface.
The obtained builder provides methods to change the configuration before the database is started.

```java
@Configuration
public class EmbeddedPostgresConfiguration {
    
    @Bean
    public Consumer<EmbeddedPostgres.Builder> embeddedPostgresCustomizer() {
        return builder -> builder.setPGStartupWait(Duration.ofSeconds(60L));
    }
}
```

```java
@RunWith(SpringRunner.class)
@AutoConfigureEmbeddedDatabase
@ContextConfiguration(classes = EmbeddedPostgresConfiguration.class)
public class EmbeddedPostgresIntegrationTest {
    // class body...
}
```

#### Background bootstrapping mode

Using this feature causes that the initialization of the data source and the execution of Flyway database migrations are performed in background bootstrap mode.
In such case, a `DataSource` proxy is immediately returned for injection purposes instead of waiting for the Flyway's bootstrapping to complete.
However, note that the first actual call to a data source method will then block until the Flyway's bootstrapping completed, if not ready by then.
For maximum benefit, make sure to avoid early data source calls in init methods of related beans.

```java
@Configuration
public class BootstrappingConfiguration {
    
    @Bean
    public FlywayDataSourceContext flywayDataSourceContext(TaskExecutor bootstrapExecutor) {
        DefaultFlywayDataSourceContext dataSourceContext = new DefaultFlywayDataSourceContext();
        dataSourceContext.setBootstrapExecutor(bootstrapExecutor);
        return dataSourceContext;
    }

    @Bean
    public TaskExecutor bootstrapExecutor() {
        return new SimpleAsyncTaskExecutor("bootstrapExecutor-");
    }
}
```

```java
@RunWith(SpringRunner.class)
@AutoConfigureEmbeddedDatabase
@ContextConfiguration(classes = BootstrappingConfiguration.class)
public class FlywayMigrationIntegrationTest {
    // class body...
}
```

## Troubleshooting

#### Running tests in Docker does not work

Running in Docker is fully supported, including Alpine Linux. But you must keep in mind that the **PostgreSQL database requires running under a non-root user**. Otherwise, the database does not start and fails with an error.

#### Frequent and repeated initialization of the database

Make sure that you do not use `org.flywaydb.test.junit.FlywayTestExecutionListener`. Because this library has its own test execution listener that can optimize database initialization.
But this optimization has no effect if `FlywayTestExecutionListener` is applied.

## Building from Source
The project uses a [Gradle](http://gradle.org)-based build system. In the instructions
below, [`./gradlew`](http://vimeo.com/34436402) is invoked from the root of the source tree and serves as
a cross-platform, self-contained bootstrap mechanism for the build.

### Prerequisites

[Git](http://help.github.com/set-up-git-redirect) and [JDK 8 or later](http://www.oracle.com/technetwork/java/javase/downloads)

Be sure that your `JAVA_HOME` environment variable points to the `jdk1.8.0` folder
extracted from the JDK download.

### Check out sources
`git clone git@github.com:zonkyio/embedded-database-spring-test.git`

### Compile and test
`./gradlew build`

## Project dependencies

* [Spring Framework](http://www.springsource.org/) (4.3.10) - `spring-test`, `spring-context` modules
* [OpenTable Embedded PostgreSQL Component](https://github.com/opentable/otj-pg-embedded) (0.12.0)
* [Flyway](https://github.com/flyway/) (5.0.7)
* [Guava](https://github.com/google/guava) (23.0)

## License
The project is released under version 2.0 of the [Apache License](http://www.apache.org/licenses/LICENSE-2.0.html).
