export type Stock = {
  stockName: string
  displayName: string
  market: string
}

export type ReportPriceImpact = {
  direction: string
  confidence: string
  reason: string
}

export type ReportCounts = {
  positive: number
  neutral: number
  negative: number
}

export type ReportNewsItem = {
  title: string
  url: string
  source: string
  publishedAt: string
  sentiment: string
  sentimentScore: number
  relevance: number
  importance: number
  recency: number
  impactScore: number
  reason: string
}

export type ReportIndirectNewsItem = ReportNewsItem & {
  relatedFactor: string
}

export type ReportCheckEvent = {
  date: string
  event: string
  whyImportant: string
}

export type Report = {
  reportId: number | null
  stockName: string
  reportDate: string
  briefSummary: string
  overallSentiment: string
  newsImpactScore: number
  priceImpact: ReportPriceImpact
  counts: ReportCounts
  directNews: ReportNewsItem[]
  indirectNews: ReportIndirectNewsItem[]
  checkEvents: ReportCheckEvent[]
  caution: string
}
