# Recruitment Landing — Design QA

visual source of truth: the existing Home page and its shared `--oa-*` design tokens
route: `/recruitment`
desktop viewport checked: 1280 × 720

## Current visual direction

- Recruitment now uses Home's neutral palette: `--oa-page-bg`, `--oa-page-soft-bg`, `--oa-text`, `--oa-muted`, `--oa-border`, and the shared active button colors.
- The former campus/project/community bitmap scenes are no longer rendered.
- Hero now reuses `HomeMapSection` in static background mode, matching Home directly without automatic earth rotation.
- Project and community chapters use restrained solid surfaces instead of generated imagery or decorative scene art.
- Department, project, gain, and process content is organized with whitespace and editorial dividers instead of floating blue cards.

## Interaction checks

- GSAP ScrollTrigger remains active.
- Hero remains pinned and scrubbed, with scale, fade, and geometric parallax.
- All five departments remain separate full-screen semantic sections.
- Department content enters automatically while scrolling; there is no route-selection click.
- The project scene remains pinned and scrubbed.
- Community geometry keeps its horizontal parallax.
- Primary CTAs remain connected to `/apply`.
- Every department still includes the “不会写代码也能参与” path.
- Every department now explains who the direction suits and which capabilities members can build.
- The fixed chapter dial uses page progress and current-stage labels instead of freeform viewport movement.
- `prefers-reduced-motion` fallback remains intact.

## Verification

- `vue-tsc --noEmit`: passed.
- `vite build`: passed.
- Browser console warnings/errors on `/recruitment`: none.
- Desktop visual check: passed for the loaded static map hero, department scene, project transition, and chapter dial.

final result: passed
