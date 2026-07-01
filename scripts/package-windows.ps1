param(
    [switch]$Installer
)

$ErrorActionPreference = 'Stop'
$root = (Resolve-Path (Join-Path $PSScriptRoot '..')).Path
$inputDir = Join-Path $root 'package-input'
$distDir = Join-Path $root 'dist'
$stagingDir = Join-Path $root 'package-output'
$productName = -join ([char[]](0x3066, 0x3063, 0x304f, 0x306b, 0x3085, 0x30fc, 0x3059))

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
    Copy-Item -LiteralPath (Join-Path $root 'target\techwatch.jar') -Destination $inputDir

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
        '--name', $productName,
        '--app-version', '1.4.0',
        '--vendor', $productName,
        '--description', '技術情報を収集・評価し、週報として届けるアプリ',
        '--main-jar', 'techwatch-gui.jar',
        '--main-class', 'com.example.techwatch.gui.GuiLauncher',
        '--add-launcher', ('techwatch-cli=' + (Join-Path $root 'scripts\techwatch-cli.properties')),
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
        $appHome = Join-Path $distDir $productName
        $stagedAppHome = Join-Path $stagingDir $productName
        $legacyAppHome = Join-Path $distDir 'TechWatch'
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
        Copy-Item -LiteralPath (Join-Path $root 'retention.yml') -Destination (Join-Path $appHome 'config\retention.yml')
        Copy-Item -LiteralPath (Join-Path $root 'src\main\resources\defaults\email.yml') -Destination (Join-Path $appHome 'config\email.yml')
        Copy-Item -LiteralPath (Join-Path $root 'scripts\register-weekly-task.ps1') -Destination $appHome
        Remove-WorkspaceDirectory $legacyAppHome
        Remove-WorkspaceDirectory $stagingDir
        Write-Host "Created: $(Join-Path $appHome ($productName + '.exe'))"
    }
} finally {
    Pop-Location
}
