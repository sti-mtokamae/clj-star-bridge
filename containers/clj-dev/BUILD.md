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

## Shell

```powershell
wslc run -v C:\dev\clj-star-bridge:/workspace -p 8080:8080 -it clj-star-bridge-dev:latest bash
```

Inside the container:

```bash
clj -M -m clj-star-bridge.core
```

Open:

```text
http://localhost:8080/
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

## Verified

Verified on 2026-09-11:

- `wslc build` creates `clj-star-bridge-dev:latest`.
- `wslc run` starts an interactive shell with Java, Clojure CLI, Node, and npm available.
- `clj -M -m clj-star-bridge.core` starts the Aleph server on port 8080.
- `http://localhost:8080/` renders in the Codex web preview.
- `POST /api/notify` broadcasts a message to the preview over SSE.

## Notes

- The repository is mounted at `/workspace`.
- Java, Clojure CLI, Node, and npm are provided by the Nix dev shell.
- Node/npm are included for future Datastar experiments, but the current Phase 1 app does not require a JavaScript build.
