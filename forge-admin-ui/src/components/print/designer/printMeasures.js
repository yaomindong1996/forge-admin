/** Preset millimetre values for print designer selects. Current custom sizes are appended. */
export const PRINT_MM_PRESETS = {
  border: [0, 0.15, 0.25, 0.35, 0.5, 0.75, 1, 1.5, 2, 3],
  radius: [0, 0.5, 1, 2, 3, 4, 6, 8, 10, 12, 16, 20],
  padding: [0, 0.5, 1, 1.5, 2, 3, 4, 5, 6, 8, 10, 12, 16, 20],
  track: [8, 10, 12, 14, 16, 18, 20, 22, 24, 28, 30, 35, 40, 45, 50, 60, 80, 100],
  paper: [80, 100, 105, 110, 140, 148, 176, 182, 210, 216, 241, 250, 257, 297, 353, 420, 594, 841],
  margin: [0, 5, 8, 10, 12, 15, 18, 20, 25, 30],
  band: [0, 8, 10, 12, 15, 18, 20, 25, 30, 40, 50],
  gap: [0, 1, 2, 3, 4, 5, 8, 10, 12, 15, 20],
  position: [0, 2, 3, 5, 8, 10, 12, 15, 20, 25, 30, 40, 50, 60, 80, 90, 100, 120, 150, 180],
  size: [0.5, 1, 2, 3, 4, 5, 8, 10, 12, 14, 16, 18, 20, 24, 28, 30, 36, 40, 45, 50, 60, 80, 90, 100, 120, 150, 180],
}

export function printMmOptions(current, presets = PRINT_MM_PRESETS.border) {
  const sizes = [...presets]
  const n = Number(current)
  if (Number.isFinite(n) && !sizes.includes(n))
    sizes.push(n)
  sizes.sort((a, b) => a - b)
  return sizes.map(value => ({ label: `${value} mm`, value }))
}
