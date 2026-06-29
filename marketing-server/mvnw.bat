@echo off
setlocal
set JAVA_HOME=D:\Java\jdk-25
set PATH=%JAVA_HOME%\bin;%PATH%

set "MAVEN_PROJECTBASEDIR=%~dp0"
set "MVNW_REPOURL=https://repo.maven.apache.org/maven2"

if not exist "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar" (
    echo Maven Wrapper JAR not found. Downloading...
    powershell -Command "Invoke-WebRequest -Uri '%MVNW_REPOURL%/org/apache/maven/wrapper/maven-wrapper/3.3.2/maven-wrapper-3.3.2.jar' -OutFile '%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar'"
)

java -jar "%MAVEN_PROJECTBASEDIR%.mvn\wrapper\maven-wrapper.jar" %*
