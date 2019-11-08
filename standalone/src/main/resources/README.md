### Rationale
The standalone module is meant to be run from command line via `java -jar standalone.jar`. As such,
all dependencies are bundled within the JAR via the
[gradle shadow plugin](https://github.com/johnrengelman/shadow).

### Note
Maven central requires uploaded artifacts to be accompanied by both a sources and a javadoc JAR
(see [Central Repository requirements](https://central.sonatype.org/pages/requirements.html#supply-javadoc-and-sources)).
On the other hand, as the gradle shadow plugin currently does not support creating sources or javadoc
for all packaged classes (see [issue 41](https://github.com/johnrengelman/shadow/issues/41)), we
provide only fake JARs and ask users to directly look at our
[Github repository](https://github.com/TNG/keycloak-mock).
