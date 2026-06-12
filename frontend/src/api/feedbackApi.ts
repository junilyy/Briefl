import { getUserIdFromCookie } from '../utils/cookie'
import { getDeviceType, getTimeStamp, getUtm } from '../utils/meta'

const GOOGLE_APPS_SCRIPT_URL = ''
const FEEDBACK_SHEET_TABLE = 'briefl_report_feedback'

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

export async function submitReportFeedback(input: ReportFeedbackInput) {
  // Google Sheet columns mirror these payload keys for the MVP feedback table.
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
    utm: getUtm(),
    device: getDeviceType(),
    userAgent: navigator.userAgent,
    time_stamp: getTimeStamp(),
  }

  if (!GOOGLE_APPS_SCRIPT_URL) {
    console.log('briefl_report_feedback payload', payload)
    return
  }

  const url = `${GOOGLE_APPS_SCRIPT_URL}?action=insert&table=${FEEDBACK_SHEET_TABLE}&data=${encodeURIComponent(
    JSON.stringify(payload),
  )}`

  const response = await fetch(url)

  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}`)
  }
}
