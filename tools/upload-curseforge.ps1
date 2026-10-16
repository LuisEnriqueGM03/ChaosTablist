# Sube el jar del mod a CurseForge.
# Token: variable de entorno CURSEFORGE_TOKEN (de la sesión o guardada con `setx`).
# Uso:  powershell -File tools/upload-curseforge.ps1 -ProjectId <id del proyecto> [-DryRun]
param(
	[Parameter(Mandatory = $true)][string]$ProjectId,
	[ValidateSet("release", "beta", "alpha")][string]$ReleaseType = "release",
	[ValidateSet("fabric", "forge", "neoforge")][string]$Loader = "fabric",
	[switch]$DryRun
)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

$token = $env:CURSEFORGE_TOKEN
if (-not $token) { $token = [Environment]::GetEnvironmentVariable("CURSEFORGE_TOKEN", "User") }
if (-not $token -and -not $DryRun) { throw "Falta CURSEFORGE_TOKEN (setx CURSEFORGE_TOKEN <token>)" }

$loaderLabel = if ($Loader -eq "fabric") { "Fabric" } elseif ($Loader -eq "neoforge") { "NeoForge" } else { "Forge" }
$dependencies = @()
$relations = @()
if ($Loader -eq "fabric") {
    $dependencies = @(@{ project_id = "P7dR8mSH"; dependency_type = "required" })
    $relations = @(@{ slug = "fabric-api"; type = "requiredDependency" })
}

$version = ((Get-Content "$root\gradle.properties") | Where-Object { $_ -match '^version=' }) -replace '^version=', ''
$jar = Get-ChildItem "$root\$Loader\build\libs" -Filter "chaostablist-$Loader-1.21.4-$version.jar" | Select-Object -First 1
if (-not $jar) { throw "No existe el JAR de $Loader para $version; compila primero ese cargador" }

if ($DryRun) {
    Write-Host "DryRun: $($jar.Name) | proyecto $ProjectId | $ReleaseType. Sin llamadas a la API ni publicación."
    Get-Content "$root/tools/changelog.md"
    exit 0
}

# Ids de CurseForge para versión de juego, cargador, Java y entorno.
$api = "https://minecraft.curseforge.com/api"
$all = Invoke-RestMethod -Uri "$api/game/versions" -Headers @{ "X-Api-Token" = $token }
$loaderLabel = if ($Loader -eq "fabric") { "Fabric" } elseif ($Loader -eq "neoforge") { "NeoForge" } else { "Forge" }
$wanted = "1.21.4", $loaderLabel, "Java 21", "Client", "Server"
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
	displayName   = "Chaos Tablist $version ($loaderLabel)"
	gameVersions  = @($ids)
	releaseType   = $ReleaseType
}
if ($relations.Count -gt 0) { $metadata.relations = @{ projects = $relations } }
$metadata = $metadata | ConvertTo-Json -Depth 5
$metaFile = [IO.Path]::GetTempFileName()
[IO.File]::WriteAllText($metaFile, $metadata, (New-Object Text.UTF8Encoding $false))

Write-Host "Proyecto $ProjectId | $($jar.Name) | Chaos-Tablist $version | $ReleaseType"


$response = (& curl.exe -sS -w "`nHTTP %{http_code}" -H "X-Api-Token: $token" `
	-F "metadata=<$metaFile" -F "file=@$($jar.FullName)" "$api/projects/$ProjectId/upload-file") -join "`n"
Remove-Item $metaFile
Write-Host $response
if ($LASTEXITCODE -ne 0 -or $response -notmatch "HTTP 2[0-9]{2}") { exit 1 }
