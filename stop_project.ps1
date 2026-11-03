# ==============================================================================
# TRACKVISION GUARD — Stop All Services
# ==============================================================================

Write-Host "Stopping all TrackVision Guard background services..." -ForegroundColor Yellow

$ports = @(5500, 8000, 8080)
foreach ($port in $ports) {
    $conns = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($conns) {
        $pids = $conns | Select-Object -ExpandProperty OwningProcess -Unique
        foreach ($procId in $pids) {
            try {
                Stop-Process -Id $procId -Force -ErrorAction SilentlyContinue
                Write-Host "[✓] Terminated process $procId listening on port $port." -ForegroundColor Green
            } catch {
                Write-Host "[!] Could not stop process $procId on port $port." -ForegroundColor Red
            }
        }
    } else {
        Write-Host "[-] Port $port is already free." -ForegroundColor Gray
    }
}

Write-Host "All TrackVision Guard services stopped successfully." -ForegroundColor Cyan
