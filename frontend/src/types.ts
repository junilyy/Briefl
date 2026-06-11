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

export type ReportReferencedNews = {
  title: string
  url: string
  source: string
  publishedAt: string
  newsType: string
}

export type ReportSentimentAnalysis = {
  sentiment: string
  summary: string
  keyPoints: string[]
  relatedNewsTitles: string[]
}

export type ReportCheckEvent = {
  date: string | null
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
  referencedNews: ReportReferencedNews[]
  sentimentAnalyses: ReportSentimentAnalysis[]
  checkEvents: ReportCheckEvent[]
  caution: string
}
