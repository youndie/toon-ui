# docs — toon-ui

toon-ui is a Compose Multiplatform UI kit in the look of 1940s–50s theatrical cartoons: ink outline
of variable weight, flat cel fills, hard shadows, and motion that squashes and lands in dust. The
documentation is layered, and links run top to bottom.

```
[ Research (why the architecture is what it is) ]
                     │
[ Service (the toon-core module, once it exists) ]
```

| Layer | Directory | Answers | Source of truth |
|---|---|---|---|
| Research | `research/` | *why* it is built this way; what is verified, what is a hypothesis | the artefacts each fact names, first of all `reference/design-system/` |
| Service | `services/` | what the library module owns, its targets, how it is built and verified | this repository |

There is no `features/`, `screens/` or `api/` layer. A component library has no end-user feature
flow or HTTP contract. Its "screens" are the design project's spec pages, vendored in
`reference/design-system/` and measured against rather than re-described here. `services/` is
empty until B-01 creates `toon-core`: `main` describes what exists.

**Backlog** — [backlog.md](../backlog.md): the order of work and the stages. The items themselves
are one file each in [`backlog/`](backlog/), cited as `[B-12](backlog/B-12-icon-button-and-badge.md)`.

## Conventions

- **`id`** in the frontmatter is unique and equals the filename.
- Cross-layer links are ids in the frontmatter and ordinary markdown links in the body.
- **The primary consumer is a coding agent.** Every document carries code anchors, paths to the
  file or directory, so the reader reaches the code in one hop. A value that lives in the engine or
  in code is cited by path, not copied.
- A file in a sibling repository is cited as `youndie/<repo>@<commit>!/<path>`.
- Language: **English** (research D10). The vendored brief is in Russian and stays so. Code
  identifiers verbatim.

## Templates

`templates/` holds a copy of the document templates, so the format travels with the repository.

## Checks

The Makefile runs docs-bootstrap's checks at the version `.github/workflows/check.yaml` pins, and CI
runs the same targets:

```bash
pip install pyyaml
make check      # the gate, then the reports: what CI runs
make fix        # regenerate the backlog index, append missing coverage-map lines
```

## Coverage map

The list below is **checked** against the files on disk: a document missing here, or an entry with
no file behind it, fails the coverage check.

### Research (1)

- [x] [research-architecture](research/research-architecture.md) — what was imported and verified, the decisions, the open hypotheses
