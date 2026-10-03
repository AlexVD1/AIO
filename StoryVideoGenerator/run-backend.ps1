$wslIp = "127.0.0.1"
try {
    $ipOutput = wsl -d podman-machine-default ip -4 addr show eth0 2>$null
    if ($ipOutput -match 'inet\s+([0-9]+\.[0-9]+\.[0-9]+\.[0-9]+)') {
        $wslIp = $matches[1]
    }
} catch {}

$env:DATABASE_URL = "jdbc:postgresql://${wslIp}:5433/storydb"
$env:DATABASE_USERNAME = 'story_user'
$env:DATABASE_PASSWORD = 'CAMBIAR_POR_PASSWORD_SEGURO'
$env:SD_API_URL = 'http://localhost:7860'
$env:STORAGE_PATH = './storage'
$env:EXPORT_PATH = './export_videos'

if (Test-Path '.env') {
    Get-Content '.env' | ForEach-Object {
        if ($_ -match '^\s*([^#=\s]+)\s*=\s*(.*)$') {
            $k = $matches[1].Trim()
            $v = $matches[2].Trim()
            [System.Environment]::SetEnvironmentVariable($k, $v, 'Process')
        }
    }
}

mvn spring-boot:run
