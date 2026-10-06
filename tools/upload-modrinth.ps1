# Sube el jar del mod a Modrinth.
# Token: variable de entorno MODRINTH_TOKEN (Modrinth > Settings > PATs, permiso "Create versions").
# Uso:  powershell -File tools/upload-modrinth.ps1 -ProjectId <id del proyecto> [-DryRun]
param(
	[Parameter(Mandatory = $true)][string]$ProjectId,
	[string]$ReleaseType = "release",
	[switch]$DryRun
)
$ErrorActionPreference = "Stop"
$root = Split-Path $PSScriptRoot -Parent

$token = $env:MODRINTH_TOKEN
if (-not $token) { $token = [Environment]::GetEnvironmentVariable("MODRINTH_TOKEN", "User") }
if (-not $token) { throw "Falta MODRINTH_TOKEN" }

$version = ((Get-Content "$root\gradle.properties") | Where-Object { $_ -match '^version=' }) -replace '^version=', ''
$jar = Get-ChildItem "$root\build\libs" -Filter "chaostablist-$version.jar" | Select-Object -First 1
if (-not $jar) { throw "No existe build/libs/chaostablist-$version.jar; ejecuta gradlew build" }

$data = @{
	name           = "Chaos-Tablist $version"
	version_number = $version
	changelog      = [IO.File]::ReadAllText("$root\tools\changelog.md", [Text.Encoding]::UTF8)
	game_versions  = @("1.21.1")
	loaders        = @("fabric")
	version_type   = $ReleaseType
	featured       = $true
	project_id     = $ProjectId
	file_parts     = @("file")
	primary_file   = "file"
	# Fabric API (P7dR8mSH), obligatoria.
	dependencies   = @(@{ project_id = "P7dR8mSH"; dependency_type = "required" })
} | ConvertTo-Json -Depth 5
$dataFile = Join-Path $env:TEMP "chaostablist-mr-data.json"
[IO.File]::WriteAllText($dataFile, $data, (New-Object Text.UTF8Encoding $false))

Write-Host "Proyecto $ProjectId | $($jar.Name) | Chaos-Tablist $version | $ReleaseType"
if ($DryRun) { Write-Host "DryRun: no se sube nada."; Get-Content $dataFile; exit 0 }

$response = (& curl.exe -s -w "`nHTTP %{http_code}" -H "Authorization: $token" -H "User-Agent: LuisEnriqueGM03/ChaosTablist" `
	-F "data=<$dataFile;type=application/json" -F "file=@$($jar.FullName);type=application/java-archive" `
	"https://api.modrinth.com/v2/version") -join "`n"
Remove-Item $dataFile
Write-Host $response
if ($response -notmatch "HTTP 200") { exit 1 }
