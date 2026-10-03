# ============================================================
# generate_30_batch.ps1 — Generador Automatizado de 30 Videos
# ============================================================
# Genera 30 videos con duracion de 60-90 segundos, 6 escenas por video,
# temas 100% diversos y variados estilos de animacion y direccion artistica.
# ============================================================

$apiUrl = "http://localhost:8081/api/v1"
$progressFile = Join-Path $PSScriptRoot "batch_30_progress.json"
$driveDir = "H:\Mi unidad\VideosStories"

# Lista de 30 historias curadas con maxima variedad tematica y artistica
$stories = @(
    @{
        id = 1
        genre = "MYSTERY"
        tone = "SUSPENSE"
        theme = "El misterio del manuscrito Voynich y sus plantas imposibles"
        subgenre = "HISTORICAL_ENIGMA"
        visualStyleName = "Dark Fantasy Tinta y Grabado"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 2
        genre = "SCI_FI"
        tone = "CEREBRAL"
        theme = "La ciudad flotante en la densa atmosfera de Venus habitada por colonos mineros"
        subgenre = "HARD_SCIFI"
        visualStyleName = "Low-Poly 3D Estilizado"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 3
        genre = "PSYCHOLOGICAL"
        tone = "UNSETTLING"
        theme = "El origen del fenomeno de Deja vu y fallos en la sincronizacion de la memoria cerebral"
        subgenre = "MIND_BENDER"
        visualStyleName = "Animacion 2D Ilustracion"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 4
        genre = "HYPOTHETICAL"
        tone = "SUSPENSE"
        theme = "Que pasaria en la Tierra si el planeta entero dejara de girar por solo un segundo"
        subgenre = "PLANETARY_CATACLYSM"
        visualStyleName = "Infografia Cientifica Animada"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 5
        genre = "URBAN_LEGEND"
        tone = "SUSPENSE"
        theme = "El caso del hombre misterioso en el aeropuerto de Tokio que decia venir del pais inexistente de Taured"
        subgenre = "PARALLEL_DIMENSIONS"
        visualStyleName = "Estilo Comic Novela Grafica"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 6
        genre = "STRANGE_EVENTS"
        tone = "CEREBRAL"
        theme = "La maquina del tiempo biologica y el secreto celular de la medusa inmortal Turritopsis dohrnii"
        subgenre = "BIOLOGICAL_ODDITY"
        visualStyleName = "Motion Graphics Geometrico"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 7
        genre = "HORROR"
        tone = "EERIE"
        theme = "El hotel de espectros sonrientes en medio del salar del desierto de Atacama"
        subgenre = "VINTAGE_CREEPY"
        visualStyleName = "Cartoon Vintage Rubberhose"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 8
        genre = "SCI_FI"
        tone = "CEREBRAL"
        theme = "Una civilizacion extraterrestre que envia mensajes cosmicos plegando el espacio como figuras de origami"
        subgenre = "FIRST_CONTACT"
        visualStyleName = "Collage y Papercraft"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 9
        genre = "PSYCHOLOGICAL"
        tone = "CEREBRAL"
        theme = "El dilema filosofico de la habitacion china de John Searle y la ilusion de la mente artificial"
        subgenre = "PHILOSOPHICAL_PARADOX"
        visualStyleName = "Animacion 2D Ilustracion"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 10
        genre = "STRANGE_EVENTS"
        tone = "SUSPENSE"
        theme = "Criaturas bioluminiscentes desconocidas y pulsos de luz en el fondo de la fosa de las Marianas"
        subgenre = "ABYSSAL_EXPLORATION"
        visualStyleName = "Cel Shading Anime"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 11
        genre = "EPISODIC"
        tone = "CEREBRAL"
        theme = "La mitica Biblioteca de Babel donde cada libro contiene todas las combinaciones posibles de letras"
        subgenre = "LITERARY_LABYRINTH"
        visualStyleName = "Pixel Art 16-Bit Retro"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 12
        genre = "MYSTERY"
        tone = "UNSETTLING"
        theme = "El zumbido acustico de baja frecuencia conocido como The Hum que solo el dos por ciento del mundo escucha"
        subgenre = "GLOBAL_ENIGMA"
        visualStyleName = "Claymation Stop-Motion"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 13
        genre = "HYPOTHETICAL"
        tone = "CEREBRAL"
        theme = "La perspectiva visual de una nave espacial cruzando el horizonte de sucesos de un agujero negro"
        subgenre = "RELATIVISTIC_ASTROPHYSICS"
        visualStyleName = "Infografia Cientifica Animada"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 14
        genre = "MYSTERY"
        tone = "SUSPENSE"
        theme = "El misterioso relojero de la gran estacion central que descubrio un segundo secreto entre las horas"
        subgenre = "TEMPORAL_ANOMALY"
        visualStyleName = "Cartoon Moderno 2D"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 15
        genre = "SCI_FI"
        tone = "SUSPENSE"
        theme = "El radiotelescopio Big Ear y la intercepcion de la misteriosa senal Wow del espacio profundo en 1977"
        subgenre = "SETI_MYSTERY"
        visualStyleName = "Pixel Art 16-Bit Retro"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 16
        genre = "PSYCHOLOGICAL"
        tone = "UNSETTLING"
        theme = "La ilusion neurologica de la mano de goma y como el cerebro adopta objetos inanimados"
        subgenre = "NEUROLOGICAL_PARADOX"
        visualStyleName = "Collage y Papercraft"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 17
        genre = "STRANGE_EVENTS"
        tone = "EERIE"
        theme = "La extrana peste del baile de 1518 donde cientos de personas danzaron sin control en las calles"
        subgenre = "HISTORICAL_HYSTERIA"
        visualStyleName = "Dark Fantasy Tinta y Grabado"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 18
        genre = "DYSTOPIA"
        tone = "DARK"
        theme = "Una sociedad hipercapitalista donde los creditos digitales caducan y se evaporan si no se gastan en 24 horas"
        subgenre = "TECHNO_DYSTOPIA"
        visualStyleName = "Motion Graphics Geometrico"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 19
        genre = "MYSTERY"
        tone = "SUSPENSE"
        theme = "La desaparicion inexplicable de los tres guardianes del faro solitario de las islas Flannan en 1900"
        subgenre = "MARITIME_MYSTERY"
        visualStyleName = "Estilo Comic Novela Grafica"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 20
        genre = "SCI_FI"
        tone = "OMINOUS"
        theme = "La paradoja de Fermi y la teoria del bosque oscuro explicando por que el cosmos permanece en silencio"
        subgenre = "COSMIC_HORROR"
        visualStyleName = "Cel Shading Anime"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 21
        genre = "STRANGE_EVENTS"
        tone = "SUSPENSE"
        theme = "El enigma de la lluvia roja que cayo durante semanas sobre los bosques tropicales de Kerala en la India"
        subgenre = "METEOROLOGICAL_MYSTERY"
        visualStyleName = "Animacion 2D Ilustracion"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 22
        genre = "HYPOTHETICAL"
        tone = "CEREBRAL"
        theme = "Que consecuencias globales desencadenaria si todos los oceanos del mundo se volvieran agua dulce de golpe"
        subgenre = "ECOLOGICAL_SIMULATION"
        visualStyleName = "Infografia Cientifica Animada"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 23
        genre = "URBAN_LEGEND"
        tone = "SUSPENSE"
        theme = "La gigantesca ciudad subterranea de Derinkuyu descubierta detras de la pared de una casa en Capadocia"
        subgenre = "ANCIENT_MEGALITHS"
        visualStyleName = "Low-Poly 3D Estilizado"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 24
        genre = "HORROR"
        tone = "EERIE"
        theme = "La fabrica de munecos de porcelana y automatas de hojalata que siguen moviendose tras noventa anos de abandono"
        subgenre = "HAUNTED_RELICS"
        visualStyleName = "Cartoon Vintage Rubberhose"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 25
        genre = "PSYCHOLOGICAL"
        tone = "SUSPENSE"
        theme = "Un astronauta explorando los canones de Marte encuentra una replica exacta de la casa de su infancia"
        subgenre = "COSMIC_SOLITUDE"
        visualStyleName = "Claymation Stop-Motion"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 26
        genre = "EPISODIC"
        tone = "CEREBRAL"
        theme = "El asombroso teatro de Epidauro en Grecia donde una moneda al caer en el centro se escucha en la ultima fila"
        subgenre = "ANCIENT_ACOUSTICS"
        visualStyleName = "Low-Poly 3D Estilizado"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 27
        genre = "HYPOTHETICAL"
        tone = "CEREBRAL"
        theme = "La teoria del universo espejo simetrico donde el tiempo fluye en direccion contraria desde el Big Bang"
        subgenre = "QUANTUM_COSMOLOGY"
        visualStyleName = "Motion Graphics Geometrico"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 28
        genre = "STRANGE_EVENTS"
        tone = "SUSPENSE"
        theme = "Las piedras navegantes del Valle de la Muerte que se desplazan solas dejando surcos kilometricos"
        subgenre = "GEOLOGICAL_PHENOMENON"
        visualStyleName = "Collage y Papercraft"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 29
        genre = "DYSTOPIA"
        tone = "DARK"
        theme = "Un operador de radioaficionado en una megaciudad captura una transmision que anuncia las noticias diez segundos antes"
        subgenre = "CYBERPUNK_THRILLER"
        visualStyleName = "Estilo Comic Novela Grafica"
        sceneCount = 6
        targetDurationSeconds = 80
    },
    @{
        id = 30
        genre = "SCI_FI"
        tone = "SUSPENSE"
        theme = "El guardian cibernetico que defiende el ultimo arbol natural en una estacion espacial de titanio"
        subgenre = "SOLARPUNK_MECHA"
        visualStyleName = "Cel Shading Anime"
        sceneCount = 6
        targetDurationSeconds = 80
    }
)

# Cargar progreso si existe
$progress = @{}
if (Test-Path $progressFile) {
    try {
        $json = Get-Content $progressFile -Raw | ConvertFrom-Json
        $json.psobject.properties | ForEach-Object { $progress[$_.Name] = $_.Value }
    } catch {}
}

function Save-Progress {
    $progress | ConvertTo-Json -Depth 5 | Set-Content -Path $progressFile -Encoding utf8 -Force
}

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  BATCH RUNNER: GENERACION DE 30 VIDEOS CORTOS (60-90s)" -ForegroundColor Green
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  Total en cola  : $($stories.Count) videos" -ForegroundColor White
Write-Host "  Directorio Drive: $driveDir" -ForegroundColor White
Write-Host "============================================================`n" -ForegroundColor Cyan

$index = 0
foreach ($item in $stories) {
    $index++
    $storyNum = $item.id
    $itemKey = "story_$storyNum"

    # Verificar si ya se completo previamente
    if ($progress.ContainsKey($itemKey) -and $progress[$itemKey].status -eq "COMPLETED") {
        Write-Host "[$index/30] Video #$storyNum YA COMPLETADO (Saltando)." -ForegroundColor DarkGray
        continue
    }

    Write-Host "------------------------------------------------------------" -ForegroundColor Cyan
    Write-Host "[$index/30] PROCESANDO VIDEO #$storyNum" -ForegroundColor Yellow
    Write-Host "  Tema   : $($item.theme)" -ForegroundColor White
    Write-Host "  Estilo : $($item.visualStyleName)" -ForegroundColor Magenta
    Write-Host "  Genero : $($item.genre) | Escenas: $($item.sceneCount)" -ForegroundColor Gray
    Write-Host "------------------------------------------------------------" -ForegroundColor Cyan

    $body = @{
        genre = $item.genre
        tone = $item.tone
        theme = $item.theme
        subgenre = $item.subgenre
        targetDurationSeconds = $item.targetDurationSeconds
        language = "es-MX"
        sceneCount = $item.sceneCount
        visualStyleName = $item.visualStyleName
        ttsVoice = "es-MX-JorgeNeural"
    } | ConvertTo-Json -Compress

    $bytes = [System.Text.Encoding]::UTF8.GetBytes($body)

    $trackingId = $null
    try {
        $resp = Invoke-RestMethod -Uri "$apiUrl/pipeline/execute" `
            -Method Post `
            -ContentType "application/json; charset=utf-8" `
            -Body $bytes

        $trackingId = $resp.storyId
        Write-Host "  -> Pipeline encolado con Tracking ID: $trackingId" -ForegroundColor Gray
    } catch {
        Write-Warning "  [!] Error al iniciar pipeline para video #$storyNum"
        continue
    }

    # Polling de seguimiento
    $completed = $false
    $attempts = 0
    $maxAttempts = 150 # hasta 12 minutos por video
    $lastStage = ""

    while (-not $completed -and $attempts -lt $maxAttempts) {
        Start-Sleep -Seconds 5
        $attempts++

        try {
            $statusResp = Invoke-RestMethod -Uri "$apiUrl/pipeline/status/$trackingId" -Method Get
            $stage = [string]$statusResp.currentStage
            $pct = [int]$statusResp.progressPercent
            $status = [string]$statusResp.status

            if ($stage -ne $lastStage) {
                Write-Host "  [$pct%] Etapa: $stage - $($statusResp.message)" -ForegroundColor Cyan
                $lastStage = $stage
            }

            if ($status -eq "COMPLETED") {
                $completed = $true
                $videoFile = $statusResp.exportedVideoPath
                Write-Host "  [+] VIDEO #$storyNum COMPLETADO CON EXITO!" -ForegroundColor Green
                Write-Host "      Archivo: $videoFile" -ForegroundColor Green

                $progress[$itemKey] = @{
                    status = "COMPLETED"
                    storyId = $statusResp.storyId
                    trackingId = $trackingId
                    title = $item.theme
                    style = $item.visualStyleName
                    completedAt = (Get-Date).ToString("o")
                }
                Save-Progress
            } elseif ($status -eq "FAILED") {
                Write-Warning "  [!] El video #$storyNum fallo: $($statusResp.message)"
                $progress[$itemKey] = @{
                    status = "FAILED"
                    error = $statusResp.message
                    failedAt = (Get-Date).ToString("o")
                }
                Save-Progress
                if ($statusResp.message -match "429") {
                    Write-Host "  -> Rate limit detectado. Pausando 30 segundos para resetear ventana de cuota..." -ForegroundColor Yellow
                    Start-Sleep -Seconds 30
                }
                break
            }
        } catch {
            # Error temporal de red/polling
        }
    }

    if (-not $completed -and $attempts -ge $maxAttempts) {
        Write-Warning "  [!] Tiempo de espera agotado para video #$storyNum."
    }

    # Pausa de 5 segundos entre videos para dar respiro a GPU y cuota de IA
    Start-Sleep -Seconds 5
}

Write-Host "`n============================================================" -ForegroundColor Cyan
Write-Host "  LOTE DE 30 VIDEOS FINALIZADO" -ForegroundColor Green
Write-Host "  Verifica tu carpeta de Google Drive: $driveDir" -ForegroundColor White
Write-Host "============================================================" -ForegroundColor Cyan
