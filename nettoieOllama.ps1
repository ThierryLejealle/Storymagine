<#
    Liste les blobs Ollama presents sur disque mais references par aucun manifeste
    (candidats "obsoletes" - pull interrompu, modele supprime sans nettoyage, etc.).
    Ne supprime rien : affichage seulement.
#>

$modelsDir = if ($env:OLLAMA_MODELS) { $env:OLLAMA_MODELS } else { "$env:USERPROFILE\.ollama\models" }

if (-not (Test-Path $modelsDir)) {
    Write-Host "Repertoire de modeles introuvable : $modelsDir" -ForegroundColor Red
    exit 1
}

Write-Host "Repertoire de modeles : $modelsDir"
Write-Host ""

# --- Blobs presents sur disque ---
$blobsDir = Join-Path $modelsDir "blobs"
$blobFiles = Get-ChildItem $blobsDir -File -ErrorAction SilentlyContinue
if (-not $blobFiles) {
    Write-Host "Aucun blob trouve dans $blobsDir"
    exit 0
}

$blobsSurDisque = @{}
foreach ($f in $blobFiles) {
    $digest = $f.Name -replace '^sha256-', 'sha256:'
    $blobsSurDisque[$digest] = $f
}

# --- Digests references par les manifestes (layers + config) ---
$manifestsDir = Join-Path $modelsDir "manifests"
$manifestFiles = Get-ChildItem $manifestsDir -File -Recurse -ErrorAction SilentlyContinue

$referencees = New-Object System.Collections.Generic.HashSet[string]
foreach ($m in $manifestFiles) {
    try {
        $json = Get-Content $m.FullName -Raw | ConvertFrom-Json
    } catch {
        Write-Host "Manifeste illisible, ignore : $($m.FullName)" -ForegroundColor Yellow
        continue
    }
    if ($json.config -and $json.config.digest) {
        [void]$referencees.Add($json.config.digest)
    }
    if ($json.layers) {
        foreach ($l in $json.layers) {
            if ($l.digest) { [void]$referencees.Add($l.digest) }
        }
    }
}

# --- Orphelins = sur disque mais jamais references ---
$orphelins = $blobsSurDisque.Keys | Where-Object { -not $referencees.Contains($_) } | Sort-Object

if (-not $orphelins) {
    Write-Host "Aucun blob orphelin trouve." -ForegroundColor Green
    exit 0
}

$totalOctets = 0
Write-Host "Blobs orphelins ($($orphelins.Count)) :"
Write-Host ("{0,-75} {1,10}" -f "Digest", "Taille")
Write-Host ("-" * 87)
foreach ($digest in $orphelins) {
    $f = $blobsSurDisque[$digest]
    $totalOctets += $f.Length
    $tailleGo = $f.Length / 1GB
    Write-Host ("{0,-75} {1,8:N2} Go" -f $digest, $tailleGo)
}
Write-Host ("-" * 87)
Write-Host ("Total orphelins : {0:N2} Go sur {1}" -f ($totalOctets / 1GB), $orphelins.Count)
Write-Host ""
Write-Host "Rien n'a ete supprime. Verifie avec 'ollama list' / 'ollama ps' avant toute suppression manuelle." -ForegroundColor Cyan
