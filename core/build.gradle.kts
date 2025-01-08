plugins {
  id("saros.gradle.eclipse.plugin")
}

val versionQualifier = ext.get("versionQualifier") as String

val log4j2ApiVersion = ext.get("log4j2ApiVersion") as String
val log4j2CoreVersion = ext.get("log4j2CoreVersion") as String
val log4j2BridgeVersion = ext.get("log4j2BridgeVersion") as String

configurations {
    // Defined in root build.gradle
    val testConfig by getting {}
    val releaseDep by getting {}

    // Default configuration
    val implementation by getting {
        extendsFrom(releaseDep)
    }
    val testImplementation by getting {
        extendsFrom(testConfig)
    }
    val plain by creating {
        extendsFrom(implementation)
    }
}

sarosEclipse {
    manifest = file("META-INF/MANIFEST.MF")
    isCreateBundleJar = true
    isAddPdeNature = true
    pluginVersionQualifier = versionQualifier
}

dependencies {
	releaseDep("commons-codec:commons-codec:1.3")
//    releaseDep("commons-codec:commons-codec:1.17.2")
    releaseDep("commons-io:commons-io:2.0.1")
//    releaseDep("commons-io:commons-io:2.18.0")
    releaseDep("org.apache.commons:commons-lang3:3.8.1")
//    releaseDep("org.apache.commons:commons-lang3:3.17.0")

    releaseDep("javax.jmdns:jmdns:3.4.1")
//    releaseDep("org.jmdns:jmdns:3.6.0")
    releaseDep("xpp3:xpp3:1.1.4c")
//    releaseDep("org.ogce:xpp3:1.1.6")
    releaseDep("com.thoughtworks.xstream:xstream:1.4.21")
    releaseDep("org.gnu.inet:libidn:1.15")

    releaseDep(log4j2ApiVersion)
    releaseDep(log4j2CoreVersion)
    releaseDep(log4j2BridgeVersion)

    // TODO: use real release. This version is a customized SNAPSHOT
    releaseDep(files("libs/weupnp.jar"))
    // Workaround until we updated to a newer smack version
    releaseDep(files("libs/smack-3.4.1.jar"))
    releaseDep(files("libs/smackx-3.4.1.jar"))
    // Workaround until we can publish and use (without a user token) the repackaged jar in GitHub Packages
    releaseDep(rootProject.files("libs/picocontainer-2.11.2-patched_relocated.jar"))
    
//    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter:5.11.4")
//    testRuntimeOnly("org.junit.platform:junit-platform-engine:1.10.0")
	testRuntimeOnly("org.junit.platform:junit-platform-engine:1.11.4")
	
	
		
}

sourceSets {
    main {
        java.srcDirs("src", "patches")
        resources.srcDirs("resources")
    }
    test {
        java.srcDirs("test/junit")
    }
}

tasks {

    val testJar by registering(Jar::class) {
        //classifier = "tests"
        archiveClassifier.set("tests")
        from(sourceSets["test"].output)
    }

    // Jar containing only the core code (the default jar is an osgi bundle
    // containing a lib dir with all dependency jars)
    val plainJar by registering(Jar::class) {
        manifest {
            from("META-INF/MANIFEST.MF")
        }
        from(sourceSets["main"].output)
        //classifier = "plain"
         archiveClassifier.set("plain")
    }

    artifacts {
        add("testing", testJar)
        add("plain", plainJar)
    }
}

tasks.withType<JavaExec> {
	jvmArgs("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
}

tasks.withType<Test> {
	jvmArgs("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
}

tasks.test {
	useJUnitPlatform()
	jvmArgs("--add-opens", "java.base/java.util.concurrent=ALL-UNNAMED")
}