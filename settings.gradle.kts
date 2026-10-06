import org.gradle.api.initialization.resolve.RepositoriesMode

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

rootProject.name = "CMFTool"

include("lib-util")
include("lib-cmf")
include("app-cmftool")
include("app-niemtran")
include("app-scheval")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)

    repositories {
        mavenCentral()
    }
}
