$projectRoot = Split-Path -Parent $MyInvocation.MyCommand.Path
$javaHome = Join-Path $projectRoot "..\\tools\\jdk\\jdk-21.0.10+7"
$mavenHome = Join-Path $projectRoot "..\\tools\\maven\\apache-maven-3.9.9"
$env:JAVA_HOME = (Resolve-Path $javaHome).Path
$env:MAVEN_HOME = (Resolve-Path $mavenHome).Path
$env:PATH = "$env:JAVA_HOME\\bin;$env:MAVEN_HOME\\bin;$env:PATH"
Set-Location $projectRoot
$mvnArgs = @(
  "-Dspring-boot.run.jvmArguments=-Dspring.devtools.restart.enabled=false",
  "spring-boot:run"
)
& mvn @mvnArgs
