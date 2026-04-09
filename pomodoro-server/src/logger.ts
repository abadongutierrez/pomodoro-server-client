function timestamp(): string {
  return new Date().toISOString()
}

export function log(tag: string, message: string, data?: unknown): void {
  const base = `[${timestamp()}] [${tag}] ${message}`
  if (data !== undefined) {
    console.log(base, JSON.stringify(data))
  } else {
    console.log(base)
  }
}
