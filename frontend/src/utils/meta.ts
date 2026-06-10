export function getDeviceType() {
  return /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(
    navigator.userAgent,
  )
    ? 'mobile'
    : 'desktop'
}

export function getUtm() {
  return new URLSearchParams(window.location.search).get('utm') ?? ''
}

function padValue(value: number) {
  return value < 10 ? `0${value}` : `${value}`
}

export function getTimeStamp() {
  const date = new Date()
  const year = date.getFullYear()
  const month = date.getMonth() + 1
  const day = date.getDate()
  const hours = date.getHours()
  const minutes = date.getMinutes()
  const seconds = date.getSeconds()

  return `${year}-${padValue(month)}-${padValue(day)} ${padValue(hours)}:${padValue(
    minutes,
  )}:${padValue(seconds)}`
}
