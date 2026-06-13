# barriereblick
Beschreibung des Repositorys

## CI / Branch Protection

Die CI ist ein reines Sicherheitsnetz vor `main`. `main` ist nur mit grüner CI
mergebar. Die Required-Status-Check-Einstellung selbst nimmt Lukas im GitHub-UI
vor (Settings → Branches → Branch protection rules).

Die Jobs sind pfad-gefiltert: Es bauen und testen nur die Module, die sich
tatsächlich geändert haben (`api`, `web`, `worker`). Änderungen, die nur
Root- oder `ai/`-Dateien betreffen, lösen keinen Modul-Job aus.

Hinweis zum Ein-Workflow-Modell: Übersprungene Jobs zählen für GitHub als grün.
Als Required Checks daher die Job-Namen `api`, `web` und `worker` eintragen –
so blockiert ein fehlschlagender Modul-Job den Merge, während übersprungene
Module den Merge nicht unnötig aufhalten.

Stolperfalle beim Einrichten: In der klassischen Branch Protection können
Required Checks, die für einen PR gar nicht starten (weil der Pfad nicht
betroffen ist), als „Expected – Waiting" hängen bleiben und den Merge doch
blockieren. Falls das auftritt, ein Repository-**Ruleset** (Settings → Rules)
statt der klassischen Branch-Protection-Regel verwenden – Rulesets behandeln
nicht gestartete Checks als erfüllt.
