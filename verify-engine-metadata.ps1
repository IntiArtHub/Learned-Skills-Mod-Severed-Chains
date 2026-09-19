param(
  [Parameter(Mandatory=$true)][string]$EngineJar,
  [string]$JavaHome = $env:JAVA_HOME
)

$ErrorActionPreference = 'Stop'
$expectedVersion = '3.0.0-1780-devbuild'
$expectedHash = 'd3d4c02cbf1c74a4db39cfc10520431a6b8cc150'
if([string]::IsNullOrWhiteSpace($JavaHome)) { throw 'Provide -JavaHome or set JAVA_HOME to JDK 25.' }
$jar = (Resolve-Path -LiteralPath $EngineJar).Path
$javap = Join-Path $JavaHome 'bin\javap.exe'
if(!(Test-Path -LiteralPath $javap)) { throw "Missing javap: $javap" }

$metadata = & $javap -classpath $jar -constants legend.core.Version 2>&1 | Out-String
if($LASTEXITCODE -ne 0) { throw "Unable to inspect legend.core.Version in $jar`n$metadata" }
if($metadata -match 'SNAPSHOT-CHANNEL|BUILD = "SNAPSHOT"|CHANNEL = "CHANNEL"|HASH = "COMMIT"') { throw "Rejected engine JAR with placeholder metadata: $jar" }
if($metadata -notmatch [regex]::Escape("FULL_VERSION = `"$expectedVersion`"")) { throw "Engine FULL_VERSION is not $expectedVersion`n$metadata" }
if($metadata -notmatch [regex]::Escape("HASH = `"$expectedHash`"")) { throw "Engine HASH is not $expectedHash`n$metadata" }
Write-Output "Engine metadata verified: $expectedVersion ($expectedHash)"
