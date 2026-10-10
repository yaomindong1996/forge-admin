export class BuildError extends Error {
  constructor(code) {
    super(code)
    this.code = code
  }
}

export function ensure(condition, code) {
  if (!condition) {
    throw new BuildError(code)
  }
}

export function errorCode(error) {
  return error instanceof BuildError ? error.code : 'PREFLIGHT_FAILED'
}
