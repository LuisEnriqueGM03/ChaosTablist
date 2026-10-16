# Sube el jar del mod a Modrinth.
# Token: variable de entorno MODRINTH_TOKEN (Modrinth > Settings > PATs, permiso "Create versions").
# Uso:  powershell -File tools/upload-modrinth.ps1 -ProjectId <id del proyecto> [-DryRun]
param(
	[Parameter(Mandatory = $true)][string]$ProjectId,
	[ValidateSet("release", "beta", "alpha")][string]$ReleaseType = "release",
	[ValidateSet("fabric", "forge", "neoforge")][string]$Loader = "fabric",
	[switch]$DryRun
)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

$token = $env:MODRINTH_TOKEN
if (-not $token) { $token = [Environment]::GetEnvironmentVariable("MODRINTH_TOKEN", "User") }
if (-not $token -and -not $DryRun) { throw "Falta MODRINTH_TOKEN" }

$loaderLabel = if ($Loader -eq "fabric") { "Fabric" } elseif ($Loader -eq "neoforge") { "NeoForge" } else { "Forge" }
$dependencies = @()
$relations = @()
if ($Loader -eq "fabric") {
    $dependencies = @(@{ project_id = "P7dR8mSH"; dependency_type = "required" })
    $relations = @(@{ slug = "fabric-api"; type = "requiredDependency" })
}

$version = ((Get-Content "$root\gradle.properties") | Where-Object { $_ -match '^version=' }) -replace '^version=', ''
$jar = Get-ChildItem "$root\$Loader\build\libs" -Filter "chaostablist-$Loader-1.20.1-$version.jar" | Select-Object -First 1
if (-not $jar) { throw "No existe el JAR de $Loader para $version; compila primero ese cargador" }

$data = @{
	name           = "Chaos Tablist $version ($loaderLabel)"
	version_number = "$version-$Loader"
	changelog      = [IO.File]::ReadAllText("$root\tools\changelog.md", [Text.Encoding]::UTF8)
	game_versions  = @("1.20.1")
	loaders        = @($Loader)
	version_type   = $ReleaseType
	featured       = $true
	project_id     = $ProjectId
	file_parts     = @("file")
	primary_file   = "file"
	# Fabric API (P7dR8mSH), obligatoria.
	dependencies   = $dependencies
} | ConvertTo-Json -Depth 5
if ($DryRun) { Write-Host "DryRun: no se sube nada."; Write-Output $data; exit 0 }
$dataFile = [IO.Path]::GetTempFileName()
[IO.File]::WriteAllText($dataFile, $data, (New-Object Text.UTF8Encoding $false))

Write-Host "Proyecto $ProjectId | $($jar.Name) | Chaos-Tablist $version | $ReleaseType"


$response = (& curl.exe -sS -w "`nHTTP %{http_code}" -H "Authorization: $token" -H "User-Agent: LuisEnriqueGM03/ChaosTablist" `
	-F "data=<$dataFile;type=application/json" -F "file=@$($jar.FullName);type=application/java-archive" `
	"https://api.modrinth.com/v2/version") -join "`n"
Remove-Item $dataFile
Write-Host $response
if ($LASTEXITCODE -ne 0 -or $response -notmatch "HTTP 2[0-9]{2}") { exit 1 }
