# Builds the data-only Fabric mod jar by zipping src/main/resources.
# Uses System.IO.Compression directly so entry paths use forward slashes
# (required for the jar to be read correctly cross-platform).
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.IO.Compression
Add-Type -AssemblyName System.IO.Compression.FileSystem

$root = Split-Path -Parent $PSScriptRoot
$res  = Join-Path $root 'src\main\resources'
$dist = Join-Path $root 'dist'

$cfg     = Get-Content (Join-Path $res 'fabric.mod.json') -Raw | ConvertFrom-Json
$version = $cfg.version
$jar     = Join-Path $dist "archetype-appetites-$version.jar"

New-Item -ItemType Directory -Force -Path $dist | Out-Null
if (Test-Path $jar) { Remove-Item $jar -Force }

$zip = [System.IO.Compression.ZipFile]::Open($jar, 'Create')
try {
    Get-ChildItem -Path $res -Recurse -File | ForEach-Object {
        $rel = $_.FullName.Substring($res.Length + 1).Replace('\', '/')
        [System.IO.Compression.ZipFileExtensions]::CreateEntryFromFile($zip, $_.FullName, $rel) | Out-Null
    }
}
finally {
    $zip.Dispose()
}

Write-Host "Built $jar"
