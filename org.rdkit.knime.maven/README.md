# Maven Plugin org.rdkit.knime:knime-maven-dependencies-injection #

This folder contains a Maven Plugin that supports you to update all JAR files that an Eclipse plugin relies on.

## How an Eclipse plugin refers to libraries ##

In the world of Eclipse plugins, the normal Maven mechanisms are not working. There are different
steps necessary to configure the dependencies when building a plugin. 

### 1. Making the libraries available in lib folder ###

As first step all JAR files must be made available in a folder, usually the `lib` folder.

### 2. Referencing the libraries in the META-INF/MANIFEST.MF file ###

The `MANIFEST.MF` file references in the `Require-Bundle` attribute other Eclipse plugins, 
which expose APIs, and in the `Bundle-ClassPath` attribute the JAR files that we bring ourselves
in our `lib` folder. As the JAR files usually contain the version numbers in their names,
we need to update the `MANIFEST.MF` file with the new names as soon as we change the JAR files.

### 3. Referencing the libraries in the build.properties file ###

The `build.properties` file in the project folder contains the attributes `bin.includes` and 
`jars.extra.classpath`. Both attributes contain beside some folder references also the full
list of all JAR files that we require from our `lib` folder.

This Maven plugin helps to execute step 2 and 3 for you fully automatically. It will save a lot of time
and typos with getting library updates properly done.

## Seeing the plugin in action ##

This Maven plugin is used in the `org.rdkit.knime.nodes` subproject. 
Please refer to the documentation contained in the `lib` folder there.

## Who do I talk to for further information? ##

* Manuel Schwarze, Novartis Biomedical Research, RX Software Engineering
