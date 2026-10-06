# Sube el jar del mod a CurseForge.
# Token: variable de entorno CURSEFORGE_TOKEN (de la sesión o guardada con `setx`).
# Uso:  powershell -File tools/upload-curseforge.ps1 -ProjectId <id del proyecto> [-DryRun]
param(
	[Parameter(Mandatory = $true)][string]$ProjectId,
	[string]$ReleaseType = "release",
	[switch]$DryRun
)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

$token = $env:CURSEFORGE_TOKEN
if (-not $token) { $token = [Environment]::GetEnvironmentVariable("CURSEFORGE_TOKEN", "User") }
if (-not $token) { throw "Falta CURSEFORGE_TOKEN (setx CURSEFORGE_TOKEN <token>)" }

$version = ((Get-Content "$root\gradle.properties") | Where-Object { $_ -match '^version=' }) -replace '^version=', ''
$jar = Get-ChildItem "$root\build\libs" -Filter "chaostablist-$version.jar" | Select-Object -First 1
if (-not $jar) { throw "No existe build/libs/chaostablist-$version.jar; ejecuta gradlew build" }

# Ids de CurseForge para versión de juego, cargador, Java y entorno.
$api = "https://minecraft.curseforge.com/api"
$all = Invoke-RestMethod -Uri "$api/game/versions" -Headers @{ "X-Api-Token" = $token }
$wanted = "1.21.1", "Fabric", "Java 21", "Client", "Server"
$ids = foreach ($name in $wanted) {
	$match = $all | Where-Object { $_.name -eq $name } | Select-Object -First 1
	if (-not $match) { throw "CurseForge no tiene la versión '$name'" }
	Write-Host "  $name -> $($match.id)"
	$match.id
}

$metadata = @{
	# ReadAllText: en PowerShell 5.1, Get-Content -Raw añade propiedades que ConvertTo-Json serializa.
	changelog     = [IO.File]::ReadAllText("$root\tools\changelog.md", [Text.Encoding]::UTF8)
	changelogType = "markdown"
	displayName   = "Chaos - Tablist $version"
	gameVersions  = @($ids)
	releaseType   = $ReleaseType
	relations     = @{ projects = @(@{ slug = "fabric-api"; type = "requiredDependency" }) }
} | ConvertTo-Json -Depth 5
$metaFile = Join-Path $env:TEMP "chaostablist-cf-metadata.json"
[IO.File]::WriteAllText($metaFile, $metadata, (New-Object Text.UTF8Encoding $false))

Write-Host "Proyecto $ProjectId | $($jar.Name) | Chaos - Tablist $version | $ReleaseType"
if ($DryRun) { Write-Host "DryRun: no se sube nada."; Get-Content $metaFile; exit 0 }

$response = (& curl.exe -s -w "`nHTTP %{http_code}" -H "X-Api-Token: $token" `
	-F "metadata=<$metaFile" -F "file=@$($jar.FullName)" "$api/projects/$ProjectId/upload-file") -join "`n"
Remove-Item $metaFile
Write-Host $response
if ($response -notmatch "HTTP 200") { exit 1 }
