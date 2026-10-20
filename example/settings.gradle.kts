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
        /*
         * HygateSWA SDK 배포 저장소.
         *
         * 고객이 쓰는 것과 **똑같은 경로**다. 로컬 소스나 동봉 AAR 을 참조하지 않는 이유가
         * 여기 있다 — 난독화와 R8 을 거친 실제 배포물이 동작하는지 확인해야 하기 때문이다.
         * "로컬은 되는데 AAR 은 깨짐" 을 잡으려면 이 경로여야 한다.
         *
         * 인터넷은 **빌드할 때만** 필요하다. SDK 는 빌드 시점에 APK 안으로 들어가므로
         * 완성된 앱은 실행할 때 이 주소를 쓰지 않는다.
         */
        maven { url = uri("https://hyconcard.github.io/HygateSwaSdkAosDist/maven") }
    }
}

rootProject.name = "hygateswa-sdk-sample"
include(":app")
