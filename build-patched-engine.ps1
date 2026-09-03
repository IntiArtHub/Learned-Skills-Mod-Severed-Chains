param(
  [Parameter(Mandatory=$true)][string]$EngineWorktree,
  [string]$JavaHome = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'
$commit = 'b69c397a2ccb6165e12480d13523840073f08a80'
$expectedVersion = '3.0.0-1739-devbuild'
if([string]::IsNullOrWhiteSpace($JavaHome)) { throw 'Provide -JavaHome or set JAVA_HOME to JDK 25.' }
$worktree = (Resolve-Path -LiteralPath $EngineWorktree).Path
$versionSource = Join-Path $worktree 'src\main\java\legend\core\Version.java'
$source = Get-Content -Raw -LiteralPath $versionSource
if($source -match 'SNAPSHOT-CHANNEL|BUILD = "SNAPSHOT"|CHANNEL = "CHANNEL"|HASH = "COMMIT"' -or $source -notmatch 'BUILD = "1739"' -or $source -notmatch 'CHANNEL = "devbuild"') {
  throw "Version.java is not prepared for $expectedVersion; refusing to build a deployable engine."
}

$previousJavaHome = $env:JAVA_HOME
$previousSha = $env:GITHUB_SHA
try {
  $env:JAVA_HOME = $JavaHome
  $env:GITHUB_SHA = $commit
  Push-Location $worktree
  try {
    & '.\gradlew.bat' clean menuExtensionTest jar --no-daemon
    if($LASTEXITCODE -ne 0) { throw "Engine build failed with exit code $LASTEXITCODE" }
  } finally { Pop-Location }
} finally {
  $env:JAVA_HOME = $previousJavaHome
  $env:GITHUB_SHA = $previousSha
}

$engineJar = Join-Path $worktree "build\libs\lod-game-$commit.jar"
& (Join-Path $PSScriptRoot 'verify-engine-metadata.ps1') -EngineJar $engineJar -JavaHome $JavaHome
Write-Output $engineJar