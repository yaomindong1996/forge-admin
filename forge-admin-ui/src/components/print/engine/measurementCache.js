/** Session-local bounded cache; a new session follows every font/resource preparation. */
export class MeasurementCache {
  constructor(limit = 1000) {
    this.limit = limit
    this.entries = new Map()
  }

  get(key) {
    return this.entries.get(JSON.stringify(key))
  }

  set(key, value) {
    const encoded = JSON.stringify(key)
    this.entries.delete(encoded)
    this.entries.set(encoded, value)
    if (this.entries.size > this.limit) {
      this.entries.delete(this.entries.keys().next().value)
    }
    return value
  }

  clear() {
    this.entries.clear()
  }
}
