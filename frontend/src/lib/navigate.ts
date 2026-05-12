let navigateFn: ((path: string) => void) | null = null

export const setNavigate = (fn: (path: string) => void): void => {
  navigateFn = fn
}

export const navigateTo = (path: string): void => {
  if (navigateFn) {
    navigateFn(path)
  } else {
    window.location.href = path
  }
}
