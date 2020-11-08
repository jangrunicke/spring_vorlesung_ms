pluginManagement {
    repositories {
        gradlePluginPortal()
        //maven("https://plugins.gradle.org/m2")
        mavenCentral()

        maven("https://dl.bintray.com/kotlin/kotlin-eap")
        maven("https://repo.spring.io/libs-milestone")
        maven("https://repo.spring.io/plugins-release")

        //jcenter()

        // Snapshots von Spring Framework, Spring Data, Spring Security und Spring Cloud
        //maven("https://repo.spring.io/libs-snapshot")
    }
}

rootProject.name = "kunde"
