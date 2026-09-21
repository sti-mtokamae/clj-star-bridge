# clj-star-bridge clj-dev image

This image provides a WSL Container based development shell for `clj-star-bridge`.

## Build

Run from the repository root:

```powershell
cd C:\dev\clj-star-bridge
wslc build -t clj-star-bridge-dev:latest -f containers/clj-dev/Dockerfile .
```

Check the image:

```powershell
wslc image list
```

## Interactive Shell

```powershell
wslc run --name clj-star-bridge-dev `
  -v C:\dev\clj-star-bridge:/workspace `
  -p 8080:8080 `
  -it clj-star-bridge-dev:latest bash
```

This mounts the Windows checkout at `/workspace`. Edits made in Windows, VS Code, or Codex are visible inside the container immediately.

Inside the container shell:

```bash
pwd
ls
clj --version
node --version
npm --version
```

## Run the App

From the container shell:

```bash
clj -M -m clj-star-bridge.core
```

Open:

```text
http://localhost:8080/
```

The server keeps this shell busy. Keep this terminal as the server terminal.

From another PowerShell terminal, attach a second interactive shell to the same running container:

```powershell
wslc list
wslc exec -it clj-star-bridge-dev bash
```

When entering with `wslc exec`, start the Nix dev shell before using `clj`:

```bash
nix develop /opt/clj-star-bridge-dev --command bash -i
```

Use the second dev shell for `clj`, `curl`, file inspection, or other experiments while the server stays up. Stop the server with `Ctrl+C` in the server terminal.

## REPL Workflow

For interactive experiments, start a plain Clojure REPL inside the container:

```bash
clj
```

Then load the app namespace and start the server from the REPL:

```clojure
(require '[clj-star-bridge.core :as app])
(def server (app/-main))
```

After editing source files from Windows or VS Code, reload the namespace and restart the server:

```clojure
(.close server)
(require '[clj-star-bridge.core :as app] :reload)
(def server (app/-main))
```

Exit the REPL with:

```clojure
(System/exit 0)
```

## Smoke Checks

From another terminal:

```powershell
curl.exe -X POST http://localhost:8080/api/notify `
  -H "Content-Type: application/json" `
  --data-raw '{"message":"hello from wslc"}'
```

For the SSE stream:

```powershell
curl.exe -N http://localhost:8080/events
```

For the Datastar increment response:

```powershell
curl.exe -N http://localhost:8080/increment `
  -H "Datastar-Request: true"
```

## Verified

Verified on 2026-09-11:

- `wslc build` creates `clj-star-bridge-dev:latest`.
- `wslc run` starts an interactive shell with Java, Clojure CLI, Node, and npm available.
- `clj -M -m clj-star-bridge.core` starts the Aleph server on port 8080.
- `http://localhost:8080/` renders in the Codex web preview.
- `POST /api/notify` broadcasts a message to the preview over SSE.

Additional verification on 2026-09-20:

- The Datastar increment response patches the server-rendered `#counter-panel` fragment.
- The replacement panel keeps its `+1` interaction and supports consecutive updates.

Additional verification on 2026-09-22:

- After `wslc start` and `wslc exec`, entering `nix develop /opt/clj-star-bridge-dev --command bash -i` restores the Clojure dev tools.
- `clj -M -m clj-star-bridge.core` starts the app from `/workspace`.
- The updated web page runs on `http://localhost:8080/` and accepts SSE clients.
- A Datastar increment response sends multiple `patch-elements!` events in one response for `#counter-panel` and `#activity-status`.
- Browser clicks update both `Count: N` and `Count updated to N`, while the `/events` JSON SSE notification log continues receiving count updates.

## Notes

- The repository is mounted at `/workspace`.
- Java, Clojure CLI, Node, and npm are provided by the Nix dev shell.
- Datastar is loaded from a CDN, so the current app does not require a JavaScript build. Node/npm remain available for future experiments.
