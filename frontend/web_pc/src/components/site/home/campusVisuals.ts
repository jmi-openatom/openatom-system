import type { ResolvedTheme } from '@/composables/useTheme'

// The regional DEM supplies the surrounding landform, not surveyed campus detail.
export const TERRAIN_EXAGGERATION = 1.15
export const ARCHITECTURE_MIN_ZOOM = 15.2
export const SUN_DIRECTION: [number, number] = [315, 42]

export function campusLighting(theme: ResolvedTheme) {
  const dark = theme === 'dark'
  return {
    ambient: dark ? '#b4c8dd' : '#f1f6fa',
    ambientIntensity: dark ? 0.48 : 0.58,
    directional: dark ? '#bcd2eb' : '#fff8ea',
    directionalIntensity: dark ? 0.36 : 0.52,
    shadowIntensity: dark ? 0.2 : 0.28,
  }
}
