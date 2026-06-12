import { getUserIdFromCookie } from '../utils/cookie'
import { getDeviceType, getTimeStamp, getUtm } from '../utils/meta'

const GOOGLE_APPS_SCRIPT_URL = import.meta.env.VITE_GOOGLE_APPS_SCRIPT_URL?.trim() ?? ''
const VISITORS_SHEET_TABLE = 'visitors_ver2'
const FEEDBACK_SHEET_TABLE = 'beta_testers_ver2'

let clientIpPromise: Promise<string> | null = null

type ReportFeedbackInput = {
  email: string
  stockName: string
  reportId: number | null
  reportDate: string
  lossAvoidanceHelp: string
  mostUseful: string[]
  willingnessToPay: string
  expectedFeature: string
  comment: string
}

async function getClientIp() {
  if (!clientIpPromise) {
    clientIpPromise = fetch('https://jsonip.com?format=json')
      .then((response) => {
        if (!response.ok) {
          throw new Error(`${response.status} ${response.statusText}`)
        }
        return response.json() as Promise<{ ip?: string }>
      })
      .then((data) => data.ip ?? 'unknown')
      .catch((error) => {
        console.warn('IP 조회에 실패했습니다.', error)
        return 'unknown'
      })
  }

  return clientIpPromise
}

async function insertGoogleSheetRow(table: string, payload: Record<string, unknown>) {
  if (!GOOGLE_APPS_SCRIPT_URL) {
    console.log(`${table} payload`, payload)
    return
  }

  const url = `${GOOGLE_APPS_SCRIPT_URL}?action=insert&table=${table}&data=${encodeURIComponent(
    JSON.stringify(payload),
  )}`

  const response = await fetch(url)

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`)
  }
}

export async function submitVisitorLog() {
  const payload = {
    id: getUserIdFromCookie(),
    landingUrl: window.location.href,
    ip: await getClientIp(),
    referer: document.referrer,
    time_stamp: getTimeStamp(),
    utm: getUtm(),
    device: getDeviceType(),
  }

  await insertGoogleSheetRow(VISITORS_SHEET_TABLE, payload)
}

export async function submitReportFeedback(input: ReportFeedbackInput) {
  const payload = {
    id: getUserIdFromCookie(),
    email: input.email,
    stockName: input.stockName,
    reportId: input.reportId,
    reportDate: input.reportDate,
    lossAvoidanceHelp: input.lossAvoidanceHelp,
    mostUseful: input.mostUseful.join(', '),
    willingnessToPay: input.willingnessToPay,
    expectedFeature: input.expectedFeature,
    comment: input.comment,
    landingUrl: window.location.href,
    ip: await getClientIp(),
    utm: getUtm(),
    device: getDeviceType(),
    userAgent: navigator.userAgent,
    time_stamp: getTimeStamp(),
  }

  await insertGoogleSheetRow(FEEDBACK_SHEET_TABLE, payload)
}
