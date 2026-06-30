param(
    [switch]$Installer
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$inputDir = Join-Path $root 'package-input'
$distDir = Join-Path $root 'dist'
$stagingDir = Join-Path $root 'package-output'

function Remove-WorkspaceDirectory([string]$Path) {
    if (-not (Test-Path -LiteralPath $Path)) { return }
    $resolved = (Resolve-Path -LiteralPath $Path).Path
    if (-not $resolved.StartsWith($root + [IO.Path]::DirectorySeparatorChar)) {
        throw "Refusing to remove a path outside the project: $resolved"
    }
    Remove-Item -LiteralPath $resolved -Recurse -Force
}

Push-Location $root
try {
    mvn clean package
    if ($LASTEXITCODE -ne 0) { throw 'Maven build failed.' }

    Remove-WorkspaceDirectory $inputDir
    if (-not $Installer) { Remove-WorkspaceDirectory $stagingDir }
    New-Item -ItemType Directory -Path $inputDir -Force | Out-Null
    New-Item -ItemType Directory -Path $distDir -Force | Out-Null
    if (-not $Installer) { New-Item -ItemType Directory -Path $stagingDir -Force | Out-Null }
    Copy-Item -LiteralPath (Join-Path $root 'target\techwatch-gui.jar') -Destination $inputDir

    $jpackage = Get-Command jpackage.exe -ErrorAction SilentlyContinue
    if (-not $jpackage -and $env:JAVA_HOME) {
        $candidate = Join-Path $env:JAVA_HOME 'bin\jpackage.exe'
        if (Test-Path -LiteralPath $candidate) { $jpackage = Get-Item -LiteralPath $candidate }
    }
    if (-not $jpackage) {
        $java = (Get-Command java.exe -ErrorAction Stop).Source
        $candidate = Join-Path (Split-Path $java -Parent) 'jpackage.exe'
        if (Test-Path -LiteralPath $candidate) { $jpackage = Get-Item -LiteralPath $candidate }
    }
    if (-not $jpackage) { throw 'jpackage.exe was not found. Set JAVA_HOME to a full JDK 21+ installation.' }

    $type = if ($Installer) { 'exe' } else { 'app-image' }
    $arguments = @(
        '--type', $type,
        '--input', $inputDir,
        '--dest', $(if ($Installer) { $distDir } else { $stagingDir }),
        '--name', 'TechWatch',
        '--app-version', '1.2.0',
        '--vendor', 'TechWatch',
        '--description', '技術情報を収集・評価し、週報として届けるアプリ',
        '--main-jar', 'techwatch-gui.jar',
        '--main-class', 'com.example.techwatch.gui.GuiLauncher',
        '--java-options', '--enable-native-access=ALL-UNNAMED',
        '--java-options', '-Dtechwatch.packaged=true'
    )
    if ($Installer) { $arguments += @('--win-menu', '--win-shortcut', '--win-dir-chooser') }
    $jpackagePath = if ($jpackage -is [System.Management.Automation.CommandInfo]) {
        $jpackage.Source
    } else {
        $jpackage.FullName
    }
    & $jpackagePath @arguments
    if ($LASTEXITCODE -ne 0) { throw 'jpackage failed.' }

    if (-not $Installer) {
        $appHome = Join-Path $distDir 'TechWatch'
        $stagedAppHome = Join-Path $stagingDir 'TechWatch'
        try {
            Remove-WorkspaceDirectory $appHome
        } catch {
            $remaining = @(Get-ChildItem -LiteralPath $appHome -Force -ErrorAction SilentlyContinue)
            if ($remaining.Count -gt 0) { throw }
            Write-Warning 'The empty output directory is locked; its contents will be refreshed in place.'
        }
        New-Item -ItemType Directory -Path $appHome -Force | Out-Null
        Get-ChildItem -LiteralPath $stagedAppHome -Force |
            Copy-Item -Destination $appHome -Recurse -Force
        New-Item -ItemType Directory -Path (Join-Path $appHome 'config') -Force | Out-Null
        New-Item -ItemType Directory -Path (Join-Path $appHome 'reports\weekly') -Force | Out-Null
        Copy-Item -LiteralPath (Join-Path $root 'sources.yml') -Destination (Join-Path $appHome 'config\sources.yml')
        Copy-Item -LiteralPath (Join-Path $root 'keywords.yml') -Destination (Join-Path $appHome 'config\keywords.yml')
        Copy-Item -LiteralPath (Join-Path $root 'job-market.csv') -Destination (Join-Path $appHome 'config\job-market.csv')
        Remove-WorkspaceDirectory $stagingDir
        Write-Host "Created: $(Join-Path $appHome 'TechWatch.exe')"
    }
} finally {
    Pop-Location
}
