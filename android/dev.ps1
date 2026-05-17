# PokeScan Android dev automation
# Usage: .\dev.ps1 <install|launch|watch|connect>

param(
    [Parameter(Position = 0)]
    [string]$Command = ''
)

$PackageName  = 'com.pokescan.app'
$MainActivity = "$PackageName/.MainActivity"
$WatchDirs    = @('.\app\src\main\java', '.\app\src\main\res', '.\app\src\main\assets')
$WatchExts    = @('.kt', '.xml', '.json')
$DebounceMs   = 2000

function ts { "[$(Get-Date -Format 'HH:mm:ss')]" }

function Invoke-GradleInstall {
    Write-Host "$(ts) Building..." -ForegroundColor Cyan
    # | Out-Host keeps Gradle stdout on console without polluting function pipeline
    & .\gradlew.bat :app:installDebug | Out-Host
    if ($LASTEXITCODE -ne 0) {
        Write-Host "$(ts) BUILD FAILED (exit $LASTEXITCODE)." -ForegroundColor Red
    } else {
        Write-Host "$(ts) Installed." -ForegroundColor Green
    }
}

function Do-Install {
    Invoke-GradleInstall
    exit $LASTEXITCODE
}

function Do-Launch {
    Invoke-GradleInstall
    if ($LASTEXITCODE -eq 0) {
        Write-Host "$(ts) Launching app..." -ForegroundColor Green
        adb shell am start -n $MainActivity | Out-Null
    }
    exit $LASTEXITCODE
}

function Do-Connect {
    $state = (adb get-state 2>$null)
    if ($state -eq 'device') {
        Write-Host "$(ts) Device ready." -ForegroundColor Green
        return
    }
    Write-Host "$(ts) State: '$state'. Restarting ADB server..." -ForegroundColor Yellow
    adb kill-server
    adb start-server
    Start-Sleep -Seconds 1
    $state = (adb get-state 2>$null)
    if ($state -eq 'device') {
        Write-Host "$(ts) Device connected." -ForegroundColor Green
    } else {
        Write-Host "$(ts) Still not ready -- state: '$state'. Check USB/wireless connection." -ForegroundColor Red
    }
}

function Do-Watch {
    Write-Host "$(ts) Watching for changes (Ctrl+C to stop)..." -ForegroundColor Cyan

    $watchers = @(foreach ($dir in $WatchDirs) {
        if (Test-Path $dir) {
            $w = [System.IO.FileSystemWatcher]::new((Resolve-Path $dir).Path)
            $w.IncludeSubdirectories = $true
            $w.NotifyFilter = [System.IO.NotifyFilters]::LastWrite -bor [System.IO.NotifyFilters]::FileName
            $w
        }
    })

    if ($watchers.Count -eq 0) {
        Write-Host "$(ts) No watch directories found. Run from android/ directory." -ForegroundColor Red
        return
    }

    $lastChange   = [DateTime]::MinValue
    $buildPending = $false

    try {
        while ($true) {
            foreach ($w in $watchers) {
                $r = $w.WaitForChanged([System.IO.WatcherChangeTypes]::All, 200)
                if (-not $r.TimedOut) {
                    $ext = [System.IO.Path]::GetExtension($r.Name)
                    if ($ext -in $WatchExts) {
                        Write-Host "$(ts) Change: $($r.Name)" -ForegroundColor Yellow
                        $lastChange   = [DateTime]::UtcNow
                        $buildPending = $true
                    }
                }
            }

            if ($buildPending) {
                $elapsed = ([DateTime]::UtcNow - $lastChange).TotalMilliseconds
                if ($elapsed -ge $DebounceMs) {
                    $buildPending = $false
                    Invoke-GradleInstall
                    if ($LASTEXITCODE -eq 0) {
                        adb shell am start -n $MainActivity | Out-Null
                        Write-Host "$(ts) App launched. Watching..." -ForegroundColor Green
                    } else {
                        Write-Host "$(ts) Watching for next change..." -ForegroundColor Cyan
                    }
                }
            }
        }
    }
    finally {
        $watchers | ForEach-Object { $_.Dispose() }
        Write-Host "$(ts) Watcher stopped." -ForegroundColor Yellow
    }
}

switch ($Command) {
    'install' { Do-Install }
    'launch'  { Do-Launch }
    'watch'   { Do-Watch }
    'connect' { Do-Connect }
    default {
        Write-Host 'PokeScan Android dev script'
        Write-Host '  .\dev.ps1 install  -- incremental build + install (keeps app data)'
        Write-Host '  .\dev.ps1 launch   -- install + launch app on device'
        Write-Host '  .\dev.ps1 watch    -- auto-rebuild on file change (Ctrl+C to stop)'
        Write-Host '  .\dev.ps1 connect  -- check/fix ADB device connection'
    }
}
