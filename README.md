# GMP [![Build Status](https://travis-ci.org/epam/GMP.svg?branch=master)](https://travis-ci.org/epam/GMP) [![Known Vulnerabilities](https://snyk.io/test/github/epam/GMP/badge.svg?targetFile=build.gradle)](https://snyk.io/test/github/epam/GMP?targetFile=build.gradle)
**Groovy ManiPulator**
is a tool to run groovy scripts organized in folder structure with configuration. Script group may have common parameters in common-config.groovy. Each script may have "personal" configuration. Each script group has it's personal classloader. This approach allows to have scripts chain executed concurrently but working with different library versions. For example two script set gathering data from two different versions of weblogic. 

## Main features
1.  Method to store Groovy scripts with modular structure (Gradle/groovy based)
2.  Easy to share/update modules (maven repository based)
3.  Dependency isolation
4.  Common and personal script configuration within a script group/global
5.  Layered configuration. (It is possible to have configuration parameters for "dev", "prod"  etc. config sets.)

## Dev features
1.  Everything is based on opensource technologies groovy/gradle/maven
2.  Easy to debug
3.  Configuration properties injection. (No need to implement configuration options.)
4.  GMP is based on Spring. So it is could be easy extended.

## Changes

### v 0.0.15

#### Environment variables

1.  gmp.home - path to gmp-home folder in URL format example: -Dgmp.home=file:///d:/gmp-home
2.  gmp.log.home - path to logs folder example:  -Dgmp.log.home=d:/gmp-home

#### Execution

java -jar {jarname} -Dgmp.home=file:///d:/gmp-home/ -Dgmp.log.home=d:/gmp-home {environment}@{script-path}/{script-name} {script params}
