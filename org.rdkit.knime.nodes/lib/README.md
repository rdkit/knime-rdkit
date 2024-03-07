# Updating Libraries To Latest Versions

This folder contains all JAR files that the RDKit nodes rely on. The pom.xml file defines the
Maven dependencies to other Maven packages that we are using, for instance opsin. 
Those Maven packages have again their own dependencies, and at the end this will result in a
dependency tree of JAR files, which ideally should be resolved with Maven mechanisms. 
As soon as security vulnerabilities are discovered we may need to update to latest versions,
and based on this change the whole list of JAR files will change to higher (not always latest) versions.

Traditionally, we check in all JAR files into git to have always access to the JAR files, regardless
if they would be available online in Maven repositories in the future or not.

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

## Automating the necessary library updates ##

The libraries can be updated in the `lib` folder semi-automatically with the following procedure.

### 1. Compile and build a Maven plugin org.rdkit.knime:knime-maven-dependencies-injection ###

In the KNIME RDKit sub project folder `org.rdkit.knime.maven` is a little Maven plugin that was
developed to update the `MANIFEST.MF` and `build.properties` file with the list of JAR files that
can be located within certain sub folders. In Eclipse just right-click on the `org.rdkit.knime.maven` folder
an select from the context menu Run As - Maven Install. This builds the plugin and installs it 
in your local Maven repository. Afterwards, it is available in Step 2.

### 2. Update the lib/pom.xml file ###

List in the `pom.xml` file all first level dependencies that the RDKit nodes require and set the version
to the version you want to use, e.g. 

```
	<dependencies>
		<dependency>
			<groupId>uk.ac.cam.ch.opsin</groupId>
			<artifactId>opsin-core</artifactId>
			<version>2.8.0</version>
		</dependency>
		<dependency>
			<groupId>uk.ac.cam.ch.opsin</groupId>
			<artifactId>opsin-core</artifactId>
			<version>2.8.0</version>
			<classifier>sources</classifier>
		</dependency>
	</dependencies>
```

You may also include sources and javadocs as you can see in the example. Those JAR files will be ignored in later
automation steps, but can be useful for development.

### 3. Update the libraries based on your latest pom.xml changes ###

Run the `UPDATE LIBRARIES.launch` file, which will run Maven as 
`mvn -U generate-resources` and will perform the following steps for you:

* Deleting all JAR files in the configured lib folder that contains the `pom.xml` file
* Resolving all defined library dependencies and fetching the full dependency tree into the `lib` folder with the new JAR files
* Updating the `MANIFEST.MF` and `build.properties` files by removing old references JAR files from the `lib` folder and adding all 
	  new JAR files now contained in the `lib` folder 

These steps are usually done manually and can be quite time-consuming and error-prone. 

### 4. Test the update ###

Run the RDKit test suite and perform additional manual tests related to the updated functionalities to ensure 
that everything is working as expected.

### 5. Commit the changes ###

Commit the library changes and all other triggered changes like the ones done in `MANIFEST.MF` and `build.properties` files.

## Using the semi-automated library update in other KNIME Community plugins ##

When using the Maven plugin `org.rdkit.knime:knime-maven-dependencies-injection` inside another Eclipse plugin you
may follow these simple 5 steps (once). 

1. Create a library folder that is fully used for Maven-based resolutions of libraries, e.g. `lib/maven`. It should not contain any other JAR files.

2. Copy the `pom.xml` file from the RDKit Nodes `lib` folder into this folder.

3. Adapt all dependencies that are defined in the `pom.xml` file to your own Eclipse plugin needs.

4. Adapt the configuration of the `org.rdkit.knime:knime-maven-dependencies-injection` Maven plugin to work with your Eclipse plugin.

	a) Set the projectDirectory parameter: It must contain the relative path from the folder that contains your pom.xml file to the
	   main project folder of your plugin. That project folder must contain the `META-INF/MANIFEST.MF` and the `build.properties` file as
	   well as the relative libraryDirectories defined in b).

	b) Set the libraryDirectories parameter: Define here a comma-separated list of folders that contain the JAR files that are
	   resolved with Maven functionality. Usually, you have just one folder, the one you created in step 1), e.g. `lib/maven`.

5. Create yourself a launch file like the `UPDATE LIBRARIES.launch` file that runs Maven as `mvn -U generate-resources`, 
   and execute it. 

From now on you can manage your dependencies inside your `pom.xml` file and run it manually with the `.launch` file whenever you want to update them.

Keep in mind that you need to compile and build the Maven plugin `org.rdkit.knime:knime-maven-dependencies-injection`
from the KNIME RDKit project (see step 1 above) to have it available in your local Maven repository.

## Maven repository and proxy settings ##

Your computer may require proper Maven repository and proxy settings to reach online resources. This is part of the local Maven setup on your machine,
usually based on company settings. It is outside the scope of this documentation.

## Who do I talk to for further information? ##

* Manuel Schwarze, Novartis Biomedical Research, RX Software Engineering
