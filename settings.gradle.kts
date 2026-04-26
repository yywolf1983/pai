pluginManagement {
    repositories {
        // 国内阿里云镜像（优先）
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/maven-central")
        maven("https://maven.aliyun.com/repository/jcenter")
        maven("https://maven.aliyun.com/repository/spring")
        maven("https://maven.aliyun.com/repository/spring-plugin")

        // 官方源（兜底）
        google()
        mavenCentral()
        gradlePluginPortal()
        mavenLocal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        // 国内阿里云镜像（优先）
        maven("https://maven.aliyun.com/repository/google")
        maven("https://maven.aliyun.com/repository/maven-central")
        maven("https://maven.aliyun.com/repository/public")
        maven("https://maven.aliyun.com/repository/jcenter")
        maven("https://maven.aliyun.com/repository/gradle-plugin")
        maven("https://maven.aliyun.com/repository/spring")
        maven("https://maven.aliyun.com/repository/spring-plugin")

        // 官方源（兜底）
        google()
        mavenCentral()
        mavenLocal()
        // GitHub 仓库源，用于获取 Markwon 依赖
        maven("https://jitpack.io")
    }
}

// 你的项目名
rootProject.name = "你的项目名"

// 有子模块就加，没有就删掉
include(":app")
include(":compose-markdown:compose-markdown-0.7.1:markdowntext")
// include(":lib")
