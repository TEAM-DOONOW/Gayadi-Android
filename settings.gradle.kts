pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "GayadiAndroid"
include(
    ":app",
    ":domain",
    ":data",
    ":di",
    ":core:designsystem",
    ":core:ui",
    ":feature:auth",
    ":feature:basicinfo",
    ":feature:survey",
    ":feature:surveyresult",
    ":feature:home",
    ":feature:trip",
    ":feature:mypage",
)
