param(
    [switch]$Rebuild
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$productName = -join ([char[]](0x3066, 0x3063, 0x304f, 0x306b, 0x3085, 0x30fc, 0x3059))
$appHome = Join-Path (Join-Path $root 'dist') $productName
$executable = Join-Path $appHome ($productName + '.exe')

try {
    $java = Get-Command java.exe -ErrorAction SilentlyContinue
    $jpackage = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if (-not $jpackage -and $env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
        if (Test-Path -LiteralPath $candidate) { $jpackage = Get-Item -LiteralPath $candidate }
    }
    if (-not $jpackage -and $java) {
        $candidate = Join-Path (Split-Path $java.Source -Parent) 'jpackage.exe'
        if (Test-Path -LiteralPath $candidate) { $jpackage = Get-Item -LiteralPath $candidate }
    }

    if (-not $java -or -not $jpackage) {
        throw 'JDK 21 or later was not found. Install a JDK and configure JAVA_HOME and PATH.'
    }

    if ($Rebuild -or -not (Test-Path -LiteralPath $executable)) {
        Write-Host 'Building TechWatch. The first build may take a few minutes.'
        & (Join-Path $PSScriptRoot 'package-windows.ps1')
        if ($LASTEXITCODE -ne 0) { throw 'Failed to build the Windows application.' }
    }

    if (-not (Test-Path -LiteralPath $executable)) {
        throw "Executable not found: $executable"
    }

    Write-Host "Starting: $executable"
    Start-Process -FilePath $executable -WorkingDirectory $appHome
} catch {
    Write-Host ''
    Write-Host ('TechWatch setup failed: ' + $_.Exception.Message) -ForegroundColor Red
    Write-Host 'See the quick-start section in README.md.' -ForegroundColor Yellow
    exit 1
}
