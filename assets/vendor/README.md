# Vendored files

Third-party browser files the site serves itself, so visitors make no
third-party requests (CLAUDE.md § Privacy). This README is copied into
`public/assets/vendor/` with them and is harmless there.

## scittle.js (fetched at build time, not committed)

`bb build` puts `scittle.js` and `scittle-LICENSE.txt` here in `public/`
(see `src/nooks/vendor.clj`). It downloads the pinned npm tarball once into
`.work/vendor/` (git-ignored), checks the tarball's sha512 and the file's
SHA-256, and fails the build on a mismatch. Scittle isn't committed: a
workstation commit hook reads constants in its minified code as personal data
(false positives), and fetching avoids an exemption file in this public repo.

| | |
|---|---|
| What | [Scittle](https://github.com/babashka/scittle) by borkdude: the SCI Clojure interpreter for the browser. Runs the Workbench, inside a Web Worker. |
| Version | 0.8.33 (npm `latest` on 2026-11-24; the prototype used the same version from jsDelivr) |
| Source | `https://registry.npmjs.org/scittle/-/scittle-0.8.33.tgz`, file `package/dist/scittle.js` |
| Tarball integrity | `sha512-XeED9aRtVi0uHStoYn8hBO/dYEMfY7W4BIZkuj1J6MSxeGslfHVlar/hhCkYvA7y1X3ISRg/XqmFoz6pEYUUSQ==` (the registry's `dist.integrity`) |
| File SHA-256 | `e04a33ff90947057772f42e683314e9c9f8671ebb922398208b4ad1c76991373` |
| Licence | EPL-1.0, `package/LICENSE` from the same tarball, served as `scittle-LICENSE.txt` |

To upgrade: change `nooks.vendor/scittle` (version, tarball URL, the
registry's `dist.integrity` for that version, and the new file's SHA-256),
update this table, then run `bb test:browser` (it runs every Workbench starter
recipe in Chrome).
