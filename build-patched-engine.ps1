param(
  [Parameter(Mandatory=$true)][string]$EngineWorktree,
  [string]$JavaHome = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'
$commit = 'd3d4c02cbf1c74a4db39cfc10520431a6b8cc150'
$expectedVersion = '3.0.0-1780-devbuild'
if([string]::IsNullOrWhiteSpace($JavaHome)) { throw 'Provide -JavaHome or set JAVA_HOME to JDK 25.' }
$worktree = (Resolve-Path -LiteralPath $EngineWorktree).Path
$versionSource = Join-Path $worktree 'src\main\java\legend\core\Version.java'
$source = Get-Content -Raw -LiteralPath $versionSource
if($source -match 'SNAPSHOT-CHANNEL|BUILD = "SNAPSHOT"|CHANNEL = "CHANNEL"|HASH = "COMMIT"' -or $source -notmatch 'BUILD = "1780"' -or $source -notmatch [regex]::Escape("HASH = `"$commit`"") -or $source -notmatch 'CHANNEL = "devbuild"') {
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
