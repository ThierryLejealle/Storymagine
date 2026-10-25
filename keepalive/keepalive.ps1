# Ping periodique du modele Ollama actuellement charge, pour eviter les blocages
# apres une longue periode d'inactivite (ex. qwen qui se met a boucler sur des zeros).
# Arret : Ctrl+C dans la fenetre.

$baseUrl     = "http://localhost:11434"
$intervalSec = 300
$prompt      = "Reponds juste : OK."

function Get-Timestamp {
    Get-Date -Format "yyyy-MM-dd HH:mm:ss"
}

function Get-LoadedModel {
    $resp = Invoke-RestMethod -Uri "$baseUrl/api/ps" -Method Get -TimeoutSec 10
    if ($resp.models -and $resp.models.Count -gt 0) {
        return $resp.models[0].name
    }
    return $null
}

function Send-Keepalive([string]$model) {
    $body = @{
        model  = $model
        prompt = $prompt
        stream = $false
    } | ConvertTo-Json

    $sw = [System.Diagnostics.Stopwatch]::StartNew()
    $resp = Invoke-RestMethod -Uri "$baseUrl/api/generate" -Method Post -Body $body -ContentType "application/json" -TimeoutSec 60
    $sw.Stop()
    return [PSCustomObject]@{ DurationMs = $sw.ElapsedMilliseconds; Response = $resp.response }
}

function Test-Timeout($exception) {
    return $exception -is [System.Net.WebException] -and $exception.Status -eq [System.Net.WebExceptionStatus]::Timeout
}

Write-Host "Keepalive Ollama demarre. Intervalle : $intervalSec s. Cible : $baseUrl. Ctrl+C pour arreter."

while ($true) {
    $ts = Get-Timestamp

    try {
        $model = Get-LoadedModel
    } catch {
        Write-Host ("{0}  ERREUR (api/ps) : {1}" -f $ts, $_.Exception.Message)
        Start-Sleep -Seconds $intervalSec
        continue
    }

    if (-not $model) {
        Write-Host ("{0}  model=(aucun)           duree=-        reponse=(skip, aucun modele charge)" -f $ts)
    } else {
        try {
            $result      = Send-Keepalive -model $model
            $respTrimmed = ($result.Response -replace "`r`n"," " -replace "`n"," ").Trim()
            if ($respTrimmed.Length -gt 60) { $respTrimmed = $respTrimmed.Substring(0, 60) + "..." }
            Write-Host ("{0}  model={1,-20} duree={2,6}ms  reponse={3}" -f $ts, $model, $result.DurationMs, $respTrimmed)
        } catch {
            if (Test-Timeout $_.Exception) {
                # modele occupe (deja en train de repondre a autre chose) : pas une erreur, on ne dit rien
            } else {
                Write-Host ("{0}  ERREUR (api/generate) : {1}" -f $ts, $_.Exception.Message)
            }
        }
    }

    Start-Sleep -Seconds $intervalSec
}
