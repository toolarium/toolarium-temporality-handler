[![License](https://img.shields.io/github/license/toolarium/toolarium-temporality-handler)](https://github.com/toolarium/toolarium-temporality-handler/blob/master/LICENSE)
[![Maven Central](https://img.shields.io/maven-central/v/com.github.toolarium/toolarium-temporality-handler/0.9.2)](https://search.maven.org/artifact/com.github.toolarium/toolarium-temporality-handler/0.9.2/jar)
[![javadoc](https://javadoc.io/badge2/com.github.toolarium/toolarium-temporality-handler/javadoc.svg)](https://javadoc.io/doc/com.github.toolarium/toolarium-temporality-handler)

# toolarium-temporality-handler

If data is to be stored in a chronological timeline, a corresponding validity is needed in addition to the data. Typically, from / to time stamps are used. This library takes over the entire logic of temporal storage. Temporal actions such as inserting, updating, deleting, terminating etc. of data records are completely implemented.

The backend part must be implemented individually. This is covered by a simple interface IDAOService which contains the methods write, delete and search.

```java
IDAOService<MyObject> daoService = ...
MyObject myObject = ...
TemporalityHandlerFactory.getInstance().getTemporalityHandler().writeTemporlityRecord(myObject), daoService);
```

The object in the above example must implement the ITemporalityRecord interface. This defines the uniuqe primary key, a data key which is a logical key to the data and the validity information.
Please see the test cases where all this is covered.

## Built With

* [cb](https://github.com/toolarium/common-build) - The toolarium common build

## Versioning

We use [SemVer](http://semver.org/) for versioning. For the versions available, see the [tags on this repository](https://github.com/toolarium/toolarium-temporality-handler/tags). 
