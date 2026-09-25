# clj-star-bridge clj-dev image

This image provides a WSL Container based development shell for
`clj-star-bridge` and its Shadow-CLJS React component project.

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

## Windows Checkouts

Keep both repositories on the Windows filesystem. The examples below expect:

```text
C:\dev\clj-star-bridge
C:\dev\clj-react-hack
```

Clone the React project from PowerShell if it is not present yet:

```powershell
cd C:\dev
git clone https://github.com/sti-mtokamae/clj-react-hack.git
```

Do not use the checkout under Ubuntu WSL for this workflow. WSLc mounts the
Windows checkouts into one development container.

## Interactive Shell

```powershell
wslc run --name clj-star-bridge-dev `
  -v C:\dev\clj-star-bridge:/workspace `
  -v C:\dev\clj-react-hack:/workspace-react `
  -p 8080:8080 `
  -p 3000:3000 `
  -p 9630:9630 `
  -it clj-star-bridge-dev:latest bash
```

This mounts the backend at `/workspace` and the React project at
`/workspace-react`. Edits made in Windows, VS Code, or Codex are visible inside
the container immediately.

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

## Run Shadow-CLJS

Use the second container shell for the React development asset server:

```bash
cd /workspace-react
npm ci
npm run build:css
npx shadow-cljs watch app \
  --config-merge /workspace/containers/clj-dev/shadow-cljs-wslc.edn
```

The override is kept in `clj-star-bridge`, so the `clj-react-hack`
configuration does not need a container-specific edit. It makes the generated
development module URLs absolute, exposes the asset server outside the
container, and directs hot reload to the published Shadow-CLJS server.

After both processes are running, open:

```text
http://localhost:8080/
```

The page itself comes from Clojure/Hiccup on port 8080. The React CSS and
JavaScript come from Shadow-CLJS on port 3000, and hot reload uses port 9630.

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

For the Datastar signals response:

```powershell
curl.exe -N -X POST http://localhost:8080/greet `
  -H "Datastar-Request: true" `
  -H "Content-Type: application/json" `
  --data-raw '{"name":"  Ada  "}'
```

For the long-lived Datastar live status stream:

```powershell
curl.exe -N http://localhost:8080/live-status `
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
- A Datastar POST sends the bound `name` signal to `/greet`; the server trims it and returns both a greeting element patch and a normalized signal patch.
- Entering `  Ada  ` updates the input to `Ada` and the greeting to `Hello, Ada!` in the browser.

Additional verification on 2026-09-23:

- `/live-status` keeps a Datastar SDK SSE response open and sends repeated `patch-elements!` events for `#live-status`.
- The live status stream updates the server timestamp and update count once per second.
- The browser automatically opens the stream through `data-init` and continuously updates the `Live Status` section.
- Closing the curl client invokes the adapter's `on-close` callback and stops the virtual-thread worker.
- The existing Datastar `/increment` response still sends both `#counter-panel` and `#activity-status` patches.

Additional verification on 2026-09-25:

- The image runs with the Windows checkouts mounted at `/workspace` and
  `/workspace-react` in one WSLc container.
- Aleph serves the Hiccup page, Datastar actions, and SSE streams on port 8080.
- Shadow-CLJS 3.1.8 serves the React development bundle on port 3000 and its
  hot-reload endpoint on port 9630.
- Windows host requests return HTTP 200 for the Hiccup page, React JavaScript,
  generated CSS, and the Shadow-CLJS endpoint.
- The React island mounts inside the Hiccup page. Its local counter and the
  Datastar server counter update independently while the live status stream
  continues updating.

## Notes

- The repository is mounted at `/workspace`.
- The React repository is mounted at `/workspace-react`.
- Java, Clojure CLI, Node, and npm are provided by the Nix dev shell.
- Shadow-CLJS is resolved from `clj-react-hack`'s lockfile with `npm ci`; it is
  not installed globally in the image.
- Datastar remains loaded from its CDN. Only the React component requires the
  local JavaScript build.
